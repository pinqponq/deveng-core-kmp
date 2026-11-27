package core.presentation.component.chips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import core.presentation.component.RoundedSurface
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.util.debouncedCombinedClickable
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_bell
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable chip component that can display text, optional leading icon and count badge.
 * Supports selection state with customizable colors, shapes, and styling.
 *
 * @param modifier Modifier to be applied to the chip container.
 * @param text Text to display in the chip.
 * @param leadingIcon Optional drawable resource for a leading icon.
 * @param leadingIconModifier Modifier to be applied to the leading icon.
 * @param leadingIconContentDescription Content description for accessibility for the leading icon.
 * @param isSelected Whether the chip is currently selected. Default is false.
 * @param onClick Callback invoked when the chip is clicked.
 * @param count Optional count value to display in a badge. If null, count section is hidden.
 * @param isCountSectionVisible Whether the count section is visible. Default is true if count is provided.
 * @param selectedBackgroundColor Background color when selected. If null, uses theme default.
 * @param unselectedBackgroundColor Background color when not selected. If null, uses theme default.
 * @param selectedBorderStroke Border stroke to apply when the chip is selected. If null, no border is drawn.
 * @param unselectedBorderStroke Border stroke to apply when the chip is not selected. If null, no border is drawn.
 * @param shape Shape of the chip container. If null, uses theme default.
 * @param horizontalPadding Horizontal padding of the chip. If null, uses theme default.
 * @param verticalPadding Vertical padding of the chip. If null, uses theme default.
 * @param contentSpacing Spacing between chip content elements. If null, uses theme default.
 * @param textStyle Text style for the chip text. If null, uses theme default.
 * @param selectedTextColor Text color when selected. If null, uses theme default.
 * @param unselectedTextColor Text color when not selected. If null, uses theme default.
 * @param selectedLeadingIconTint Leading icon tint when selected. If null, uses theme default.
 * @param unselectedLeadingIconTint Leading icon tint when not selected. If null, uses theme default.
 * @param countSectionBackgroundColor Background color for the count badge. If null, uses theme default.
 * @param countSectionTextColor Text color for the count badge. If null, uses theme default.
 * @param countSectionTextStyle Text style for the count badge. If null, uses theme default.
 * @param countSectionHeight Height of the count badge. If null, uses theme default.
 * @param countSectionWidth Width of the count badge. If null, uses theme default.
 * @param countSectionShape Shape of the count badge. If null, uses theme default.
 */
