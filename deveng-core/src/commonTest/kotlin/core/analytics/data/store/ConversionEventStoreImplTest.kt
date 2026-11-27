package core.analytics.data.store

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionEventStoreImplTest {

    private val settings = MapSettings()
    private val conversionEventStore = ConversionEventStoreImpl(settings = settings)

    @Test
    fun hasFired_onFreshInstall_returnsFalse() {
        assertFalse(conversionEventStore.hasFired(SIGNUP_COMPLETED))
    }

    @Test
    fun markFired_twoEvents_storesBothUnderExistingKey() {
        conversionEventStore.markFired(SIGNUP_COMPLETED)
        conversionEventStore.markFired(PAIRING_COMPLETED)

        assertTrue(conversionEventStore.hasFired(SIGNUP_COMPLETED))
        assertTrue(conversionEventStore.hasFired(PAIRING_COMPLETED))
        assertEquals("$SIGNUP_COMPLETED,$PAIRING_COMPLETED", settings.getStringOrNull(KEY_FIRED_EVENTS))
    }

    @Test
    fun markFired_sameEventTwice_storesItOnce() {
        conversionEventStore.markFired(SIGNUP_COMPLETED)
        conversionEventStore.markFired(SIGNUP_COMPLETED)

        assertEquals(SIGNUP_COMPLETED, settings.getStringOrNull(KEY_FIRED_EVENTS))
    }

    @Test
    fun hasFired_withValueWrittenByEarlierAppVersion_readsTrimmedEntries() {
        settings.putString(KEY_FIRED_EVENTS, "$SIGNUP_COMPLETED, $PAIRING_COMPLETED,")

        assertTrue(conversionEventStore.hasFired(SIGNUP_COMPLETED))
        assertTrue(conversionEventStore.hasFired(PAIRING_COMPLETED))
        assertFalse(conversionEventStore.hasFired(""))
    }

    @Test
    fun hasFired_withEmptyStoredValue_returnsFalse() {
        settings.putString(KEY_FIRED_EVENTS, "")

        assertFalse(conversionEventStore.hasFired(""))
    }

    @Test
    fun clear_afterMarkFired_removesStoredEvents() {
        conversionEventStore.markFired(SIGNUP_COMPLETED)

        conversionEventStore.clear()

        assertFalse(conversionEventStore.hasFired(SIGNUP_COMPLETED))
        assertNull(settings.getStringOrNull(KEY_FIRED_EVENTS))
    }

    private companion object {
        const val KEY_FIRED_EVENTS = "conversion_fired_events"
        const val SIGNUP_COMPLETED = "SIGNUP_COMPLETED"
        const val PAIRING_COMPLETED = "PAIRING_COMPLETED"
    }
}
