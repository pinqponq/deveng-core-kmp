package core.widget.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TEMPORARY_FILE_SUFFIX = ".tmp"

/** File.createTempFile rejects shorter prefixes. */
private const val MINIMUM_IMAGE_FILE_NAME_LENGTH = 3

/**
 * The single image a widget renders from. Glance may redraw at any time and must not wait on a download, so the
 * widget only ever decodes this local copy.
 */
class HomeScreenWidgetImageStore(
    private val imageDirectory: File,
    private val imageFileName: String,
) {
    val widgetImageFile: File = File(imageDirectory, imageFileName)

    init {
        require(imageFileName.length >= MINIMUM_IMAGE_FILE_NAME_LENGTH) {
            "The widget image file name is also the temporary file prefix and needs at least $MINIMUM_IMAGE_FILE_NAME_LENGTH characters."
        }
    }

    /** Downloads, shrinks and stores the image off the main thread. */
    suspend fun downloadAndReplaceImage(imageUrl: String, connectTimeoutMilliseconds: Int, readTimeoutMilliseconds: Int) {
        withContext(Dispatchers.IO) {
            val imageBytes = HomeScreenWidgetImageDownloader.downloadImage(
                imageUrl = imageUrl,
                connectTimeoutMilliseconds = connectTimeoutMilliseconds,
                readTimeoutMilliseconds = readTimeoutMilliseconds,
            )

            replaceImageAtomically(HomeScreenWidgetBitmaps.shrinkImageForStorage(imageBytes))
        }
    }

    /**
     * Writes the new image to a temporary file first and then renames it over the old one, so a redraw running at
     * the same time never decodes a half-written file. Every call gets its own temporary file: an immediate and a
     * periodic refresh can run together, and with one shared name the first rename took the file away from the
     * second, which then failed and dropped the image it had just stored.
     */
    internal fun replaceImageAtomically(imageBytes: ByteArray) {
        imageDirectory.mkdirs()
        val temporaryImageFile = File.createTempFile(imageFileName, TEMPORARY_FILE_SUFFIX, imageDirectory)
        try {
            temporaryImageFile.writeBytes(imageBytes)
            if (!temporaryImageFile.renameTo(widgetImageFile)) {
                temporaryImageFile.copyTo(widgetImageFile, overwrite = true)
            }
        } finally {
            temporaryImageFile.delete()
        }
    }

    fun deleteImage() {
        widgetImageFile.delete()
    }
}