@Composable
fun ChipItem(
    modifier: Modifier = Modifier,
    text: String,
    leadingIcon: DrawableResource? = null,
    leadingIconModifier: Modifier = Modifier,
    leadingIconContentDescription: String? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    count: Int? = null,
    isCountSectionVisible: Boolean = false,
    selectedBackgroundColor: Color? = null,
    unselectedBackgroundColor: Color? = null,
    selectedBorderStroke: BorderStroke? = null,
    unselectedBorderStroke: BorderStroke? = null,
    shape: CornerBasedShape? = null,
    horizontalPadding: Dp? = null,
    verticalPadding: Dp? = null,
    contentSpacing: Dp? = null,
    textStyle: TextStyle? = null,
    selectedTextColor: Color? = null,
    unselectedTextColor: Color? = null,
    selectedLeadingIconTint: Color? = null,
    unselectedLeadingIconTint: Color? = null,
    countSectionBackgroundColor: Color? = null,
    countSectionTextColor: Color? = null,
    countSectionTextStyle: TextStyle? = null,
    countSectionHeight: Dp? = null,
    countSectionWidth: Dp? = null,
    countSectionShape: CornerBasedShape? = null
) {
    val componentTheme = LocalComponentTheme.current
    val chipTheme = componentTheme.chipItem

    val finalSelectedBackgroundColor = selectedBackgroundColor ?: chipTheme.selectedBackgroundColor
    val finalUnselectedBackgroundColor =
        unselectedBackgroundColor ?: chipTheme.unselectedBackgroundColor
    val finalSelectedBorderStroke = selectedBorderStroke ?: chipTheme.selectedBorderStroke
    val finalUnSelectedBorderStroke = unselectedBorderStroke ?: chipTheme.unselectedBorderStroke
    val finalShape = shape ?: chipTheme.shape
    val finalHorizontalPadding = horizontalPadding ?: chipTheme.horizontalPadding
    val finalVerticalPadding = verticalPadding ?: chipTheme.verticalPadding
    val finalContentSpacing = contentSpacing ?: chipTheme.contentSpacing
    val finalTextStyle = textStyle ?: chipTheme.textStyle
    val finalSelectedTextColor = selectedTextColor ?: chipTheme.selectedTextColor
    val finalUnselectedTextColor = unselectedTextColor ?: chipTheme.unselectedTextColor
    val finalSelectedLeadingIconTint = selectedLeadingIconTint ?: chipTheme.selectedLeadingIconTint
    val finalUnselectedLeadingIconTint =
        unselectedLeadingIconTint ?: chipTheme.unselectedLeadingIconTint
    val finalCountSectionBackgroundColor =
        countSectionBackgroundColor ?: chipTheme.countSectionBackgroundColor
    val finalCountSectionTextColor = countSectionTextColor ?: chipTheme.countSectionTextColor
    val finalCountSectionTextStyle = countSectionTextStyle ?: chipTheme.countSectionTextStyle
    val finalCountSectionHeight = countSectionHeight ?: chipTheme.countSectionHeight
    val finalCountSectionWidth = countSectionWidth ?: chipTheme.countSectionWidth
    val finalCountSectionShape = countSectionShape ?: chipTheme.countSectionShape

    val resolvedBackgroundColor =
        if (isSelected) finalSelectedBackgroundColor else finalUnselectedBackgroundColor
    val resolvedTextColor =
        if (isSelected) finalSelectedTextColor else finalUnselectedTextColor
    val resolvedLeadingIconTint =
        if (isSelected) finalSelectedLeadingIconTint else finalUnselectedLeadingIconTint
    val resolvedBorderStroke =
        if (isSelected) finalSelectedBorderStroke else finalUnSelectedBorderStroke
    val isLeadingIconVisible = leadingIcon != null

    RoundedSurface(
        modifier = modifier
            .clip(finalShape)
            .debouncedCombinedClickable(
                onClick = { onClick?.invoke() },
                isDebounce = false
            ),
        borderStroke = resolvedBorderStroke,
        shape = finalShape,
        color = resolvedBackgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = finalHorizontalPadding,
                    vertical = finalVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(finalContentSpacing)
        ) {
            if (isLeadingIconVisible) {
                Icon(
                    modifier = leadingIconModifier,
                    painter = painterResource(leadingIcon),
                    contentDescription = leadingIconContentDescription,
                    tint = resolvedLeadingIconTint
                )
            }

            Text(
                text = text,
                style = finalTextStyle.copy(
                    color = resolvedTextColor
                )
            )

            if (isCountSectionVisible) {
                Box(
                    modifier = Modifier
                        .height(finalCountSectionHeight)
                        .width(finalCountSectionWidth)
                        .clip(finalCountSectionShape)
                        .background(finalCountSectionBackgroundColor)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = count.toString(),
                        style = finalCountSectionTextStyle.copy(
                            color = finalCountSectionTextColor
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ChipPreview() {
    AppTheme {
        ChipItem(
            leadingIcon = Res.drawable.shared_ic_bell,
            text = "İstekler & Davetler",
            count = 2
        )
    }
}

@Preview
@Composable
fun ChipSelectedPreview() {
    AppTheme {
        ChipItem(
            leadingIcon = Res.drawable.shared_ic_bell,
            text = "Selected Chip",
            isSelected = true
        )
    }
}

@Preview
@Composable
fun ChipWithIconPreview() {
    AppTheme {
        ChipItem(
            text = "Chip with Icon",
            leadingIcon = Res.drawable.shared_ic_bell
        )
    }
}