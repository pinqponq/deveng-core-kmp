package core.domain.camera.analysis.ocr

import ai.onnxruntime.OnnxJavaType
import core.domain.camera.analysis.ocr.model.FastPlateOcrModelProfile
import core.domain.camera.analysis.onnx.OnnxModelMetadata
import core.domain.camera.analysis.onnx.OnnxTensorMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class FastPlateOcrModelProfileTest {
    @Test
    fun dynamicBatchAndMetadataNamesResolveAgainstTheOcrProfile() {
        val resolved = FastPlateOcrModelProfile.resolve(
            metadata(
                inputShape = listOf(-1L, 64L, 128L, 3L),
                outputShape = listOf(-1L, 10L, 37L),
            ),
        )

        assertEquals("FastPlateOCR", resolved.profileName)
        assertEquals("renamed_input", resolved.input.name)
        assertEquals("renamed_output", resolved.output.name)
        assertEquals(listOf(1L, 64L, 128L, 3L), resolved.input.resolvedShape)
        assertEquals(listOf(1L, 10L, 37L), resolved.output.resolvedShape)
        assertEquals(64 * 128 * 3, resolved.input.valueCount)
        assertEquals(10 * 37, resolved.output.valueCount)
    }

    private fun metadata(
        inputShape: List<Long>,
        outputShape: List<Long>,
    ): OnnxModelMetadata = OnnxModelMetadata(
        inputs = listOf(OnnxTensorMetadata("renamed_input", OnnxJavaType.FLOAT, inputShape)),
        outputs = listOf(OnnxTensorMetadata("renamed_output", OnnxJavaType.FLOAT, outputShape)),
    )
}
