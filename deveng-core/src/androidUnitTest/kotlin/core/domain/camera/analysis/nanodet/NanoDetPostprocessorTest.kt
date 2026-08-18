package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.onnx.OnnxModelErrorCode
import core.domain.camera.analysis.onnx.OnnxModelException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NanoDetPostprocessorTest {
    private val postprocessor = NanoDetPostprocessor(NanoDetConfig())

    @Test
    fun centerPriorsMatchTheVerifiedOutputLayout() {
        val priors = postprocessor.createCenterPriors()

        assertEquals(NanoDetModelContract.PREDICTION_COUNT, priors.size)
        assertEquals(CenterPrior(0f, 0f, 8f), priors.first())
        assertEquals(CenterPrior(384f, 384f, 64f), priors.last())
    }

    @Test
    fun dflSoftmaxReturnsExpectedBinForDominantLogit() {
        val prediction = FloatArray(NanoDetModelContract.OUTPUT_CHANNELS)
        prediction[1 + 5] = 20f

        val distance = postprocessor.distributionToDistance(prediction, offset = 1)

        assertEquals(5f, distance, absoluteTolerance = 0.0001f)
    }

    @Test
    fun predictionIsDecodedAndClampedToDetectorCoordinates() {
        val predictions = emptyPredictions()
        predictions[0][0] = 0.90f
        repeat(4) { side ->
            predictions[0][1 + side * 8 + 1] = 20f
        }

        val detections = postprocessor.decode(predictions)

        assertEquals(1, detections.size)
        assertEquals(0.90f, detections.single().confidence)
        assertEquals(0f, detections.single().boundingBox.left)
        assertEquals(0f, detections.single().boundingBox.top)
        assertEquals(8f, detections.single().boundingBox.right, absoluteTolerance = 0.001f)
        assertEquals(8f, detections.single().boundingBox.bottom, absoluteTolerance = 0.001f)
    }

    @Test
    fun flatDirectOutputBufferMatchesNestedArrayDecode() {
        val predictions = emptyPredictions()
        predictions[0][0] = 0.90f
        repeat(4) { side -> predictions[0][1 + side * 8 + 1] = 20f }
        val flat = ByteBuffer
            .allocateDirect(NanoDetModelContract.OUTPUT_VALUE_COUNT * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        predictions.forEachIndexed { prediction, values ->
            values.forEachIndexed { channel, value ->
                flat.put(prediction * NanoDetModelContract.OUTPUT_CHANNELS + channel, value)
            }
        }

        assertEquals(postprocessor.decode(predictions), postprocessor.decode(flat))
    }

    @Test
    fun nmsKeepsHighestConfidenceAndNonOverlappingDetection() {
        val highest = detection(0f, 0f, 100f, 100f, 0.9f)
        val overlapping = detection(5f, 5f, 95f, 95f, 0.8f)
        val separate = detection(200f, 200f, 250f, 250f, 0.7f)

        val selected = postprocessor.nms(listOf(overlapping, separate, highest))

        assertEquals(listOf(highest, separate), selected)
    }

    @Test
    fun unexpectedOutputShapeUsesTypedError() {
        val error = assertFailsWith<OnnxModelException> {
            postprocessor.decode(Array(1) { FloatArray(NanoDetModelContract.OUTPUT_CHANNELS) })
        }

        assertEquals(OnnxModelErrorCode.INVALID_MODEL_OUTPUT, error.code)
        assertTrue(error.message.orEmpty().contains("Prediction count"))
    }

    @Test
    fun unexpectedOutputChannelCountUsesTypedError() {
        val predictions = emptyPredictions()
        predictions[0] = FloatArray(NanoDetModelContract.OUTPUT_CHANNELS - 1)

        val error = assertFailsWith<OnnxModelException> {
            postprocessor.decode(predictions)
        }

        assertEquals(OnnxModelErrorCode.INVALID_MODEL_OUTPUT, error.code)
        assertTrue(error.message.orEmpty().contains("channel count"))
    }

    private fun emptyPredictions() = Array(NanoDetModelContract.PREDICTION_COUNT) {
        FloatArray(NanoDetModelContract.OUTPUT_CHANNELS)
    }

    private fun detection(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        confidence: Float,
    ) = PlateDetection(
        boundingBox = DetectorBoundingBox(left, top, right, bottom),
        confidence = confidence,
    )
}
