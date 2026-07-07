package core.domain.camera.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineStart
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import core.domain.camera.controller.CameraController
import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKStateHolder
import core.domain.camera.state.CameraUIState
import core.domain.camera.video.VideoCaptureResult
import core.domain.camera.video.VideoConfiguration
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource
import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.FlashMode
import core.domain.camera.result.ImageCaptureResult
import core.domain.camera.ui.CameraIcons
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreRegularTextStyle
import core.presentation.theme.LocalComponentTheme
import core.util.multiplatform.Platform
import kotlinx.coroutines.launch
import kotlin.math.max

private enum class CameraCaptureMode { Photo, Video }

/** AE compensation steps when moon boost is on (from baseline); larger than 2 for a visible delta on preview. */
private const val ExposureBoostSteps = 6

/** Half of [debouncedCombinedClickable] default (600ms) — top camera chrome (flash / moon) feels more responsive. */
private const val CameraChromeClickDebounceMillis = 300L

private fun computeTopChromeIconCount(
    cameraLens: CameraLens,
    showMoonButton: Boolean,
): Int {
    var count = 2 // composition grid + switch camera (always)
    if (cameraLens != CameraLens.FRONT) {
        count += 1 // flash
    }
    if (showMoonButton) {
        count += 1
    }
    return count
}

/**
 * When false, the flash / low-light / switch row is not composed (hidden from UI only).
 * When true, that row is composed next to [DefaultCameraPreviewContent] in the root [Box] (not inside the clipped preview stack).
 */
private const val ShowCameraPreviewTopLensChromeUi = true

/** Fill alpha for [TapToFocusExclusionDebugOverlay] when [showTapToFocusExclusionDebugOverlay] is true (very subtle red). */
private const val TapToFocusExclusionDebugOverlayAlpha = 0.09f

/** Tight halo around controls: only taps clearly next to chrome suppress tap-to-focus. */
private val TapToFocusChromeClearanceDp = 12.dp

/** Top-start / top-end radius for the live preview (card-style UI; bottom corners stay square). */
private val CameraPreviewTopCornerRadius = 28.dp

/** True: exposure slider to the end (right in LTR) of the focus ring; false: below the ring (Android-style). */
private fun Platform.exposureSliderToEndOfRing(): Boolean =
    this == Platform.IOS || this == Platform.NATIVE

private fun buildTapToFocusExclusionRects(
    overlayW: Int,
    overlayH: Int,
    density: Density,
    topChromeRowPaddingTop: Dp,
    topChromeIconCount: Int,
    iconButtonDp: Dp,
    includeTopTrailingChromeExclusion: Boolean = true,
): List<Rect> {
    if (overlayW <= 0 || overlayH <= 0) return emptyList()
    val w = overlayW.toFloat()
    val h = overlayH.toFloat()
    val c = with(density) { TapToFocusChromeClearanceDp.toPx() }

    val topChromeRect: Rect? = if (includeTopTrailingChromeExclusion) {
        val endPad = with(density) { 8.dp.toPx() }
        val topPad = with(density) { topChromeRowPaddingTop.toPx() }
        val btn = with(density) { iconButtonDp.toPx() }
        val gap = with(density) { 8.dp.toPx() }
        val rowW = topChromeIconCount * btn + max(0, topChromeIconCount - 1) * gap
        Rect(
            left = w - endPad - rowW - c,
            top = topPad - c,
            right = w,
            bottom = topPad + btn + c,
        )
    } else {
        null
    }

    val bottomPad = with(density) { 20.dp.toPx() }
    val chipH = with(density) { 40.dp.toPx() }
    val spacer = with(density) { 8.dp.toPx() }
    val mainH = with(density) { 72.dp.toPx() }
    val yMainBottom = h - bottomPad
    val yMainTop = yMainBottom - mainH
    val yChipBottom = yMainTop - spacer
    val yChipTop = yChipBottom - chipH
    val chipHalfW = with(density) { 100.dp.toPx() }
    val chipRow = Rect(
        left = w / 2f - chipHalfW - c,
        top = yChipTop - c,
        right = w / 2f + chipHalfW + c,
        bottom = yChipBottom + c,
    )

    val third = w / 3f
    val gallerySpan = with(density) { 130.dp.toPx() }
    val shutterHalf = with(density) { 38.dp.toPx() }
    val modeW = with(density) { 125.dp.toPx() }
    val modeLeft = third * 2f - with(density) { 4.dp.toPx() }

    val mainTop = yMainTop - c
    val mainBottom = yMainBottom + c
    val galleryRow = Rect(
        left = third - gallerySpan - c,
        top = mainTop,
        right = third + c,
        bottom = mainBottom,
    )
    val shutterRow = Rect(
        left = w / 2f - shutterHalf - c,
        top = mainTop,
        right = w / 2f + shutterHalf + c,
        bottom = mainBottom,
    )
    val modeRow = Rect(
        left = modeLeft - c,
        top = mainTop,
        right = modeLeft + modeW + c,
        bottom = mainBottom,
    )

    return listOfNotNull(topChromeRect, chipRow, galleryRow, shutterRow, modeRow)
}

/**
 * Mirrors [DefaultCameraPreview] top-trailing icon row and compact bottom control hit areas
 * (zoom pill center, gallery cluster, shutter, Photo/Video) so the rest of the preview still focuses.
 */
private fun suppressTapToFocusNearDefaultCameraChrome(
    nx: Float,
    ny: Float,
    overlayW: Int,
    overlayH: Int,
    density: Density,
    topChromeRowPaddingTop: Dp,
    topChromeIconCount: Int,
    iconButtonDp: Dp,
    includeTopTrailingChromeExclusion: Boolean = true,
): Boolean {
    if (overlayW <= 0 || overlayH <= 0) return false
    val p = Offset(x = nx * overlayW, y = ny * overlayH)
    return buildTapToFocusExclusionRects(
        overlayW = overlayW,
        overlayH = overlayH,
        density = density,
        topChromeRowPaddingTop = topChromeRowPaddingTop,
        topChromeIconCount = topChromeIconCount,
        iconButtonDp = iconButtonDp,
        includeTopTrailingChromeExclusion = includeTopTrailingChromeExclusion,
    ).any { it.contains(p) }
}

