package core.analytics.data.store

import com.russhwolf.settings.Settings
import core.analytics.domain.store.ConversionEventStore

private const val KEY_FIRED_EVENTS = "conversion_fired_events"
private const val SEPARATOR = ","

class ConversionEventStoreImpl(
    private val settings: Settings,
) : ConversionEventStore {

    override fun hasFired(eventKey: String): Boolean {
        return eventKey in readFiredEvents()
    }

    override fun markFired(eventKey: String) {
        val updatedFiredEvents = readFiredEvents() + eventKey
        settings.putString(KEY_FIRED_EVENTS, updatedFiredEvents.joinToString(SEPARATOR))
    }

    override fun clear() {
        settings.remove(KEY_FIRED_EVENTS)
    }

    private fun readFiredEvents(): Set<String> {
        val storedEvents = settings.getStringOrNull(KEY_FIRED_EVENTS).orEmpty()
        return storedEvents.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
