package core.presentation.component.mediaviewer.zoom

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor

@Immutable
data class ContentTransformation(
    val scale: ScaleFactor = ScaleFactor(1f, 1f),
    val offset: Offset = Offset.Zero,
    val rotationZ: Float = 0f,
) {
    val isIdentity: Boolean
        get() = scale == ScaleFactor(1f, 1f) && offset == Offset.Zero && rotationZ == 0f

    val scaleValue: Float
        get() = (scale.scaleX + scale.scaleY) / 2f

    companion object {
        val Identity: ContentTransformation = ContentTransformation()
    }
}
