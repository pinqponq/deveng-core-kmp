package core.presentation.component.mediaviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MediaViewerDefaults {
    val BackgroundColor: Color = Color.Black
    val PageSpacing: Dp = 16.dp
    const val BeyondViewportPageCount: Int = 2

    /**
     * Neighbor pages kept alive off-screen in vertical (reels-style) mode. Lower than
     * [BeyondViewportPageCount] because each neighbor is typically a live video player, not a
     * static image — keeping 2 alive on each side means 5 concurrent players.
     */
    const val VerticalBeyondViewportPageCount: Int = 1
    const val DismissThreshold: Float = 0.25f
    const val DismissVelocityThreshold: Float = 1000f
}
