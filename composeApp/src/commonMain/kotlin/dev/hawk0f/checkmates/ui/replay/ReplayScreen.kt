package dev.hawk0f.checkmates.ui.replay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.shared.engine.GameAnalyzer
import dev.hawk0f.checkmates.shared.engine.MoveQuality
import dev.hawk0f.checkmates.ui.theme.AppAccents
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
import dev.hawk0f.checkmates.resources.replay_accuracy
import dev.hawk0f.checkmates.resources.replay_analyse
import dev.hawk0f.checkmates.resources.replay_analysing
import dev.hawk0f.checkmates.resources.replay_back_to_history
import dev.hawk0f.checkmates.resources.replay_best_was
import dev.hawk0f.checkmates.resources.replay_quality_best
import dev.hawk0f.checkmates.resources.replay_quality_blunder
import dev.hawk0f.checkmates.resources.replay_quality_good
import dev.hawk0f.checkmates.resources.replay_quality_inaccuracy
import dev.hawk0f.checkmates.resources.replay_quality_mistake
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
fun ReplayScreen(
    item: GameHistoryItem,
    onBack: () -> Unit,
    analysisViewModel: ReplayAnalysisViewModel = viewModel { ReplayAnalysisViewModel() }
) {
    var moveIndex by remember(item) { mutableIntStateOf(item.uciHistory.size) }
    val analysis by analysisViewModel.uiState.collectAsStateWithLifecycle()
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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
        val currentAnalysis = analysis.summary?.moves?.getOrNull(moveIndex - 1)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (analysis.summary != null) {
                EvaluationBar(
                    whiteScore = currentAnalysis?.let(GameAnalyzer::whitePerspective) ?: 0,
                    flipped = flipped,
                    modifier = Modifier.fillMaxHeight().padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                )
            }
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

            if (currentAnalysis != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = qualityLabel(currentAnalysis.quality),
                        style = MaterialTheme.typography.titleMedium,
                        color = qualityColor(currentAnalysis.quality, accents, scheme)
                    )
                    if (currentAnalysis.bestMove != null && currentAnalysis.bestMove != currentAnalysis.uci) {
                        Text(
                            text = stringResource(Res.string.replay_best_was, currentAnalysis.bestMove.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.bandStrong
                        )
                    }
                }
            }

            analysis.summary?.let { summary ->
                Text(
                    text = stringResource(
                        Res.string.replay_accuracy,
                        summary.whiteAverageLoss,
                        summary.blackAverageLoss
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.bandStrong
                )
            }

            if (total > 0 && analysis.summary == null) {
                PillButton(
                    text = if (analysis.running) {
                        stringResource(Res.string.replay_analysing, analysis.analysedPlies, analysis.totalPlies)
                    } else {
                        stringResource(Res.string.replay_analyse)
                    },
                    onClick = { analysisViewModel.analyse(item.uciHistory) },
                    enabled = !analysis.running,
                    tone = PillTone.ACCENT,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
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

@Composable
private fun EvaluationBar(whiteScore: Int, flipped: Boolean, modifier: Modifier = Modifier) {
    val fraction = GameAnalyzer.evaluationBarFraction(whiteScore)
    val whiteShare = if (flipped) 1f - fraction else fraction
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.width(10.dp).clip(RoundedCornerShape(5.dp)).background(scheme.inverseSurface)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .weight((1f - whiteShare).coerceIn(0.01f, 0.99f))
                .background(scheme.inverseSurface)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(whiteShare.coerceIn(0.01f, 0.99f))
                .background(scheme.surface)
        )
    }
}

@Composable
private fun qualityLabel(quality: MoveQuality): String = stringResource(
    when (quality) {
        MoveQuality.BEST -> Res.string.replay_quality_best
        MoveQuality.GOOD -> Res.string.replay_quality_good
        MoveQuality.INACCURACY -> Res.string.replay_quality_inaccuracy
        MoveQuality.MISTAKE -> Res.string.replay_quality_mistake
        MoveQuality.BLUNDER -> Res.string.replay_quality_blunder
    }
)

private fun qualityColor(quality: MoveQuality, accents: AppAccents, scheme: ColorScheme): Color = when (quality) {
    MoveQuality.BEST, MoveQuality.GOOD -> accents.positive
    MoveQuality.INACCURACY -> accents.bandStrong
    MoveQuality.MISTAKE, MoveQuality.BLUNDER -> accents.negative
}

private fun scoreLabel(item: GameHistoryItem): String = when (item.winner) {
    PieceColor.WHITE -> "1–0"
    PieceColor.BLACK -> "0–1"
    null -> "½–½"
}

@Composable
private fun reasonShort(item: GameHistoryItem): String = reasonLabel(item.reason)
