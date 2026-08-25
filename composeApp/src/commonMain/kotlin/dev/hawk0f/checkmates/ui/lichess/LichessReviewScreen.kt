package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.review_accuracy
import dev.hawk0f.checkmates.resources.review_accuracy_black
import dev.hawk0f.checkmates.resources.review_accuracy_white
import dev.hawk0f.checkmates.resources.review_back
import dev.hawk0f.checkmates.resources.review_cloud_eval_details
import dev.hawk0f.checkmates.resources.review_eval_fallback
import dev.hawk0f.checkmates.resources.review_evaluation_off
import dev.hawk0f.checkmates.resources.review_game_and_move
import dev.hawk0f.checkmates.resources.review_no_cloud_eval
import dev.hawk0f.checkmates.resources.review_no_stored_analysis
import dev.hawk0f.checkmates.resources.review_open_in_explorer
import dev.hawk0f.checkmates.resources.review_principal_variations
import dev.hawk0f.checkmates.resources.review_title
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_close
import dev.hawk0f.checkmates.resources.a11y_next_move
import dev.hawk0f.checkmates.resources.a11y_previous_move

@Composable
fun LichessReviewScreen(
    gameId: String,
    onBack: () -> Unit,
    onOpenExplorer: ((String) -> Unit)? = null,
    viewModel: LichessReviewViewModel = viewModel { LichessReviewViewModel(gameId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SectionLabel(
                    text = stringResource(Res.string.review_game_and_move, uiState.gameId, uiState.moveIndex),
                    color = accents.bandStrong
                )
                Text(stringResource(Res.string.review_title), style = MaterialTheme.typography.displaySmall)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.export?.players?.white?.analysis?.accuracy?.let { accuracy ->
                    CodeChip("${accuracy.toInt()}%")
                }
                CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_close)) {
                    CloseIcon(color = scheme.onSurfaceVariant)
                }
            }
        }

        val blocked = uiState.blocked
        if (blocked != null) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
            ) {
                Text(stringResource(Res.string.review_evaluation_off), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = blocked,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                PillButton(
                    text = stringResource(Res.string.review_back),
                    onClick = onBack,
                    tone = PillTone.SOFT,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@Column
        }

        if (uiState.loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = scheme.primary)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.inverseSurface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val eval = uiState.eval
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = eval?.let { formatEval(it.pvs.firstOrNull()?.cp, it.pvs.firstOrNull()?.mate) }
                            ?: stringResource(Res.string.review_no_cloud_eval),
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.inverseOnSurface
                    )
                    Text(
                        text = eval?.let {
                            stringResource(Res.string.review_cloud_eval_details, it.depth, it.knodes)
                        } ?: stringResource(Res.string.review_eval_fallback),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.inverseOnSurface.copy(alpha = 0.6f)
                    )
                }
            }

            uiState.eval?.let { eval ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(
                        text = stringResource(Res.string.review_principal_variations, eval.pvs.size),
                        color = accents.bandStrong
                    )
                    for (pv in eval.pvs) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = formatEval(pv.cp, pv.mate),
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.primary
                            )
                            Text(
                                text = pv.moves.split(' ').take(6).joinToString(" "),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val total = uiState.moves.size
            if (total > 0) {
                Slider(
                    value = uiState.moveIndex.toFloat(),
                    onValueChange = { viewModel.goTo(it.toInt()) },
                    valueRange = 0f..total.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.inverseSurface,
                        activeTrackColor = scheme.primary,
                        inactiveTrackColor = scheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleButton(
                        onClick = { viewModel.step(-1) },
                        enabled = uiState.moveIndex > 0,
                        size = 48.dp,
                        contentDescription = stringResource(Res.string.a11y_previous_move)
                    ) {
                        ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand)
                    }
                    val fen = uiState.gameState?.fen
                    if (onOpenExplorer != null && fen != null) {
                        PillButton(
                            text = stringResource(Res.string.review_open_in_explorer),
                            onClick = { onOpenExplorer(fen) },
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    CircleButton(
                        onClick = { viewModel.step(1) },
                        enabled = uiState.moveIndex < total,
                        size = 48.dp,
                        contentDescription = stringResource(Res.string.a11y_next_move)
                    ) {
                        ChevronIcon(direction = ChevronDirection.RIGHT, color = accents.onBand)
                    }
                }
            }

            uiState.export?.let { export ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel(stringResource(Res.string.review_accuracy), color = accents.bandStrong)
                    Text(
                        text = listOfNotNull(
                            export.players?.white?.analysis?.accuracy?.let {
                                stringResource(Res.string.review_accuracy_white, it.toInt())
                            },
                            export.players?.black?.analysis?.accuracy?.let {
                                stringResource(Res.string.review_accuracy_black, it.toInt())
                            }
                        ).joinToString(" · ").ifEmpty { stringResource(Res.string.review_no_stored_analysis) },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.review_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

private fun formatEval(cp: Int?, mate: Int?): String {
    if (mate != null) {
        return if (mate > 0) "#$mate" else "#$mate"
    }
    if (cp == null) {
        return "—"
    }
    val pawns = cp / 100.0
    val rounded = (pawns * 10).toInt() / 10.0
    return if (rounded > 0) "+$rounded" else rounded.toString()
}
