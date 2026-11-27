package global.deveng.core

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getNewPhotoSavePath(): String {
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "brindle",
    )
    dir.mkdirs()
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File(dir, "IMG_$timestamp.jpg").absolutePath
}
