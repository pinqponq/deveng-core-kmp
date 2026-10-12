package core.presentation.component.mediaviewer.internal

/**
 * Seek target and progress math for a video whose duration is known only after its metadata loads.
 * A seek requested before that is kept and applied on the first call that knows the duration.
 */
internal class VideoSeekPosition {
    private var pendingFraction: Float? = null

    fun requestSeek(fraction: Float) {
        pendingFraction = fraction
    }

    /** Playback time to seek to, or null when nothing is pending or [durationSeconds] is still unknown. */
    fun takeSeekTime(durationSeconds: Double): Double? {
        val fraction = pendingFraction ?: return null
        if (!isKnownDuration(durationSeconds)) return null
        pendingFraction = null
        return fraction.coerceIn(0f, 1f).toDouble() * durationSeconds
    }

    /** Elapsed fraction of the video, or null while [durationSeconds] is still unknown. */
    fun progress(currentTimeSeconds: Double, durationSeconds: Double): Float? {
        if (!isKnownDuration(durationSeconds)) return null
        return (currentTimeSeconds / durationSeconds).toFloat().coerceIn(0f, 1f)
    }

    private fun isKnownDuration(durationSeconds: Double): Boolean = !durationSeconds.isNaN() && durationSeconds > 0.0
}
