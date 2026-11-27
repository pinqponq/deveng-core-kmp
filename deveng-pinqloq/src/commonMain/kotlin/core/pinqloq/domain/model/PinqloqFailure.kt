package core.pinqloq.domain.model

data class PinqloqFailure(
    val reason: PinqloqFailureReason,
    val droppedLogCount: Int,
    val cause: Throwable? = null
)
