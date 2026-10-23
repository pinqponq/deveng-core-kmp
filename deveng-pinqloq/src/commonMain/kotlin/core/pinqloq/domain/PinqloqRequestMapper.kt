package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.model.PinqloqPlatformInfo
import kotlinx.serialization.json.JsonObject

/**
 * Decides the wire shape of one log.
 * [platformMetadata] lists the platform values that go into the log's `metadata`; the values the mapper writes as
 * top-level fields itself are left out of it.
 */
interface PinqloqRequestMapper {
    fun platformMetadata(platformInfo: PinqloqPlatformInfo): Map<String, String>

    fun mapEntry(entry: PinqloqLogEntry): JsonObject
}
