package core.widget.domain.push

sealed interface HomeScreenWidgetPushUpdateOutcome {
    val elapsedMillis: Long

    data class Applied(override val elapsedMillis: Long) : HomeScreenWidgetPushUpdateOutcome

    /** The widget could not be updated inside the push callback; the caller falls back to its background refresh. */
    data class FallbackRequired(
        override val elapsedMillis: Long,
        val updateException: Exception,
    ) : HomeScreenWidgetPushUpdateOutcome
}
