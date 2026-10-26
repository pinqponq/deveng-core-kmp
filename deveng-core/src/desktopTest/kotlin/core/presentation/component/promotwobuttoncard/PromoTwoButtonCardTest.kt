package core.presentation.component.promotwobuttoncard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import core.presentation.theme.AppTheme
import core.presentation.theme.ComponentTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PromoTwoButtonCardTest {

    @Test
    fun clickButtons_whenBothShown_sendsEachClickOnce() = runComposeUiTest {
        var primaryClickCount = 0
        var secondaryClickCount = 0
        setContent {
            AppTheme {
                PromoTwoButtonCard(
                    title = TITLE,
                    body = BODY,
                    primaryButtonLabel = PRIMARY_LABEL,
                    secondaryButtonLabel = SECONDARY_LABEL,
                    onPrimaryClick = { primaryClickCount++ },
                    onSecondaryClick = { secondaryClickCount++ }
                )
            }
        }

        onNodeWithText(PRIMARY_LABEL).performClick()
        onNodeWithText(SECONDARY_LABEL).performClick()

        assertEquals(1, primaryClickCount)
        assertEquals(1, secondaryClickCount)
    }

    @Test
    fun showCard_withCustomPaddingTheme_movesContentByThemeValues() = runComposeUiTest {
        var componentTheme by mutableStateOf(DefaultComponentTheme)
        showCard(componentTheme = { componentTheme })
        val defaultLayout = measureLayout()

        componentTheme = CUSTOM_THEME
        waitForIdle()
        val customLayout = measureLayout()

        val defaultTheme = DefaultComponentTheme.promoTwoButtonCard
        val customTheme = CUSTOM_THEME.promoTwoButtonCard
        assertDpEquals(customTheme.horizontalPadding - defaultTheme.horizontalPadding, customLayout.heroLeft - defaultLayout.heroLeft)
        assertDpEquals(customTheme.verticalPadding - defaultTheme.verticalPadding, customLayout.heroTop - defaultLayout.heroTop)
        assertDpEquals(customTheme.contentSpacing * 2, customLayout.titleTop - customLayout.heroBottom)
        assertDpEquals(
            customTheme.horizontalPadding + customTheme.bodyHorizontalPadding,
            customLayout.bodyLeft - customLayout.cardLeft
        )
        assertDpEquals(customTheme.buttonVerticalPadding * 2, customLayout.primaryButtonExtraHeight)
        assertDpEquals(customTheme.buttonVerticalPadding * 2, customLayout.secondaryButtonExtraHeight)
    }

    private fun ComposeUiTest.showCard(componentTheme: () -> ComponentTheme) {
        setContent {
            AppTheme(darkTheme = false, componentTheme = componentTheme()) {
                Box(modifier = Modifier.width(CARD_WIDTH).testTag(CARD_TAG)) {
                    PromoTwoButtonCard(
                        title = TITLE,
                        body = LONG_BODY,
                        primaryButtonLabel = PRIMARY_LABEL,
                        secondaryButtonLabel = SECONDARY_LABEL,
                        onPrimaryClick = {},
                        onSecondaryClick = {},
                        heroContent = {
                            Box(modifier = Modifier.fillMaxWidth().height(HERO_HEIGHT).testTag(HERO_TAG))
                        }
                    )
                }
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.measureLayout(): CardLayout {
        val cardBounds = onNodeWithTag(CARD_TAG).getUnclippedBoundsInRoot()
        val heroBounds = onNodeWithTag(HERO_TAG).getUnclippedBoundsInRoot()
        return CardLayout(
            cardLeft = cardBounds.left,
            heroLeft = heroBounds.left - cardBounds.left,
            heroTop = heroBounds.top - cardBounds.top,
            heroBottom = heroBounds.bottom,
            titleTop = onNodeWithText(TITLE).getUnclippedBoundsInRoot().top,
            bodyLeft = onNodeWithText(LONG_BODY).getUnclippedBoundsInRoot().left,
            primaryButtonExtraHeight = extraHeightAroundLabel(PRIMARY_LABEL),
            secondaryButtonExtraHeight = extraHeightAroundLabel(SECONDARY_LABEL)
        )
    }

    private fun ComposeUiTest.extraHeightAroundLabel(label: String): Dp {
        val buttonBounds: DpRect = onNodeWithText(label).getUnclippedBoundsInRoot()
        val labelBounds: DpRect = onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
        return buttonBounds.height - labelBounds.height
    }

    private fun assertDpEquals(expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= DP_TOLERANCE, "expected $expected but was $actual")
    }

    private data class CardLayout(
        val cardLeft: Dp,
        val heroLeft: Dp,
        val heroTop: Dp,
        val heroBottom: Dp,
        val titleTop: Dp,
        val bodyLeft: Dp,
        val primaryButtonExtraHeight: Dp,
        val secondaryButtonExtraHeight: Dp
    )

    private companion object {
        const val DP_TOLERANCE = 0.5f
        const val CARD_TAG = "promo_card"
        const val HERO_TAG = "promo_hero"
        const val TITLE = "Stay in touch"
        const val BODY = "Turn on notifications."
        const val LONG_BODY = "Turn on notifications to hear when your partner answers, when a new photo arrives " +
            "and when a reminder you set is due."
        const val PRIMARY_LABEL = "Turn on"
        const val SECONDARY_LABEL = "Not now"
        val HERO_HEIGHT = 10.dp
        val CARD_WIDTH = 320.dp
        val CUSTOM_THEME = DefaultComponentTheme.copy(
            promoTwoButtonCard = DefaultComponentTheme.promoTwoButtonCard.copy(
                horizontalPadding = 40.dp,
                verticalPadding = 50.dp,
                contentSpacing = 12.dp,
                bodyHorizontalPadding = 30.dp,
                buttonVerticalPadding = 22.dp
            )
        )
    }
}
