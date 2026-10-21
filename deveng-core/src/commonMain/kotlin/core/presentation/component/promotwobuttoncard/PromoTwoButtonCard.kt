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
import androidx.compose.ui.unit.dp
import core.presentation.component.Slot
import core.presentation.theme.LocalComponentTheme
import core.util.debouncedCombinedClickable

private const val PRIMARY_BUTTON_SPOT_SHADOW_ALPHA = 0.35f
private const val PRIMARY_BUTTON_AMBIENT_SHADOW_ALPHA = 0.2f
private val CardHorizontalPadding = 25.dp
private val CardVerticalPadding = 33.dp
private val ContentSpacing = 8.dp
private val BodyHorizontalPadding = 16.dp
private val PrimaryButtonShadowElevation = 8.dp
private val ButtonVerticalPadding = 16.dp
private val SecondaryButtonBorderWidth = 1.dp

/**
 * Promotional card with an optional hero, a title, a body text, a filled primary button and an optional
 * outlined secondary button.
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
                .padding(horizontal = CardHorizontalPadding, vertical = CardVerticalPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ContentSpacing)
        ) {
            heroContent?.invoke()

            Text(
                modifier = Modifier.padding(top = ContentSpacing),
                text = title,
                style = promoTheme.titleTextStyle
            )

            Text(
                modifier = Modifier.padding(horizontal = BodyHorizontalPadding),
                text = body,
                style = promoTheme.bodyTextStyle
            )

            Box(
                modifier = Modifier
                    .padding(top = ContentSpacing)
                    .fillMaxWidth()
                    .shadow(
                        elevation = PrimaryButtonShadowElevation,
                        shape = promoTheme.buttonShape,
                        spotColor = promoTheme.primaryButtonColor.copy(alpha = PRIMARY_BUTTON_SPOT_SHADOW_ALPHA),
                        ambientColor = promoTheme.primaryButtonColor.copy(alpha = PRIMARY_BUTTON_AMBIENT_SHADOW_ALPHA)
                    )
                    .clip(promoTheme.buttonShape)
                    .background(color = promoTheme.primaryButtonColor)
                    .debouncedCombinedClickable(onClick = onPrimaryClick)
                    .padding(vertical = ButtonVerticalPadding),
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
                    border = BorderStroke(SecondaryButtonBorderWidth, promoTheme.secondaryButtonBorderColor)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ButtonVerticalPadding),
                        text = secondaryButtonLabel,
                        style = promoTheme.secondaryButtonTextStyle
                    )
                }
            }
        }
    }
}
