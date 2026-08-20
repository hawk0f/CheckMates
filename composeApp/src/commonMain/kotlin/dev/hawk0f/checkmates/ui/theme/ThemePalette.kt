package dev.hawk0f.checkmates.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class BoardColors(
    val lightSquare: Color,
    val darkSquare: Color,
    val lightHighlight: Color? = null,
    val darkHighlight: Color? = null
)

data class AppAccents(
    val pageAlt: Color,
    val band: Color,
    val onBand: Color,
    val bandStrong: Color,
    val positive: Color,
    val negative: Color
)

enum class ThemePalette(
    val id: String,
    val title: String,
    val light: ColorScheme,
    val dark: ColorScheme,
    val boardLight: BoardColors,
    val boardDark: BoardColors,
    val accentsLight: AppAccents? = null,
    val accentsDark: AppAccents? = null
) {
    SAGE(
        id = "sage",
        title = "Board First",
        light = lightColorScheme(
            primary = Color(0xFFC67139),
            onPrimary = Color(0xFFFFF2EB),
            primaryContainer = Color(0xFFFFE1D0),
            onPrimaryContainer = Color(0xFF8C491A),
            secondary = Color(0xFF56633F),
            onSecondary = Color(0xFFF0FAE1),
            secondaryContainer = Color(0xFFE1EECC),
            onSecondaryContainer = Color(0xFF3D472B),
            tertiary = Color(0xFF7A8A5E),
            onTertiary = Color(0xFFF0FAE1),
            tertiaryContainer = Color(0xFFEBDDC5),
            onTertiaryContainer = Color(0xFF474238),
            background = Color(0xFFF5EAD8),
            onBackground = Color(0xFF201E1D),
            surface = Color(0xFFF5EAD8),
            onSurface = Color(0xFF201E1D),
            surfaceVariant = Color(0xFFEBDDC5),
            onSurfaceVariant = Color(0xFF645C50),
            surfaceContainer = Color(0xFFF9F4ED),
            surfaceContainerHigh = Color(0xFFEBDDC5),
            surfaceContainerLow = Color(0xFFF9F4ED),
            outline = Color(0xFFA19786),
            outlineVariant = Color(0xFFC0B6A5),
            inverseSurface = Color(0xFF201E1D),
            inverseOnSurface = Color(0xFFF5EAD8),
            error = Color(0xFF8C491A),
            onError = Color(0xFFFFF2EB),
            errorContainer = Color(0xFFFFE1D0),
            onErrorContainer = Color(0xFF8C491A)
        ),
        dark = darkColorScheme(
            primary = Color(0xFFDF9257),
            onPrimary = Color(0xFF2E1607),
            primaryContainer = Color(0xFF5E3013),
            onPrimaryContainer = Color(0xFFFFE1D0),
            secondary = Color(0xFFAEC08C),
            onSecondary = Color(0xFF232B18),
            secondaryContainer = Color(0xFF3B452C),
            onSecondaryContainer = Color(0xFFE1EECC),
            tertiary = Color(0xFF9CAE7C),
            onTertiary = Color(0xFF222B15),
            tertiaryContainer = Color(0xFF3A352C),
            onTertiaryContainer = Color(0xFFE7DCC9),
            background = Color(0xFF1B1917),
            onBackground = Color(0xFFF2E7D6),
            surface = Color(0xFF1B1917),
            onSurface = Color(0xFFF2E7D6),
            surfaceVariant = Color(0xFF332F28),
            onSurfaceVariant = Color(0xFFC5BAA8),
            surfaceContainer = Color(0xFF2A2721),
            surfaceContainerHigh = Color(0xFF332F28),
            surfaceContainerLow = Color(0xFF232019),
            outline = Color(0xFF8B8171),
            outlineVariant = Color(0xFF4A443A),
            inverseSurface = Color(0xFFF2E7D6),
            inverseOnSurface = Color(0xFF1B1917),
            error = Color(0xFFE7A277),
            onError = Color(0xFF3A1B06),
            errorContainer = Color(0xFF5E3013),
            onErrorContainer = Color(0xFFFFE1D0)
        ),
        boardLight = BoardColors(
            lightSquare = Color(0xFFF0FAE1),
            darkSquare = Color(0xFF8FA073),
            lightHighlight = Color(0xFFE1EECC),
            darkHighlight = Color(0xFF728157)
        ),
        boardDark = BoardColors(
            lightSquare = Color(0xFFC7D4AB),
            darkSquare = Color(0xFF64744C),
            lightHighlight = Color(0xFFB2C293),
            darkHighlight = Color(0xFF52603C)
        ),
        accentsLight = AppAccents(
            pageAlt = Color(0xFFF0FAE1),
            band = Color(0xFFE1EECC),
            onBand = Color(0xFF56633F),
            bandStrong = Color(0xFF7A8A5E),
            positive = Color(0xFF56633F),
            negative = Color(0xFF8C491A)
        ),
        accentsDark = AppAccents(
            pageAlt = Color(0xFF1A1F16),
            band = Color(0xFF2F3A24),
            onBand = Color(0xFFC9D9A8),
            bandStrong = Color(0xFF7A8A5E),
            positive = Color(0xFFAEC08C),
            negative = Color(0xFFE7A277)
        )
    ),
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
        fun byId(id: String?): ThemePalette = entries.firstOrNull { it.id == id } ?: SAGE
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
