package core.presentation.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavTransitionsAnimationTest {

    @Test
    fun navigateForward_midTransition_newScreenComesFromEndAndOldLeavesToStart() = runDesktopComposeUiTest {
        val navigation = setNavigationContent(isReduceMotionEnabled = false)

        navigation.navigateTo(screen = SECOND_SCREEN, isBack = false)
        advanceToMidTransition()

        assertTrue(screenLeftPx(SECOND_SCREEN) > 0f)
        assertTrue(screenLeftPx(FIRST_SCREEN) < 0f)
    }

    @Test
    fun navigateForward_firstFrame_newScreenStartsAQuarterWidthFromEnd() = runDesktopComposeUiTest {
        val navigation = setNavigationContent(isReduceMotionEnabled = false)

        navigation.navigateTo(screen = SECOND_SCREEN, isBack = false)
        mainClock.advanceTimeByFrame()

        val containerWidthPx = onNodeWithTag(FIRST_SCREEN).fetchSemanticsNode().size.width
        val quarterWidthPx = containerWidthPx * MotionTokens.NAVIGATION_SLIDE_FRACTION
        val secondScreenLeftPx = screenLeftPx(SECOND_SCREEN)
        assertTrue(secondScreenLeftPx <= quarterWidthPx + POSITION_TOLERANCE_PX)
        assertTrue(secondScreenLeftPx > quarterWidthPx / 2)
    }

    @Test
    fun navigateBack_midTransition_previousScreenComesFromStartAndCurrentLeavesToEnd() = runDesktopComposeUiTest {
        val navigation = setNavigationContent(isReduceMotionEnabled = false)

        navigation.navigateTo(screen = SECOND_SCREEN, isBack = true)
        advanceToMidTransition()

        assertTrue(screenLeftPx(SECOND_SCREEN) < 0f)
        assertTrue(screenLeftPx(FIRST_SCREEN) > 0f)
    }

    @Test
    fun navigateForward_withReduceMotion_switchesScreensWithoutTransition() = runDesktopComposeUiTest {
        val navigation = setNavigationContent(isReduceMotionEnabled = true)

        navigation.navigateTo(screen = SECOND_SCREEN, isBack = false)
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        onNodeWithTag(FIRST_SCREEN).assertDoesNotExist()
        assertTrue(screenLeftPx(SECOND_SCREEN) == 0f)
    }

    private class Navigation {
        var currentScreen by mutableStateOf(FIRST_SCREEN)
        var isBackNavigation by mutableStateOf(false)

        fun navigateTo(screen: String, isBack: Boolean) {
            isBackNavigation = isBack
            currentScreen = screen
        }
    }

    private fun ComposeUiTest.setNavigationContent(isReduceMotionEnabled: Boolean): Navigation {
        val navigation = Navigation()
        mainClock.autoAdvance = false
        setContent {
            Box(modifier = Modifier.size(CONTAINER_SIZE_DP.dp)) {
                AnimatedContent(
                    targetState = navigation.currentScreen,
                    transitionSpec = {
                        if (navigation.isBackNavigation) {
                            NavTransitions.popEnter(isReduceMotionEnabled) togetherWith
                                NavTransitions.popExit(isReduceMotionEnabled)
                        } else {
                            NavTransitions.enter(isReduceMotionEnabled) togetherWith
                                NavTransitions.exit(isReduceMotionEnabled)
                        }
                    }
                ) { screen ->
                    Box(modifier = Modifier.fillMaxSize().testTag(screen))
                }
            }
        }

        mainClock.advanceTimeByFrame()
        return navigation
    }

    private fun ComposeUiTest.advanceToMidTransition() {
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(MotionTokens.DURATION_MEDIUM_MILLIS / 2L)
    }

    private fun ComposeUiTest.screenLeftPx(screen: String): Float =
        onNodeWithTag(screen).fetchSemanticsNode().positionInRoot.x

    private companion object {
        const val FIRST_SCREEN = "first"
        const val SECOND_SCREEN = "second"
        const val CONTAINER_SIZE_DP = 200
        const val POSITION_TOLERANCE_PX = 1f
    }
}
