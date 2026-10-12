package core.testing

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenshotTestDefaultsTest {

    @Test
    fun screenshotPath_withName_pointsToCommittedBaselineFolder() {
        assertEquals("src/desktopTest/screenshots/login_invalid_email.png", screenshotPath(name = "login_invalid_email"))
    }
}
