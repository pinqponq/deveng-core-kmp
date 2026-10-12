package core.data.upload.session

/** Where prepared request bodies wait for the background transfer that reads them from disk. */
interface BackgroundUploadFileStore {

    /** Writes [bytes] for [uploadId] and returns the file path, or null when nothing could be written. */
    fun write(uploadId: String, bytes: ByteArray): String?

    fun delete(uploadId: String)

    /** Upload ids that still have a body file on disk. */
    fun listUploadIds(): List<String>
}
