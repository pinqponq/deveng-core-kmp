package com.deveng.calf.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

/**
 * Settings for the file picker dialog.
 *
 * Common properties ([title], [initialDirectory]) are available on all platforms.
 * Desktop adds a [parentWindow][FilePickerSettings] overload in its source set.
 *
 * Create instances using the [FilePickerSettings] factory function or
 * [rememberFilePickerSettings] composable:
 * ```
 * // commonMain
 * val settings = FilePickerSettings(
 *     title = "Pick an image",
 *     initialDirectory = "/home/user/Pictures",
 * )
 *
 * // or inside a @Composable
 * val settings = rememberFilePickerSettings(
 *     title = "Pick an image",
 *     initialDirectory = "/home/user/Pictures",
 * )
 * ```
 */
@Immutable
interface FilePickerSettings {
    val title: String?
    val initialDirectory: String?

    /**
     * Controls how the iOS photo picker returns image assets.
     *
     * - [ImageRepresentationMode.Compatible] (default): transcodes HEIC → JPEG for
     *   compatibility with Compose/Skia which cannot decode HEIC.
     * - [ImageRepresentationMode.Current]: returns the original format (HEIC, RAW, etc.).
     *
     * This setting only affects iOS; it is ignored on other platforms.
     */
    val imageRepresentationMode: ImageRepresentationMode
        get() = ImageRepresentationMode.Compatible

    /**
     * Maximum size, in bytes, a picked file may be before the picker returns it without making a
     * local copy. On iOS the photo picker otherwise copies the whole asset to a temp file before
     * the caller can inspect it; when the exported asset exceeds this limit, the picker skips that
     * copy and returns a lightweight descriptor carrying only the size, so the caller can reject it
     * without materializing a huge file. `null` (default) keeps the always-copy behavior.
     *
     * This setting only affects iOS; it is ignored on other platforms.
     */
    val maxFileSizeBytes: Long?
        get() = null
}

/**
 * Controls how the iOS photo picker returns image asset representations.
 * Ignored on non-iOS platforms.
 */
@Immutable
enum class ImageRepresentationMode {
    /**
     * Returns assets in a compatible format (e.g. HEIC → JPEG).
     * Recommended when displaying images with Compose/Skia, which cannot decode HEIC.
     * This is the default.
     */
    Compatible,

    /**
     * Returns assets in their original format (e.g. HEIC, RAW).
     * Use this when you need the original quality/format and handle decoding yourself.
     */
    Current,
}

internal data class FilePickerSettingsImpl(
    override val title: String? = null,
    override val initialDirectory: String? = null,
    override val imageRepresentationMode: ImageRepresentationMode = ImageRepresentationMode.Compatible,
    override val maxFileSizeBytes: Long? = null,
) : FilePickerSettings

/**
 * Creates a [FilePickerSettings] with the given properties.
 *
 * On Desktop, use the overload that also accepts `parentWindow`.
 *
 * @param imageRepresentationMode Controls whether the iOS photo picker transcodes assets
 * to a compatible format (JPEG) or returns the original (HEIC). Ignored on other platforms.
 */
fun FilePickerSettings(
    title: String? = null,
    initialDirectory: String? = null,
    imageRepresentationMode: ImageRepresentationMode = ImageRepresentationMode.Compatible,
    maxFileSizeBytes: Long? = null,
): FilePickerSettings = FilePickerSettingsImpl(
    title = title,
    initialDirectory = initialDirectory,
    imageRepresentationMode = imageRepresentationMode,
    maxFileSizeBytes = maxFileSizeBytes,
)

/**
 * Default [FilePickerSettings] with no customization.
 */
fun defaultFilePickerSettings(): FilePickerSettings = FilePickerSettings()

/**
 * Remembers a [FilePickerSettings] instance that updates when [title] or [initialDirectory] change.
 *
 * On Desktop, use the overload that also accepts `parentWindow`.
 */
@Composable
fun rememberFilePickerSettings(
    title: String? = null,
    initialDirectory: String? = null,
    imageRepresentationMode: ImageRepresentationMode = ImageRepresentationMode.Compatible,
    maxFileSizeBytes: Long? = null,
): FilePickerSettings = remember(title, initialDirectory, imageRepresentationMode, maxFileSizeBytes) {
    FilePickerSettings(
        title = title,
        initialDirectory = initialDirectory,
        imageRepresentationMode = imageRepresentationMode,
        maxFileSizeBytes = maxFileSizeBytes,
    )
}
