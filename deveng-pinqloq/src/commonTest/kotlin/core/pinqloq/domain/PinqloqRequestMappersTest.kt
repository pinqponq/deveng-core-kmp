package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqLevel
import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.model.PinqloqPlatformInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class PinqloqRequestMappersTest {

    @Test
    fun logLevelWithDate_mapEntry_writesEventLogLevelMetadataDetailAndDate() {
        val body = PinqloqRequestMappers.LogLevelWithDate.mapEntry(createEntry())

        assertEquals(
            parse(
                """{"event":"opened","logLevel":"Warning","metadata":{"userId":"42"},"detail":{"stack":"trace"},""" +
                    """"date":"2023-11-14T22:13:20Z"}"""
            ),
            body
        )
    }

    @Test
    fun logLevelWithDate_mapEntry_omitsEmptyDetail() {
        val body = PinqloqRequestMappers.LogLevelWithDate.mapEntry(createEntry(detail = emptyMap()))

        assertEquals(setOf("event", "logLevel", "metadata", "date"), body.keys)
    }

    @Test
    fun logLevelWithDate_platformMetadata_listsVersionDeviceUidAndDeviceName() {
        val platformMetadata = PinqloqRequestMappers.LogLevelWithDate.platformMetadata(PLATFORM_INFO)

        assertEquals(
            mapOf("appVersionName" to "1.2.3", "deviceUid" to "device-uid", "deviceName" to "Pixel"),
            platformMetadata
        )
    }

    @Test
    fun levelWithDeviceIdentifier_mapEntry_writesEventDeviceIdentifierLevelVersionMetadataAndDetail() {
        val body = PinqloqRequestMappers.LevelWithDeviceIdentifier.mapEntry(createEntry())

        assertEquals(
            parse(
                """{"event":"opened","deviceIdentifier":"device-uid","level":"Warning","appVersionName":"1.2.3",""" +
                    """"metadata":{"userId":"42"},"detail":{"stack":"trace"}}"""
            ),
            body
        )
    }

    @Test
    fun levelWithDeviceIdentifier_mapEntry_omitsEmptyMetadataAndDetail() {
        val body = PinqloqRequestMappers.LevelWithDeviceIdentifier.mapEntry(
            createEntry(metadata = emptyMap(), detail = emptyMap())
        )

        assertEquals(setOf("event", "deviceIdentifier", "level", "appVersionName"), body.keys)
    }

    @Test
    fun levelWithDeviceIdentifier_platformMetadata_listsOnlyDeviceName() {
        val platformMetadata = PinqloqRequestMappers.LevelWithDeviceIdentifier.platformMetadata(PLATFORM_INFO)

        assertEquals(mapOf("deviceName" to "Pixel"), platformMetadata)
    }

    private fun createEntry(
        metadata: Map<String, String> = mapOf("userId" to "42"),
        detail: Map<String, String> = mapOf("stack" to "trace")
    ) = PinqloqLogEntry(
        event = "opened",
        level = PinqloqLevel.Warning,
        metadata = metadata,
        detail = detail,
        platformInfo = PLATFORM_INFO,
        occurredAt = Instant.fromEpochSeconds(1_700_000_000)
    )

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text) as JsonObject

    private companion object {
        val PLATFORM_INFO = PinqloqPlatformInfo(
            appVersionName = "1.2.3",
            deviceUid = "device-uid",
            deviceName = "Pixel"
        )
    }
}
