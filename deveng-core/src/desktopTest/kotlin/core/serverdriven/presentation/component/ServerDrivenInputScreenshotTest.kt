package core.serverdriven.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ServerDrivenInputScreenshotTest {

    @Test
    fun label_matchesBaseline() = captureComponent(
        componentJson = ServerDrivenInputTestLayout.LABEL_JSON,
        name = "server_driven_label"
    )

    @Test
    fun segmented_withSelectedOption_matchesBaseline() = captureComponent(
        componentJson = ServerDrivenInputTestLayout.SEGMENTED_JSON,
        name = "server_driven_segmented"
    )

    @Test
    fun radio_withSelectedOption_matchesBaseline() = captureComponent(
        componentJson = ServerDrivenInputTestLayout.RADIO_JSON,
        name = "server_driven_radio"
    )

    @Test
    fun rating_withThreeStars_matchesBaseline() = captureComponent(
        componentJson = ServerDrivenInputTestLayout.RATING_JSON,
        name = "server_driven_rating"
    )

    @Test
    fun section_withLabelAndDate_matchesBaseline() = captureComponent(
        componentJson = ServerDrivenInputTestLayout.SECTION_JSON,
        name = "server_driven_section"
    )

    @Test
    fun date_withSelectedDate_matchesBaseline() = captureComponent(
        componentJson = ServerDrivenInputTestLayout.DATE_JSON,
        name = "server_driven_date"
    )

    private fun captureComponent(
        componentJson: String,
        name: String
    ) = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = ServerDrivenInputTestLayout.layoutOf(componentJson))

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    ServerDrivenScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        engine = engine,
                        onComponentEvent = {}
                    )
                }
            }
        }

        waitForIdle()

        onRoot().captureRoboImage(filePath = screenshotPath(name = name))
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 1000
        const val CANVAS_HEIGHT_PX = 700
    }
}
