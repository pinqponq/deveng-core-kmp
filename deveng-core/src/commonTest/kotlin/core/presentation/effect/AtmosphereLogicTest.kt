package core.presentation.effect

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class AtmosphereLogicTest {

    private val palette = SwirlPalette(
        base = Color(red = 0.1f, green = 0.2f, blue = 0.3f, alpha = 0.4f),
        accent = Color(red = 0.5f, green = 0.6f, blue = 0.7f, alpha = 0.8f),
        shade = Color(red = 0.9f, green = 0.8f, blue = 0.7f, alpha = 0.6f)
    )

    @Test
    fun crtUniforms_carryTheConfigAndScaleScanlinesByHeight() {
        val config = CrtConfig(intensity = 0.1f, noiseAmount = 0.2f, bloomAmount = 0.3f, scanlinesPerHeight = 0.5f, vignette = 0.6f)

        val uniforms = crtUniforms(config = config, width = 200f, height = 400f, time = 7f).associate { it.name to it.values.toList() }

        assertEquals(
            mapOf(
                "resolution" to listOf(200f, 400f),
                "time" to listOf(7f),
                "intensity" to listOf(0.1f),
                "noiseAmount" to listOf(0.2f),
                "bloomAmount" to listOf(0.3f),
                "scanlineCount" to listOf(200f),
                "vignette" to listOf(0.6f)
            ),
            uniforms
        )
    }

    @Test
    fun swirlUniforms_carryEveryValueAndTheThreeColors() {
        val uniforms = swirlUniforms(
            palette = palette, width = 100f, height = 300f, time = 1f, spinTime = 2f, spinAmount = 0.5f, contrast = 1.5f
        ).associate { it.name to it.values.toList() }

        assertEquals(
            mapOf(
                "resolution" to listOf(100f, 300f),
                "time" to listOf(1f),
                "spinTime" to listOf(2f),
                "spinAmount" to listOf(0.5f),
                "contrast" to listOf(1.5f),
                "colour1" to listOf(0.1f, 0.2f, 0.3f, 0.4f),
                "colour2" to listOf(0.5f, 0.6f, 0.7f, 0.8f),
                "colour3" to listOf(0.9f, 0.8f, 0.7f, 0.6f)
            ),
            uniforms.mapValues { (_, values) -> values.map { value -> (value * 100).toInt() / 100f } }
        )
    }

    @Test
    fun vortexStrength_isZeroAtTheEndsAndOneInTheMiddle() {
        assertEquals(0f, vortexStrength(progress = 0f), absoluteTolerance = 0.0001f)
        assertEquals(1f, vortexStrength(progress = 0.5f), absoluteTolerance = 0.0001f)
        assertEquals(0f, vortexStrength(progress = 1f), absoluteTolerance = 0.0001f)
    }

    @Test
    fun vortexStrength_staysInsideTheRangeForOutOfRangeProgress() {
        assertEquals(0f, vortexStrength(progress = -2f), absoluteTolerance = 0.0001f)
        assertEquals(0f, vortexStrength(progress = 3f), absoluteTolerance = 0.0001f)
    }

    @Test
    fun fallbackLineOffsets_startAtZeroAndStepUntilTheHeight() {
        assertContentEquals(listOf(0f, 4f, 8f), fallbackLineOffsets(height = 10f))
    }

    @Test
    fun fallbackLineOffsets_areEmptyForNoHeight() {
        assertContentEquals(emptyList(), fallbackLineOffsets(height = 0f))
    }
}
