package core.presentation.component.promotwobuttoncard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import core.presentation.component.Slot
import core.presentation.theme.LocalComponentTheme
import core.util.debouncedCombinedClickable

/**
 * Promotional card with an optional hero, a title, a body text, a filled primary button and an optional
 * outlined secondary button. Colors, text styles, paddings and the button shadow come from
 * [core.presentation.theme.PromoTwoButtonCardTheme].
 *
 * @param title Title displayed under the hero.
 * @param body Body text displayed under the title.
 * @param primaryButtonLabel Label of the filled primary button.
 * @param onPrimaryClick Callback invoked when the primary button is clicked.
 * @param modifier Modifier to be applied to the card.
 * @param secondaryButtonLabel Label of the outlined secondary button.
 * @param onSecondaryClick Callback invoked when the secondary button is clicked. The secondary button is shown
 * only when both [secondaryButtonLabel] and [onSecondaryClick] are set.
 * @param heroContent Optional content displayed above the title, e.g. a logo.
 */
@Composable
fun PromoTwoButtonCard(
    title: String,
    body: String,
    primaryButtonLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButtonLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    heroContent: Slot? = null
) {
    val promoTheme = LocalComponentTheme.current.promoTwoButtonCard

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = promoTheme.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = promoTheme.elevation)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = promoTheme.horizontalPadding, vertical = promoTheme.verticalPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(promoTheme.contentSpacing)
        ) {
            heroContent?.invoke()

            Text(
                modifier = Modifier.padding(top = promoTheme.contentSpacing),
                text = title,
                style = promoTheme.titleTextStyle
            )

            Text(
                modifier = Modifier.padding(horizontal = promoTheme.bodyHorizontalPadding),
                text = body,
                style = promoTheme.bodyTextStyle
            )

            Box(
                modifier = Modifier
                    .padding(top = promoTheme.contentSpacing)
                    .fillMaxWidth()
                    .shadow(
                        elevation = promoTheme.primaryButtonShadowElevation,
                        shape = promoTheme.buttonShape,
                        spotColor = promoTheme.primaryButtonColor.copy(alpha = promoTheme.primaryButtonSpotShadowAlpha),
                        ambientColor = promoTheme.primaryButtonColor.copy(alpha = promoTheme.primaryButtonAmbientShadowAlpha)
                    )
                    .clip(promoTheme.buttonShape)
                    .background(color = promoTheme.primaryButtonColor)
                    .debouncedCombinedClickable(onClick = onPrimaryClick)
                    .padding(vertical = promoTheme.buttonVerticalPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = primaryButtonLabel,
                    style = promoTheme.primaryButtonTextStyle
                )
            }

            if (secondaryButtonLabel != null && onSecondaryClick != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .debouncedCombinedClickable(onClick = onSecondaryClick),
                    shape = promoTheme.buttonShape,
                    color = promoTheme.secondaryButtonColor,
                    border = BorderStroke(promoTheme.secondaryButtonBorderWidth, promoTheme.secondaryButtonBorderColor)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = promoTheme.buttonVerticalPadding),
                        text = secondaryButtonLabel,
                        style = promoTheme.secondaryButtonTextStyle
                    )
                }
            }
        }
    }
}
