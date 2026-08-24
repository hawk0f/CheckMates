package dev.hawk0f.checkmates.ui.replay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.platform.formatDate
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.PgnBuilder
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.PlayIcon
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.replay_back_to_history
import dev.hawk0f.checkmates.resources.replay_move_of_total
import dev.hawk0f.checkmates.resources.replay_numbered_move
import dev.hawk0f.checkmates.resources.replay_phase_middlegame
import dev.hawk0f.checkmates.resources.replay_phase_opening
import dev.hawk0f.checkmates.resources.replay_players
import dev.hawk0f.checkmates.resources.replay_score_and_date
import dev.hawk0f.checkmates.resources.replay_start_position
import dev.hawk0f.checkmates.ui.theme.reasonLabel
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.a11y_jump_to_end
import dev.hawk0f.checkmates.resources.a11y_jump_to_start
import dev.hawk0f.checkmates.resources.a11y_next_move
import dev.hawk0f.checkmates.resources.a11y_previous_move
import dev.hawk0f.checkmates.resources.a11y_share

@Composable
fun ReplayScreen(item: GameHistoryItem, onBack: () -> Unit) {
    var moveIndex by remember(item) { mutableIntStateOf(item.uciHistory.size) }
    val gameState = remember(item, moveIndex) {
        val game = ChessGame()
        for (uci in item.uciHistory.take(moveIndex)) {
            game.applyUci(uci)
        }
        game.state()
    }
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val shareText = rememberShareText()
    val total = item.uciHistory.size

    Column(modifier = Modifier.fillMaxSize().background(accents.pageAlt)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleButton(
                onClick = onBack,
                container = scheme.onSurface.copy(alpha = 0.08f),
                contentDescription = stringResource(Res.string.a11y_back)
            ) {
                ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.replay_players, item.whiteName, item.blackName),
                    style = MaterialTheme.typography.titleMedium
                )
                SectionLabel(
                    text = stringResource(
                        Res.string.replay_score_and_date,
                        scoreLabel(item),
                        formatDate(item.finishedAtMillis)
                    ),
                    color = accents.bandStrong
                )
            }
            CircleButton(
                contentDescription = stringResource(Res.string.a11y_share),
                onClick = {
                    shareText(
                        PgnBuilder.build(
                            whiteName = item.whiteName,
                            blackName = item.blackName,
                            winner = item.winner,
                            reason = item.reason,
                            uciHistory = item.uciHistory,
                            dateMillis = item.finishedAtMillis
                        )
                    )
                },
                container = scheme.onSurface.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "PGN",
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.onBand
                )
            }
        }

        val flipped = item.myColor == PieceColor.BLACK
        BoardBox(modifier = Modifier.weight(1f)) { boardModifier ->
            ChessBoard(
                gameState = gameState,
                selected = null,
                legalTargets = emptySet(),
                flipped = flipped,
                onSquareTap = {},
                interactive = false,
                modifier = boardModifier
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (moveIndex == 0) {
                        stringResource(Res.string.replay_start_position)
                    } else {
                        stringResource(
                            Res.string.replay_numbered_move,
                            (moveIndex + 1) / 2,
                            item.uciHistory[moveIndex - 1]
                        )
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(Res.string.replay_move_of_total, moveIndex, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.bandStrong
                )
            }

            if (total > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Slider(
                        value = moveIndex.toFloat(),
                        onValueChange = { moveIndex = it.toInt().coerceIn(0, total) },
                        valueRange = 0f..total.toFloat(),
                        steps = (total - 1).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.inverseSurface,
                            activeTrackColor = scheme.primary,
                            inactiveTrackColor = scheme.onSurface.copy(alpha = 0.12f),
                            activeTickColor = scheme.primary,
                            inactiveTickColor = scheme.onSurface.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel(stringResource(Res.string.replay_phase_opening), color = accents.bandStrong)
                        SectionLabel(stringResource(Res.string.replay_phase_middlegame), color = accents.bandStrong)
                        SectionLabel(reasonShort(item), color = accents.bandStrong)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleButton(
                        onClick = { moveIndex = (moveIndex - 1).coerceAtLeast(0) },
                        enabled = moveIndex > 0,
                        size = 48.dp,
                        container = scheme.onSurface.copy(alpha = 0.07f),
                        contentDescription = stringResource(Res.string.a11y_previous_move)
                    ) {
                        ChevronIcon(
                            direction = ChevronDirection.LEFT,
                            color = accents.onBand,
                            doubled = true
                        )
                    }
                    CircleButton(
                        onClick = { moveIndex = if (moveIndex >= total) 0 else total },
                        size = 64.dp,
                        container = scheme.inverseSurface,
                        contentDescription = stringResource(
                            if (moveIndex >= total) {
                                Res.string.a11y_jump_to_start
                            } else {
                                Res.string.a11y_jump_to_end
                            }
                        )
                    ) {
                        if (moveIndex >= total) {
                            ChevronIcon(
                                direction = ChevronDirection.LEFT,
                                color = scheme.inverseOnSurface,
                                size = 22.dp,
                                doubled = true
                            )
                        } else {
                            PlayIcon(color = scheme.inverseOnSurface, size = 22.dp)
                        }
                    }
                    CircleButton(
                        onClick = { moveIndex = (moveIndex + 1).coerceAtMost(total) },
                        enabled = moveIndex < total,
                        size = 48.dp,
                        container = scheme.onSurface.copy(alpha = 0.07f),
                        contentDescription = stringResource(Res.string.a11y_next_move)
                    ) {
                        ChevronIcon(
                            direction = ChevronDirection.RIGHT,
                            color = accents.onBand,
                            doubled = true
                        )
                    }
                }
            }

            PillButton(
                text = stringResource(Res.string.replay_back_to_history),
                onClick = onBack,
                tone = PillTone.SOFT,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun scoreLabel(item: GameHistoryItem): String = when (item.winner) {
    PieceColor.WHITE -> "1–0"
    PieceColor.BLACK -> "0–1"
    null -> "½–½"
}

@Composable
private fun reasonShort(item: GameHistoryItem): String = reasonLabel(item.reason)
