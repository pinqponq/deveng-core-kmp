package core.data.upload.work

import co.touchlab.kermit.Logger
import core.data.upload.BackgroundUploadDescriptorStorage
import core.data.upload.session.BackgroundUploadPartHandler
import core.data.upload.session.BackgroundUploadPreparation
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import core.domain.upload.model.PartByteRange
import core.domain.upload.model.PartedUpload
import core.domain.upload.model.StoredPart
import kotlin.coroutines.cancellation.CancellationException

private const val HTTP_FORBIDDEN = 403
private const val HTTP_SUCCESS_STATUS_CODES_START = 200
private const val HTTP_SUCCESS_STATUS_CODES_END = 299

/** Sends one part's bytes to a presigned url; the app implements it with its own HTTP client. */
fun interface BackgroundUploadPartSender {

    /**
     * PUTs [byteRange] of [filePath] to [url] without adding headers and returns the answer.
     * A network error is thrown, so the scheduled work runs again and continues from this part.
     */
    suspend fun sendPart(url: String, filePath: String, byteRange: PartByteRange): BackgroundUploadPartAnswer
}

/** [entityTag] is the ETag response header, null when storage sent none. */
data class BackgroundUploadPartAnswer(
    val statusCode: Int,
    val entityTag: String?,
)

/**
 * Sends a parted upload inside one run of scheduled work (Android WorkManager), part after part,
 * and saves each stored part in the descriptor at once. A run that is stopped or throws leaves the
 * stored parts behind, so the next run continues from the first part that is not stored.
 * Use it from a [BackgroundUploadWork]: a thrown exception there retries the work.
 */
class BackgroundPartedUploadLoop<T>(
    private val descriptorStorage: BackgroundUploadDescriptorStorage,
    private val partHandler: BackgroundUploadPartHandler<T>,
    private val partSender: BackgroundUploadPartSender,
    private val logTag: String,
) {

    /**
     * Continues the parted upload stored for [descriptor], or opens one with [openParts] when none
     * is stored. [openParts] runs the app's request that opens the multipart upload on its server.
     */
    suspend fun upload(
        descriptor: BackgroundUploadDescriptor,
        openParts: suspend () -> BackgroundUploadPreparation.DirectFileInParts,
    ): BackgroundUploadAttempt<T> {
        val partedDescriptor = descriptorStorage.load(descriptor.uploadId)?.takeIf { stored -> stored.partedUpload != null }
            ?: openPartedUpload(descriptor = descriptor, preparation = openParts())
            ?: return BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.INVALID_PART_LAYOUT)
        return sendPendingParts(partedDescriptor)
    }

    private fun openPartedUpload(
        descriptor: BackgroundUploadDescriptor,
        preparation: BackgroundUploadPreparation.DirectFileInParts,
    ): BackgroundUploadDescriptor? {
        val partedUpload = try {
            PartedUpload(
                remoteKey = preparation.remoteKey,
                filePath = preparation.filePath,
                totalBytes = preparation.totalBytes,
                partSizeBytes = preparation.partSizeBytes,
            )
        } catch (exception: IllegalArgumentException) {
            Logger.e(exception) { "[$logTag] Invalid part layout uploadId=${descriptor.uploadId}" }
            return null
        }
        val partedDescriptor = descriptor.copy(partedUpload = partedUpload)
        descriptorStorage.save(partedDescriptor)
        Logger.i { "[$logTag] Parted upload opened parts=${partedUpload.partCount} uploadId=${descriptor.uploadId}" }
        return partedDescriptor
    }

    private suspend fun sendPendingParts(startDescriptor: BackgroundUploadDescriptor): BackgroundUploadAttempt<T> {
        var descriptor = startDescriptor
        while (true) {
            val partedUpload = requireNotNull(descriptor.partedUpload) { "Only parted descriptors reach the part loop" }
            val partNumber = partedUpload.nextPendingPartNumber()
                ?: return BackgroundUploadAttempt.Uploaded(
                    result = partHandler.completeParts(descriptor = descriptor, storedParts = partedUpload.storedParts),
                )

            val answer = sendPartWithFreshSignature(descriptor = descriptor, partedUpload = partedUpload, partNumber = partNumber)
            val isSuccessStatusCode = answer.statusCode in HTTP_SUCCESS_STATUS_CODES_START..HTTP_SUCCESS_STATUS_CODES_END
            if (!isSuccessStatusCode) {
                Logger.e { "[$logTag] Part rejected part=$partNumber statusCode=${answer.statusCode} uploadId=${descriptor.uploadId}" }
                return failPartedUpload(descriptor, BackgroundUploadFailureReason.httpStatus(answer.statusCode))
            }
            val entityTag = answer.entityTag?.takeIf { tag -> tag.isNotBlank() }
                ?: return failPartedUpload(descriptor, BackgroundUploadFailureReason.PART_ENTITY_TAG_MISSING)

            descriptor = descriptor.copy(
                partedUpload = partedUpload.withStoredPart(StoredPart(partNumber = partNumber, entityTag = entityTag)),
            )
            descriptorStorage.save(descriptor)
            Logger.i { "[$logTag] Part stored part=$partNumber/${partedUpload.partCount} uploadId=${descriptor.uploadId}" }
        }
    }

    // Storage answers 403 when the presigned url expired, so the part is signed once more and sent
    // again before the answer counts.
    private suspend fun sendPartWithFreshSignature(
        descriptor: BackgroundUploadDescriptor,
        partedUpload: PartedUpload,
        partNumber: Int,
    ): BackgroundUploadPartAnswer {
        val byteRange = partedUpload.byteRangeOf(partNumber)
        val firstAnswer = sendSignedPart(descriptor = descriptor, partedUpload = partedUpload, partNumber = partNumber, byteRange = byteRange)
        if (firstAnswer.statusCode != HTTP_FORBIDDEN) return firstAnswer
        Logger.w { "[$logTag] Part signature rejected, signing again part=$partNumber uploadId=${descriptor.uploadId}" }
        return sendSignedPart(descriptor = descriptor, partedUpload = partedUpload, partNumber = partNumber, byteRange = byteRange)
    }

    private suspend fun sendSignedPart(
        descriptor: BackgroundUploadDescriptor,
        partedUpload: PartedUpload,
        partNumber: Int,
        byteRange: PartByteRange,
    ): BackgroundUploadPartAnswer {
        val partUrl = partHandler.signPart(descriptor = descriptor, partNumber = partNumber)
        return partSender.sendPart(url = partUrl, filePath = partedUpload.filePath, byteRange = byteRange)
    }

    private suspend fun failPartedUpload(descriptor: BackgroundUploadDescriptor, reason: String): BackgroundUploadAttempt<T> {
        try {
            partHandler.abortParts(descriptor)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag] Abort of parted upload failed uploadId=${descriptor.uploadId}" }
        }
        return BackgroundUploadAttempt.Failed(reason = reason)
    }
}
