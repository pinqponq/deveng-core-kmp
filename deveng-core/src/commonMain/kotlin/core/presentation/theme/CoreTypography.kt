package core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

val CORE_REGULAR_FONT_WEIGHT = FontWeight(400)
val CORE_MEDIUM_FONT_WEIGHT = FontWeight(500)
val CORE_SEMI_BOLD_FONT_WEIGHT = FontWeight(600)
val CORE_BOLD_FONT_WEIGHT = FontWeight(700)

/**
 * Returns a regular weight (400) text style.
 * Uses the font family from ComponentTheme if provided, otherwise uses platform default (Urbanist on Android/Desktop, system on iOS).
 */
@Composable
fun CoreRegularTextStyle(): TextStyle {
    val componentTheme = LocalComponentTheme.current
    val fontFamily = componentTheme.typography.fontFamily ?: CoreDefaultFontFamily()
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = CORE_REGULAR_FONT_WEIGHT
    )
}

/**
 * Returns a medium weight (500) text style.
 * Uses the font family from ComponentTheme if provided, otherwise uses platform default (Urbanist on Android/Desktop, system on iOS).
 */
@Composable
fun CoreMediumTextStyle(): TextStyle {
    val componentTheme = LocalComponentTheme.current
    val fontFamily = componentTheme.typography.fontFamily ?: CoreDefaultFontFamily()
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = CORE_MEDIUM_FONT_WEIGHT
    )
}

/**
 * Returns a semi-bold weight (600) text style.
 * Uses the font family from ComponentTheme if provided, otherwise uses platform default (Urbanist on Android/Desktop, system on iOS).
 */
@Composable
fun CoreSemiBoldTextStyle(): TextStyle {
    val componentTheme = LocalComponentTheme.current
    val fontFamily = componentTheme.typography.fontFamily ?: CoreDefaultFontFamily()
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = CORE_SEMI_BOLD_FONT_WEIGHT
    )
}

/**
 * Returns a bold weight (700) text style.
 * Uses the font family from ComponentTheme if provided, otherwise uses platform default (Urbanist on Android/Desktop, system on iOS).
 */
@Composable
fun CoreBoldTextStyle(): TextStyle {
    val componentTheme = LocalComponentTheme.current
    val fontFamily = componentTheme.typography.fontFamily ?: CoreDefaultFontFamily()
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = CORE_BOLD_FONT_WEIGHT
    )
}