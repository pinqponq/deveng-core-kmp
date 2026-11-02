package core.crash.data

import kotlin.test.Test
import kotlin.test.assertEquals

class FirebasePlatformCrashReporterTest {

    @Test
    fun mapToCrashlyticsUserId_withUserId_keepsUserId() {
        assertEquals(USER_ID, mapToCrashlyticsUserId(USER_ID))
    }

    @Test
    fun mapToCrashlyticsUserId_withNullUserId_returnsEmptyStringThatClearsTheUser() {
        assertEquals("", mapToCrashlyticsUserId(null))
    }

    private companion object {
        const val USER_ID = "user-42"
    }
}
