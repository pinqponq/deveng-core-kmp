package core.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import core.presentation.theme.LocalComponentTheme

/**
 * A switch component with an associated label.
 * The label can be positioned at the start or end of the switch.
 *
 * @param label Text label displayed next to the switch.
 * @param isChecked Current checked state of the switch.
 * @param modifier Modifier to be applied to the container row.
 * @param isLabelAtStart Whether the label is positioned at the start (true) or end (false) of the switch. Default is true.
 * @param labelTextStyle Text style for the label. If null, uses theme default.
 * @param checkedThumbColor Color of the switch thumb when checked. If null, uses theme default.
 * @param checkedTrackColor Color of the switch track when checked. If null, uses theme default.
 * @param checkedBorderColor Color of the switch border when checked. If null, uses theme default.
 * @param uncheckedThumbColor Color of the switch thumb when unchecked. If null, uses theme default.
 * @param uncheckedTrackColor Color of the switch track when unchecked. If null, uses theme default.
 * @param uncheckedBorderColor Color of the switch border when unchecked. If null, uses theme default.
 * @param switchScale Scale factor to resize the switch. If null, uses theme default.
 * @param isEnabled Whether the switch accepts clicks. Default is true.
 * @param onSwitchClick Callback invoked when the switch is clicked, receives the new checked state.
 */
@Composable
fun LabeledSwitch(
    label: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    isLabelAtStart: Boolean = true,
    labelTextStyle: TextStyle? = null,
    checkedThumbColor: Color? = null,
    checkedTrackColor: Color? = null,
    checkedBorderColor: Color? = null,
    uncheckedThumbColor: Color? = null,
    uncheckedTrackColor: Color? = null,
    uncheckedBorderColor: Color? = null,
    switchScale: Float? = null,
    isEnabled: Boolean = true,
    onSwitchClick: (Boolean) -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val labeledSwitchTheme = componentTheme.labeledSwitch

    val finalLabelTextStyle = labelTextStyle ?: labeledSwitchTheme.labelTextStyle
    val finalCheckedThumbColor = checkedThumbColor ?: labeledSwitchTheme.checkedThumbColor
    val finalCheckedTrackColor = checkedTrackColor ?: labeledSwitchTheme.checkedTrackColor
    val finalCheckedBorderColor = checkedBorderColor ?: labeledSwitchTheme.checkedBorderColor
    val finalUncheckedThumbColor = uncheckedThumbColor ?: labeledSwitchTheme.uncheckedThumbColor
    val finalUncheckedTrackColor = uncheckedTrackColor ?: labeledSwitchTheme.uncheckedTrackColor
    val finalUncheckedBorderColor = uncheckedBorderColor ?: labeledSwitchTheme.uncheckedBorderColor
    val finalScale = switchScale ?: labeledSwitchTheme.switchScale

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isLabelAtStart) {
            Arrangement.SpaceBetween
        } else {
            Arrangement.End
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = finalLabelTextStyle
        )

        Switch(
            modifier = Modifier.scale(finalScale),
            checked = isChecked,
            enabled = isEnabled,
            onCheckedChange = onSwitchClick,
            colors = SwitchDefaults.colors(
                checkedThumbColor = finalCheckedThumbColor,
                checkedTrackColor = finalCheckedTrackColor,
                checkedBorderColor = finalCheckedBorderColor,
                uncheckedThumbColor = finalUncheckedThumbColor,
                uncheckedTrackColor = finalUncheckedTrackColor,
                uncheckedBorderColor = finalUncheckedBorderColor
            )
        )
    }
}