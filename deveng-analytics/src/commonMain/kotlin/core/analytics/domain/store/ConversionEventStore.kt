package core.analytics.domain.store

/**
 * Persists which "fire once" conversion events have already been emitted on this install,
 * so first-time milestones are never double-counted.
 */
interface ConversionEventStore {
    fun hasFired(eventKey: String): Boolean
    fun markFired(eventKey: String)
    fun clear()
}
