package dev.hawk0f.checkmates.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class BoardColors(
    val lightSquare: Color,
    val darkSquare: Color
)

enum class ThemePalette(
    val id: String,
    val title: String,
    val light: ColorScheme,
    val dark: ColorScheme,
    val boardLight: BoardColors,
    val boardDark: BoardColors
) {
    ROYAL(
        id = "royal",
        title = "Royal Violet",
        light = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE9DDFF),
            onPrimaryContainer = Color(0xFF22005D),
            secondary = Color(0xFF625B71),
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1E192B),
            tertiary = Color(0xFF7E5260),
            background = Color(0xFFFDF7FF),
            surface = Color(0xFFFDF7FF),
            surfaceVariant = Color(0xFFE7E0EB),
            onSurfaceVariant = Color(0xFF49454E)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFCFBCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378A),
            onPrimaryContainer = Color(0xFFE9DDFF),
            secondary = Color(0xFFCBC2DB),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8),
            background = Color(0xFF141218),
            surface = Color(0xFF141218),
            surfaceVariant = Color(0xFF49454E),
            onSurfaceVariant = Color(0xFFCAC4CF)
        ),
        boardLight = BoardColors(Color(0xFFF0D9B5), Color(0xFFB58863)),
        boardDark = BoardColors(Color(0xFFC3B091), Color(0xFF7A5C43))
    ),
    EMERALD(
        id = "emerald",
        title = "Emerald",
        light = lightColorScheme(
            primary = Color(0xFF2E6B4F),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFB1F1CB),
            onPrimaryContainer = Color(0xFF002114),
            secondary = Color(0xFF4E6355),
            secondaryContainer = Color(0xFFD0E8D6),
            onSecondaryContainer = Color(0xFF0B1F14),
            tertiary = Color(0xFF3C6472),
            background = Color(0xFFF5FBF3),
            surface = Color(0xFFF5FBF3),
            surfaceVariant = Color(0xFFDCE5DB),
            onSurfaceVariant = Color(0xFF404942)
        ),
        dark = darkColorScheme(
            primary = Color(0xFF95D5B0),
            onPrimary = Color(0xFF003824),
            primaryContainer = Color(0xFF115236),
            onPrimaryContainer = Color(0xFFB1F1CB),
            secondary = Color(0xFFB4CCBA),
            secondaryContainer = Color(0xFF374B3E),
            onSecondaryContainer = Color(0xFFD0E8D6),
            tertiary = Color(0xFFA4CDDD),
            background = Color(0xFF10140F),
            surface = Color(0xFF10140F),
            surfaceVariant = Color(0xFF404942),
            onSurfaceVariant = Color(0xFFC0C9BF)
        ),
        boardLight = BoardColors(Color(0xFFEBECD0), Color(0xFF739552)),
        boardDark = BoardColors(Color(0xFFB8BBA0), Color(0xFF54713D))
    ),
    OCEAN(
        id = "ocean",
        title = "Ocean",
        light = lightColorScheme(
            primary = Color(0xFF29638A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFCBE6FF),
            onPrimaryContainer = Color(0xFF001E30),
            secondary = Color(0xFF50606E),
            secondaryContainer = Color(0xFFD3E5F6),
            onSecondaryContainer = Color(0xFF0C1D29),
            tertiary = Color(0xFF64597B),
            background = Color(0xFFF7F9FF),
            surface = Color(0xFFF7F9FF),
            surfaceVariant = Color(0xFFDDE3EA),
            onSurfaceVariant = Color(0xFF41474D)
        ),
        dark = darkColorScheme(
            primary = Color(0xFF97CCF8),
            onPrimary = Color(0xFF003350),
            primaryContainer = Color(0xFF004B71),
            onPrimaryContainer = Color(0xFFCBE6FF),
            secondary = Color(0xFFB7C9D9),
            secondaryContainer = Color(0xFF384956),
            onSecondaryContainer = Color(0xFFD3E5F6),
            tertiary = Color(0xFFCEC0E8),
            background = Color(0xFF101417),
            surface = Color(0xFF101417),
            surfaceVariant = Color(0xFF41474D),
            onSurfaceVariant = Color(0xFFC1C7CE)
        ),
        boardLight = BoardColors(Color(0xFFDEE3E6), Color(0xFF8CA2AD)),
        boardDark = BoardColors(Color(0xFFADB8BE), Color(0xFF5E7580))
    ),
    AMBER(
        id = "amber",
        title = "Amber Club",
        light = lightColorScheme(
            primary = Color(0xFF855318),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDDB8),
            onPrimaryContainer = Color(0xFF2B1700),
            secondary = Color(0xFF725A42),
            secondaryContainer = Color(0xFFFEDDBE),
            onSecondaryContainer = Color(0xFF291806),
            tertiary = Color(0xFF58633A),
            background = Color(0xFFFFF8F4),
            surface = Color(0xFFFFF8F4),
            surfaceVariant = Color(0xFFF0E0D0),
            onSurfaceVariant = Color(0xFF50453A)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFFCB975),
            onPrimary = Color(0xFF482A00),
            primaryContainer = Color(0xFF683D00),
            onPrimaryContainer = Color(0xFFFFDDB8),
            secondary = Color(0xFFE1C1A4),
            secondaryContainer = Color(0xFF58422C),
            onSecondaryContainer = Color(0xFFFEDDBE),
            tertiary = Color(0xFFC0CC99),
            background = Color(0xFF19120C),
            surface = Color(0xFF19120C),
            surfaceVariant = Color(0xFF50453A),
            onSurfaceVariant = Color(0xFFD4C4B5)
        ),
        boardLight = BoardColors(Color(0xFFF3E1C4), Color(0xFFA0642F)),
        boardDark = BoardColors(Color(0xFFC9B392), Color(0xFF6E4520))
    );

    companion object {
        fun byId(id: String?): ThemePalette = entries.firstOrNull { it.id == id } ?: ROYAL
    }
}

enum class DarkModePreference(val id: String, val title: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun byId(id: String?): DarkModePreference = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
