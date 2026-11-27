package core.data.temp

import java.io.File

/**
 * Desktop: uses user home + path so temp files survive app restart.
 *
 * @param subdir Path under user home (e.g. ".deveng/temp_storage", ".brindle/camera_temp").
 *               Can contain slashes; parent dirs are created.
 *
 * Use [forApp] to build path as ".appName/subdir" (e.g. ".brindle/camera_temp").
 */
class DesktopTempStorageDirProvider(
    private val subdir: String = ".deveng/temp_storage"
) : TempStorageDirProvider {

    override fun getPath(): String {
        val dir = File(System.getProperty("user.home"), subdir)
        dir.mkdirs()
        return dir.absolutePath
    }

    companion object {
        /**
         * Path under user home as ".appName/subdir" (e.g. ".brindle/camera_temp").
         * Use this so app-specific temp dirs live in core; no custom provider needed per app.
         */
        fun forApp(appName: String, subdir: String): TempStorageDirProvider =
            DesktopTempStorageDirProvider(".$appName/$subdir")
    }
}
