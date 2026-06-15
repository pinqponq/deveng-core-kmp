package core.domain.camera.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import core.domain.camera.controller.CameraController

/**
 * WASM/JS live camera preview.
 *
 * The live feed is a DOM `<video>` element mounted on `document.documentElement` behind the
 * Compose canvas at `z-index: -1`, owned by [CameraController]. To reveal it, this
 * composable draws a `BlendMode.Clear` rectangle across its bounds, erasing Skia's
 * hardcoded opaque-white pixels in that region so the browser-composited DOM video shows
 * through. Compose overlays drawn on top of `CameraPreviewView` (viewfinder brackets, back
 * button, capture buttons, etc.) render as normal — the last sibling in a `Box` paints
 * last and overwrites the cleared pixels wherever the overlay covers them.
 *
 * Technique reference: the `BlendMode.Clear` workaround for Skiko's missing canvas
 * transparency API, discussed in the Kotlin Slack #multiplatform channel and tracked
 * upstream as [skiko#949](https://github.com/JetBrains/skiko/issues/949).
 *
 * Cost: zero per-frame Kotlin/Wasm work. The browser composites the `<video>` natively
 * (the same path it takes for every `<video>` element on the web), so preview rendering is
 * essentially free regardless of resolution.
 *
 * **Caveat:** if an ancestor introduces an offscreen layer (e.g. `Modifier.alpha(< 1f)`,
 * `Modifier.graphicsLayer { ... }`, or `clipToBounds` under certain combinations), the
 * `Clear` blend is confined to that layer instead of writing through to the root Skia
 * canvas. Avoid such modifiers on `CameraPreviewView` and its ancestors on WASM.
 */
@Composable
actual fun CameraPreviewView(controller: CameraController, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Transparent,
                blendMode = BlendMode.Clear,
            )
        }
    }
}
