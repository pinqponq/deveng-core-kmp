package core.util.media

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager {
    actual suspend fun shareSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean {
        return false
    }

    actual suspend fun shareMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Boolean {
        return false
    }

    actual suspend fun saveSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean {
        return false
    }

    actual suspend fun saveMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Int {
        return 0
    }
}
