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
                        val side = if (uiState.sideToMove == PieceColor.WHITE) "white" else "black"
                        "Puzzle $id · $side to move"
                    } ?: "Loading",
                    color = accents.bandStrong
                )
                Text("Daily puzzle", style = MaterialTheme.typography.displaySmall)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodeChip("streak ${uiState.streak}")
                CircleButton(onClick = onBack) {
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
                BoardBox(modifier = Modifier.weight(1f)) { boardModifier ->
                    ChessBoard(
                        gameState = state,
                        selected = uiState.selected,
                        legalTargets = uiState.legalTargets,
                        flipped = uiState.flipped,
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
                text = "Next puzzle",
                onClick = { viewModel.loadNext() },
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = "Daily puzzle",
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
            title = { Text("Puzzle", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun FeedbackCard(uiState: LichessPuzzleUiState) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val headline = when (uiState.feedback) {
        PuzzleFeedback.NONE -> "Find the move"
        PuzzleFeedback.CORRECT -> "${uiState.lastMoveSan.orEmpty()} — right move"
        PuzzleFeedback.WRONG -> "Not this one"
        PuzzleFeedback.SOLVED -> "Solved"
    }
    val note = when (uiState.feedback) {
        PuzzleFeedback.NONE -> "Puzzle rating ${uiState.rating ?: "?"}"
        PuzzleFeedback.CORRECT -> if (uiState.movesLeft <= 1) {
            "One move left in the solution."
        } else {
            "${uiState.movesLeft} moves left in the solution."
        }

        PuzzleFeedback.WRONG -> "Try again — the position did not change."
        PuzzleFeedback.SOLVED -> "Whole line played out."
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
            SectionLabel("Your puzzle rating", color = accents.bandStrong)
            Text(
                text = uiState.myPuzzleRating?.toString() ?: "sign in for history",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
