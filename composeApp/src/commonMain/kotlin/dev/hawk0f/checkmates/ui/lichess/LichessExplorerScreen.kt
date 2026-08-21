package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.net.lichess.LichessExplorerMove
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SegmentedTabs
import dev.hawk0f.checkmates.ui.theme.WinRateBar

@Composable
fun LichessExplorerScreen(
    onBack: () -> Unit,
    viewModel: LichessExplorerViewModel = viewModel { LichessExplorerViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val position = uiState.position

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SectionLabel(
                    text = position?.opening?.let { opening ->
                        listOfNotNull(opening.eco, opening.name).joinToString(" · ")
                    } ?: "Starting position",
                    color = accents.bandStrong
                )
                Text(
                    text = position?.opening?.name ?: "Opening explorer",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            CircleButton(onClick = onBack) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SegmentedTabs(
                options = listOf("Lichess", "Masters", uiState.username ?: "You"),
                selectedIndex = uiState.source.ordinal,
                onSelect = { viewModel.onSourceChange(ExplorerSource.entries[it]) },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.gameState?.let { state ->
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    ChessBoard(
                        gameState = state,
                        selected = null,
                        legalTargets = emptySet(),
                        flipped = false,
                        onSquareTap = {},
                interactive = false,
                        showCoordinates = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(
                    text = "Replies · ${formatCount(position?.total ?: 0)} games",
                    color = accents.bandStrong
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CircleButton(
                        onClick = viewModel::undo,
                        enabled = uiState.moves.isNotEmpty(),
                        size = 38.dp
                    ) {
                        ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand, size = 14.dp)
                    }
                    PillButton("Reset", viewModel::reset, tone = PillTone.SOFT, compact = true)
                }
            }

            if (position != null) {
                for (move in position.moves.take(8)) {
                    MoveRow(move = move, onPlay = { viewModel.playMove(move.uci) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendDot("White", scheme.surfaceBright)
                    LegendDot("Draw", scheme.outline)
                    LegendDot("Black", scheme.inverseSurface)
                }
                position.topGames.firstOrNull()?.let { game ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(scheme.surfaceContainer)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SectionLabel("Top game", color = accents.bandStrong)
                        Text(
                            text = "${game.white?.name ?: "?"} — ${game.black?.name ?: "?"}" +
                                (game.year?.let { ", $it" } ?: ""),
                            style = MaterialTheme.typography.titleSmall
                        )
                        CodeChip(uiState.moves.joinToString(" ").ifEmpty { "start" })
                    }
                }
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Explorer", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun MoveRow(move: LichessExplorerMove, onPlay: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = move.san ?: move.uci,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(52.dp)
        )
        WinRateBar(
            white = move.white,
            draws = move.draws,
            black = move.black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatCount(move.total),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
                .padding(vertical = 5.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 100_000 / 10.0}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}
