package dev.hawk0f.checkmates.ui.puzzle

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
import dev.hawk0f.checkmates.resources.common_black_side
import dev.hawk0f.checkmates.resources.common_white_side
import dev.hawk0f.checkmates.resources.puzzles_empty
import dev.hawk0f.checkmates.resources.puzzles_failed
import dev.hawk0f.checkmates.resources.puzzles_hint
import dev.hawk0f.checkmates.resources.puzzles_next
import dev.hawk0f.checkmates.resources.puzzles_rating
import dev.hawk0f.checkmates.resources.puzzles_solved
import dev.hawk0f.checkmates.resources.puzzles_streak
import dev.hawk0f.checkmates.resources.puzzles_theme
import dev.hawk0f.checkmates.resources.puzzles_title
import dev.hawk0f.checkmates.resources.puzzles_to_move
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
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun PuzzleScreen(
    onBack: () -> Unit,
    viewModel: PuzzleViewModel = viewModel { PuzzleViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accents = LocalAppAccents.current

    if (uiState.pendingPromotion != null) {
        PromotionDialog(
            color = uiState.gameState?.sideToMove ?: uiState.solverColor,
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
            Column {
                Text(stringResource(Res.string.puzzles_title), style = MaterialTheme.typography.displaySmall)
                SectionLabel(
                    text = stringResource(Res.string.puzzles_rating, uiState.rating),
                    color = accents.bandStrong
                )
            }
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(
                    direction = ChevronDirection.LEFT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val gameState = uiState.gameState
        val puzzle = uiState.puzzle
        if (gameState == null || puzzle == null) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(Res.string.puzzles_empty), style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }

        val sideToMove = uiState.solverColor
        BoardBox(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) { boardModifier ->
            ChessBoard(
                gameState = gameState,
                selected = uiState.selected ?: uiState.hintSquare,
                legalTargets = uiState.legalTargets,
                flipped = sideToMove == PieceColor.BLACK,
                onSquareTap = viewModel::onSquareTap,
                interactive = uiState.outcome == PuzzleOutcome.UNSOLVED,
                modifier = boardModifier
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (uiState.outcome) {
                    PuzzleOutcome.UNSOLVED -> stringResource(
                        Res.string.puzzles_to_move,
                        stringResource(
                            if (sideToMove == PieceColor.WHITE) Res.string.common_white_side else Res.string.common_black_side
                        )
                    )

                    PuzzleOutcome.SOLVED -> stringResource(
                        Res.string.puzzles_solved,
                        if (uiState.ratingDelta >= 0) "+${uiState.ratingDelta}" else uiState.ratingDelta.toString()
                    )

                    PuzzleOutcome.FAILED -> stringResource(Res.string.puzzles_failed)
                },
                style = MaterialTheme.typography.titleLarge,
                color = when (uiState.outcome) {
                    PuzzleOutcome.UNSOLVED -> MaterialTheme.colorScheme.onBackground
                    PuzzleOutcome.SOLVED -> accents.positive
                    PuzzleOutcome.FAILED -> accents.negative
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel(
                    text = stringResource(Res.string.puzzles_streak, uiState.streak),
                    color = accents.bandStrong
                )
                if (uiState.outcome != PuzzleOutcome.UNSOLVED) {
                    SectionLabel(
                        text = stringResource(Res.string.puzzles_theme, puzzle.theme.id),
                        color = accents.bandStrong
                    )
                }
            }

            if (uiState.outcome == PuzzleOutcome.UNSOLVED) {
                PillButton(
                    text = stringResource(Res.string.puzzles_hint),
                    onClick = viewModel::showHint,
                    tone = PillTone.SOFT,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PillButton(
                    text = stringResource(Res.string.puzzles_next),
                    onClick = viewModel::loadNext,
                    tone = PillTone.ACCENT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
