package core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.scrollbar.scrollbarWithScrollState
import core.presentation.component.textfield.CustomTextField
import core.presentation.theme.LocalComponentTheme
import core.util.EMPTY
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_description_angle_down
import global.deveng.deveng_core.generated.resources.shared_ic_angle_down
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A customizable dropdown menu component with text field and scrollable list of items.
 * Displays a text field that opens a dropdown menu when clicked, allowing selection from a list of items.
 *
 * @param modifier Modifier to be applied to the dropdown container.
 * @param title Optional title text displayed above the dropdown field.
 * @param hintMessage Placeholder text shown when no item is selected.
 * @param titleColor Color of the title text. If null, uses theme default.
 * @param backgroundColor Background color of the dropdown field and menu. If null, uses theme default.
 * @param textColor Color of the selected item text. If null, uses theme default.
 * @param unfocusedBorderColor Border color when the field is not focused. If null, uses theme default.
 * @param focusedBorderColor Border color when the field is focused. If null, uses theme default.
 * @param dividerColor Color of dividers between menu items. If null, uses theme default.
 * @param isScrollBarVisible Whether to show the scrollbar in the dropdown menu. If null, uses theme default.
 * @param scrollBarColor Color of the scrollbar thumb. If null, uses theme default.
 * @param scrollBarTrackColor Color of the scrollbar track. If null, uses theme default.
 * @param dropDownMenuHeight Maximum height of the dropdown menu. Default is 450.dp.
 * @param isEnabled Whether the menu opens and accepts a selection. Default is true.
 * @param items List of items to display in the dropdown menu.
 * @param selectedItem Currently selected item, or null if none selected.
 * @param onItemSelected Callback invoked when an item is selected from the dropdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CustomDropDownMenu(
    modifier: Modifier = Modifier,
    title: String? = String.EMPTY,
    hintMessage: String = String.EMPTY,
    titleColor: Color? = null,
    backgroundColor: Color? = null,
    dropDownMenuBackgroundColor: Color? = null,
    textColor: Color? = null,
    unfocusedBorderColor: Color? = null,
    focusedBorderColor: Color? = null,
    dividerColor: Color? = null,
    isScrollBarVisible: Boolean? = null,
    scrollBarColor: Color? = null,
    scrollBarTrackColor: Color? = null,
    dropDownMenuHeight: Dp = 450.dp,
    isEnabled: Boolean = true,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val dropdownTheme = componentTheme.customDropDownMenu

    val finalTitleColor = titleColor ?: dropdownTheme.titleColor
    val finalBackgroundColor = backgroundColor ?: dropdownTheme.backgroundColor
    val finalDropDownMenuBackgroundColor = dropDownMenuBackgroundColor ?: dropdownTheme.dropDownMenuBackgroundColor
    val finalTextColor = textColor ?: dropdownTheme.textColor
    val finalHintTextColor = dropdownTheme.hintTextColor
    val finalUnfocusedBorderColor = unfocusedBorderColor ?: dropdownTheme.unfocusedBorderColor
    val finalFocusedBorderColor = focusedBorderColor ?: dropdownTheme.focusedBorderColor
    val finalDividerColor = dividerColor ?: dropdownTheme.dividerColor
    val finalScrollBarColor = scrollBarColor ?: dropdownTheme.scrollBarColor
    val finalScrollBarTrackColor = scrollBarTrackColor ?: dropdownTheme.scrollBarTrackColor
    val finalIsScrollBarVisible = isScrollBarVisible ?: dropdownTheme.isScrollBarVisible
    val finalFieldTextStyle = dropdownTheme.fieldTextStyle
    val finalMenuItemTextStyle = dropdownTheme.menuItemTextStyle
    val finalShape = dropdownTheme.shape

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ExposedDropdownMenuBox(
            modifier = Modifier.fillMaxWidth(),
            expanded = isExpanded,
            onExpandedChange = { isExpanded = isEnabled && !isExpanded }
        ) {
            val displayText = if (selectedItem == null ||
                selectedItem.toString().isEmpty() ||
                selectedItem.toString() == hintMessage
            ) hintMessage else selectedItem.toString()

            val displayColor =
                if (displayText == hintMessage) finalHintTextColor else finalTextColor
            val resolvedFieldTextStyle = finalFieldTextStyle.copy(color = displayColor)

            CustomTextField(
                title = title,
                titleColor = finalTitleColor,
                textFieldModifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = isEnabled
                    )
                    .height(50.dp)
                    .fillMaxWidth(),
                textStyle = resolvedFieldTextStyle,
                value = displayText,
                onValueChange = {},
                readOnly = true,
                enabled = isEnabled,
                trailingSlot = {
                    Icon(
                        modifier = Modifier
                            .height(15.dp)
                            .width(15.dp),
                        painter = painterResource(Res.drawable.shared_ic_angle_down),
                        tint = finalTextColor,
                        contentDescription = stringResource(Res.string.shared_content_description_angle_down)
                    )
                },
                textColor = displayColor,
                readOnlyTextColor = displayColor,
                unfocusedBorderColor = finalUnfocusedBorderColor,
                focusedBorderColor = finalFocusedBorderColor,
                containerColor = finalBackgroundColor,
                shape = finalShape
            )

            val scrollState = rememberScrollState()

            val dropdownModifier = Modifier
                .heightIn(
                    max = dropDownMenuHeight
                )
                .background(
                    color = finalDropDownMenuBackgroundColor
                )
                .exposedDropdownSize(true)
                .then(
                    if (finalIsScrollBarVisible) {
                        Modifier.scrollbarWithScrollState(
                            scrollState = scrollState,
                            scrollBarColor = finalScrollBarColor,
                            scrollBarTrackColor = finalScrollBarTrackColor
                        )
                    } else {
                        Modifier
                    }
                )

            ExposedDropdownMenu(
                scrollState = scrollState,
                modifier = dropdownModifier,
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        modifier = Modifier.background(finalBackgroundColor),
                        onClick = {
                            onItemSelected(item)
                            isExpanded = false
                        },
                        text = {
                            Text(
                                text = item.toString(),
                                style = finalMenuItemTextStyle.copy(
                                    color = finalTextColor
                                )
                            )
                        }
                    )
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = finalDividerColor
                        )
                    }
                }
            }
        }
    }
}