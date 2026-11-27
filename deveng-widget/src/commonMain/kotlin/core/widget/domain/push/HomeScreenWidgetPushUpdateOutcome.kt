package core.widget.domain.push

sealed interface HomeScreenWidgetPushUpdateOutcome {
    val elapsedMilliseconds: Long

    data class Applied(override val elapsedMilliseconds: Long) : HomeScreenWidgetPushUpdateOutcome

    /** The widget could not be updated inside the push callback; the caller falls back to its background refresh. */
    data class FallbackRequired(
        override val elapsedMilliseconds: Long,
        val updateException: Exception,
    ) : HomeScreenWidgetPushUpdateOutcome
}
