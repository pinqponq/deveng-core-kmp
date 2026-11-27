package core.testing

const val SCREENSHOT_DENSITY = 2f

const val SCREENSHOT_DIRECTORY = "src/desktopTest/screenshots"

fun screenshotPath(name: String): String = "$SCREENSHOT_DIRECTORY/$name.png"
