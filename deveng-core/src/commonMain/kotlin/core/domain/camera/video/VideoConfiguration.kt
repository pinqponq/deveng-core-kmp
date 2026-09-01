package core.domain.camera.video

import androidx.compose.runtime.Immutable

/**
 * Immutable configuration for video recording sessions.
 *
 * @property quality Resolution and bitrate preset.
 * @property enableAudio Whether to record microphone audio (AAC).
 * @property maxDurationMs Maximum recording duration in milliseconds. 0 means unlimited.
 * @property outputDirectory Absolute path to directory for saving the video file.
 *                           Null uses the platform default (DCIM/CameraK on Android, temp on iOS/Desktop).
 * @property filePrefix Prefix for the generated filename.
 * @property shouldChainNewSegmentAtMaxDuration With [maxDurationMs] > 0, reaching the cap ends the clip
 *                           but not the take: the finished file is reported through
 *                           [core.domain.camera.state.CameraKEvent.RecordingStopped] and a new
 *                           recording starts right away, until
 *                           [core.domain.camera.state.CameraKStateHolder.stopRecording] is called.
 *                           A long take is captured as consecutive clips instead of being cut off.
 *                           Ignored when [maxDurationMs] is 0.
 * @property shouldMirrorFrontLensToMatchPreview Whether a front-lens recording is flipped horizontally
 *                           so playback matches the mirrored selfie preview. Set false when the clip is
 *                           handed to someone other than the person who recorded it — a mirrored selfie
 *                           reads as reversed to every other viewer. Applied by
 *                           [core.domain.camera.controller.CameraController.applyRecordingPostProcessing]
 *                           on hosts that mirror recordings (Android); hosts that already save
 *                           non-mirrored files (iOS) are unaffected either way.
 */
@Immutable
data class VideoConfiguration(
    val quality: VideoQuality = VideoQuality.FHD,
    val enableAudio: Boolean = true,
    val maxDurationMs: Long = 0L,
    val outputDirectory: String? = null,
    val filePrefix: String = "VID",
    val shouldChainNewSegmentAtMaxDuration: Boolean = false,
    val shouldMirrorFrontLensToMatchPreview: Boolean = true,
)
