package core.domain.camera.analysis.onnx

import ai.onnxruntime.OnnxJavaType

/** Immutable technical metadata read from one ONNX Runtime session. */
internal data class OnnxTensorMetadata(
    val name: String,
    val type: OnnxJavaType,
    val shape: List<Long>
)