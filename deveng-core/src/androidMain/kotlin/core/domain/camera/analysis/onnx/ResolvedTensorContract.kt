package core.domain.camera.analysis.onnx

import ai.onnxruntime.OnnxJavaType

internal data class ResolvedTensorContract(
    val name: String,
    val type: OnnxJavaType,
    val metadataShape: List<Long>,
    val resolvedShape: List<Long>
) {
    val valueCount: Int
        get() {
            var count = 1L
            for (dimension in resolvedShape) {
                count *= dimension
            }
            return count.toInt()
        }
}