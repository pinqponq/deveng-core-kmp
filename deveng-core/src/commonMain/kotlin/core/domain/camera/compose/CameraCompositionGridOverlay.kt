package core.domain.camera.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Rule-of-thirds guide lines over the live camera preview. */
@Composable
fun CameraCompositionGridOverlay(
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White.copy(alpha = 0.45f),
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = Stroke(width = 1.dp.toPx())
        val thirdW = size.width / 3f
        val thirdH = size.height / 3f
        drawLine(
            color = lineColor,
            start = Offset(x = thirdW, y = 0f),
            end = Offset(x = thirdW, y = size.height),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = lineColor,
            start = Offset(x = thirdW * 2f, y = 0f),
            end = Offset(x = thirdW * 2f, y = size.height),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = lineColor,
            start = Offset(x = 0f, y = thirdH),
            end = Offset(x = size.width, y = thirdH),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = lineColor,
            start = Offset(x = 0f, y = thirdH * 2f),
            end = Offset(x = size.width, y = thirdH * 2f),
            strokeWidth = stroke.width,
        )
    }
}
