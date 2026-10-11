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
        val next = readFiredEvents() + eventKey
        settings.putString(KEY_FIRED_EVENTS, next.joinToString(SEPARATOR))
    }

    override fun clear() {
        settings.remove(KEY_FIRED_EVENTS)
    }

    private fun readFiredEvents(): Set<String> {
        val raw = settings.getStringOrNull(KEY_FIRED_EVENTS) ?: return emptySet()
        if (raw.isEmpty()) return emptySet()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun Settings.getStringOrNull(key: String): String? =
        if (hasKey(key)) getString(key, "") else null
}
