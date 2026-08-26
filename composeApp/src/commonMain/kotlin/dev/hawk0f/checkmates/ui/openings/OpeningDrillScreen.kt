package dev.hawk0f.checkmates.ui.openings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.openings_completed
import dev.hawk0f.checkmates.resources.openings_restart
import dev.hawk0f.checkmates.resources.openings_wrong
import dev.hawk0f.checkmates.resources.openings_your_move
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.game.PromotionDialog
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpeningDrillScreen(
    lineId: String,
    onBack: () -> Unit,
    viewModel: OpeningDrillViewModel = viewModel(key = lineId) { OpeningDrillViewModel(lineId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accents = LocalAppAccents.current

    if (uiState.pendingPromotion != null) {
        PromotionDialog(
            color = uiState.gameState.sideToMove,
            onChoose = viewModel::onPromotionChosen,
            onDismiss = viewModel::onPromotionDismissed
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(uiState.line.name, style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(
                    direction = ChevronDirection.LEFT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        BoardBox(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) { boardModifier ->
            ChessBoard(
                gameState = uiState.gameState,
                selected = uiState.selected,
                legalTargets = uiState.legalTargets,
                flipped = uiState.line.trainedColor == PieceColor.BLACK,
                onSquareTap = viewModel::onSquareTap,
                interactive = uiState.status != DrillStatus.COMPLETED,
                modifier = boardModifier
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (uiState.status) {
                    DrillStatus.PLAYING -> stringResource(Res.string.openings_your_move)
                    DrillStatus.WRONG_MOVE -> stringResource(
                        Res.string.openings_wrong,
                        uiState.expectedMove.orEmpty()
                    )

                    DrillStatus.COMPLETED -> stringResource(Res.string.openings_completed, uiState.mistakes)
                },
                style = MaterialTheme.typography.titleMedium,
                color = when (uiState.status) {
                    DrillStatus.PLAYING -> MaterialTheme.colorScheme.onBackground
                    DrillStatus.WRONG_MOVE -> accents.negative
                    DrillStatus.COMPLETED -> accents.positive
                }
            )
            PillButton(
                text = stringResource(Res.string.openings_restart),
                onClick = viewModel::restart,
                tone = if (uiState.status == DrillStatus.COMPLETED) PillTone.ACCENT else PillTone.SOFT,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
