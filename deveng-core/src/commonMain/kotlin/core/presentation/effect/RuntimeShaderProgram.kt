package core.presentation.effect

import androidx.compose.runtime.compositionLocalOf
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

internal class ShaderUniform(val name: String, val values: FloatArray)

internal fun RuntimeShaderProgram.apply(uniforms: List<ShaderUniform>) {
    uniforms.forEach { uniform -> setFloats(uniform.name, *uniform.values) }
}

/** Whether the effects use runtime shaders. Tests and previews can provide false to see the simpler look. */
internal val LocalRuntimeShaderEnabled = compositionLocalOf { isRuntimeShaderSupported() }
