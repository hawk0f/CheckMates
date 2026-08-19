package dev.hawk0f.checkmates.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.ui.theme.BoardColors
import dev.hawk0f.checkmates.ui.theme.DarkModePreference
import dev.hawk0f.checkmates.ui.theme.ThemeManager
import dev.hawk0f.checkmates.ui.theme.ThemePalette

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DarkModePreference.entries.forEachIndexed { index, preference ->
                SegmentedButton(
                    selected = ThemeManager.darkMode == preference,
                    onClick = { ThemeManager.selectDarkMode(preference) },
                    shape = SegmentedButtonDefaults.itemShape(index, DarkModePreference.entries.size)
                ) {
                    Text(preference.title)
                }
            }
        }

        Text("Color theme", style = MaterialTheme.typography.titleMedium)
        ThemePalette.entries.forEach { palette ->
            PaletteCard(
                palette = palette,
                selected = ThemeManager.palette == palette,
                onSelect = { ThemeManager.selectPalette(palette) }
            )
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun PaletteCard(palette: ThemePalette, selected: Boolean, onSelect: () -> Unit) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoardSwatch(palette.boardLight)
            Column(modifier = Modifier.weight(1f)) {
                Text(palette.title, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    ColorDot(palette)
                }
            }
            RadioButton(selected = selected, onClick = onSelect)
        }
    }
}

@Composable
private fun BoardSwatch(colors: BoardColors) {
    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
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
private fun ColorDot(palette: ThemePalette) {
    listOf(palette.light.primary, palette.light.primaryContainer, palette.dark.primary).forEach { color ->
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
        )
    }
}
