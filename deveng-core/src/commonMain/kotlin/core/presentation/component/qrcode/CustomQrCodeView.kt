package core.presentation.component.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.presentation.theme.LocalComponentTheme
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * Multiplatform QR code view backed by [QRose](https://github.com/alexzhirkevich/qrose).
 *
 * Renders [data] as a QR code Painter. An optional [overlayContent] is centered on top
 * of the code (typically a logo). The overlay size is controlled by
 * [core.presentation.theme.QrCodeViewTheme.overlayFraction].
 *
 * Colors, overlay sizing, and module shapes come from `LocalComponentTheme.qrCodeView`
 * and can be overridden globally via `AppTheme(componentTheme = ...)` or per-call via
 * the parameters below.
 *
 * @param data Payload to encode (URL, text, etc.).
 * @param modifier Modifier applied to the surrounding Box. Provide a size, e.g. `Modifier.size(280.dp)`.
 * @param contentDescription Accessibility description for the rendered QR code.
 * @param errorCorrectionLevel Reed–Solomon recovery level. Defaults to [QrErrorCorrectionLevel.High]
 *   (~30% damage tolerance) so callers using [overlayContent] are safe by default. Lower it to
 *   [QrErrorCorrectionLevel.Medium] (or [QrErrorCorrectionLevel.Auto]) for a tidier, less dense
 *   code when there is no logo overlay and the code is shown on a clean medium.
 * @param overlayFraction Per-call override for the overlay's share of the QR's width/height
 *   (`0f..1f`). When `null` (default) the value from
 *   [core.presentation.theme.QrCodeViewTheme.overlayFraction] is used. See the theme KDoc for
 *   safe-range guidance — values much above ~0.30 risk unreadable codes even at level High.
 *   Ignored when [overlayContent] is `null`.
 * @param pixelShape Per-call override for the dark module ("pixel") shape. When `null` (default)
 *   the value from `LocalComponentTheme.qrCodeView.pixelShape` is used. Use the factories on
 *   [QrPixelShape.Companion] (e.g. `QrPixelShape.circle()`).
 * @param ballShape Per-call override for the inner finder-eye shape. When `null` (default) the
 *   theme value is used. Factories on [QrBallShape.Companion].
 * @param frameShape Per-call override for the outer finder-eye frame shape. When `null` (default)
 *   the theme value is used. Factories on [QrFrameShape.Companion].
 * @param overlayContent Optional composable rendered centered over the code.
 */
@Composable
fun CustomQrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    errorCorrectionLevel: QrErrorCorrectionLevel = QrErrorCorrectionLevel.High,
    overlayFraction: Float? = null,
    pixelShape: QrPixelShape? = null,
    ballShape: QrBallShape? = null,
    frameShape: QrFrameShape? = null,
    overlayContent: (@Composable () -> Unit)? = null,
) {
    val theme = LocalComponentTheme.current.qrCodeView
    val resolvedOverlayFraction = overlayFraction ?: theme.overlayFraction

    val painter = rememberQrCodePainter(
        data = data,
        shapes = QrShapes(
            darkPixel = pixelShape ?: theme.pixelShape,
            ball = ballShape ?: theme.ballShape,
            frame = frameShape ?: theme.frameShape,
        ),
        colors = QrColors(
            dark = QrBrush.solid(theme.darkColor),
            light = QrBrush.solid(theme.lightColor)
        ),
        errorCorrectionLevel = errorCorrectionLevel
    )

    Box(
        modifier = modifier.background(theme.lightColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
        if (overlayContent != null) {
            Box(
                modifier = Modifier.fillMaxSize(fraction = resolvedOverlayFraction),
                contentAlignment = Alignment.Center
            ) {
                overlayContent()
            }
        }
    }
}
