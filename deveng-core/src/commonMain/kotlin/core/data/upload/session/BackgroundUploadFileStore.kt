package core.data.upload.session

/** Where prepared request bodies wait for the background transfer that reads them from disk. */
interface BackgroundUploadFileStore {

    /** Writes [bytes] for [uploadId] and returns the file path, or null when nothing could be written. */
    fun write(uploadId: String, bytes: ByteArray): String?

    /**
     * Copies [length] bytes of [sourceFilePath] from [offset] into the body file of [transferId] and
     * returns its path, or null when nothing could be written. A background transfer only sends
     * whole files, so every part of a parted upload gets its own file.
     */
    fun writePart(transferId: String, sourceFilePath: String, offset: Long, length: Long): String?

    fun delete(uploadId: String)

    /** Transfer ids (see [BackgroundUploadTransferId]) that still have a body file on disk. */
    fun listUploadIds(): List<String>
}
