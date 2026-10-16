package core.domain.camera.analysis.onnx

internal data class OnnxModelMetadata(
    val inputs: List<OnnxTensorMetadata>,
    val outputs: List<OnnxTensorMetadata>
)