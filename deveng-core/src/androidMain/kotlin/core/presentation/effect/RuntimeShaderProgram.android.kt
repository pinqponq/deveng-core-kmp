package core.presentation.effect

import android.annotation.SuppressLint
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect

@SuppressLint("NewApi")
internal actual class RuntimeShaderProgram actual constructor(source: String) {
    private val shader = RuntimeShader(source)

    actual fun setFloats(name: String, vararg values: Float) {
        shader.setFloatUniform(name, values)
    }

    actual fun toBrush(): Brush = ShaderBrush(shader)

    actual fun toRenderEffect(contentName: String): RenderEffect =
        AndroidRenderEffect.createRuntimeShaderEffect(shader, contentName).asComposeRenderEffect()
}

internal actual fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
