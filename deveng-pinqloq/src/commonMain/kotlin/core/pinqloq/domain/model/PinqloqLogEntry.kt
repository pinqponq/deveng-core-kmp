package core.pinqloq.domain.model

import kotlin.time.Instant

data class PinqloqLogEntry(
    val event: String,
    val level: PinqloqLevel,
    val metadata: Map<String, String>,
    val detail: Map<String, String>,
    val platformInfo: PinqloqPlatformInfo,
    val occurredAt: Instant
)
