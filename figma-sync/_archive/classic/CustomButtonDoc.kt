package core.presentation.figma

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.figma.code.connect.FigmaConnect
import core.presentation.component.CustomButton
import core.presentation.theme.CoreOnPrimaryColor
import core.presentation.theme.CoreOnSurfaceColor
import core.presentation.theme.CorePrimaryColor
import core.presentation.theme.CoreSecondaryColor
import core.presentation.theme.CoreSurfaceColor
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_left
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_next
import org.jetbrains.compose.resources.DrawableResource

@FigmaConnect(
    url = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=150-16&m=dev"
)
class CustomButtonDoc {

    // --- INTERNAL ENUMS MIRRORING FIGMA VARIANTS / PROPS ---

    enum class ColorPreset { Primary, Secondary, Surface, Transparent }

    enum class IconPosition { None, Leading, Trailing, Both }

    enum class Alignment { Center, Start, SpaceBetween }

    enum class ElevationLevel { None, Low, Medium, High }

    // --- FIGMA-PROPERTIES (MAPPED TO FIGMA COMPONENT PROPS) ---

    val text: String = "Custom Button"

    val enabled: Boolean = true

    val colorPreset: ColorPreset = ColorPreset.Primary

    val elevationLevel: ElevationLevel = ElevationLevel.Low

    val alignment: Alignment = Alignment.Center

    val iconPosition: IconPosition = IconPosition.Both

    val iconTint: Color = Color.Black

    val leadingIconContentDescription: String? = null

    val trailingIconContentDescription: String? = null

    // --- DERIVED VALUES (FROM ABOVE PROPS) ---

    val containerColor: Color?
        get() = when (colorPreset) {
            ColorPreset.Primary -> CorePrimaryColor
            ColorPreset.Secondary -> CoreSecondaryColor
            ColorPreset.Surface -> CoreSurfaceColor
            ColorPreset.Transparent -> Color.Transparent
        }

    val contentColor: Color?
        get() = when (colorPreset) {
            ColorPreset.Primary,
            ColorPreset.Secondary -> CoreOnPrimaryColor

            ColorPreset.Surface,
            ColorPreset.Transparent -> CoreOnSurfaceColor
        }

    val disabledContainerColor: Color?
        get() = containerColor?.copy(alpha = 0.4f)

    val disabledContentColor: Color?
        get() = contentColor?.copy(alpha = 0.4f)

    val elevationDp: Dp
        get() = when (elevationLevel) {
            ElevationLevel.None -> 0.dp
            ElevationLevel.Low -> 1.dp
            ElevationLevel.Medium -> 3.dp
            ElevationLevel.High -> 6.dp
        }

    val contentArrangement: Arrangement.Horizontal
        get() = when (alignment) {
            Alignment.Center -> Arrangement.Center
            Alignment.Start -> Arrangement.Start
            Alignment.SpaceBetween -> Arrangement.SpaceBetween
        }

    val leadingIcon: DrawableResource?
        get() = when (iconPosition) {
            IconPosition.Leading,
            IconPosition.Both -> Res.drawable.shared_ic_arrow_left

            IconPosition.None,
            IconPosition.Trailing -> null
        }

    val trailingIcon: DrawableResource?
        get() = when (iconPosition) {
            IconPosition.Trailing,
            IconPosition.Both -> Res.drawable.shared_ic_arrow_next

            IconPosition.None,
            IconPosition.Leading -> null
        }

    val leadingIconModifier: Modifier = Modifier
    val trailingIconModifier: Modifier = Modifier
    val textStyle: TextStyle? = null
    val textModifier: Modifier = Modifier
    val onClick: () -> Unit = {}

    // --- FINAL COMPOSABLE USED BY CODE CONNECT ---

    @Composable
    fun Component() {
        val elevation = ButtonDefaults.buttonElevation(defaultElevation = elevationDp)

        CustomButton(
            text = text,
            textStyle = textStyle,
            textModifier = textModifier,
            enabled = enabled,
            containerColor = containerColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            contentColor = contentColor,
            elevation = elevation,
            trailingIconModifier = trailingIconModifier,
            trailingIcon = trailingIcon,
            trailingIconTint = iconTint,
            trailingIconContentDescription = trailingIconContentDescription,
            leadingIconModifier = leadingIconModifier,
            leadingIcon = leadingIcon,
            leadingIconTint = iconTint,
            leadingIconContentDescription = leadingIconContentDescription,
            contentArrangement = contentArrangement,
            onClick = onClick
        )
    }
}
