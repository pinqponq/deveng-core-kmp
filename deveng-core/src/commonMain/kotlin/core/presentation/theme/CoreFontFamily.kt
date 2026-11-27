package core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Platform-specific default font family for core typography.
 * On Android/Desktop uses Urbanist from resources; on iOS (and other platforms where
 * Compose Resources font loading is problematic) uses system default to avoid crashes.
 */
@Composable
expect fun CoreDefaultFontFamily(): FontFamily
