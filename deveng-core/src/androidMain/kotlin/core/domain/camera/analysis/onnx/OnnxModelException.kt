package core.domain.camera.analysis.onnx

internal enum class OnnxModelErrorCode {
    MODEL_NOT_FOUND,
    MODEL_NOT_READABLE,
    MODEL_INITIALIZATION_FAILED,
    INVALID_MODEL_INPUT,
    INVALID_MODEL_OUTPUT,
    FRAME_CONVERSION_FAILED,
    DETECTOR_INFERENCE_FAILED,
    SESSION_CLOSED,
}

internal class OnnxModelException(
    val code: OnnxModelErrorCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