@Composable
private fun TapToFocusExclusionDebugOverlay(
    overlayW: Int,
    overlayH: Int,
    density: Density,
    topChromeRowPaddingTop: Dp,
    topChromeIconCount: Int,
    iconButtonDp: Dp,
    includeTopTrailingChromeExclusion: Boolean,
    fillAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val rects = remember(
        overlayW,
        overlayH,
        topChromeRowPaddingTop,
        topChromeIconCount,
        iconButtonDp,
        density,
        includeTopTrailingChromeExclusion,
    ) {
        buildTapToFocusExclusionRects(
            overlayW = overlayW,
            overlayH = overlayH,
            density = density,
            topChromeRowPaddingTop = topChromeRowPaddingTop,
            topChromeIconCount = topChromeIconCount,
            iconButtonDp = iconButtonDp,
            includeTopTrailingChromeExclusion = includeTopTrailingChromeExclusion,
        )
    }
    Canvas(modifier = modifier) {
        val fill = Color.Red.copy(alpha = fillAlpha)
        for (r in rects) {
            drawRect(
                color = fill,
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
            )
        }
    }
}

private fun formatRecordingDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secondsStr = seconds.toString().padStart(2, '0')
    return "$minutes:$secondsStr"
}

/** When [maxDurationMs] > 0, shows `elapsed / max`; otherwise elapsed only. */
private fun formatRecordingProgress(elapsedMs: Long, maxDurationMs: Long): String {
    val elapsed = formatRecordingDuration(elapsedMs)
    if (maxDurationMs <= 0L) return elapsed
    return "$elapsed / ${formatRecordingDuration(maxDurationMs)}"
}

