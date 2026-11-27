package core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * iOS/native: use system default font to avoid ArrayIndexOutOfBoundsException
 * when Compose Resources reads font bytes (DefaultIOsResourceReader).
 */
@Composable
actual fun CoreDefaultFontFamily(): FontFamily = FontFamily.Default
