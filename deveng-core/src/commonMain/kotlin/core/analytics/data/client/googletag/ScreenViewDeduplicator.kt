package core.analytics.data.client.googletag

/** Lets a screen name through once until a different screen name arrives, so a re-composition is not a new view. */
internal class ScreenViewDeduplicator {
    private var lastScreenName: String? = null

    fun shouldReport(screenName: String): Boolean {
        if (screenName.isBlank() || screenName == lastScreenName) return false
        lastScreenName = screenName
        return true
    }
}
