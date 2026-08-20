package dev.hawk0f.checkmates.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.ThemeManager
import dev.hawk0f.checkmates.ui.theme.ThemePalette

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack) {
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
                SectionLabel("Appearance")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    for (preference in DarkModePreference.entries) {
                        SelectPill(
                            text = preference.title,
                            selected = ThemeManager.darkMode == preference,
                            onClick = { ThemeManager.selectDarkMode(preference) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel("Board theme")
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
                    text = palette.title,
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
