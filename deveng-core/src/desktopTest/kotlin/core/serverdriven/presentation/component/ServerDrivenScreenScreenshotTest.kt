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
import core.serverdriven.presentation.model.ComponentEvent
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenScreenshotTest {

    @Test
    fun everyComponentType_withExpandedShutterAndLockedDropdown_matchesBaseline() =
        runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
            val engine = ServerDrivenEngine()
            engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)
            runBlocking {
                engine.handleEvent(ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID))
            }

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

            onRoot().captureRoboImage(filePath = screenshotPath(name = "server_driven_screen_every_component"))
        }

    private companion object {
        const val CANVAS_WIDTH_PX = 1000
        const val CANVAS_HEIGHT_PX = 1400
    }
}
