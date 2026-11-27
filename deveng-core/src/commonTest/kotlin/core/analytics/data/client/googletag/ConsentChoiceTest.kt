package core.analytics.data.client.googletag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsentChoiceTest {

    @Test
    fun fromStorageValue_withStoredValues_returnsTheChoice() {
        assertEquals(ConsentChoice.GRANTED, ConsentChoice.fromStorageValue("granted"))
        assertEquals(ConsentChoice.DENIED, ConsentChoice.fromStorageValue("denied"))
    }

    @Test
    fun fromStorageValue_withMissingOrUnknownValue_returnsNull() {
        assertNull(ConsentChoice.fromStorageValue(null))
        assertNull(ConsentChoice.fromStorageValue(""))
        assertNull(ConsentChoice.fromStorageValue("Granted"))
        assertNull(ConsentChoice.fromStorageValue("true"))
    }

    @Test
    fun isAnalyticsCookie_withGoogleAnalyticsCookies_returnsTrue() {
        assertTrue(ConsentChoice.isAnalyticsCookie("_ga"))
        assertTrue(ConsentChoice.isAnalyticsCookie("_ga_ABC123"))
    }

    @Test
    fun isAnalyticsCookie_withOtherCookies_returnsFalse() {
        assertFalse(ConsentChoice.isAnalyticsCookie("session"))
        assertFalse(ConsentChoice.isAnalyticsCookie("x_ga"))
    }
}
