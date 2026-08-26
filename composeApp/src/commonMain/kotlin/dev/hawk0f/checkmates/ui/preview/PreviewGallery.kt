package dev.hawk0f.checkmates.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.ui.theme.AppTheme
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.DarkModePreference
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SegmentedPills
import dev.hawk0f.checkmates.ui.theme.ThemeManager

@Composable
fun PreviewGallery(onExit: () -> Unit = {}) {
    var openSpecId by remember { mutableStateOf<String?>(null) }
    val spec = previewSpecs.firstOrNull { it.id == openSpecId }

    if (spec == null) {
        AppTheme {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                PreviewIndex(
                    onOpen = { openSpecId = it },
                    onExit = onExit,
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
        return
    }

    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton(
                        text = "Catalog",
                        onClick = { openSpecId = null },
                        tone = PillTone.INK,
                        compact = true
                    )
                    Text(spec.id, style = MaterialTheme.typography.titleSmall)
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (spec.fillsScreen) {
                        PreviewFrame(spec)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PreviewFrame(spec)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewIndex(onOpen: (String) -> Unit, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val groups = listOf(
        "Elements" to componentPreviewSpecs,
        "Dialogs" to dialogPreviewSpecs,
        "Boards" to boardPreviewSpecs,
        "Screens" to screenPreviewSpecs
    )
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preview catalog", style = MaterialTheme.typography.displaySmall)
            PillButton(text = "Close", onClick = onExit, tone = PillTone.SOFT, compact = true)
        }
        SegmentedPills(
            options = DarkModePreference.entries.map { it.id.replaceFirstChar { char -> char.uppercase() } },
            selectedIndex = DarkModePreference.entries.indexOf(ThemeManager.darkMode),
            onSelect = { ThemeManager.selectDarkMode(DarkModePreference.entries[it]) }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for ((title, specs) in groups) {
                item(key = "header-$title") {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                        SectionLabel("$title · ${specs.size}")
                        Hairline()
                    }
                }
                items(specs, key = { it.id }) { spec ->
                    ListRow(
                        title = spec.id,
                        trailing = { if (!spec.capturable) CodeChip("no golden") },
                        onClick = { onOpen(spec.id) }
                    )
                }
            }
        }
    }
}
