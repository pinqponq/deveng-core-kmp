package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.model.PinqloqPlatformInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private const val EVENT_FIELD = "event"
private const val METADATA_FIELD = "metadata"
private const val DETAIL_FIELD = "detail"
private const val APP_VERSION_NAME_KEY = "appVersionName"
private const val DEVICE_UID_KEY = "deviceUid"
private const val LOG_LEVEL_FIELD = "logLevel"
private const val LEVEL_FIELD = "level"
private const val DATE_FIELD = "date"
private const val DEVICE_IDENTIFIER_FIELD = "deviceIdentifier"
private const val DEVICE_NAME_KEY = "deviceName"

object PinqloqRequestMappers {
    /**
     * Fields: `event`, `logLevel`, `metadata`, `detail`, `date`.
     * `metadata` also carries `appVersionName`, `deviceUid` and `deviceName`.
     */
    val LogLevelWithDate: PinqloqRequestMapper = object : PinqloqRequestMapper {
        override fun platformMetadata(platformInfo: PinqloqPlatformInfo): Map<String, String> = mapOf(
            APP_VERSION_NAME_KEY to platformInfo.appVersionName,
            DEVICE_UID_KEY to platformInfo.deviceUid,
            DEVICE_NAME_KEY to platformInfo.deviceName
        )

        override fun mapEntry(entry: PinqloqLogEntry): JsonObject = buildJsonObject {
            put(EVENT_FIELD, JsonPrimitive(entry.event))
            put(LOG_LEVEL_FIELD, JsonPrimitive(entry.level.backendValue))
            putTextMaps(entry)
            put(DATE_FIELD, JsonPrimitive(entry.occurredAt.toString()))
        }
    }

    /**
     * Fields: `event`, `deviceIdentifier`, `level`, `appVersionName`, `metadata`, `detail`.
     * `metadata` also carries `deviceName`.
     */
    val LevelWithDeviceIdentifier: PinqloqRequestMapper = object : PinqloqRequestMapper {
        override fun platformMetadata(platformInfo: PinqloqPlatformInfo): Map<String, String> = mapOf(
            DEVICE_NAME_KEY to platformInfo.deviceName
        )

        override fun mapEntry(entry: PinqloqLogEntry): JsonObject = buildJsonObject {
            put(EVENT_FIELD, JsonPrimitive(entry.event))
            put(DEVICE_IDENTIFIER_FIELD, JsonPrimitive(entry.platformInfo.deviceUid))
            put(LEVEL_FIELD, JsonPrimitive(entry.level.backendValue))
            put(APP_VERSION_NAME_KEY, JsonPrimitive(entry.platformInfo.appVersionName))
            putTextMaps(entry)
        }
    }
}

private fun JsonObjectBuilder.putTextMaps(entry: PinqloqLogEntry) {
    if (entry.metadata.isNotEmpty()) {
        put(METADATA_FIELD, entry.metadata.toJsonObject())
    }
    if (entry.detail.isNotEmpty()) {
        put(DETAIL_FIELD, entry.detail.toJsonObject())
    }
}

private fun Map<String, String>.toJsonObject(): JsonObject = JsonObject(
    mapValues { (_, value) -> JsonPrimitive(value) }
)
