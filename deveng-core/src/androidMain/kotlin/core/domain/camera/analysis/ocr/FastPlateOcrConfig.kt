package core.domain.camera.analysis.ocr

import core.domain.camera.analysis.onnx.OnnxExecutionProvider

data class FastPlateOcrConfig(
    val confidenceThreshold: Float = 0.90f,
    val executionProvider: OnnxExecutionProvider = OnnxExecutionProvider.CPU,
    val xnnpackThreads: Int = 1,
    val ortIntraOpThreads: Int = 2,
    val ortInterOpThreads: Int = 1
)