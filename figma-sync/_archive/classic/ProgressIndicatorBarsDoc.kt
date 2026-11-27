package core.presentation.figma

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.figma.code.connect.FigmaConnect
import core.presentation.component.progressindicatorbars.IndicatorType
import core.presentation.component.progressindicatorbars.ProgressIndicatorBars

@FigmaConnect(
    url = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=4-44&m=dev"
)
class ProgressIndicatorBarsDoc {
    // --- INTERNAL ENUMS MIRRORING FIGMA VARIANTS / PROPS ---

    enum class IndicatorTypeVariant { HighlightCurrent, HighlightUntilCurrent }

    // --- FIGMA-PROPERTIES (MAPPED TO FIGMA COMPONENT PROPS) ---

    val pageCount: Int = 3

    val currentPage: Int = 1

    val indicatorTypeVariant: IndicatorTypeVariant = IndicatorTypeVariant.HighlightCurrent
    // Highlight current: Only the indicator at currentPage index is highlighted.
    // Highlight until current: All indicators from index 0 up to and including currentPage are highlighted.

    // --- DERIVED VALUES (NOT DIRECTLY FIGMA PROPS, BUT NEEDED FOR FULL API) ---

    // 1. modifier
    val modifier: Modifier = Modifier

    // 2. indicatorType
    // Controls which indicators are filled/highlighted:
    // - HIGH_LIGHT_CURRENT: Only the indicator at currentPage is filled (others use default color)
    // - HIGH_LIGHT_UNTIL_CURRENT: All indicators from 0 to currentPage (inclusive) are filled
    val indicatorType: IndicatorType
        get() = when (indicatorTypeVariant) {
            IndicatorTypeVariant.HighlightCurrent -> IndicatorType.HIGH_LIGHT_CURRENT
            IndicatorTypeVariant.HighlightUntilCurrent -> IndicatorType.HIGH_LIGHT_UNTIL_CURRENT
        }

    // 3. colors (null = use theme defaults)
    val filledIndicatorColor: Color? = null
    val defaultIndicatorColor: Color? = null

    // 4. dimensions (null = use theme defaults)
    val indicatorHeight: Dp? = null
    val indicatorSpacing: Dp? = null
    val indicatorCornerRadius: Dp? = null

    // --- THE COMPOSABLE SNIPPET (USES *ALL* PARAMETERS EXPLICITLY) ---

    @Composable
    fun Component() {
        ProgressIndicatorBars(
            modifier = modifier,
            pageCount = pageCount,
            currentPage = currentPage,
            indicatorType = indicatorType,
            filledIndicatorColor = filledIndicatorColor,
            defaultIndicatorColor = defaultIndicatorColor,
            indicatorHeight = indicatorHeight,
            indicatorSpacing = indicatorSpacing,
            indicatorCornerRadius = indicatorCornerRadius
        )
    }
}
