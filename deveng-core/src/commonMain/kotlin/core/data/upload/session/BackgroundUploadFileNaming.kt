package core.data.upload.session

private const val DEFAULT_FILE_NAME_SUFFIX = ".multipart"

/** Maps an upload id to its body file name and back, so leftover files can be matched to descriptors. */
class BackgroundUploadFileNaming(
    private val fileNamePrefix: String,
    private val fileNameSuffix: String = DEFAULT_FILE_NAME_SUFFIX,
) {
    fun fileNameOf(uploadId: String): String = "$fileNamePrefix$uploadId$fileNameSuffix"

    fun uploadIdOf(fileName: String): String? {
        val isBodyFile = fileName.startsWith(fileNamePrefix) && fileName.endsWith(fileNameSuffix)
        if (!isBodyFile) return null
        return fileName
            .removePrefix(fileNamePrefix)
            .removeSuffix(fileNameSuffix)
            .takeIf { uploadId -> uploadId.isNotEmpty() }
    }
}
