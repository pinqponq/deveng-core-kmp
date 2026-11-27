package core.util.multiplatform

/**
 * Host / runtime surface (e.g. from app or [MultiPlatformUtils.getPlatformConfig]).
 * [IOS] is for Apple mobile; [NATIVE] remains the value reported by multiplatform config on Kotlin/Native.
 */
enum class Platform {
    ANDROID,
    NATIVE,
    WEB,
    DESKTOP,
    /** Apple mobile (iPhone / iPad); use when the app knows it runs on iOS. */
    IOS,
}