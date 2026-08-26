package dev.hawk0f.checkmates.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hawk0f.checkmates.ui.profile.AvatarBadge
import dev.hawk0f.checkmates.ui.theme.ChoiceCard
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.FlagIcon
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.OutlineTile
import dev.hawk0f.checkmates.ui.theme.PauseIcon
import dev.hawk0f.checkmates.ui.theme.PillAction
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillGrid
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.PlayIcon
import dev.hawk0f.checkmates.ui.theme.PlusIcon
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SegmentedPills
import dev.hawk0f.checkmates.ui.theme.SegmentedTabs
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.SoftTextField
import dev.hawk0f.checkmates.ui.theme.StatTile
import dev.hawk0f.checkmates.ui.theme.WinRateBar
import androidx.compose.ui.tooling.preview.Preview

internal val pillButtonsSpec = PreviewSpec("pill-buttons") {
    for (tone in PillTone.entries) {
        PillButton(
            text = tone.name.lowercase().replaceFirstChar { it.uppercase() },
            onClick = {},
            tone = tone,
            modifier = Modifier.fillMaxWidth()
        )
    }
    PillButton(
        text = "Disabled",
        onClick = {},
        tone = PillTone.ACCENT,
        enabled = false,
        modifier = Modifier.fillMaxWidth()
    )
}

internal val pillGridSpec = PreviewSpec("pill-grid") {
    PillGrid(
        actions = listOf(
            PillAction("Rematch", {}, PillTone.ACCENT),
            PillAction("Analyse", {}),
            PillAction("Share PGN", {}),
            PillAction("Exit", {}, PillTone.SOFT, enabled = false)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

internal val segmentedControlsSpec = PreviewSpec("segmented-controls") {
    SegmentedPills(
        options = listOf("Bullet", "Blitz", "Rapid", "Classical"),
        selectedIndex = 1,
        onSelect = {}
    )
    SegmentedTabs(
        options = listOf("Games", "Puzzles", "Openings"),
        selectedIndex = 0,
        onSelect = {},
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectPill(text = "3+2", selected = true, onClick = {})
        SelectPill(text = "5+0", selected = false, onClick = {})
        SelectPill(text = "10+0", selected = false, onClick = {})
    }
}

internal val circleButtonsSpec = PreviewSpec("circle-buttons") {
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleButton(onClick = {}, contentDescription = "Play") {
            PlayIcon(color = scheme.onSurfaceVariant)
        }
        CircleButton(onClick = {}, container = scheme.primary, contentDescription = "Pause") {
            PauseIcon(color = scheme.onPrimary)
        }
        CircleButton(onClick = {}, contentDescription = "Resign") {
            FlagIcon(color = scheme.error)
        }
        CircleButton(onClick = {}, enabled = false, contentDescription = "Add") {
            PlusIcon(color = scheme.outline)
        }
    }
}

internal val statTilesSpec = PreviewSpec("stat-tiles") {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(value = "1842", label = "Rating", accent = true, modifier = Modifier.weight(1f))
        StatTile(value = "64%", label = "Win rate", modifier = Modifier.weight(1f))
        StatTile(value = "127", label = "Games", modifier = Modifier.weight(1f))
    }
}

internal val softCardSpec = PreviewSpec("soft-card") {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Last game")
            Text("Ruy Lopez, Berlin Defence", style = MaterialTheme.typography.titleMedium)
            Hairline()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeChip("C65")
                CodeChip("32 moves")
                CodeChip("+18")
            }
        }
    }
}

internal val textFieldsSpec = PreviewSpec("text-fields") {
    SoftTextField(
        value = "",
        onValueChange = {},
        placeholder = "Invite code",
        modifier = Modifier.fillMaxWidth()
    )
    SoftTextField(
        value = "hawk0f",
        onValueChange = {},
        placeholder = "Username",
        modifier = Modifier.fillMaxWidth()
    )
}

