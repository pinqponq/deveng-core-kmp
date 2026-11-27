package core.presentation.figma

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.figma.code.connect.FigmaConnect
import com.figma.code.connect.FigmaProperty
import com.figma.code.connect.FigmaType
import core.presentation.component.LabeledSwitch

@FigmaConnect(
    url = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=148-87&m=draw"
)
class LabeledSwitchDoc {
    // --- FIGMA-PROPERTIES (MAPPED TO FIGMA COMPONENT PROPS) ---

    @FigmaProperty(FigmaType.Text, "label")
    val label: String = "Switch label"

    @FigmaProperty(FigmaType.Boolean, "isChecked")
    val isChecked: Boolean = false

    @FigmaProperty(FigmaType.Boolean, "isLabelAtStart")
    val isLabelAtStart: Boolean = true
    // When true: Label is positioned at the start, arrangement is SpaceBetween (label left, switch right).
    // When false: Label is positioned at the end, arrangement is End (switch left, label right).

    // --- DERIVED VALUES (NOT DIRECTLY FIGMA PROPS, BUT NEEDED FOR FULL API) ---

    // 1. modifier
    val modifier: Modifier = Modifier

    // 2. text style (null = use theme defaults)
    val labelTextStyle: TextStyle? = null

    // 3. colors (null = use theme defaults)
    val checkedThumbColor: Color? = null
    val checkedTrackColor: Color? = null
    val checkedBorderColor: Color? = null
    val uncheckedThumbColor: Color? = null
    val uncheckedTrackColor: Color? = null
    val uncheckedBorderColor: Color? = null

    // 4. scale
    // Scale factor applied to the switch component to resize it.
    // If null, uses theme's default scale value.
    // Applied using Compose's scale modifier to the Switch component.
    val switchScale: Float? = null

    // 5. callback
    val onSwitchClick: (Boolean) -> Unit = {}

    // --- THE COMPOSABLE SNIPPET (USES *ALL* PARAMETERS EXPLICITLY) ---

    @Composable
    fun Component() {
        LabeledSwitch(
            label = label,
            isChecked = isChecked,
            modifier = modifier,
            isLabelAtStart = isLabelAtStart,
            labelTextStyle = labelTextStyle,
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,
            checkedBorderColor = checkedBorderColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedBorderColor = uncheckedBorderColor,
            switchScale = switchScale,
            onSwitchClick = onSwitchClick
        )
    }
}
