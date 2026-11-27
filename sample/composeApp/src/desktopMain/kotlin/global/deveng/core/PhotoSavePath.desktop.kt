package global.deveng.core

actual fun getNewPhotoSavePath(): String =
    "capture_${kotlin.random.Random.nextLong()}.jpg"
