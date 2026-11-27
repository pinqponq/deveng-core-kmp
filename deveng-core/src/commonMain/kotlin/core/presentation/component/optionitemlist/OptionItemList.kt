package core.presentation.component.optionitemlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.RoundedSurface
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource

/**
 * A list component displaying a collection of selectable option items.
 * Each item can have a leading icon/slot and optional check icon indicating selection.
 *
 * @param optionList List of items of type T to display.
 * @param optionText Composable function that returns the text to display for each item.
 * @param optionId Function that returns a unique identifier (Any) for each item, used for selection comparison.
 * @param leadingIcon Optional function that returns a drawable resource for the leading icon of each item.
 * @param leadingOptionSlot Composable slot for custom leading content for each item. Default is empty.
 * @param isCheckIconsVisible Whether to display check icons indicating selected items. Default is false.
 * @param selectedOption Currently selected item, or null if none selected.
 * @param onOptionItemClick Callback invoked when an option item is clicked, receives the clicked item.
 * @param optionItemBackgroundColor Background color for option items. If null, uses theme default.
 * @param optionItemHorizontalPadding Horizontal padding for option items. If null, uses theme default.
 * @param optionItemCheckIconTint Color tint for check icons. If null, uses theme default.
 * @param optionItemTextStyle Text style for option items. If null, uses theme default.
 */
@Composable
fun <T> OptionItemList(
    optionList: List<T>,
    optionText: @Composable (T) -> String,
    optionId: (T) -> Any,
    leadingIcon: ((T) -> DrawableResource)? = null,
    leadingOptionSlot: @Composable (T) -> Unit = {},
    isCheckIconsVisible: Boolean = false,
    selectedOption: T? = null,
    onOptionItemClick: (T) -> Unit,
    optionItemBackgroundColor: Color? = null,
    optionItemHorizontalPadding: Dp? = null,
    optionItemCheckIconTint: Color? = null,
    optionItemTextStyle: TextStyle? = null
) {
    val componentTheme = LocalComponentTheme.current
    val optionListTheme = componentTheme.optionItemList

    val finalOptionItemBackgroundColor = optionItemBackgroundColor ?: optionListTheme.optionItemBackgroundColor
    val finalOptionItemHorizontalPadding = optionItemHorizontalPadding ?: optionListTheme.optionItemHorizontalPadding
    val finalOptionItemCheckIconTint = optionItemCheckIconTint ?: optionListTheme.optionItemCheckIconTint
    val finalOptionItemTextStyle = optionItemTextStyle ?: optionListTheme.optionItemTextStyle

    RoundedSurface(
        shape = optionListTheme.containerShape,
        color = optionListTheme.containerColor
    ) {
        Column {
            optionList.forEachIndexed { index, item ->
                OptionItem(
                    text = optionText(item),
                    isCheckIconVisible = isCheckIconsVisible,
                    leadingIcon = leadingIcon?.invoke(item),
                    leadingSlot = { leadingOptionSlot(item) },
                    isSelected = selectedOption?.let { optionId(it) == optionId(item) }
                        ?: false,
                    onItemClick = { onOptionItemClick(item) },
                    backgroundColor = finalOptionItemBackgroundColor,
                    horizontalPadding = finalOptionItemHorizontalPadding,
                    checkIconTint = finalOptionItemCheckIconTint,
                    textStyle = finalOptionItemTextStyle
                )

                if (index < optionList.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.height(1.dp),
                        color = optionListTheme.dividerColor
                    )
                }
            }
        }
    }
}
