package core.domain.camera.analysis.nanodet.model

internal object NanoDetModelContract {
    const val OUTPUT_CHANNELS = 33
    const val REG_MAX = 7
    val strides: List<Int> = listOf(8, 16, 32, 64)
    val mean: List<Float> = listOf(103.53f, 116.28f, 123.675f)
    val standardDeviation: List<Float> = listOf(57.375f, 57.12f, 58.395f)
}
