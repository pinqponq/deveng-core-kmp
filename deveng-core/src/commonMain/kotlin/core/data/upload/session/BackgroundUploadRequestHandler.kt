package core.data.upload.session

import core.data.upload.MultipartBody
import core.domain.upload.model.BackgroundUploadDescriptor

/**
 * The app's part of a background upload: what to send for a descriptor, how to read the server's
 * answer, and what to do once the upload has landed. Endpoints and request bodies live here, never
 * in the library.
 */
interface BackgroundUploadRequestHandler<T> {

    suspend fun prepareUpload(descriptor: BackgroundUploadDescriptor): BackgroundUploadPreparation

    /** Reads the result of a multipart upload from its 2xx response body; throws when it cannot. */
    fun parseResponse(responseBody: String): T

    /** Finishes an upload that went straight to storage (see [BackgroundUploadPreparation.DirectFile]). */
    suspend fun commitDirectUpload(descriptor: BackgroundUploadDescriptor, remoteId: Int): T

    /** Runs after the upload slot is released; a failure here is logged and never fails the upload. */
    suspend fun onUploadSucceeded(uploadId: String, result: T)
}

sealed interface BackgroundUploadPreparation {

    /** A multipart POST to [url] carrying [body]; the scheduler adds the bearer token. */
    data class Multipart(
        val url: String,
        val headers: Map<String, String>,
        val body: MultipartBody,
    ) : BackgroundUploadPreparation

    /**
     * A file PUT straight to a presigned [url]. No authorization header is added: [contentType] is
     * part of the signature and any extra header would invalidate it. [remoteId] is what
     * [BackgroundUploadRequestHandler.commitDirectUpload] receives once the bytes are stored.
     */
    data class DirectFile(
        val url: String,
        val contentType: String,
        val filePath: String,
        val remoteId: Int,
    ) : BackgroundUploadPreparation

    /** Gives the upload up for good and reports [reason] as a permanent failure. */
    data class Failed(val reason: String) : BackgroundUploadPreparation

    /** Stops without a completion event, e.g. the queued item is already gone. */
    data class Abandoned(val logDetail: String) : BackgroundUploadPreparation
}