/**
 * Default camera preview with built-in controls: flash / low-light / switch (root overlay), zoom chips,
 * gallery button, and capture button. Use this when you want a ready-to-use camera UI.
 *
 * @param controller The camera controller from [CameraKState.Ready].
 * @param onImageCaptured Callback when a photo is taken (file or error). The app handles the result.
 * @param onGalleryClick Optional callback when the gallery button is tapped (e.g. open gallery).
 * @param onLastPhotoClick Optional callback when the thumbnail is tapped; receives the [ImageBitmap] (initial or last captured).
 * @param initialThumbnailBitmap Optional bitmap to show as thumbnail when opening the camera (e.g. last photo). Replaced by captured photo when user takes a picture.
 * @param thumbnailTopEndContent Slot for content placed at the top-end of the thumbnail (e.g. a count badge). Design is fully controlled by the caller.
 * @param stateHolder Optional [CameraKStateHolder] from [rememberCameraKState] (via onHolder). When set, shows Photo/Video tab selection and the center button acts as capture in Photo mode or start/stop in Video mode.
 * @param maxVideoRecordingDurationMs Maximum video length in milliseconds when recording via [stateHolder]. `0` means unlimited (see [VideoConfiguration.maxDurationMs]). When greater than zero, the recording timer shows `elapsed / max` (e.g. `0:45 / 1:30`).
 * @param onRecordingStopped Optional callback when a video recording stops (success or error). Use it to load a first-frame thumbnail and pass it as [lastRecordedVideoThumbnail].
 * @param lastRecordedVideoThumbnail Optional bitmap to show as thumbnail for the last recorded video (e.g. first frame). Shown when the last capture was video; replaced when user takes a photo.
 * @param showTapToFocusExclusionDebugOverlay When true, draws a very faint red overlay on regions where tap-to-focus is suppressed (for tuning/debug).
 * @param hostPlatform Host OS for tap-to-focus exposure slider placement: [Platform.IOS] or [Platform.NATIVE] places the slider to the right of the reticle; [Platform.ANDROID] keeps it below the ring (also used for [Platform.WEB] and [Platform.DESKTOP]). The app must pass the correct value; core does not detect the platform.
 * @param thumbnailSaveInProgress When true, shows a progress indicator on the last-capture thumbnail and blocks thumbnail taps (e.g. while persisting to disk). Thumbnail is also blocked for the in-flight interval from shutter until [onImageCaptured] returns.
 * @param onPhotoCaptureEngaged Invoked after the still-capture request is queued (before [onImageCaptured]) — use to flip app-level “saving” UI and avoid thumbnail races.
 * @param onPhotoCaptureFailed Invoked when still capture throws before [onImageCaptured] runs; pair with [onPhotoCaptureEngaged] to clear app state.
 * @param showLastCaptureThumbnail When false, the last-capture thumbnail (and the gallery-icon fallback shown when no thumbnail exists yet) is not rendered — for capture flows where reviewing/re-opening a previous frame doesn't apply (e.g. single-shot / view-once capture).
 * @param singleCaptureModeEnabled When true, only one capture — a photo OR a video recording — is allowed per composition: the shutter is disabled after a photo is taken or a recording starts (an in-progress recording can still be stopped), and Photo/Video mode switching is disabled once that slot is claimed. To reset, recompose with a fresh key/controller (e.g. leaving and re-entering the camera screen).
 * @param modifier Modifier for the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultCameraPreview(
    controller: CameraController,
    onImageCaptured: (ImageCaptureResult) -> Unit,
    onGalleryClick: (() -> Unit)? = null,
    onLastPhotoClick: ((ImageBitmap) -> Unit)? = null,
    initialThumbnailBitmap: ImageBitmap? = null,
    thumbnailSaveInProgress: Boolean = false,
    onPhotoCaptureEngaged: () -> Unit = {},
    onPhotoCaptureFailed: () -> Unit = {},
    thumbnailTopEndContent: @Composable () -> Unit = {},
    showLastCaptureThumbnail: Boolean = true,
    singleCaptureModeEnabled: Boolean = false,
    stateHolder: CameraKStateHolder? = null,
    maxVideoRecordingDurationMs: Long = 0L,
    onRecordingStarted: (() -> Unit)? = null,
    onRecordingStopped: ((VideoCaptureResult) -> Unit)? = null,
    lastRecordedVideoThumbnail: ImageBitmap? = null,
    showTapToFocusExclusionDebugOverlay: Boolean = false,
    hostPlatform: Platform = Platform.ANDROID,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    /** True from photo shutter pipeline until [onImageCaptured] returns; avoids thumbnail taps before [thumbnailSaveInProgress] can flip. */
    var awaitingThumbnailUnlockAfterCapture by remember { mutableStateOf(false) }
    val thumbnailBusy = thumbnailSaveInProgress || awaitingThumbnailUnlockAfterCapture
    val recordingUiState by (stateHolder?.uiState?.collectAsState(CameraUIState())
        ?: remember { mutableStateOf(CameraUIState()) })
    val isRecordingVideo = recordingUiState.isRecording
    val lensAndZoomControlsEnabled = !isRecordingVideo
    val zoomLevelState = remember { mutableStateOf(1f) }
    var zoomLevel by zoomLevelState
    val maxZoomState = remember { mutableStateOf(1f) }
    var maxZoom by maxZoomState
    var currentFlashMode by remember { mutableStateOf(FlashMode.OFF) }
    /** Tracked in Compose so flash visibility updates when lens changes (controller alone does not trigger recomposition). */
    var currentCameraLens by remember { mutableStateOf(controller.getCameraLens() ?: CameraLens.BACK) }
    var currentCameraDeviceType by remember { mutableStateOf(controller.getPreferredCameraDeviceType()) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var overlaySizePx by remember { mutableStateOf(IntSize.Zero) }
    var brightnessIndex by remember { mutableStateOf(0f) }
    /** Like flash ON/OFF: drives moon tint; ON = boosted exposure from baseline, OFF = default. */
    var isLowLightBoostOn by remember { mutableStateOf(false) }
    var isAdjustingBrightness by remember { mutableStateOf(false) }
    var lastCapturedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    /** True if [lastCapturedBitmap] was taken with the front lens (mirrors thumbnail to match PreviewView). */
    var lastCapturedWithFrontLens by remember { mutableStateOf(false) }
    /**
     * Set once a photo is captured OR a video recording starts when [singleCaptureModeEnabled] is
     * true; disables the shutter/mode-switch for the rest of this composition (an in-progress
     * recording can still be stopped — see [ShutterButton]'s `enabled` computation below).
     */
    var hasClaimedSingleCapture by remember { mutableStateOf(false) }
    var nightModeSupported by remember { mutableStateOf(controller.isNightModeSupported()) }
    var captureMode by remember { mutableStateOf(CameraCaptureMode.Photo) }
    LaunchedEffect(captureMode) {
        val isVideoMode = captureMode == CameraCaptureMode.Video
        controller.setPreviewStabilizationEnabled(isVideoMode)
        controller.applyCaptureModeSessionPreset(isVideoMode)
    }
    var isWideSelfie by remember { mutableStateOf(true) }
    var shutterEffectTrigger by remember { mutableStateOf(0) }
    var showShutterFlash by remember { mutableStateOf(false) }
    var isCompositionGridVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val iconButtonDp = LocalComponentTheme.current.iconButton.buttonSize
    val letterboxTopInsetDp = computeCameraLetterboxTopInsetDp(
        overlayWidthPx = overlaySizePx.width,
        overlayHeightPx = overlaySizePx.height,
        density = density,
    )
    val topChromeRowPaddingTop = computeCameraTopChromeRowPaddingTop(
        letterboxTopInset = letterboxTopInsetDp,
        iconRowHeight = iconButtonDp,
    )
    val topChromeIconCount = computeTopChromeIconCount(
        cameraLens = currentCameraLens,
        showMoonButton = nightModeSupported,
    )

    val onTopChromeToggleFlash: () -> Unit = {
        controller.toggleFlashMode()
        currentFlashMode = controller.getFlashMode() ?: FlashMode.OFF
    }
    val onTopChromeMoonClick: () -> Unit = {
        val newState = !isLowLightBoostOn
        if (controller.isNightModeSupported()) {
            controller.setNightMode(newState)
            isLowLightBoostOn = newState
        } else {
            val (min, max) = controller.getExposureCompensationRange()
            if (min < max) {
                val baseline = 0.coerceIn(min, max)
                if (newState) {
                    val target = (baseline + ExposureBoostSteps).coerceIn(min, max)
                    controller.setExposureCompensationIndex(target)
                    brightnessIndex = target.toFloat()
                    isLowLightBoostOn = true
                } else {
                    controller.setExposureCompensationIndex(baseline)
                    brightnessIndex = baseline.toFloat()
                    isLowLightBoostOn = false
                }
            }
        }
    }
    val onTopChromeSwitchCamera: () -> Unit = {
        if (lensAndZoomControlsEnabled) {
            if (stateHolder != null) stateHolder.toggleCameraLens()
            else controller.toggleCameraLens()
            currentCameraLens = controller.getCameraLens() ?: CameraLens.BACK
            currentCameraDeviceType = controller.getPreferredCameraDeviceType()
            currentFlashMode = controller.getFlashMode() ?: FlashMode.OFF
            isLowLightBoostOn = false
            if (currentCameraLens == CameraLens.FRONT) {
                isWideSelfie = true
                controller.setWideSelfieMode(true)
                controller.setZoom(1f)
            }
            maxZoomState.value = controller.getMaxZoom()
            zoomLevelState.value = controller.getZoom()
        }
    }

    LaunchedEffect(stateHolder) {
        stateHolder?.events?.collect { event ->
            when (event) {
                is CameraKEvent.RecordingStarted -> {
                    onRecordingStarted?.invoke()
                }
                is CameraKEvent.RecordingStopped -> {
                    lastCapturedBitmap = null
                    lastCapturedWithFrontLens = false
                    // Recording never actually produced a video — the single-use slot claimed at
                    // record-start (see onVideoStart below) was never fulfilled; allow a retry.
                    if (singleCaptureModeEnabled && event.result is VideoCaptureResult.Error) {
                        hasClaimedSingleCapture = false
                    }
                    onRecordingStopped?.invoke(event.result)
                }
                else -> { }
            }
        }
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            controller.setExposureCompensationIndex(0)
            brightnessIndex = 0f
            // Night mode is a camera-level mode, not just EV; keep it active on focus tap.
            // EV-based boost (fallback path) is reset because it's tied to a specific brightness target.
            if (!controller.isNightModeSupported() || !isLowLightBoostOn) {
                isLowLightBoostOn = false
            }
        }
    }

    // iOS: native UIKit gesture recognizers handle taps because the first Compose touch
    // is lost to UIKit interop routing. Wire callbacks to update Compose state.
    val isRecordingVideoState = rememberUpdatedState(isRecordingVideo)
    DisposableEffect(
        controller,
        overlaySizePx,
        topChromeIconCount,
        iconButtonDp,
        topChromeRowPaddingTop,
        density,
        isRecordingVideo,
    ) {
        controller.shouldSuppressTapToFocus = { nx, ny ->
            suppressTapToFocusNearDefaultCameraChrome(
                nx = nx,
                ny = ny,
                overlayW = overlaySizePx.width,
                overlayH = overlaySizePx.height,
                density = density,
                topChromeRowPaddingTop = topChromeRowPaddingTop,
                topChromeIconCount = topChromeIconCount,
                iconButtonDp = iconButtonDp,
                includeTopTrailingChromeExclusion = ShowCameraPreviewTopLensChromeUi,
            )
        }
        controller.onPreviewTapListener = { nx, ny ->
            val w = overlaySizePx.width
            val h = overlaySizePx.height
            if (w > 0 && h > 0) {
                focusTapOffset = Offset(x = nx * w, y = ny * h)
            }
        }
        controller.onPreviewDoubleTapListener = {
            if (!isRecordingVideoState.value) {
                if (stateHolder != null) stateHolder.toggleCameraLens()
                else controller.toggleCameraLens()
                currentCameraLens = controller.getCameraLens() ?: CameraLens.BACK
                currentCameraDeviceType = controller.getPreferredCameraDeviceType()
                currentFlashMode = controller.getFlashMode() ?: FlashMode.OFF
                isLowLightBoostOn = false
                if (currentCameraLens == CameraLens.FRONT) {
                    isWideSelfie = true
                    controller.setWideSelfieMode(true)
                    controller.setZoom(1f)
                }
                maxZoomState.value = controller.getMaxZoom()
                zoomLevelState.value = controller.getZoom()
            }
        }
        onDispose {
            controller.shouldSuppressTapToFocus = null
            controller.onPreviewTapListener = null
            controller.onPreviewDoubleTapListener = null
        }
    }

    LaunchedEffect(recordingUiState.cameraLens, recordingUiState.flashMode, stateHolder) {
        if (stateHolder != null) {
            recordingUiState.cameraLens?.let { currentCameraLens = it }
            recordingUiState.flashMode?.let { currentFlashMode = it }
        }
    }

    LaunchedEffect(controller) {
        isLowLightBoostOn = false
        nightModeSupported = false
        currentCameraLens = controller.getCameraLens() ?: CameraLens.BACK
        currentCameraDeviceType = controller.getPreferredCameraDeviceType()
        currentFlashMode = controller.getFlashMode() ?: FlashMode.OFF
        brightnessIndex = controller.getExposureCompensationIndex().toFloat()
        maxZoomState.value = controller.getMaxZoom()
        zoomLevelState.value = controller.getZoom()
        // Re-read values after binding (Android reports maxZoom and extension support only after bind)
        repeat(5) {
            kotlinx.coroutines.delay(400L)
            val newMax = controller.getMaxZoom()
            val newZoom = controller.getZoom()
            if (newMax > 0f) maxZoomState.value = newMax
            if (newZoom > 0f) zoomLevelState.value = newZoom
            nightModeSupported = controller.isNightModeSupported()
        }
        if (currentCameraLens == CameraLens.FRONT) {
            // Default front-camera mode is group.
            isWideSelfie = true
            controller.setWideSelfieMode(true)
            controller.setZoom(1f)
            zoomLevelState.value = controller.getZoom()
        }
    }

    val capturePhotoDuringPreview: () -> Unit = capturePhoto@{
        // Guard + flip BEFORE launching: takePictureToFile() takes 100s of ms, and a rapid
        // second tap landing in that window would otherwise queue a second real capture before
        // the disabled-shutter recomposition ever lands. Setting the flag synchronously here
        // (not after the capture resolves) closes that race down to a single Compose frame.
        if (singleCaptureModeEnabled && hasClaimedSingleCapture) return@capturePhoto
        if (singleCaptureModeEnabled) hasClaimedSingleCapture = true
        awaitingThumbnailUnlockAfterCapture = true
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val mode = controller.getFlashMode() ?: FlashMode.OFF
                val deferShutterUntilAfterCapture =
                    currentCameraLens != CameraLens.FRONT && mode != FlashMode.OFF
                if (!deferShutterUntilAfterCapture) {
                    showShutterFlash = true
                    shutterEffectTrigger++
                }
                // Queue CameraX capture before engagement callbacks / recomposition so the frame
                // matches the tap instant (Android ZSL + no main-queue deferral).
                val result = controller.takePictureToFile()
                onPhotoCaptureEngaged()
                if (result is ImageCaptureResult.Success) {
                    lastCapturedBitmap = result.bitmap
                    lastCapturedWithFrontLens = currentCameraLens == CameraLens.FRONT
                } else {
                    lastCapturedBitmap = null
                    lastCapturedWithFrontLens = false
                    // Capture failed — the single shot was never actually taken; allow a retry.
                    if (singleCaptureModeEnabled) hasClaimedSingleCapture = false
                }
                onImageCaptured(result)
                if (deferShutterUntilAfterCapture) {
                    showShutterFlash = true
                    shutterEffectTrigger++
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                if (singleCaptureModeEnabled) hasClaimedSingleCapture = false
                onPhotoCaptureFailed()
            } finally {
                awaitingThumbnailUnlockAfterCapture = false
            }
        }
    }

    /** Camera surface, gestures, and bottom chrome — same role as “CameraPreview” in a `Box { …; lens row }` split. */
    @Composable
    fun DefaultCameraPreviewContent() {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { overlaySizePx = it },
        ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(
                    RoundedCornerShape(
                        topStart = CameraPreviewTopCornerRadius,
                        topEnd = CameraPreviewTopCornerRadius,
                    ),
                ),
        ) {
            // Preview first so overlay can be drawn on top (important on iOS: native preview must not steal first tap)
            CameraPreviewView(
                controller = controller,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCompositionGridVisible) {
                CameraCompositionGridOverlay(
                    modifier = Modifier.fillMaxSize().zIndex(0.5f),
                )
            }
        }
        if (showTapToFocusExclusionDebugOverlay &&
            overlaySizePx.width > 0 &&
            overlaySizePx.height > 0
        ) {
            TapToFocusExclusionDebugOverlay(
                overlayW = overlaySizePx.width,
                overlayH = overlaySizePx.height,
                density = density,
                topChromeRowPaddingTop = topChromeRowPaddingTop,
                topChromeIconCount = topChromeIconCount,
                iconButtonDp = iconButtonDp,
                includeTopTrailingChromeExclusion = ShowCameraPreviewTopLensChromeUi,
                fillAlpha = TapToFocusExclusionDebugOverlayAlpha,
                modifier = Modifier.fillMaxSize().zIndex(1f),
            )
        }
        // Full-screen pinch/tap for zoom & focus (Android/Desktop). Must stay *below* chrome (zIndex 4f+)
        // or it wins hit-testing and blocks capture / flash / gallery.
        val gestureOverlayZIndex =
            if (hostPlatform == Platform.WEB) 2f else 3f
        CameraZoomGestureOverlay(
            controller = controller,
            modifier = Modifier.fillMaxSize().zIndex(gestureOverlayZIndex),
            onZoomChange = { zoomLevelState.value = it },
            onDoubleTap = {
                if (lensAndZoomControlsEnabled) {
                    if (stateHolder != null) stateHolder.toggleCameraLens()
                    else controller.toggleCameraLens()
                    currentCameraLens = controller.getCameraLens() ?: CameraLens.BACK
                    currentCameraDeviceType = controller.getPreferredCameraDeviceType()
                    currentFlashMode = controller.getFlashMode() ?: FlashMode.OFF
                    isLowLightBoostOn = false
                    if (currentCameraLens == CameraLens.FRONT) {
                        isWideSelfie = true
                        controller.setWideSelfieMode(true)
                        controller.setZoom(1f)
                    }
                    maxZoomState.value = controller.getMaxZoom()
                    zoomLevelState.value = controller.getZoom()
                }
            },
            onFocusPointTapped = { nx, ny ->
                val w = overlaySizePx.width
                val h = overlaySizePx.height
                if (w > 0 && h > 0) {
                    val offset = Offset(x = nx * w, y = ny * h)
                    focusTapOffset = offset
                }
            },
        )
        // Shutter flash effect when a photo is captured
        if (showShutterFlash) {
            ShutterFlashOverlay(
                trigger = shutterEffectTrigger,
                onFlashComplete = { showShutterFlash = false },
                modifier = Modifier.fillMaxSize().zIndex(5f),
            )
        }
        // Snapchat-style focus indicator: circular reticle with pop-in, ripple, bounce, fade-out
        FocusIndicator(
            tapPosition = focusTapOffset,
            onAnimationComplete = { focusTapOffset = null },
            modifier = Modifier.fillMaxSize().zIndex(2f),
            keepVisible = isAdjustingBrightness,
        )
        // Brightness slider: below focus circle (Android-style) or vertical to the right of the ring (iOS-style).
        focusTapOffset?.let { tap ->
            val (min, max) = controller.getExposureCompensationRange()
            if (min != max) {
                val density = LocalDensity.current
                val useVerticalExposureDrag =
                    hostPlatform == Platform.IOS || hostPlatform == Platform.NATIVE
                val brightnessIndexState = rememberUpdatedState(brightnessIndex)
                FocusRingExposureDragOverlay(
                    tap = tap,
                    ringRadiusPx = with(density) { (FocusIndicatorRingDiameter / 2f).toPx() },
                    useVerticalDrag = useVerticalExposureDrag,
                    min = min,
                    max = max,
                    brightnessIndexState = brightnessIndexState,
                    onBrightnessChange = { new ->
                        brightnessIndex = new
                        controller.setExposureCompensationIndex(new.toInt())
                        val baseline = 0.coerceIn(min, max)
                        if (new.toInt() == baseline) {
                            isLowLightBoostOn = false
                        }
                    },
                    onDragStarted = { isAdjustingBrightness = true },
                    onDragFinished = { isAdjustingBrightness = false },
                    onTap = { newTap ->
                        val w = overlaySizePx.width
                        val h = overlaySizePx.height
                        if (w > 0 && h > 0) {
                            val nx = (newTap.x / w).coerceIn(0f, 1f)
                            val ny = (newTap.y / h).coerceIn(0f, 1f)
                            controller.setFocusPoint(nx, ny)
                            focusTapOffset = newTap
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(3.4f),
                )
                with(density) {
                    val circleRadiusPx = 38.dp.toPx()
                    val minTop = 0f
                    val overlayW = overlaySizePx.width.toFloat()
                    val overlayH = overlaySizePx.height.toFloat()

                    val isExposureSliderVertical = hostPlatform.exposureSliderToEndOfRing()

                    @Composable
                    fun ExposureSliderContent(modifier: Modifier = Modifier) {
                        Slider(
                            modifier = modifier,
                            value = brightnessIndex,
                            onValueChange = { new ->
                                isAdjustingBrightness = true
                                brightnessIndex = new
                                controller.setExposureCompensationIndex(new.toInt())
                                val baseline = 0.coerceIn(min, max)
                                if (new.toInt() == baseline) {
                                    isLowLightBoostOn = false
                                }
                            },
                            onValueChangeFinished = { isAdjustingBrightness = false },
                            valueRange = min.toFloat()..max.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White,
                            ),
                            thumb = {
                                Icon(
                                    painter = painterResource(CameraIcons.sun),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(if (isExposureSliderVertical) 14.dp else 18.dp)
                                        .then(
                                            if (isExposureSliderVertical) {
                                                Modifier.graphicsLayer {
                                                    rotationZ = 90f
                                                    transformOrigin = TransformOrigin.Center
                                                }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(
                                        if (isExposureSliderVertical) 1.dp else 1.5.dp,
                                    ),
                                    drawStopIndicator = null,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Transparent,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White,
                                    ),
                                    enabled = true,
                                )
                            },
                        )
                    }

                    if (isExposureSliderVertical) {
                        // Vertical slider: track length matches focus ring diameter (see [FocusIndicatorRingDiameter]).
                        // Outer width must be >= track width before rotation; a narrow box (e.g. 52.dp) clamps the
                        // Slider and collapses the track to almost nothing.
                        val trackLengthDp = FocusIndicatorRingDiameter
                        val trackThicknessDp = 36.dp
                        // Center the control so the vertical track sits just outside the ring (not box-left at ring+gap,
                        // which leaves half the slab width as empty air between ring and track).
                        val horizontalPad = 0.dp
                        // Gap from ring outer edge to vertical track center.
                        val gapRingToTrackCenterPx = 8.dp.toPx()
                        val slabW = trackLengthDp + horizontalPad * 2
                        val slabH = trackLengthDp + 24.dp
                        val slabWpx = slabW.toPx()
                        val slabHpx = slabH.toPx()
                        val maxLeft = (overlayW - slabWpx).coerceAtLeast(0f)
                        val maxTop = (overlayH - slabHpx).coerceAtLeast(minTop)
                        val trackCenterX = tap.x + circleRadiusPx + gapRingToTrackCenterPx
                        val leftPx = (trackCenterX - slabWpx / 2f).coerceIn(0f, maxLeft)
                        val topPx = (tap.y - slabHpx / 2f).coerceIn(minTop, maxTop)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .zIndex(4f)
                                .absoluteOffset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                                .width(slabW)
                                .height(slabH)
                                .padding(horizontal = horizontalPad, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ExposureSliderContent(
                                modifier = Modifier
                                    .width(trackLengthDp)
                                    .height(trackThicknessDp)
                                    .graphicsLayer {
                                        rotationZ = -90f
                                        transformOrigin = TransformOrigin.Center
                                    },
                            )
                        }
                    } else {
                        val sliderWidthPx = 100.dp.toPx()
                        val sliderBlockHeightPx = 40.dp.toPx()
                        val maxTop = (overlayH - sliderBlockHeightPx).coerceAtLeast(minTop)
                        val maxLeftForSlider = (overlayW - sliderWidthPx).coerceAtLeast(0f)
                        val gapBelowPx = (-9).dp.toPx()
                        val leftPx = (tap.x - sliderWidthPx / 2f).coerceIn(0f, maxLeftForSlider)
                        val topPx = (tap.y + circleRadiusPx + gapBelowPx).coerceIn(minTop, maxTop)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .zIndex(4f)
                                .absoluteOffset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                                .width(100.dp)
                                .padding(horizontal = 8.dp),
                        ) {
                            ExposureSliderContent(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        val bottomChromeZIndex =
            if (hostPlatform == Platform.WEB) 24f else 4f
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(bottomChromeZIndex)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (currentCameraLens == CameraLens.FRONT) {
                FrontCameraModeChips(
                    enabled = lensAndZoomControlsEnabled,
                    isWideSelfie = isWideSelfie,
                    onSelect = { wide ->
                        if (wide == isWideSelfie) return@FrontCameraModeChips
                        isWideSelfie = wide
                        controller.setWideSelfieMode(wide)
                        val targetFrontZoom = if (wide) 1f else 1.2f
                        val maxZoomRatio = maxZoomState.value.coerceAtLeast(1f)
                        controller.setZoom(targetFrontZoom.coerceAtMost(maxZoomRatio))
                        zoomLevelState.value = controller.getZoom()
                    },
                )
            } else {
                BackCameraZoomChips(
                    enabled = lensAndZoomControlsEnabled,
                    zoomLevel = zoomLevel,
                    cameraDeviceType = currentCameraDeviceType,
                    maxZoom = maxZoom,
                    onZoomChange = { level ->
                        if (level <= 0.5f) {
                            // Try seamless logical-camera ultra-wide first (no rebind on supported devices).
                            controller.setZoom(0.5f)
                            val appliedZoom = controller.getZoom()
                            val seamlessUltraWideApplied = appliedZoom < 0.75f
                            if (!seamlessUltraWideApplied) {
                                // Fallback for devices that don't expose minZoom < 1.0
                                controller.setPreferredCameraDeviceType(CameraDeviceType.ULTRA_WIDE)
                                controller.setZoom(1f)
                            }
                        } else {
                            // Keep logical/default camera to avoid unnecessary rebind on 1x/2x taps.
                            if (currentCameraDeviceType == CameraDeviceType.ULTRA_WIDE) {
                                controller.setPreferredCameraDeviceType(CameraDeviceType.DEFAULT)
                            }
                            controller.setZoom(level)
                        }
                        currentCameraDeviceType = controller.getPreferredCameraDeviceType()
                        maxZoomState.value = controller.getMaxZoom()
                        zoomLevelState.value = controller.getZoom()
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (showLastCaptureThumbnail) {
                        val thumbnailBitmap = lastCapturedBitmap ?: lastRecordedVideoThumbnail ?: initialThumbnailBitmap
                        val mirrorThumbnailHorizontally =
                            lastCapturedBitmap != null &&
                                lastCapturedWithFrontLens &&
                                thumbnailBitmap === lastCapturedBitmap
                        thumbnailBitmap?.let { bitmap ->
                            // Top-end square so the count badge meets the frame; other corners rounded (bottom-start = bottom-left in LTR).
                            val thumbShape = RoundedCornerShape(
                                topStart = 8.dp,
                                topEnd = 0.dp,
                                bottomEnd = 8.dp,
                                bottomStart = 8.dp,
                            )
                            // Clip only the image so the count badge is not cut off by [thumbShape] (badge sits in margin).
                            // Clicks: put [clickable] on the image stack and on the badge — some hosts (e.g. web/WASM)
                            // do not deliver taps from [Image] to a parent [clickable] on the outer frame.
                            val openLastCapture: () -> Unit = { onLastPhotoClick?.invoke(bitmap) }
                            val thumbImageInteraction = remember(bitmap) { MutableInteractionSource() }
                            val thumbBadgeInteraction = remember(bitmap, "badge") { MutableInteractionSource() }
                            val thumbImageClickModifier =
                                if (thumbnailBusy || onLastPhotoClick == null) {
                                    Modifier
                                } else {
                                    Modifier.clickable(
                                        interactionSource = thumbImageInteraction,
                                        indication = null,
                                        onClick = openLastCapture,
                                    )
                                }
                            val thumbBadgeClickModifier =
                                if (thumbnailBusy || onLastPhotoClick == null) {
                                    Modifier
                                } else {
                                    Modifier.clickable(
                                        interactionSource = thumbBadgeInteraction,
                                        indication = null,
                                        onClick = openLastCapture,
                                    )
                                }
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 56.dp)
                                    .border(2.dp, Color.White, thumbShape),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(thumbShape)
                                        .then(thumbImageClickModifier),
                                ) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Photo thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (mirrorThumbnailHorizontally) {
                                                    Modifier.graphicsLayer {
                                                        scaleX = -1f
                                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .then(thumbBadgeClickModifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    thumbnailTopEndContent()
                                }
                                if (thumbnailBusy) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(thumbShape)
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        if (thumbnailBitmap == null) {
                            CustomIconButton(
                                icon = CameraIcons.galleryPhotoLibrary,
                                iconDescription = "Gallery",
                                iconTint = Color.White,
                                backgroundColor = Color.Transparent,
                                shadowElevation = 0.dp,
                                onClick = { onGalleryClick?.invoke() ?: Unit },
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    ShutterButton(
                        mode = captureMode,
                        isRecording = recordingUiState.isRecording,
                        recordingDurationMs = recordingUiState.recordingDurationMs,
                        stateHolder = stateHolder,
                        // Disabled once the single-capture slot is claimed (photo taken, or video
                        // recording started) — EXCEPT while actively recording, so the in-progress
                        // recording can still be stopped via this same button.
                        enabled = !(
                            singleCaptureModeEnabled &&
                                hasClaimedSingleCapture &&
                                !recordingUiState.isRecording
                            ),
                        onPhotoCapture = capturePhotoDuringPreview,
                        onVideoStart = {
                            // Claim synchronously at record-start, mirroring capturePhotoDuringPreview's
                            // guard — starting a recording spends the single-use slot immediately.
                            if (singleCaptureModeEnabled) hasClaimedSingleCapture = true
                            stateHolder?.startRecording(
                                VideoConfiguration(maxDurationMs = maxVideoRecordingDurationMs),
                            )
                        },
                        onVideoStop = { stateHolder?.stopRecording() },
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (stateHolder != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            ModeSwitcher(
                                currentMode = captureMode,
                                onModeChange = { newMode ->
                                    captureMode = newMode
                                },
                                enabled = !recordingUiState.isRecording &&
                                    !(singleCaptureModeEnabled && hasClaimedSingleCapture),
                            )
                            Box(
                                modifier = Modifier.heightIn(min = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    captureMode == CameraCaptureMode.Video &&
                                        recordingUiState.isRecording -> {
                                        // Already functionally inert once the slot is claimed at record-start
                                        // (capturePhotoDuringPreview's own guard no-ops the tap) — this only
                                        // adds the matching visual disabled state for clarity.
                                        val inlinePhotoDuringRecordingEnabled =
                                            !(singleCaptureModeEnabled && hasClaimedSingleCapture)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Text(
                                                text = formatRecordingProgress(
                                                    recordingUiState.recordingDurationMs,
                                                    maxVideoRecordingDurationMs,
                                                ),
                                                color = Color.White,
                                                style = CoreRegularTextStyle().copy(fontSize = 13.sp),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color.White, CircleShape)
                                                    .alpha(if (inlinePhotoDuringRecordingEnabled) 1f else 0.45f)
                                                    .clickable(
                                                        enabled = inlinePhotoDuringRecordingEnabled,
                                                        interactionSource = remember {
                                                            MutableInteractionSource()
                                                        },
                                                        indication = null,
                                                        onClick = capturePhotoDuringPreview,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White),
                                                )
                                            }
                                        }
                                    }
                                    recordingUiState.isRecording -> {
                                        Text(
                                            text = formatRecordingProgress(
                                                recordingUiState.recordingDurationMs,
                                                maxVideoRecordingDurationMs,
                                            ),
                                            color = Color.White,
                                            style = CoreRegularTextStyle().copy(fontSize = 16.sp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    /** Flash / low-light / switch row — sibling overlay; apps can mirror this with their own leading (e.g. logo). */
    @Composable
    fun BoxScope.DefaultCameraPreviewLensChromeOverlay() {
        if (ShowCameraPreviewTopLensChromeUi) {
            DefaultCameraPreviewTopLensChromeRow(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(4.5f)
                    .padding(top = topChromeRowPaddingTop, end = 8.dp),
                currentCameraLens = currentCameraLens,
                currentFlashMode = currentFlashMode,
                isLowLightBoostOn = isLowLightBoostOn,
                isCompositionGridVisible = isCompositionGridVisible,
                showMoonButton = nightModeSupported,
                onToggleFlash = onTopChromeToggleFlash,
                onToggleCompositionGrid = { isCompositionGridVisible = !isCompositionGridVisible },
                onMoonClick = onTopChromeMoonClick,
                onSwitchCamera = onTopChromeSwitchCamera,
            )
        }
    }

    // Box { camera preview stack; top lens chrome } — chrome sits outside the clipped preview layer.
    Box(modifier.fillMaxSize()) {
        DefaultCameraPreviewContent()
        DefaultCameraPreviewLensChromeOverlay()
    }
}

@Composable
private fun DefaultCameraPreviewTopLensChromeRow(
    modifier: Modifier = Modifier,
    currentCameraLens: CameraLens,
    currentFlashMode: FlashMode,
    isLowLightBoostOn: Boolean,
    isCompositionGridVisible: Boolean,
    showMoonButton: Boolean,
    onToggleFlash: () -> Unit,
    onToggleCompositionGrid: () -> Unit,
    onMoonClick: () -> Unit,
    onSwitchCamera: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentCameraLens != CameraLens.FRONT) {
            CustomIconButton(
                icon = CameraIcons.flash,
                iconDescription = "Flash",
                iconTint = when (currentFlashMode) {
                    FlashMode.ON, FlashMode.AUTO -> Color.White
                    FlashMode.OFF -> Color.White.copy(alpha = 0.5f)
                },
                backgroundColor = Color.Transparent,
                shadowElevation = 0.dp,
                clickDebounceMillis = CameraChromeClickDebounceMillis,
                onClick = onToggleFlash,
            )
        }
        CustomIconButton(
            icon = CameraIcons.grid,
            iconDescription = "Composition grid",
            iconTint = if (isCompositionGridVisible) {
                Color.White
            } else {
                Color.White.copy(alpha = 0.5f)
            },
            backgroundColor = Color.Transparent,
            shadowElevation = 0.dp,
            clickDebounceMillis = CameraChromeClickDebounceMillis,
            onClick = onToggleCompositionGrid,
        )
        if (showMoonButton) {
            CustomIconButton(
                icon = CameraIcons.moon,
                iconDescription = "Night mode",
                iconTint = if (isLowLightBoostOn) Color.White else Color.White.copy(alpha = 0.5f),
                backgroundColor = Color.Transparent,
                shadowElevation = 0.dp,
                clickDebounceMillis = CameraChromeClickDebounceMillis,
                onClick = onMoonClick,
            )
        }
        CustomIconButton(
            icon = CameraIcons.switchCamera,
            iconDescription = "Switch camera",
            iconTint = Color.White,
            backgroundColor = Color.Transparent,
            shadowElevation = 0.dp,
            onClick = onSwitchCamera,
        )
    }
}

/**
 * Full-screen black flash overlay to indicate photo capture (shutter effect).
 * Animates in quickly, then fades out and calls [onFlashComplete].
 */
@Composable
private fun ShutterFlashOverlay(
    trigger: Int,
    onFlashComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        alpha.snapTo(0f)
        alpha.animateTo(0.75f, animationSpec = tween(durationMillis = 40))
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 120))
        onFlashComplete()
    }

    Box(
        modifier = modifier
            .alpha(alpha.value)
            .background(Color.Black),
    )
}

@Composable
private fun ModeSwitcher(
    currentMode: CameraCaptureMode,
    onModeChange: (CameraCaptureMode) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CameraCaptureMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clickable(enabled = enabled) { onModeChange(mode) },
            ) {
                Text(
                    text = mode.name.uppercase(),
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    style = CoreRegularTextStyle().copy(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (isSelected) 24.dp else 0.dp)
                        .background(
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(1.5.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(
    mode: CameraCaptureMode,
    isRecording: Boolean,
    recordingDurationMs: Long,
    stateHolder: CameraKStateHolder?,
    onPhotoCapture: () -> Unit,
    onVideoStart: () -> Unit,
    onVideoStop: () -> Unit,
    enabled: Boolean = true,
) {
    val isVideoMode = mode == CameraCaptureMode.Video && stateHolder != null
    val outerColor = if (isRecording) Color.Red else Color.White
    val innerColor = if (isVideoMode) Color.Red.copy(alpha = 0.9f) else Color.White
    val innerSize = if (isRecording) 24.dp else 60.dp
    val innerShape = if (isRecording) RoundedCornerShape(8.dp) else CircleShape

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isVideoMode) {
                        if (isRecording) onVideoStop() else onVideoStart()
                    } else {
                        onPhotoCapture()
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(3.dp, outerColor, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(innerShape)
                .background(innerColor)
                .border(
                    width = 2.dp,
                    color = Color.White,
                    shape = innerShape,
                ),
        )
    }
}

@Composable
private fun FrontCameraModeChips(
    isWideSelfie: Boolean,
    onSelect: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.45f)
                .padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(false, true).forEach { wide ->
                val active = wide == isWideSelfie
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable(
                            enabled = enabled,
                            onClick = { onSelect(wide) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (wide) CameraIcons.group else CameraIcons.person,
                        ),
                        contentDescription = if (wide) "Group" else "Person",
                        tint = if (active) Color.White else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Drag on the focus ring to adjust exposure: vertical on iOS/NATIVE, horizontal on Android.
 * Maps drag distance to [min.max] like the sun [Slider]: one full ring diameter ≈ full range
 * (same reference length as the vertical exposure track).
 */
@Composable
private fun FocusRingExposureDragOverlay(
    tap: Offset,
    ringRadiusPx: Float,
    useVerticalDrag: Boolean,
    min: Int,
    max: Int,
    brightnessIndexState: State<Float>,
    onBrightnessChange: (Float) -> Unit,
    onDragStarted: () -> Unit,
    onDragFinished: () -> Unit,
    onTap: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hitExpansionPx = with(density) { 12.dp.toPx() }
    val hitPx = ringRadiusPx * 2f + hitExpansionPx * 2f
    val hitSizeDp = with(density) { hitPx.toDp() }
    val minState = rememberUpdatedState(min)
    val maxState = rememberUpdatedState(max)
    val indexState = rememberUpdatedState(brightnessIndexState.value)
    val trackPx = (ringRadiusPx * 2f).coerceAtLeast(1f)
    val onTapState = rememberUpdatedState(onTap)
    val tapState = rememberUpdatedState(tap)

    Box(
        modifier = modifier
            .absoluteOffset {
                IntOffset(
                    (tap.x - hitPx / 2f).roundToInt(),
                    (tap.y - hitPx / 2f).roundToInt(),
                )
            }
            .size(hitSizeDp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { local ->
                        val origin = tapState.value
                        val absolute = Offset(
                            x = origin.x - hitPx / 2f + local.x,
                            y = origin.y - hitPx / 2f + local.y,
                        )
                        onTapState.value(absolute)
                    },
                )
            }
            .pointerInput(useVerticalDrag, tap, ringRadiusPx, min, max, hitExpansionPx) {
                var current = indexState.value
                detectDragGestures(
                    onDragStart = {
                        val lo = minState.value.toFloat()
                        val hi = maxState.value.toFloat()
                        current = indexState.value.coerceIn(lo, hi)
                        onDragStarted()
                    },
                    onDragEnd = {
                        onDragFinished()
                    },
                    onDragCancel = {
                        onDragFinished()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaPx = if (useVerticalDrag) -dragAmount.y else dragAmount.x
                        val lo = minState.value.toFloat()
                        val hi = maxState.value.toFloat()
                        val span = hi - lo
                        if (span > 0f) {
                            val deltaValue = (deltaPx / trackPx) * span
                            current = (current + deltaValue).coerceIn(lo, hi)
                            onBrightnessChange(current)
                        }
                    },
                )
            },
    )
}

@Composable
private fun BackCameraZoomChips(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    zoomLevel: Float,
    cameraDeviceType: CameraDeviceType,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
) {
    val effectiveZoomLevel = if (zoomLevel < 0.75f || cameraDeviceType == CameraDeviceType.ULTRA_WIDE) 0.5f else zoomLevel
    val chipMaxZoom = 10f.coerceAtMost(maxZoom)
    val stops = buildList {
        add(0.5f)
        add(1f)
        if (maxZoom >= 2f) add(2f)
        if (chipMaxZoom > 2f) add(chipMaxZoom)
    }.distinct()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.45f)
                .padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stops.forEach { stop ->
                val isActive = (effectiveZoomLevel - stop).let { it > -0.15f && it < 0.15f }
                val label = if (stop == stop.toLong().toFloat()) {
                    "${stop.toInt()}x"
                } else {
                    "${(stop * 10).toInt() / 10f}x"
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable(
                            enabled = enabled,
                            onClick = { onZoomChange(stop) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                        style = CoreRegularTextStyle().copy(fontSize = 10.sp),
                    )
                }
            }
        }
    }
}
