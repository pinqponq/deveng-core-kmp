package core.domain.upload.model

/**
 * Reason codes carried by [BackgroundUploadCompletion.PermanentFailure]. They are diagnostic: the
 * UI shows a generic failure, these say which step gave up, so a report from the field can be read
 * without a debugger attached. Apps add their own codes for steps only they have.
 */
object BackgroundUploadFailureReason {
    const val BYTES_MISSING = "bytes_missing"
    const val SIZE_UNREADABLE = "size_unreadable"
    const val DURATION_UNREADABLE = "duration_unreadable"
    const val WRITE_MULTIPART_FILE_FAILED = "write_multipart_file_failed"
    const val PRESIGN_FAILED = "presign_failed"
    const val INVALID_PRESIGNED_URL = "invalid_presigned_url"
    const val COMMIT_FAILED = "commit_failed"
    const val MAX_ATTEMPTS_EXCEEDED = "max_attempts_exceeded"
    const val UNAUTHORIZED = "unauthorized"
    const val EMPTY_RESPONSE = "empty_response"
    const val PART_HANDLER_MISSING = "part_handler_missing"
    const val WRITE_PART_FILE_FAILED = "write_part_file_failed"
    const val PART_ENTITY_TAG_MISSING = "part_entity_tag_missing"
    const val INVALID_PART_LAYOUT = "invalid_part_layout"
    private const val HTTP_STATUS_PREFIX = "http_"
    private const val PARSE_ERROR_PREFIX = "parse_error: "

    fun httpStatus(statusCode: Int): String = "$HTTP_STATUS_PREFIX$statusCode"

    fun parseError(message: String?): String = "$PARSE_ERROR_PREFIX$message"
}
