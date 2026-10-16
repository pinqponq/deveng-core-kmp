package core.domain.camera.analysis.nanodet.pipeline

import core.domain.camera.analysis.nanodet.NanoDetConfig
import core.domain.camera.analysis.nanodet.model.NanoDetModelContract
import core.domain.camera.analysis.nanodet.model.NanoDetModelProfile
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

internal class NanoDetPostprocessor(
    private val config: NanoDetConfig,
    private val inputWidth: Int,
    private val inputHeight: Int
) {
    private val predictionCount = NanoDetModelProfile.calculatePredictionCount(inputWidth, inputHeight)
    private val priors = createCenterPriors()

    fun decode(predictions: FloatBuffer): PlateDetectionResult? {
        var bestDetection: PlateDetectionResult? = null
        var maxConfidence = config.confidenceThreshold

        for (index in 0 until predictionCount) {
            val predictionOffset = index * NanoDetModelContract.OUTPUT_CHANNELS
            val confidence = predictions.get(predictionOffset)
            if (confidence < maxConfidence) continue

            val prior = priors[index]
            val leftDistance = distributionToDistance(predictions, predictionOffset, side = 0) * prior.stride
            val topDistance = distributionToDistance(predictions, predictionOffset, side = 1) * prior.stride
            val rightDistance = distributionToDistance(predictions, predictionOffset, side = 2) * prior.stride
            val bottomDistance = distributionToDistance(predictions, predictionOffset, side = 3) * prior.stride

            bestDetection = PlateDetectionResult(
                confidence = confidence,
                boundingBox = PlateBoundingBox(
                    left = max(0f, prior.x - leftDistance),
                    top = max(0f, prior.y - topDistance),
                    right = min(inputWidth - 1f, prior.x + rightDistance),
                    bottom = min(inputHeight - 1f, prior.y + bottomDistance)
                )
            )

            maxConfidence = confidence
        }

        return bestDetection
    }

    private fun distributionToDistance(
        predictions: FloatBuffer,
        predictionOffset: Int,
        side: Int
    ): Float {
        val regressionChannelsPerSide = NanoDetModelContract.REG_MAX + 1
        val offset = predictionOffset + 1 + side * regressionChannelsPerSide
        var maxChannelValue = predictions.get(offset)

        for (index in 1 until regressionChannelsPerSide) {
            maxChannelValue = max(maxChannelValue, predictions.get(offset + index))
        }

        var sum = 0.0
        var weightedSum = 0.0

        for (index in 0 until regressionChannelsPerSide) {
            val probability = exp((predictions.get(offset + index) - maxChannelValue).toDouble())
            sum += probability
            weightedSum += probability * index
        }

        return (weightedSum / sum).toFloat()
    }

    internal fun distributionToDistance(prediction: FloatArray, offset: Int): Float {
        val binCount = NanoDetModelContract.REG_MAX + 1
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
            val featureWidth = ceil(inputWidth.toDouble() / stride).toInt()
            val featureHeight = ceil(inputHeight.toDouble() / stride).toInt()
            for (y in 0 until featureHeight) {
                for (x in 0 until featureWidth) {
                    add(
                        CenterPrior(
                            x = x * stride.toFloat(),
                            y = y * stride.toFloat(),
                            stride = stride.toFloat()
                        )
                    )
                }
            }
        }
    }

    internal data class CenterPrior(
        val x: Float,
        val y: Float,
        val stride: Float
    )
}