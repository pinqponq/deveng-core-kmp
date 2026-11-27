package core.presentation.figma

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.figma.code.connect.FigmaConnect
import core.presentation.component.CustomHeader
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_angle_right
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_left
import org.jetbrains.compose.resources.DrawableResource

@FigmaConnect(
    url = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=62-78&m=draw"
)
class CustomHeaderDoc {
    // --- INTERNAL ENUMS MIRRORING FIGMA VARIANTS / PROPS ---

    enum class LeftIconVariant { ArrowLeft, AngleRight, None }

    enum class RightIconVariant { AngleRight, ArrowLeft, None }

    enum class CenterIconVariant { AngleRight, ArrowLeft, None }

    // --- FIGMA-PROPERTIES (MAPPED TO FIGMA COMPONENT PROPS) ---

    val leftIconVariant: LeftIconVariant = LeftIconVariant.ArrowLeft

    val rightIconVariant: RightIconVariant = RightIconVariant.AngleRight

    val centerIconVariant: CenterIconVariant = CenterIconVariant.None

    val isLeftIconButtonVisible: Boolean = true

    val isRightIconButtonVisible: Boolean = true
    // Default behavior: Automatically true if rightIcon is not null, false otherwise.
    // This property allows manual override of the default visibility logic.

    val isCenterIconVisible: Boolean = true

    val leftIconDescription: String = "Back"

    val rightIconDescription: String = "Next"

    val centerIconDescription: String = "Center icon"

    // --- DERIVED VALUES (NOT DIRECTLY FIGMA PROPS, BUT NEEDED FOR FULL API) ---

    // 1. leftIcon
    val leftIcon: DrawableResource
        get() = when (leftIconVariant) {
            LeftIconVariant.ArrowLeft -> Res.drawable.shared_ic_arrow_left
            LeftIconVariant.AngleRight -> Res.drawable.shared_ic_angle_right
            LeftIconVariant.None -> Res.drawable.shared_ic_arrow_left // default
        }

    // 2. rightIcon
    // If rightIcon is null, isRightIconButtonVisible defaults to false.
    // If rightIcon is not null, isRightIconButtonVisible defaults to true (unless explicitly overridden).
    val rightIcon: DrawableResource?
        get() = when (rightIconVariant) {
            RightIconVariant.AngleRight -> Res.drawable.shared_ic_angle_right
            RightIconVariant.ArrowLeft -> Res.drawable.shared_ic_arrow_left
            RightIconVariant.None -> null
        }

    // 3. centerIcon
    // If centerIcon is null, the component falls back to the theme's default center icon.
    // The center icon is displayed as an Image (not a button) and is centered horizontally.
    val centerIcon: DrawableResource?
        get() = when (centerIconVariant) {
            CenterIconVariant.AngleRight -> Res.drawable.shared_ic_angle_right
            CenterIconVariant.ArrowLeft -> Res.drawable.shared_ic_arrow_left
            CenterIconVariant.None -> null
        }

    // 4. modifier
    val modifier: Modifier = Modifier

    // 5. containerPadding
    val containerPadding: PaddingValues? = null

    // 6. colors (null = use theme defaults)
    val backgroundColor: Color? = null
    val leftIconTint: Color? = null
    val rightIconTint: Color? = null
    val leftIconBackgroundColor: Color? = null
    val rightIconBackgroundColor: Color? = null

    // 7. iconButtonSize
    val iconButtonSize: Dp? = null

    // 8. callbacks
    val onLeftIconClick: () -> Unit = {}
    val onRightIconClick: () -> Unit = {}

    // --- THE COMPOSABLE SNIPPET (USES *ALL* PARAMETERS EXPLICITLY) ---

    @Composable
    fun Component() {
        CustomHeader(
            modifier = modifier,
            leftIcon = leftIcon,
            rightIcon = rightIcon,
            centerIcon = centerIcon,
            leftIconDescription = leftIconDescription,
            rightIconDescription = rightIconDescription,
            centerIconDescription = centerIconDescription,
            isLeftIconButtonVisible = isLeftIconButtonVisible,
            isRightIconButtonVisible = isRightIconButtonVisible,
            isCenterIconVisible = isCenterIconVisible,
            containerPadding = containerPadding,
            backgroundColor = backgroundColor,
            leftIconTint = leftIconTint,
            rightIconTint = rightIconTint,
            leftIconBackgroundColor = leftIconBackgroundColor,
            rightIconBackgroundColor = rightIconBackgroundColor,
            iconButtonSize = iconButtonSize,
            onLeftIconClick = onLeftIconClick,
            onRightIconClick = onRightIconClick
        )
    }
}
