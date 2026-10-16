package core.domain.camera.analysis.onnx

import ai.onnxruntime.OnnxJavaType
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolvedTensorContractTest {
    @Test
    fun resolvedTensorValueCountMatchesTheConcreteShape() {
        val tensor = ResolvedTensorContract(
            name = "input",
            type = OnnxJavaType.FLOAT,
            metadataShape = listOf(-1L, 3L, -1L, -1L),
            resolvedShape = listOf(1L, 3L, 256L, 256L),
        )

        assertEquals(3 * 256 * 256, tensor.valueCount)
    }
}
