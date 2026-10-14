package core.util.multiplatform

/**
 * How a [MultiPlatformUtils.shareTextWithResult] call ended.
 *
 * @param outcome what the platform reported once the share sheet closed.
 * @param targetApp the app or share extension the user picked (an iOS activity type such as
 * `com.apple.UIKit.activity.Message`, or an Android package name), when the platform exposes it.
 */
data class ShareTextResult(
    val outcome: ShareTextOutcome,
    val targetApp: String? = null,
)

/** What the platform reported once the share sheet closed. */
enum class ShareTextOutcome {
    /** iOS: the picked activity finished and reported success. */
    SHARED,

    /** Android: the user picked a target app. Android does not report whether the text was sent. */
    TARGET_SELECTED,

    /**
     * The share sheet closed without a target. On Android this also covers the chooser's own
     * actions (Copy, Nearby Share), which do not report a picked component.
     */
    DISMISSED,

    /** Desktop and web: there is no share sheet, so the text was copied to the clipboard. */
    COPIED_TO_CLIPBOARD,

    /** Nothing was shown: the text was blank or there was no window to present from. */
    NOT_SHOWN,
}
