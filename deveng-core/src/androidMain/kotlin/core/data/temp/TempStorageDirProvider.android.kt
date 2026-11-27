package core.data.temp

import android.content.Context
import java.io.File

/**
 * Android: uses app filesDir + [subdir] so temp files survive process death.
 * @param subdir Subdirectory name under filesDir (e.g. "temp_storage", "camera_temp"). Default "temp_storage".
 */
class AndroidTempStorageDirProvider(
    private val context: Context,
    private val subdir: String = "temp_storage"
) : TempStorageDirProvider {

    override fun getPath(): String {
        return File(context.applicationContext.filesDir, subdir).apply { mkdirs() }.absolutePath
    }
}
