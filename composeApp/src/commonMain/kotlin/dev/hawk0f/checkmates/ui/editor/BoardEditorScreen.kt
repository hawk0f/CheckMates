package dev.hawk0f.checkmates.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.common_black_side
import dev.hawk0f.checkmates.resources.common_white_side
import dev.hawk0f.checkmates.resources.editor_brush
import dev.hawk0f.checkmates.resources.editor_clear
import dev.hawk0f.checkmates.resources.editor_erase
import dev.hawk0f.checkmates.resources.editor_import
import dev.hawk0f.checkmates.resources.editor_import_failed
import dev.hawk0f.checkmates.resources.editor_import_label
import dev.hawk0f.checkmates.resources.editor_open_pgn
import dev.hawk0f.checkmates.resources.editor_play_computer
import dev.hawk0f.checkmates.resources.editor_play_hotseat
import dev.hawk0f.checkmates.resources.editor_problem_black_king
import dev.hawk0f.checkmates.resources.editor_problem_check
import dev.hawk0f.checkmates.resources.editor_problem_kings
import dev.hawk0f.checkmates.resources.editor_problem_pawn
import dev.hawk0f.checkmates.resources.editor_problem_position
import dev.hawk0f.checkmates.resources.editor_problem_white_king
import dev.hawk0f.checkmates.resources.editor_reset
import dev.hawk0f.checkmates.resources.editor_share_fen
import dev.hawk0f.checkmates.resources.editor_side_to_move
import dev.hawk0f.checkmates.resources.editor_title
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.Piece
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.PositionProblem
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import org.jetbrains.compose.resources.stringResource

private val brushOrder = listOf(
    PieceKind.PAWN,
    PieceKind.KNIGHT,
    PieceKind.BISHOP,
    PieceKind.ROOK,
    PieceKind.QUEEN,
    PieceKind.KING
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun BoardEditorScreen(
    onPlayHotseat: (String) -> Unit,
    onPlayComputer: (String) -> Unit,
    onOpenImportedGame: (List<String>) -> Unit,
    onBack: () -> Unit,
    viewModel: BoardEditorViewModel = viewModel { BoardEditorViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shareText = rememberShareText()
    var importText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.editor_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(
                    direction = ChevronDirection.LEFT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoardBox(modifier = Modifier.fillMaxWidth()) { boardModifier ->
                ChessBoard(
                    gameState = GameState(
                        fen = uiState.fen,
                        pieces = uiState.pieces,
                        sideToMove = uiState.sideToMove,
                        lastMove = null,
                        inCheck = false,
                        result = null,
                        uciHistory = emptyList()
                    ),
                    selected = null,
                    legalTargets = emptySet(),
                    flipped = false,
                    onSquareTap = viewModel::onSquareTap,
                    interactive = true,
                    modifier = boardModifier
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionLabel(stringResource(Res.string.editor_brush))
                for (color in listOf(PieceColor.WHITE, PieceColor.BLACK)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        for (kind in brushOrder) {
                            val piece = Piece(color, kind)
                            SelectPill(
                                text = pieceLabel(piece),
                                selected = uiState.brush == piece,
                                onClick = { viewModel.selectBrush(piece) }
                            )
                        }
                    }
                }
                SelectPill(
                    text = stringResource(Res.string.editor_erase),
                    selected = uiState.brush == null,
                    onClick = { viewModel.selectBrush(null) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionLabel(stringResource(Res.string.editor_side_to_move))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SelectPill(
                        text = stringResource(Res.string.common_white_side),
                        selected = uiState.sideToMove == PieceColor.WHITE,
                        onClick = { viewModel.setSideToMove(PieceColor.WHITE) }
                    )
                    SelectPill(
                        text = stringResource(Res.string.common_black_side),
                        selected = uiState.sideToMove == PieceColor.BLACK,
                        onClick = { viewModel.setSideToMove(PieceColor.BLACK) }
                    )
                }
            }

            uiState.problem?.let { problem ->
                Text(
                    text = problemLabel(problem),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = uiState.fen,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PillButton(
                    text = stringResource(Res.string.editor_reset),
                    onClick = viewModel::resetToStart,
                    tone = PillTone.SOFT,
                    compact = true
                )
                PillButton(
                    text = stringResource(Res.string.editor_clear),
                    onClick = viewModel::clearBoard,
                    tone = PillTone.SOFT,
                    compact = true
                )
                PillButton(
                    text = stringResource(Res.string.editor_share_fen),
                    onClick = { shareText(uiState.fen) },
                    tone = PillTone.SOFT,
                    compact = true
                )
            }

            OutlinedTextField(
                value = importText,
                onValueChange = {
                    importText = it
                    viewModel.clearImport()
                },
                label = { Text(stringResource(Res.string.editor_import_label)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = stringResource(Res.string.editor_import),
                onClick = { viewModel.importText(importText) },
                tone = PillTone.SOFT,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.importError) {
                Text(
                    text = stringResource(Res.string.editor_import_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (uiState.importedMoves.isNotEmpty()) {
                PillButton(
                    text = stringResource(Res.string.editor_open_pgn, uiState.importedMoves.size),
                    onClick = { onOpenImportedGame(uiState.importedMoves) },
                    tone = PillTone.ACCENT,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PillButton(
                text = stringResource(Res.string.editor_play_computer),
                onClick = { onPlayComputer(uiState.fen) },
                enabled = uiState.playable,
                tone = PillTone.ACCENT,
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = stringResource(Res.string.editor_play_hotseat),
                onClick = { onPlayHotseat(uiState.fen) },
                enabled = uiState.playable,
                tone = PillTone.SOFT,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun pieceLabel(piece: Piece): String {
    val letter = when (piece.kind) {
        PieceKind.PAWN -> "P"
        PieceKind.KNIGHT -> "N"
        PieceKind.BISHOP -> "B"
        PieceKind.ROOK -> "R"
        PieceKind.QUEEN -> "Q"
        PieceKind.KING -> "K"
    }
    return if (piece.color == PieceColor.WHITE) letter else letter.lowercase()
}

@Composable
private fun problemLabel(problem: PositionProblem): String = stringResource(
    when (problem) {
        PositionProblem.MISSING_WHITE_KING -> Res.string.editor_problem_white_king
        PositionProblem.MISSING_BLACK_KING -> Res.string.editor_problem_black_king
        PositionProblem.TOO_MANY_KINGS -> Res.string.editor_problem_kings
        PositionProblem.PAWN_ON_LAST_RANK -> Res.string.editor_problem_pawn
        PositionProblem.OPPONENT_IN_CHECK -> Res.string.editor_problem_check
        PositionProblem.NOT_A_POSITION -> Res.string.editor_problem_position
    }
)
