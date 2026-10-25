package core.presentation.motion

/**
 * Default timing and size values of the reveal motion components ([JuiceState], [FlipCard],
 * [CountUpText]) and of [swipeToReply]. Every component takes these as parameters, so a screen can change them.
 */
object MotionDefaults {
    const val POP_PEAK_SCALE = 1.12f
    const val POP_RISE_MILLIS = 72
    const val POP_SETTLE_MILLIS = 108

    const val FLIP_DURATION_MILLIS = 500
    const val FLIP_CAMERA_DISTANCE = 12f

    const val COUNT_UP_STEP_DELAY_MILLIS = 120L
    const val COUNT_UP_MAX_STEP_COUNT = 12
    const val COUNT_UP_MAX_TOTAL_MILLIS = 1500L

    const val HAPTIC_MIN_INTERVAL_MILLIS = 40L

    const val SWIPE_TO_REPLY_TRIGGER_DISTANCE_DP = 64
    const val SWIPE_TO_REPLY_MAX_DISTANCE_DP = 96
    const val SWIPE_TO_REPLY_RESISTANCE_EXPONENT = 1.5f
    const val SWIPE_TO_REPLY_RETURN_MILLIS = 200
}
