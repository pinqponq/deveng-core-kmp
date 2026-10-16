package core.domain.camera.analysis.onnx

internal data class ResolvedModelContract(
    val profileName: String,
    val input: ResolvedTensorContract,
    val output: ResolvedTensorContract
)