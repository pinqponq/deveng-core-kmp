package core.testing

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenshotTestDefaultsTest {

    @Test
    fun createScreenshotPath_withName_pointsToCommittedBaselineFolder() {
        assertEquals("src/desktopTest/screenshots/login_invalid_email.png", createScreenshotPath(name = "login_invalid_email"))
    }
}
