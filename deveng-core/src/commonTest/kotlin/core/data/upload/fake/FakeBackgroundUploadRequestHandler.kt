package core.data.upload.fake

import core.data.upload.session.BackgroundUploadPreparation
import core.data.upload.session.BackgroundUploadRequestHandler
import core.domain.upload.model.BackgroundUploadDescriptor

class FakeBackgroundUploadRequestHandler : BackgroundUploadRequestHandler<String> {
    var preparation: BackgroundUploadPreparation = BackgroundUploadPreparation.Abandoned(logDetail = "not configured")
    var commitFailure: Exception? = null
    var successFollowUpFailure: Exception? = null
    val preparedDescriptors = mutableListOf<BackgroundUploadDescriptor>()
    val committedRemoteIds = mutableListOf<Int>()
    val succeededUploads = mutableListOf<Pair<String, String>>()

    override suspend fun prepareUpload(descriptor: BackgroundUploadDescriptor): BackgroundUploadPreparation {
        preparedDescriptors += descriptor
        return preparation
    }

    override fun parseResponse(responseBody: String): String {
        require(responseBody.startsWith(RESULT_PREFIX)) { "Unexpected body: $responseBody" }
        return responseBody.removePrefix(RESULT_PREFIX)
    }

    override suspend fun commitDirectUpload(descriptor: BackgroundUploadDescriptor, remoteId: Int): String {
        commitFailure?.let { failure -> throw failure }
        committedRemoteIds += remoteId
        return "$COMMITTED_RESULT_PREFIX$remoteId"
    }

    override suspend fun onUploadSucceeded(uploadId: String, result: String) {
        succeededUploads += uploadId to result
        successFollowUpFailure?.let { failure -> throw failure }
    }

    companion object {
        const val RESULT_PREFIX = "result:"
        const val COMMITTED_RESULT_PREFIX = "committed:"
    }
}
