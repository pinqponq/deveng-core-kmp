package core.domain.camera.compose

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Estimates the black letterbox height above 16:9 preview content when the surface is
 * bottom-aligned (e.g. Android [androidx.camera.view.PreviewView] FIT_END), matching
 * [DefaultCameraPreview]'s preview stack. Varies by device and window size.
 */
fun computeCameraLetterboxTopInsetDp(
    overlayWidthPx: Int,
    overlayHeightPx: Int,
    density: Density,
): Dp = with(density) {
    if (overlayWidthPx <= 0 || overlayHeightPx <= 0) return@with 0.dp
    val estimatedPreviewHeightPx = overlayWidthPx * (16f / 9f)
    (overlayHeightPx - estimatedPreviewHeightPx).coerceAtLeast(0f).toDp()
}

/**
 * Top padding so the trailing icon row (height [iconRowHeight]) is vertically centered in the letterbox band.
 * When there is no letterbox, returns [0.dp].
 */
fun computeCameraTopChromeRowPaddingTop(
    letterboxTopInset: Dp,
    iconRowHeight: Dp,
): Dp = ((letterboxTopInset - iconRowHeight) / 2f).coerceAtLeast(0.dp)

/**
 * Top padding for a leading asset shorter than the chrome row (e.g. logo) so its vertical center matches the icon row.
 */
fun computeCameraLeadingLogoTopPadding(
    lensRowPaddingTop: Dp,
    iconRowHeight: Dp,
    logoHeight: Dp,
): Dp = lensRowPaddingTop + (iconRowHeight - logoHeight) / 2f
