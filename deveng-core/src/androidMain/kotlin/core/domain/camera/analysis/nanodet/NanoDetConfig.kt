package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.onnx.OnnxExecutionProvider

data class NanoDetConfig(
    val confidenceThreshold: Float = 0.50f,
    val executionProvider: OnnxExecutionProvider = OnnxExecutionProvider.XNNPACK,
    val xnnpackThreads: Int = 3,
    val ortIntraOpThreads: Int = 1,
    val ortInterOpThreads: Int = 1
)