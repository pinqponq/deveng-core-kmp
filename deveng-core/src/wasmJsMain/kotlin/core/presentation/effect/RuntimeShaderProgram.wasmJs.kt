package core.presentation.effect

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual class RuntimeShaderProgram actual constructor(source: String) {
    private val builder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(source))

    actual fun setFloats(name: String, vararg values: Float) {
        builder.uniform(name, values)
    }

    actual fun toBrush(): Brush = ShaderBrush(builder.makeShader())

    actual fun toRenderEffect(contentName: String): RenderEffect =
        ImageFilter.makeRuntimeShader(builder, contentName, null).asComposeRenderEffect()
}

internal actual fun isRuntimeShaderSupported(): Boolean = true
