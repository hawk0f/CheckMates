package dev.hawk0f.checkmates.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.ui.theme.BoardColors
import dev.hawk0f.checkmates.ui.theme.CheckIcon
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.DarkModePreference
import dev.hawk0f.checkmates.resources.flow_checkmates_name
import dev.hawk0f.checkmates.resources.flow_current
import dev.hawk0f.checkmates.resources.flow_lichess_name
import dev.hawk0f.checkmates.resources.flow_section
import dev.hawk0f.checkmates.resources.flow_switch_to
import dev.hawk0f.checkmates.session.AppFlow
import dev.hawk0f.checkmates.session.FlowManager
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.ThemeManager
import dev.hawk0f.checkmates.ui.theme.ThemePalette
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.theme_amber
import dev.hawk0f.checkmates.resources.theme_emerald
import dev.hawk0f.checkmates.resources.theme_ocean
import dev.hawk0f.checkmates.resources.theme_royal
import dev.hawk0f.checkmates.resources.theme_sage
import dev.hawk0f.checkmates.resources.settings_appearance
import dev.hawk0f.checkmates.resources.settings_board_theme
import dev.hawk0f.checkmates.resources.settings_dark_mode_dark
import dev.hawk0f.checkmates.resources.settings_dark_mode_light
import dev.hawk0f.checkmates.resources.settings_dark_mode_system
import dev.hawk0f.checkmates.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_back

@Composable
fun SettingsScreen(onBack: () -> Unit, onSwitchFlow: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(
                    direction = ChevronDirection.LEFT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.flow_section))
                val flowName = stringResource(
                    if (FlowManager.current == AppFlow.LICHESS) {
                        Res.string.flow_lichess_name
                    } else {
                        Res.string.flow_checkmates_name
                    }
                )
                val otherName = stringResource(
                    if (FlowManager.other() == AppFlow.LICHESS) {
                        Res.string.flow_lichess_name
                    } else {
                        Res.string.flow_checkmates_name
                    }
                )
                Text(
                    text = stringResource(Res.string.flow_current, flowName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillButton(
                    text = stringResource(Res.string.flow_switch_to, otherName),
                    onClick = onSwitchFlow,
                    tone = PillTone.BAND,
                    compact = true
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.settings_appearance))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    for (preference in DarkModePreference.entries) {
                        SelectPill(
                            text = darkModeLabel(preference),
                            selected = ThemeManager.darkMode == preference,
                            onClick = { ThemeManager.selectDarkMode(preference) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.settings_board_theme))
                for (palette in ThemePalette.entries) {
                    PaletteRow(
                        palette = palette,
                        selected = ThemeManager.palette == palette,
                        onSelect = { ThemeManager.selectPalette(palette) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRow(palette: ThemePalette, selected: Boolean, onSelect: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        container = if (selected) scheme.primary else scheme.surfaceVariant,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoardSwatch(palette.boardLight)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paletteTitle(palette),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) scheme.onPrimary else scheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    for (color in listOf(
                        palette.light.primary,
                        palette.light.secondary,
                        palette.dark.primary
                    )) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(scheme.onPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    CheckIcon(color = scheme.primary, size = 14.dp)
                }
            }
        }
    }
}

@Composable
private fun BoardSwatch(colors: BoardColors) {
    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        repeat(2) { row ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                repeat(2) { column ->
                    val dark = (row + column) % 2 == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(if (dark) colors.darkSquare else colors.lightSquare)
                    )
                }
            }
        }
    }
}

@Composable
private fun darkModeLabel(preference: DarkModePreference): String = stringResource(
    when (preference) {
        DarkModePreference.SYSTEM -> Res.string.settings_dark_mode_system
        DarkModePreference.LIGHT -> Res.string.settings_dark_mode_light
        DarkModePreference.DARK -> Res.string.settings_dark_mode_dark
    }
)

@Composable
private fun paletteTitle(palette: ThemePalette): String = stringResource(
    when (palette) {
        ThemePalette.SAGE -> Res.string.theme_sage
        ThemePalette.ROYAL -> Res.string.theme_royal
        ThemePalette.EMERALD -> Res.string.theme_emerald
        ThemePalette.OCEAN -> Res.string.theme_ocean
        ThemePalette.AMBER -> Res.string.theme_amber
    }
)