internal val choiceCardsSpec = PreviewSpec("choice-cards") {
    ChoiceCard(
        title = "Play online",
        subtitle = "Rated games against the pool",
        selected = true,
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    )
    ChoiceCard(
        title = "Play a friend",
        subtitle = "Share a code or scan a QR",
        selected = false,
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    )
}

internal val listRowsSpec = PreviewSpec("list-rows") {
    ListRow(
        title = "Magnus",
        subtitle = "Online now",
        leading = { AvatarBadge(kind = "piece", value = "wq", size = 38.dp, fontSize = 20.sp) },
        trailing = { CodeChip("2882") },
        onClick = {}
    )
    Hairline()
    ListRow(
        title = "Anna",
        subtitle = "Last seen 2h ago",
        leading = { InitialsBadge("an") },
        trailing = { CodeChip("1710") },
        onClick = {}
    )
}

internal val winRateBarsSpec = PreviewSpec("win-rate-bars") {
    SectionLabel("Master database")
    WinRateBar(white = 49, draws = 8, black = 43, modifier = Modifier.fillMaxWidth())
    SectionLabel("Your results")
    WinRateBar(white = 20, draws = 5, black = 75, modifier = Modifier.fillMaxWidth())
}

internal val outlineTilesSpec = PreviewSpec("outline-tiles") {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlineTile(selected = true, onClick = {}, modifier = Modifier.weight(1f)) {
            Text("White", style = MaterialTheme.typography.titleSmall)
        }
        OutlineTile(selected = false, onClick = {}, modifier = Modifier.weight(1f)) {
            Text("Random", style = MaterialTheme.typography.titleSmall)
        }
        OutlineTile(selected = false, onClick = {}, modifier = Modifier.weight(1f)) {
            Text("Black", style = MaterialTheme.typography.titleSmall)
        }
    }
}

internal val avatarBadgesSpec = PreviewSpec("avatar-badges") {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBadge(kind = "emoji", value = "🦊", size = 56.dp, fontSize = 30.sp)
        AvatarBadge(kind = "piece", value = "wk", size = 56.dp, fontSize = 30.sp)
        AvatarBadge(kind = "piece", value = "bn", size = 44.dp, fontSize = 22.sp)
        InitialsBadge(text = "hk", size = 44.dp)
    }
}

internal val componentPreviewSpecs = listOf(
    pillButtonsSpec,
    pillGridSpec,
    segmentedControlsSpec,
    circleButtonsSpec,
    statTilesSpec,
    softCardSpec,
    textFieldsSpec,
    choiceCardsSpec,
    listRowsSpec,
    winRateBarsSpec,
    outlineTilesSpec,
    avatarBadgesSpec
)

@Preview
@Composable
internal fun PillButtonsPreview() = PreviewFrame(pillButtonsSpec)

@Preview
@Composable
internal fun PillGridPreview() = PreviewFrame(pillGridSpec)

@Preview
@Composable
internal fun SegmentedControlsPreview() = PreviewFrame(segmentedControlsSpec)

@Preview
@Composable
internal fun CircleButtonsPreview() = PreviewFrame(circleButtonsSpec)

@Preview
@Composable
internal fun StatTilesPreview() = PreviewFrame(statTilesSpec)

@Preview
@Composable
internal fun SoftCardPreview() = PreviewFrame(softCardSpec)

@Preview
@Composable
internal fun TextFieldsPreview() = PreviewFrame(textFieldsSpec)

@Preview
@Composable
internal fun ChoiceCardsPreview() = PreviewFrame(choiceCardsSpec)

@Preview
@Composable
internal fun ListRowsPreview() = PreviewFrame(listRowsSpec)

@Preview
@Composable
internal fun WinRateBarsPreview() = PreviewFrame(winRateBarsSpec)

@Preview
@Composable
internal fun OutlineTilesPreview() = PreviewFrame(outlineTilesSpec)

@Preview
@Composable
internal fun AvatarBadgesPreview() = PreviewFrame(avatarBadgesSpec)
