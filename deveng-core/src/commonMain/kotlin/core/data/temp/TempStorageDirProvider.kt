package core.data.temp

/**
 * Provides the app-specific directory path for temp file storage.
 * Implementations are platform-specific (e.g. app filesDir on Android, user home on Desktop).
 * Use one provider per use case (e.g. camera_temp, documents_temp) by returning the appropriate path.
 */
interface TempStorageDirProvider {

    fun getPath(): String
}
