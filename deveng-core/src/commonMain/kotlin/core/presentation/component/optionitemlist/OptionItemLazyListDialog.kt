package core.presentation.component.optionitemlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.alertdialog.CustomDialog
import core.presentation.component.scrollbar.scrollbarWithLazyListState
import core.presentation.theme.LocalComponentTheme
import core.util.ifTrue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A dialog component displaying a lazy-scrollable list of selectable option items.
 * Uses LazyColumn for efficient rendering of large lists. Maximum height is constrained to 585.dp.
 * Includes scrollbar support for better UX.
 *
 * @param optionsList List of items of type T to display.
 * @param optionText Function that returns the text to display for each item.
 * @param optionId Function that returns a unique identifier (Any) for each item, used as LazyColumn key.
 * @param leadingIcon Optional function that returns a drawable resource for the leading icon of each item.
 * @param leadingOptionSlot Composable slot for custom leading content for each item. Default is empty.
 * @param isDialogVisible Whether the dialog is visible.
 * @param isCheckIconsVisible Whether to display check icons indicating selected items. Default is true.
 * @param isScrollBarVisible Whether to display a scrollbar at the right of the list. Default is true.
 * @param selectedOption Currently selected item, or null if none selected.
 * @param onOptionItemClick Callback invoked when an option item is clicked, receives the clicked item.
 * @param onDismissRequest Callback invoked when the dialog should be dismissed.
 * @param optionItemBackgroundColor Background color for option items. If null, uses theme default.
 * @param optionItemHorizontalPadding Horizontal padding for option items. If null, uses theme default.
 * @param optionItemCheckIconTint Color tint for check icons. If null, uses theme default.
 * @param optionItemTextStyle Text style for option items. If null, uses theme default.
 */
@Composable
fun <T> OptionItemLazyListDialog(
    optionsList: List<T>,
    optionText: (T) -> String,
    optionId: (T) -> Any,
    leadingIcon: ((T) -> DrawableResource)? = null,
    leadingOptionSlot: @Composable (T) -> Unit = {},
    isDialogVisible: Boolean,
    isCheckIconsVisible: Boolean = true,
    isScrollBarVisible: Boolean = true,
    selectedOption: T? = null,
    onOptionItemClick: (T) -> Unit,
    onDismissRequest: () -> Unit,
    optionItemBackgroundColor: Color? = null,
    optionItemHorizontalPadding: Dp? = null,
    optionItemCheckIconTint: Color? = null,
    optionItemTextStyle: TextStyle? = null
) {
    val lazyListState = rememberLazyListState()
    val componentTheme = LocalComponentTheme.current
    val optionListTheme = componentTheme.optionItemList

    val finalOptionItemBackgroundColor =
        optionItemBackgroundColor ?: optionListTheme.optionItemBackgroundColor
    val finalOptionItemHorizontalPadding =
        optionItemHorizontalPadding ?: optionListTheme.optionItemHorizontalPadding
    val finalOptionItemCheckIconTint =
        optionItemCheckIconTint ?: optionListTheme.optionItemCheckIconTint
    val finalOptionItemTextStyle = optionItemTextStyle ?: optionListTheme.optionItemTextStyle

    if (isDialogVisible) {
        CustomDialog(
            modifier = Modifier
                .heightIn(max = 585.dp),
            onDismissRequest = onDismissRequest
        ) {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .ifTrue(isScrollBarVisible) {
                            scrollbarWithLazyListState(
                                listState = lazyListState,
                                width = optionListTheme.lazyListScrollbarWidth,
                                scrollBarColor = optionListTheme.lazyListScrollbarColor,
                                topPadding = optionListTheme.lazyListScrollbarTopPadding,
                                bottomPadding = optionListTheme.lazyListScrollbarBottomPadding
                            )
                        },
                    state = lazyListState
                ) {
                    itemsIndexed(
                        items = optionsList,
                        key = { _, item -> optionId(item) }
                    ) { index, item ->
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

                        if (index < optionsList.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.height(1.dp),
                                color = optionListTheme.dividerColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun OptionItemLazyListDialogPreview() {
    OptionItemLazyListDialog(
        optionsList = listOf(
            "item0",
            "item1",
            "item2",
            "item3item4item5item6item7item8item9item10item11item12item13item14"
        ),
        optionText = { option -> option },
        optionId = { option -> option.hashCode() },
        isDialogVisible = true,
        onOptionItemClick = {},
        onDismissRequest = {}
    )
}