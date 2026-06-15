package core.domain.camera.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Default viewfinder overlay for [QrScannerView].
 *
 * Draws a rounded-rectangle cutout in the center of the screen with a semi-transparent scrim
 * around it and four corner brackets on the cutout edge.
 *
 * Replace this with your own composable via [QrScannerView]'s `overlay` slot if you need
 * custom branding, instructions, a scanning animation, or a different shape.
 *
 * @param modifier Optional modifier applied to the underlying `Canvas`.
 * @param scrimColor Color of the darkening layer outside the cutout.
 * @param cornerColor Color of the four corner brackets.
 * @param cutoutSizeFraction Side length of the (square) cutout, as a fraction of the
 *   shorter side of the container. Default `0.7f`.
 */
@Composable
fun DefaultScannerOverlay(
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.55f),
    cornerColor: Color = Color.White,
    cutoutSizeFraction: Float = 0.7f,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val side = min(size.width, size.height) * cutoutSizeFraction
            val cornerRadius = 24.dp.toPx()
            val topLeft = Offset(
                x = (size.width - side) / 2f,
                y = (size.height - side) / 2f,
            )

            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(
                    RoundRect(
                        rect = Rect(topLeft, Size(side, side)),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    ),
                )
            }
            drawPath(path = scrimPath, color = scrimColor)

            val bracketLength = side * 0.12f
            val strokeWidth = 4.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val left = topLeft.x
            val top = topLeft.y
            val right = topLeft.x + side
            val bottom = topLeft.y + side

            drawPath(
                path = Path().apply {
                    moveTo(left, top + bracketLength)
                    lineTo(left, top)
                    lineTo(left + bracketLength, top)
                },
                color = cornerColor,
                style = stroke,
            )
            drawPath(
                path = Path().apply {
                    moveTo(right - bracketLength, top)
                    lineTo(right, top)
                    lineTo(right, top + bracketLength)
                },
                color = cornerColor,
                style = stroke,
            )
            drawPath(
                path = Path().apply {
                    moveTo(left, bottom - bracketLength)
                    lineTo(left, bottom)
                    lineTo(left + bracketLength, bottom)
                },
                color = cornerColor,
                style = stroke,
            )
            drawPath(
                path = Path().apply {
                    moveTo(right - bracketLength, bottom)
                    lineTo(right, bottom)
                    lineTo(right, bottom - bracketLength)
                },
                color = cornerColor,
                style = stroke,
            )
        }
    }
}
