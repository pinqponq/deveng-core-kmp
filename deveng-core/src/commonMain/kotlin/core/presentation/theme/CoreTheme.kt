package core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import core.presentation.motion.LocalReduceMotion
import core.presentation.motion.rememberSystemReduceMotion

private val DarkColorScheme = darkColorScheme(
    primary = CorePrimaryColor,
    onPrimary = CoreOnPrimaryColor,
    secondary = CoreSecondaryColor,
    onSecondary = CoreOnSecondaryColor,
    background = CoreBackgroundColor,
    onBackground = CoreOnBackgroundColor,
    surface = CoreSurfaceColor,
    onSurface = CoreOnSurfaceColor,
    error = CoreErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary = CorePrimaryColor,
    onPrimary = CoreOnPrimaryColor,
    secondary = CoreSecondaryColor,
    onSecondary = CoreOnSecondaryColor,
    background = CoreBackgroundColor,
    onBackground = CoreOnBackgroundColor,
    surface = CoreSurfaceColor,
    onSurface = CoreOnSurfaceColor,
    error = CoreErrorColor
)

/**
 * Main theme composable that sets up Material3 theme and component theme.
 * 
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param componentTheme Custom component theme for overriding component colors and typography.
 *                      If not provided, uses [DefaultComponentTheme].
 * @param reduceMotion Whether core animations switch to instant changes, provided as
 *                      [LocalReduceMotion]. Defaults to the system reduce-motion setting.
 * @param content The composable content that will use the theme.
 * 
 * Example usage with custom component theme:
 * ```
 * val myTheme = ComponentTheme(
 *     button = ButtonTheme(containerColor = Color.Red)
 * )
 * 
 * AppTheme(componentTheme = myTheme) {
 *     CustomButton(text = "Click me", onClick = {})
 * }
 * ```
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    componentTheme: ComponentTheme = DefaultComponentTheme,
    reduceMotion: Boolean = rememberSystemReduceMotion(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = {
            CompositionLocalProvider(
                LocalComponentTheme provides componentTheme,
                LocalReduceMotion provides reduceMotion
            ) {
                content()
            }
        }
    )
}
