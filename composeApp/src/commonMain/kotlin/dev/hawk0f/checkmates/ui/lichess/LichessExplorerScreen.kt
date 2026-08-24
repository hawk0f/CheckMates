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
import dev.hawk0f.checkmates.ui.game.BoardBox
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
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.explorer_dialog_title
import dev.hawk0f.checkmates.resources.explorer_legend_black
import dev.hawk0f.checkmates.resources.explorer_legend_draw
import dev.hawk0f.checkmates.resources.explorer_legend_white
import dev.hawk0f.checkmates.resources.explorer_play_and_continue
import dev.hawk0f.checkmates.resources.explorer_players_and_year
import dev.hawk0f.checkmates.resources.explorer_replies
import dev.hawk0f.checkmates.resources.explorer_reset
import dev.hawk0f.checkmates.resources.explorer_share_percent
import dev.hawk0f.checkmates.resources.explorer_start_chip
import dev.hawk0f.checkmates.resources.explorer_starting_position
import dev.hawk0f.checkmates.resources.explorer_tab_lichess
import dev.hawk0f.checkmates.resources.explorer_tab_masters
import dev.hawk0f.checkmates.resources.explorer_tab_you
import dev.hawk0f.checkmates.resources.explorer_title
import dev.hawk0f.checkmates.resources.explorer_top_game
import dev.hawk0f.checkmates.resources.explorer_top_master_game
import dev.hawk0f.checkmates.resources.explorer_unknown_player
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_close
import dev.hawk0f.checkmates.resources.a11y_undo_move

@Composable
fun LichessExplorerScreen(
    onBack: () -> Unit,
    startFen: String? = null,
    viewModel: LichessExplorerViewModel = viewModel { LichessExplorerViewModel(startFen) }
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
                    } ?: stringResource(Res.string.explorer_starting_position),
                    color = accents.bandStrong
                )
                Text(
                    text = position?.opening?.name ?: stringResource(Res.string.explorer_title),
                    style = MaterialTheme.typography.displaySmall
                )
            }
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_close)) {
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
                options = listOf(
                    stringResource(Res.string.explorer_tab_lichess),
                    stringResource(Res.string.explorer_tab_masters),
                    uiState.username ?: stringResource(Res.string.explorer_tab_you)
                ),
                selectedIndex = uiState.source.ordinal,
                onSelect = { viewModel.onSourceChange(ExplorerSource.entries[it]) },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.gameState?.let { state ->
                BoardBox(modifier = Modifier.fillMaxWidth()) { boardModifier ->
                    ChessBoard(
                        gameState = state,
                        selected = null,
                        legalTargets = emptySet(),
                        flipped = false,
                        onSquareTap = {},
                        interactive = false,
                        showCoordinates = false,
                        modifier = boardModifier.clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(
                    text = stringResource(Res.string.explorer_replies, formatCount(position?.total ?: 0)),
                    color = accents.bandStrong
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CircleButton(
                        onClick = viewModel::undo,
                        enabled = uiState.moves.isNotEmpty(),
                        size = 38.dp,
                        contentDescription = stringResource(Res.string.a11y_undo_move)
                    ) {
                        ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand, size = 14.dp)
                    }
                    PillButton(stringResource(Res.string.explorer_reset), viewModel::reset, tone = PillTone.SOFT, compact = true)
                }
            }

            if (position != null) {
                for (move in position.moves.take(8)) {
                    MoveRow(
                        move = move,
                        share = if (position.total > 0) move.total.toFloat() / position.total else 0f,
                        onPlay = { viewModel.playMove(move.uci) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendDot(stringResource(Res.string.explorer_legend_white), scheme.surfaceBright)
                    LegendDot(stringResource(Res.string.explorer_legend_draw), scheme.outline)
                    LegendDot(stringResource(Res.string.explorer_legend_black), scheme.inverseSurface)
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
                        SectionLabel(
                            text = stringResource(
                                if (uiState.source == ExplorerSource.MASTERS) {
                                    Res.string.explorer_top_master_game
                                } else {
                                    Res.string.explorer_top_game
                                }
                            ),
                            color = accents.bandStrong
                        )
                        Text(
                            text = stringResource(
                                Res.string.explorer_players_and_year,
                                game.white?.name ?: stringResource(Res.string.explorer_unknown_player),
                                game.black?.name ?: stringResource(Res.string.explorer_unknown_player)
                            ) + (game.year?.let { ", $it" } ?: ""),
                            style = MaterialTheme.typography.titleSmall
                        )
                        CodeChip(uiState.moves.joinToString(" ").ifEmpty { stringResource(Res.string.explorer_start_chip) })
                    }
                }
                position.moves.firstOrNull()?.let { top ->
                    PillButton(
                        text = stringResource(
                            Res.string.explorer_play_and_continue,
                            top.san ?: top.uci
                        ),
                        onClick = { viewModel.playMove(top.uci) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.explorer_dialog_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun MoveRow(move: LichessExplorerMove, share: Float, onPlay: () -> Unit) {
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
            text = stringResource(Res.string.explorer_share_percent, (share * 100).toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
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
