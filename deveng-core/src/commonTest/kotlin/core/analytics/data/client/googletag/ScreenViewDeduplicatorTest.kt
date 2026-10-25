package core.analytics.data.client.googletag

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenViewDeduplicatorTest {

    private val deduplicator = ScreenViewDeduplicator()

    @Test
    fun shouldReport_withFirstScreen_returnsTrue() {
        assertTrue(deduplicator.shouldReport("Home"))
    }

    @Test
    fun shouldReport_withSameScreenTwice_returnsFalseTheSecondTime() {
        deduplicator.shouldReport("Home")

        assertFalse(deduplicator.shouldReport("Home"))
    }

    @Test
    fun shouldReport_withReturnToEarlierScreen_returnsTrue() {
        deduplicator.shouldReport("Home")
        deduplicator.shouldReport("Settings")

        assertTrue(deduplicator.shouldReport("Home"))
    }

    @Test
    fun shouldReport_withBlankScreenName_returnsFalseAndKeepsTheLastScreen() {
        deduplicator.shouldReport("Home")

        assertFalse(deduplicator.shouldReport(" "))
        assertFalse(deduplicator.shouldReport("Home"))
    }
}
