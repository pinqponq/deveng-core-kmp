package core.domain.camera.analysis.nanodet

import ai.onnxruntime.OnnxJavaType
import core.domain.camera.analysis.nanodet.model.NanoDetModelContract
import core.domain.camera.analysis.nanodet.model.NanoDetModelProfile
import core.domain.camera.analysis.onnx.OnnxModelMetadata
import core.domain.camera.analysis.onnx.OnnxTensorMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class NanoDetModelProfileTest {
    @Test
    fun staticAndroidContractResolvesMetadataNamesAndShapes() {
        val resolved = NanoDetModelProfile.resolve(
            metadata(
                inputShape = listOf(1L, 3L, 256L, 256L),
                outputShape = listOf(1L, 1360L, 33L),
            ),
        )

        assertEquals("NanoDet", resolved.profileName)
        assertEquals("renamed_input", resolved.input.name)
        assertEquals("renamed_output", resolved.output.name)
        assertEquals(listOf(1L, 3L, 256L, 256L), resolved.input.metadataShape)
        assertEquals(listOf(1L, 3L, 256L, 256L), resolved.input.resolvedShape)
        assertEquals(listOf(1L, 1360L, 33L), resolved.output.metadataShape)
        assertEquals(listOf(1L, 1360L, 33L), resolved.output.resolvedShape)
        assertEquals(3 * 256 * 256, resolved.input.valueCount)
        assertEquals(1360 * 33, resolved.output.valueCount)
    }

    @Test
    fun rectangularResolvedInputUsesMatchingNanoDetPredictionCount() {
        val inputWidth = 320
        val inputHeight = 256
        val resolved = NanoDetModelProfile.resolve(
            metadata(
                inputShape = listOf(1L, 3L, inputHeight.toLong(), inputWidth.toLong()),
                outputShape = listOf(
                    1L,
                    NanoDetModelProfile.calculatePredictionCount(inputWidth, inputHeight).toLong(),
                    33L,
                ),
            ),
        )

        assertEquals(listOf(1L, 3L, inputHeight.toLong(), inputWidth.toLong()), resolved.input.resolvedShape)
        assertEquals(
            listOf(1L, NanoDetModelProfile.calculatePredictionCount(inputWidth, inputHeight).toLong(), 33L),
            resolved.output.resolvedShape,
        )
    }

    private fun metadata(
        inputShape: List<Long>,
        outputShape: List<Long>,
    ): OnnxModelMetadata = OnnxModelMetadata(
        inputs = listOf(OnnxTensorMetadata("renamed_input", OnnxJavaType.FLOAT, inputShape)),
        outputs = listOf(OnnxTensorMetadata("renamed_output", OnnxJavaType.FLOAT, outputShape)),
    )
}
