package dev.hawk0f.checkmates.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings

object ThemeManager {

    private const val KEY_PALETTE = "theme.palette"
    private const val KEY_DARK_MODE = "theme.darkMode"
    private const val KEY_HOTSEAT_FACING = "theme.hotseatFacing"

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }

    var palette by mutableStateOf(ThemePalette.byId(settings?.getStringOrNull(KEY_PALETTE)))
        private set

    var darkMode by mutableStateOf(DarkModePreference.byId(settings?.getStringOrNull(KEY_DARK_MODE)))
        private set

    var hotseatFacing by mutableStateOf(HotseatFacing.byId(settings?.getStringOrNull(KEY_HOTSEAT_FACING)))
        private set

    fun selectHotseatFacing(value: HotseatFacing) {
        hotseatFacing = value
        settings?.putString(KEY_HOTSEAT_FACING, value.id)
    }

    fun selectPalette(value: ThemePalette) {
        palette = value
        settings?.putString(KEY_PALETTE, value.id)
    }

    fun selectDarkMode(value: DarkModePreference) {
        darkMode = value
        settings?.putString(KEY_DARK_MODE, value.id)
    }
}
