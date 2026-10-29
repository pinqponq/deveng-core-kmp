package core.data.upload.fake

import core.data.upload.session.BackgroundUploadPartHandler
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.StoredPart

class FakeBackgroundUploadPartHandler : BackgroundUploadPartHandler<String> {
    var signFailure: Exception? = null
    var completeFailure: Exception? = null
    var abortFailure: Exception? = null
    val signedPartNumbers = mutableListOf<Int>()
    val completedParts = mutableListOf<List<StoredPart>>()
    val abortedUploadIds = mutableListOf<String>()

    override suspend fun signPart(descriptor: BackgroundUploadDescriptor, partNumber: Int): String {
        signedPartNumbers += partNumber
        signFailure?.let { failure -> throw failure }
        return urlOf(partNumber = partNumber, signature = signedPartNumbers.count { signed -> signed == partNumber })
    }

    override suspend fun completeParts(descriptor: BackgroundUploadDescriptor, storedParts: List<StoredPart>): String {
        completedParts += storedParts
        completeFailure?.let { failure -> throw failure }
        return "$COMPLETED_RESULT_PREFIX${descriptor.partedUpload?.remoteKey}"
    }

    override suspend fun abortParts(descriptor: BackgroundUploadDescriptor) {
        abortedUploadIds += descriptor.uploadId
        abortFailure?.let { failure -> throw failure }
    }

    companion object {
        const val COMPLETED_RESULT_PREFIX = "completed:"

        fun urlOf(partNumber: Int, signature: Int = 1): String =
            "https://storage.example.com/video?partNumber=$partNumber&signature=$signature"
    }
}
