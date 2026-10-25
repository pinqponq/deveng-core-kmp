package core.analytics.data.client.googletag

enum class ConsentChoice(val storageValue: String) {
    GRANTED("granted"),
    DENIED("denied");

    companion object {
        fun fromStorageValue(storageValue: String?): ConsentChoice? =
            entries.firstOrNull { choice -> choice.storageValue == storageValue }

        fun isAnalyticsCookie(cookieName: String): Boolean = cookieName.startsWith(ANALYTICS_COOKIE_PREFIX)

        private const val ANALYTICS_COOKIE_PREFIX = "_ga"
    }
}
