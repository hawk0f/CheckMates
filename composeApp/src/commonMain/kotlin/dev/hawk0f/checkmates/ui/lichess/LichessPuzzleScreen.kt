package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.puzzle_daily
import dev.hawk0f.checkmates.resources.puzzle_dialog_title
import dev.hawk0f.checkmates.resources.puzzle_find_the_move
import dev.hawk0f.checkmates.resources.puzzle_id_and_side
import dev.hawk0f.checkmates.resources.puzzle_line_played_out
import dev.hawk0f.checkmates.resources.puzzle_loading
import dev.hawk0f.checkmates.resources.puzzle_moves_left
import dev.hawk0f.checkmates.resources.puzzle_next
import dev.hawk0f.checkmates.resources.puzzle_not_this_one
import dev.hawk0f.checkmates.resources.puzzle_one_move_left
import dev.hawk0f.checkmates.resources.puzzle_rating
import dev.hawk0f.checkmates.resources.puzzle_rating_unknown
import dev.hawk0f.checkmates.resources.puzzle_right_move
import dev.hawk0f.checkmates.resources.puzzle_side_black
import dev.hawk0f.checkmates.resources.puzzle_side_white
import dev.hawk0f.checkmates.resources.puzzle_sign_in_for_history
import dev.hawk0f.checkmates.resources.puzzle_solved
import dev.hawk0f.checkmates.resources.puzzle_streak
import dev.hawk0f.checkmates.resources.puzzle_try_again
import dev.hawk0f.checkmates.resources.puzzle_your_rating
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_close

@Composable
fun LichessPuzzleScreen(
    onBack: () -> Unit,
    viewModel: LichessPuzzleViewModel = viewModel { LichessPuzzleViewModel() }
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
                    text = uiState.puzzleId?.let { id ->
                        val side = stringResource(
                            if (uiState.sideToMove == PieceColor.WHITE) {
                                Res.string.puzzle_side_white
                            } else {
                                Res.string.puzzle_side_black
                            }
                        )
                        stringResource(Res.string.puzzle_id_and_side, id, side)
                    } ?: stringResource(Res.string.puzzle_loading),
                    color = accents.bandStrong
                )
                Text(stringResource(Res.string.puzzle_daily), style = MaterialTheme.typography.displaySmall)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodeChip(stringResource(Res.string.puzzle_streak, uiState.streak))
                CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_close)) {
                    CloseIcon(color = scheme.onSurfaceVariant)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                for ((angle, label) in PUZZLE_ANGLES) {
                    SelectPill(
                        text = label,
                        selected = uiState.angle == angle,
                        onClick = { viewModel.loadNext(angle) }
                    )
                }
            }

            if (uiState.themes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    for (theme in uiState.themes.take(4)) {
                        CodeChip(theme)
                    }
                }
            }

            val state = uiState.gameState
            if (state == null || uiState.loading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = scheme.primary)
                }
            } else {
                val selected = uiState.selected
                val legalTargets = uiState.legalTargets
                val flipped = uiState.flipped
                BoardBox(modifier = Modifier.weight(1f)) { boardModifier ->
                    ChessBoard(
                        gameState = state,
                        selected = selected,
                        legalTargets = legalTargets,
                        flipped = flipped,
                        onSquareTap = viewModel::onSquareTap,
                        modifier = boardModifier
                    )
                }
            }

            FeedbackCard(uiState = uiState)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            PillButton(
                text = stringResource(Res.string.puzzle_next),
                onClick = { viewModel.loadNext() },
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = stringResource(Res.string.puzzle_daily),
                onClick = viewModel::loadDaily,
                tone = PillTone.SOFT,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.puzzle_dialog_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun FeedbackCard(uiState: LichessPuzzleUiState) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val headline = when (uiState.feedback) {
        PuzzleFeedback.NONE -> stringResource(Res.string.puzzle_find_the_move)
        PuzzleFeedback.CORRECT -> stringResource(Res.string.puzzle_right_move, uiState.lastMoveSan.orEmpty())
        PuzzleFeedback.WRONG -> stringResource(Res.string.puzzle_not_this_one)
        PuzzleFeedback.SOLVED -> stringResource(Res.string.puzzle_solved)
    }
    val note = when (uiState.feedback) {
        PuzzleFeedback.NONE -> stringResource(
            Res.string.puzzle_rating,
            uiState.rating?.toString() ?: stringResource(Res.string.puzzle_rating_unknown)
        )
        PuzzleFeedback.CORRECT -> if (uiState.movesLeft <= 1) {
            stringResource(Res.string.puzzle_one_move_left)
        } else {
            stringResource(Res.string.puzzle_moves_left, uiState.movesLeft)
        }

        PuzzleFeedback.WRONG -> stringResource(Res.string.puzzle_try_again)
        PuzzleFeedback.SOLVED -> stringResource(Res.string.puzzle_line_played_out)
    }
    val container = when (uiState.feedback) {
        PuzzleFeedback.WRONG -> scheme.errorContainer
        PuzzleFeedback.SOLVED -> accents.band
        else -> scheme.surfaceContainer
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = headline, style = MaterialTheme.typography.titleMedium)
        Text(text = note, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(stringResource(Res.string.puzzle_your_rating), color = accents.bandStrong)
            Text(
                text = uiState.myPuzzleRating?.toString()
                    ?: stringResource(Res.string.puzzle_sign_in_for_history),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
