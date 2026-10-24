package core.presentation.effect

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect

/** A compiled AGSL (Android) or SkSL (other targets) program. Create it only when [isRuntimeShaderSupported]. */
internal expect class RuntimeShaderProgram(source: String) {
    fun setFloats(name: String, vararg values: Float)

    fun toBrush(): Brush

    /** Runs the program over the layer it is applied to; the layer arrives as the shader named [contentName]. */
    fun toRenderEffect(contentName: String): RenderEffect
}

/** False on Android below 13, where callers draw a simpler look instead. */
internal expect fun isRuntimeShaderSupported(): Boolean
