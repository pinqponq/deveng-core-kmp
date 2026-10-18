package core.push.fake

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class FakeClock(var currentInstant: Instant = Instant.fromEpochMilliseconds(START_EPOCH_MILLISECONDS)) : Clock {
    override fun now(): Instant = currentInstant

    fun advanceBy(duration: Duration) {
        currentInstant += duration
    }

    private companion object {
        const val START_EPOCH_MILLISECONDS = 1_760_000_000_000L
    }
}
