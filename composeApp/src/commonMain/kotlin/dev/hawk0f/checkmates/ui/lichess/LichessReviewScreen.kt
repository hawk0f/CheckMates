package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun LichessReviewScreen(
    gameId: String,
    onBack: () -> Unit,
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
                    text = "${uiState.gameId} · move ${uiState.moveIndex}",
                    color = accents.bandStrong
                )
                Text("Review", style = MaterialTheme.typography.displaySmall)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.export?.players?.white?.analysis?.accuracy?.let { accuracy ->
                    CodeChip("${accuracy.toInt()}%")
                }
                CircleButton(onClick = onBack) {
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
                Text("Evaluation is off", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = blocked,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                PillButton(
                    text = "Back",
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
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    ChessBoard(
                        gameState = state,
                        selected = null,
                        legalTargets = emptySet(),
                        flipped = false,
                        onSquareTap = {},
                        showCoordinates = false,
                        modifier = Modifier.fillMaxWidth()
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
                            ?: "no cloud eval",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.inverseOnSurface
                    )
                    Text(
                        text = eval?.let {
                            "cloud eval · depth ${it.depth} · ${it.knodes} knodes"
                        } ?: "Unevaluated positions fall back to stored analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.inverseOnSurface.copy(alpha = 0.6f)
                    )
                }
            }

            uiState.eval?.let { eval ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(
                        text = "Principal variations · multiPv ${eval.pvs.size}",
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
                ) {
                    CircleButton(
                        onClick = { viewModel.step(-1) },
                        enabled = uiState.moveIndex > 0,
                        size = 48.dp
                    ) {
                        ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand)
                    }
                    CircleButton(
                        onClick = { viewModel.step(1) },
                        enabled = uiState.moveIndex < total,
                        size = 48.dp
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
                    SectionLabel("Accuracy", color = accents.bandStrong)
                    Text(
                        text = listOfNotNull(
                            export.players?.white?.analysis?.accuracy?.let { "white ${it.toInt()}%" },
                            export.players?.black?.analysis?.accuracy?.let { "black ${it.toInt()}%" }
                        ).joinToString(" · ").ifEmpty { "no stored analysis" },
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
            title = { Text("Review", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
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
