package core.push.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private const val DEFAULT_MAX_ASK_COUNT = 7
private val DEFAULT_MINIMUM_INTERVAL = 3.days

/**
 * How often the app may show its own request to turn notifications on: a user who declined should
 * meet the request again later, not on every app start.
 */
data class SoftAskPolicy(
    val maxAskCount: Int = DEFAULT_MAX_ASK_COUNT,
    val minimumInterval: Duration = DEFAULT_MINIMUM_INTERVAL,
)
