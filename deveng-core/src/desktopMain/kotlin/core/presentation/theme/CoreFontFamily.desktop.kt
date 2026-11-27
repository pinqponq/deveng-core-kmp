package core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.urbanistbold
import global.deveng.deveng_core.generated.resources.urbanistmedium
import global.deveng.deveng_core.generated.resources.urbanistregular
import global.deveng.deveng_core.generated.resources.urbanistsemibold
import org.jetbrains.compose.resources.Font

@Composable
actual fun CoreDefaultFontFamily(): FontFamily = FontFamily(
    Font(resource = Res.font.urbanistregular, weight = CORE_REGULAR_FONT_WEIGHT),
    Font(resource = Res.font.urbanistmedium, weight = CORE_MEDIUM_FONT_WEIGHT),
    Font(resource = Res.font.urbanistsemibold, weight = CORE_SEMI_BOLD_FONT_WEIGHT),
    Font(resource = Res.font.urbanistbold, weight = CORE_BOLD_FONT_WEIGHT)
)
