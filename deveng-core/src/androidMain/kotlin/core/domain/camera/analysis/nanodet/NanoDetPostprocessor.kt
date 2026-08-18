package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.onnx.OnnxModelErrorCode
import core.domain.camera.analysis.onnx.OnnxModelException
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

internal class NanoDetPostprocessor(
    private val config: NanoDetConfig,
) {
    private val priors = createCenterPriors()

    fun decode(predictions: Array<FloatArray>): List<PlateDetection> {
        if (predictions.size != NanoDetModelContract.PREDICTION_COUNT) {
            invalidOutput(
                "Prediction count ${predictions.size} does not match " +
                    "${NanoDetModelContract.PREDICTION_COUNT}.",
            )
        }
        predictions.firstOrNull { it.size != NanoDetModelContract.OUTPUT_CHANNELS }?.let {
            invalidOutput(
                "Prediction channel count ${it.size} does not match " +
                    "${NanoDetModelContract.OUTPUT_CHANNELS}.",
            )
        }

        return decodeValues { prediction, channel -> predictions[prediction][channel] }
    }

    fun decode(predictions: FloatBuffer): List<PlateDetection> {
        val expectedValues = NanoDetModelContract.PREDICTION_COUNT *
            NanoDetModelContract.OUTPUT_CHANNELS
        if (predictions.capacity() != expectedValues) {
            invalidOutput(
                "Prediction value count ${predictions.capacity()} does not match $expectedValues.",
            )
        }
        return decodeValues { prediction, channel ->
            predictions.get(prediction * NanoDetModelContract.OUTPUT_CHANNELS + channel)
        }
    }

    private inline fun decodeValues(valueAt: (prediction: Int, channel: Int) -> Float): List<PlateDetection> {
        val candidates = mutableListOf<PlateDetection>()
        for (index in 0 until NanoDetModelContract.PREDICTION_COUNT) {
            val confidence = valueAt(index, 0)
            if (confidence < config.confidenceThreshold) continue

            val prior = priors[index]
            val leftDistance = distributionToDistance(index, side = 0, valueAt) * prior.stride
            val topDistance = distributionToDistance(index, side = 1, valueAt) * prior.stride
            val rightDistance = distributionToDistance(index, side = 2, valueAt) * prior.stride
            val bottomDistance = distributionToDistance(index, side = 3, valueAt) * prior.stride
            candidates += PlateDetection(
                confidence = confidence,
                boundingBox = DetectorBoundingBox(
                    left = max(0f, prior.x - leftDistance),
                    top = max(0f, prior.y - topDistance),
                    right = min(NanoDetModelContract.INPUT_SIZE - 1f, prior.x + rightDistance),
                    bottom = min(NanoDetModelContract.INPUT_SIZE - 1f, prior.y + bottomDistance),
                ),
            )
        }

        return nms(candidates)
    }

    private inline fun distributionToDistance(
        prediction: Int,
        side: Int,
        valueAt: (prediction: Int, channel: Int) -> Float,
    ): Float {
        val binCount = NanoDetModelContract.REG_MAX + 1
        val offset = 1 + side * binCount
        var maxValue = valueAt(prediction, offset)
        for (index in 1 until binCount) {
            maxValue = max(maxValue, valueAt(prediction, offset + index))
        }
        var sum = 0.0
        var weightedSum = 0.0
        for (index in 0 until binCount) {
            val probability = exp((valueAt(prediction, offset + index) - maxValue).toDouble())
            sum += probability
            weightedSum += probability * index
        }
        return (weightedSum / sum).toFloat()
    }

    internal fun distributionToDistance(prediction: FloatArray, offset: Int): Float {
        val binCount = NanoDetModelContract.REG_MAX + 1
        require(offset >= 0 && offset + binCount <= prediction.size) {
            "DFL distribution is outside the prediction array."
        }
        var maxValue = prediction[offset]
        for (index in 1 until binCount) {
            maxValue = max(maxValue, prediction[offset + index])
        }
        var sum = 0.0
        var weightedSum = 0.0
        for (index in 0 until binCount) {
            val probability = exp((prediction[offset + index] - maxValue).toDouble())
            sum += probability
            weightedSum += probability * index
        }
        return (weightedSum / sum).toFloat()
    }

    internal fun createCenterPriors(): List<CenterPrior> = buildList {
        NanoDetModelContract.strides.forEach { stride ->
            val featureSize = ceil(NanoDetModelContract.INPUT_SIZE.toDouble() / stride).toInt()
            for (y in 0 until featureSize) {
                for (x in 0 until featureSize) {
                    add(
                        CenterPrior(
                            x = x * stride.toFloat(),
                            y = y * stride.toFloat(),
                            stride = stride.toFloat(),
                        ),
                    )
                }
            }
        }
    }

    internal fun nms(detections: List<PlateDetection>): List<PlateDetection> {
        val remaining = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<PlateDetection>()
        while (remaining.isNotEmpty()) {
            val best = remaining.removeAt(0)
            selected += best
            remaining.removeAll { intersectionOverUnion(best.boundingBox, it.boundingBox) > config.nmsIouThreshold }
        }
        return selected
    }

    internal fun intersectionOverUnion(a: DetectorBoundingBox, b: DetectorBoundingBox): Float {
        val intersectionWidth = max(0f, min(a.right, b.right) - max(a.left, b.left))
        val intersectionHeight = max(0f, min(a.bottom, b.bottom) - max(a.top, b.top))
        val intersection = intersectionWidth * intersectionHeight
        val areaA = max(0f, a.right - a.left) * max(0f, a.bottom - a.top)
        val areaB = max(0f, b.right - b.left) * max(0f, b.bottom - b.top)
        val union = areaA + areaB - intersection
        return if (union > 0f) intersection / union else 0f
    }

    private fun invalidOutput(message: String): Nothing = throw OnnxModelException(
        code = OnnxModelErrorCode.INVALID_MODEL_OUTPUT,
        message = message,
    )
}

internal data class CenterPrior(
    val x: Float,
    val y: Float,
    val stride: Float,
)
