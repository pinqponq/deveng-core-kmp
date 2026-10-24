package core.presentation.effect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.motion.LocalReduceMotion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AtmospherePixelTest {

    private val palette = SwirlPalette(base = Color(0xFF204060), accent = Color(0xFFFFC83D), shade = Color(0xFF7C3AED))

    @Test
    fun crtScreen_withShader_changesTheContentPixels() {
        val plain = render { Box(Modifier.fillMaxSize().background(Color(0xFF808080))) }
        val crt = render { Box(Modifier.fillMaxSize().crtScreen().background(Color(0xFF808080))) }

        assertNotEquals(plain.centerPixel(), crt.centerPixel())
    }

    @Test
    fun crtScreen_withShader_darkensTheCornersMoreThanTheCenter() {
        val crt = render { Box(Modifier.fillMaxSize().crtScreen().background(Color(0xFF808080))) }

        assertTrue(crt.luma(x = 3, y = 3) < crt.luma(x = WIDTH / 2, y = HEIGHT / 2))
    }

    @Test
    fun crtScreen_withoutShader_keepsContentAndDrawsLinesAndVignette() {
        val crt = render(isShaderEnabled = false) { Box(Modifier.fillMaxSize().crtScreen().background(Color(0xFF808080))) }

        val rowsOnLine = (0 until HEIGHT).count { y -> crt.luma(x = WIDTH / 2, y = y) < crt.luma(x = WIDTH / 2, y = 1) - 0.0001f }
        assertTrue(rowsOnLine > 0)
        assertTrue(crt.luma(x = 1, y = HEIGHT / 2 + 1) < crt.luma(x = WIDTH / 2, y = HEIGHT / 2 + 1))
        assertEquals(0f, crt.luma(x = WIDTH / 2, y = 0) - crt.luma(x = WIDTH / 2, y = 0), absoluteTolerance = 0f)
    }

    @Test
    fun crtScreen_withoutShader_leavesGapRowsAtTheContentColor() {
        val crt = render(isShaderEnabled = false) { Box(Modifier.fillMaxSize().crtScreen().background(Color.White)) }

        val middleColumn = (0 until HEIGHT).map { y -> crt.luma(x = WIDTH / 2, y = y) }
        assertTrue(middleColumn.any { luma -> luma < 0.99f })
        assertTrue(middleColumn.any { luma -> luma > 0.99f })
        assertTrue(middleColumn.indices.filter { y -> y % 4 == 0 && y in 1..HEIGHT - 2 }.all { y -> middleColumn[y] < middleColumn[y + 1] })
    }

    @Test
    fun swirlBackground_withShader_paintsVariedOpaquePixels() {
        val image = render { SwirlBackground(palette = palette) }

        val colors = (0 until HEIGHT step 8).flatMap { y -> (0 until WIDTH step 8).map { x -> image.pixel(x, y) } }.toSet()
        assertTrue(colors.size > 10)
        assertTrue(colors.all { color -> color.alpha == 1f })
    }

    @Test
    fun swirlBackground_spinAmountAndContrast_changeThePicture() {
        val plain = render { SwirlBackground(palette = palette) }
        val spun = render { SwirlBackground(palette = palette, spinAmount = 1f) }
        val contrasted = render { SwirlBackground(palette = palette, contrast = 2f) }

        assertNotEquals(plain.centerPixel(), spun.centerPixel())
        assertNotEquals(plain.pixel(10, 10), contrasted.pixel(10, 10))
    }

    @Test
    fun swirlBackground_withoutShader_paintsTheBaseColorAndSoftGlows() {
        val image = render(isShaderEnabled = false) { SwirlBackground(palette = palette) }

        assertNotEquals(palette.base, image.pixel(WIDTH / 2, HEIGHT / 2))
        assertEquals(1f, image.pixel(WIDTH - 1, 0).alpha)
    }

    @Test
    fun swirlBackground_withoutShader_movesTheGlowsWhenMotionIsOn() {
        val still = render(isShaderEnabled = false) { SwirlBackground(palette = palette) }
        val moving = render(isShaderEnabled = false, isReduceMotion = false, advanceMillis = 3000L) { SwirlBackground(palette = palette) }

        assertNotEquals(still.pixel(WIDTH / 4, HEIGHT / 3), moving.pixel(WIDTH / 4, HEIGHT / 3))
    }

    @Test
    fun swirlBackground_withShader_movesWhenMotionIsOn() {
        val still = render { SwirlBackground(palette = palette) }
        val moving = render(isReduceMotion = false, advanceMillis = 20_000L) { SwirlBackground(palette = palette) }

        assertNotEquals(still.samples(), moving.samples())
    }

    @Test
    fun vortexFlash_atTheEnds_drawsNothing() {
        val start = render { Box(Modifier.fillMaxSize().background(Color.Black)) { VortexFlash(progress = 0f, palette = palette) } }
        val end = render { Box(Modifier.fillMaxSize().background(Color.Black)) { VortexFlash(progress = 1f, palette = palette) } }

        assertEquals(Color.Black, start.centerPixel())
        assertEquals(Color.Black, end.centerPixel())
    }

    @Test
    fun vortexFlash_withShader_atThePeak_coversTheScreen() {
        val peak = render { Box(Modifier.fillMaxSize().background(Color.Black)) { VortexFlash(progress = 0.5f, palette = palette) } }

        assertNotEquals(Color.Black, peak.centerPixel())
        assertNotEquals(Color.Black, peak.pixel(5, 5))
    }

    @Test
    fun vortexFlash_withShader_changesBetweenProgressValues() {
        val early = render { Box(Modifier.fillMaxSize().background(Color.Black)) { VortexFlash(progress = 0.3f, palette = palette) } }
        val late = render { Box(Modifier.fillMaxSize().background(Color.Black)) { VortexFlash(progress = 0.7f, palette = palette) } }

        assertNotEquals(early.centerPixel(), late.centerPixel())
    }

    @Test
    fun vortexFlash_withoutShader_tintsWithTheAccentColorAtThePeak() {
        val peak = render(isShaderEnabled = false) {
            Box(Modifier.fillMaxSize().background(Color.Black)) { VortexFlash(progress = 0.5f, palette = palette) }
        }

        val pixel = peak.centerPixel()
        assertEquals(palette.accent.red * 0.6f, pixel.red, absoluteTolerance = 0.02f)
        assertEquals(palette.accent.green * 0.6f, pixel.green, absoluteTolerance = 0.02f)
    }

    private fun render(
        isShaderEnabled: Boolean = true,
        isReduceMotion: Boolean = true,
        advanceMillis: Long = 0L,
        content: @Composable () -> Unit
    ): ImageBitmap {
        lateinit var image: ImageBitmap
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalRuntimeShaderEnabled provides isShaderEnabled,
                    LocalReduceMotion provides isReduceMotion
                ) { content() }
            }
            mainClock.advanceTimeByFrame()
            if (advanceMillis > 0) mainClock.advanceTimeBy(advanceMillis)
            image = onRoot().captureToImage()
        }
        return image
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Color = toPixelMap()[x, y]

    private fun ImageBitmap.samples(): List<Color> = (0 until HEIGHT step 20).flatMap { y -> (0 until WIDTH step 20).map { x -> pixel(x, y) } }

    private fun ImageBitmap.centerPixel(): Color = pixel(WIDTH / 2, HEIGHT / 2)

    private fun ImageBitmap.luma(x: Int, y: Int): Float = pixel(x, y).let { color -> (color.red + color.green + color.blue) / 3f }

    private companion object {
        const val WIDTH = 120
        const val HEIGHT = 200
    }
}
