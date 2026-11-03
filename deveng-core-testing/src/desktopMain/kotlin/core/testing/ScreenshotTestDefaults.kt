package core.testing

const val PHONE_SCREENSHOT_WIDTH_PX = 1080
const val PHONE_SCREENSHOT_HEIGHT_PX = 2400
const val PHONE_SCREENSHOT_DENSITY = 2.625f
const val LARGE_FONT_SCALE = 1.5f

const val SCREENSHOT_DIRECTORY = "src/desktopTest/screenshots"

fun createScreenshotPath(name: String): String = "$SCREENSHOT_DIRECTORY/$name.png"
