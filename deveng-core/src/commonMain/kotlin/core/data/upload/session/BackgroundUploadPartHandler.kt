package core.data.upload.session

import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.StoredPart

/**
 * The app's part of an upload that goes to storage in parts (see
 * [BackgroundUploadPreparation.DirectFileInParts]). The app's server owns the multipart upload:
 * it signs each part and completes the upload, so endpoints stay in the app.
 */
interface BackgroundUploadPartHandler<T> {

    /**
     * Returns a presigned PUT url for [partNumber]. It is asked again when storage rejected the
     * previous url (an expired signature), so the app must not cache a url past its expiry.
     */
    suspend fun signPart(descriptor: BackgroundUploadDescriptor, partNumber: Int): String

    /** Completes the multipart upload with every stored part, in part order, and returns the app's result. */
    suspend fun completeParts(descriptor: BackgroundUploadDescriptor, storedParts: List<StoredPart>): T

    /** Gives the multipart upload up on the server; called once when the upload fails for good. A failure is only logged. */
    suspend fun abortParts(descriptor: BackgroundUploadDescriptor)
}
