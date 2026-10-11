package core.ads.fake

import core.analytics.domain.store.ConversionEventStore

class FakeConversionEventStore : ConversionEventStore {

    private val firedEventKeys = mutableSetOf<String>()

    override fun hasFired(eventKey: String): Boolean = eventKey in firedEventKeys

    override fun markFired(eventKey: String) {
        firedEventKeys += eventKey
    }

    override fun clear() {
        firedEventKeys.clear()
    }
}
