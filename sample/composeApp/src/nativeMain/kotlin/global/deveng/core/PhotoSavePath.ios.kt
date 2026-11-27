package global.deveng.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
actual fun getNewPhotoSavePath(): String {
    val documentsDir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: return "IMG_${Random.nextLong()}.jpg"
    val cameraKDir = "$documentsDir/brindle"
    NSFileManager.defaultManager.apply {
        if (!fileExistsAtPath(cameraKDir)) {
            createDirectoryAtPath(cameraKDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }
    return "$cameraKDir/IMG_${Random.nextLong()}.jpg"
}
