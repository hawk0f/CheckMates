package dev.hawk0f.checkmates.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBoardColors = staticCompositionLocalOf { ThemePalette.SAGE.boardLight }

val LocalAppAccents = staticCompositionLocalOf { ThemePalette.SAGE.accentsLight!! }

private fun derivedAccents(scheme: ColorScheme) = AppAccents(
    pageAlt = scheme.surface,
    band = scheme.secondaryContainer,
    onBand = scheme.onSecondaryContainer,
    bandStrong = scheme.secondary,
    positive = scheme.primary,
    negative = scheme.error
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val palette = ThemeManager.palette
    val useDark = when (ThemeManager.darkMode) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
    val colorScheme = if (useDark) palette.dark else palette.light
    val boardColors = if (useDark) palette.boardDark else palette.boardLight
    val accents = (if (useDark) palette.accentsDark else palette.accentsLight)
        ?: derivedAccents(colorScheme)
    CompositionLocalProvider(
        LocalBoardColors provides boardColors,
        LocalAppAccents provides accents
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = appTypography(), content = content)
    }
}
