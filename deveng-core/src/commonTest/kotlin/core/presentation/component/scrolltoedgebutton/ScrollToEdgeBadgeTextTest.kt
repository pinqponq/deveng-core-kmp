package core.presentation.component.scrolltoedgebutton

import kotlin.test.Test
import kotlin.test.assertEquals

class ScrollToEdgeBadgeTextTest {

    @Test
    fun badgeText_withSingleDigitCount_showsTheNumber() {
        assertEquals("7", scrollToEdgeBadgeText(count = 7))
    }

    @Test
    fun badgeText_atTheCap_showsTheNumber() {
        assertEquals("999", scrollToEdgeBadgeText(count = 999))
    }

    @Test
    fun badgeText_aboveTheCap_showsCapWithPlus() {
        assertEquals("999+", scrollToEdgeBadgeText(count = 1000))
    }
}
