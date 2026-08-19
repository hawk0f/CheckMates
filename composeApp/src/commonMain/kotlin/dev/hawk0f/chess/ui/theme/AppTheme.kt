package dev.hawk0f.chess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBoardColors = staticCompositionLocalOf { ThemePalette.ROYAL.boardLight }

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
    CompositionLocalProvider(LocalBoardColors provides boardColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
