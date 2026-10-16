package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.nanodet.model.NanoDetModelContract
import core.domain.camera.analysis.nanodet.model.NanoDetModelProfile
import core.domain.camera.analysis.nanodet.pipeline.NanoDetPostprocessor
import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class NanoDetPostprocessorTest {
    private val postprocessor = NanoDetPostprocessor(
        config = NanoDetConfig(),
        inputWidth = 256,
        inputHeight = 256,
    )

    @Test
    fun centerPriorsMatchTheAndroidReferenceOutputLayout() {
        val priors = postprocessor.createCenterPriors()

        assertEquals(1360, priors.size)
        assertEquals(NanoDetPostprocessor.CenterPrior(0f, 0f, 8f), priors.first())
        assertEquals(NanoDetPostprocessor.CenterPrior(192f, 192f, 64f), priors.last())
    }

    @Test
    fun rectangularResolvedInputCreatesMatchingPriorsAndClampsBothAxes() {
        val inputWidth = 320
        val inputHeight = 256
        val rectangular = NanoDetPostprocessor(
            config = NanoDetConfig(),
            inputWidth = inputWidth,
            inputHeight = inputHeight,
        )
        val predictionCount = NanoDetModelProfile.calculatePredictionCount(inputWidth, inputHeight)
        val predictions = emptyPredictions(predictionCount)
        val lastPrediction = predictionCount - 1
        predictions.put(lastPrediction * NanoDetModelContract.OUTPUT_CHANNELS, 0.90f)
        repeat(4) { side ->
            predictions.put(lastPrediction * NanoDetModelContract.OUTPUT_CHANNELS + 1 + side * 8 + 7, 20f)
        }

        val detection = rectangular.decode(predictions)

        assertEquals(predictionCount, rectangular.createCenterPriors().size)
        assertEquals(inputWidth - 1f, detection?.boundingBox?.right)
        assertEquals(inputHeight - 1f, detection?.boundingBox?.bottom)
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
        predictions.put(0, 0.90f)
        repeat(4) { side ->
            predictions.put(1 + side * 8 + 1, 20f)
        }

        val detection = requireNotNull(postprocessor.decode(predictions))

        assertEquals(0.90f, detection.confidence)
        assertEquals(0f, detection.boundingBox.left)
        assertEquals(0f, detection.boundingBox.top)
        assertEquals(8f, detection.boundingBox.right, absoluteTolerance = 0.001f)
        assertEquals(8f, detection.boundingBox.bottom, absoluteTolerance = 0.001f)
    }

    @Test
    fun decodeSelectsHighestConfidenceCandidate() {
        val predictions = emptyPredictions()
        val channels = NanoDetModelContract.OUTPUT_CHANNELS
        predictions.put(0 * channels + 0, 0.70f)
        repeat(4) { side -> predictions.put(0 * channels + 1 + side * 8 + 1, 20f) }
        predictions.put(1 * channels + 0, 0.95f)
        repeat(4) { side -> predictions.put(1 * channels + 1 + side * 8 + 2, 20f) }

        val best = postprocessor.decode(predictions)

        assertEquals(0.95f, best?.confidence)
    }

    private fun emptyPredictions(
        predictionCount: Int = 1360,
    ): FloatBuffer = FloatBuffer.allocate(predictionCount * NanoDetModelContract.OUTPUT_CHANNELS)
}
