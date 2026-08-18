package core.domain.camera.analysis.nanodet

internal data class NanoDetConfig(
    val confidenceThreshold: Float = 0.50f,  // modelin tahmini kabul etmesi için min güven skoru
    val nmsIouThreshold: Float = 0.60f,  // bir frame'de birden fazla tespit yapılırsa benzerlik oranı bu değerden yüksekse en iyi skorlu kabul edilir
    val executionProvider: NanoDetExecutionProvider = NanoDetExecutionProvider.XNNPACK,
    val xnnpackThreads: Int = 2,
) {  // buradaki ifadelere gerek yok init kısmı gereksiz
    init {
        require(confidenceThreshold in 0f..1f) {
            "confidenceThreshold must be between 0 and 1."
        }
        require(nmsIouThreshold in 0f..1f) {
            "nmsIouThreshold must be between 0 and 1."
        }
        require(xnnpackThreads > 0) {
            "xnnpackThreads must be positive."
        }
    }
}

internal enum class NanoDetExecutionProvider {
    CPU,
    XNNPACK,
}

internal object NanoDetModelContract {
    const val INPUT_NAME = "data"  // input tensor adı
    const val OUTPUT_NAME = "output"  // output tensor adı
    const val INPUT_SIZE = 416  // modele giren frame boyutu 416x416
    const val PREDICTION_COUNT = 3598  // modelin frame'de bakacağı tahmin noktası sayısı
    const val OUTPUT_CHANNELS = 33
    const val REG_MAX = 7
    const val INPUT_VALUE_COUNT = 3 * INPUT_SIZE * INPUT_SIZE
    const val OUTPUT_VALUE_COUNT = PREDICTION_COUNT * OUTPUT_CHANNELS

    val inputShape: LongArray  // modelin input tensor'u
        get() = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())

    val outputShape: LongArray  // modelin output tensor'u
        get() = longArrayOf(1, PREDICTION_COUNT.toLong(), OUTPUT_CHANNELS.toLong())

    val strides: List<Int> = listOf(8, 16, 32, 64)
    val mean: List<Float> = listOf(103.53f, 116.28f, 123.675f)
    val standardDeviation: List<Float> = listOf(57.375f, 57.12f, 58.395f)
}
