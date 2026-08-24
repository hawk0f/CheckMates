package dev.hawk0f.checkmates.ui.editor

import androidx.lifecycle.ViewModel
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.Piece
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.PgnReader
import dev.hawk0f.checkmates.shared.domain.PositionEditor
import dev.hawk0f.checkmates.shared.domain.PositionProblem
import dev.hawk0f.checkmates.shared.domain.PositionValidity
import dev.hawk0f.checkmates.shared.domain.Square
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

data class BoardEditorUiState(
    val pieces: Map<Square, Piece> = emptyMap(),
    val sideToMove: PieceColor = PieceColor.WHITE,
    val brush: Piece? = Piece(PieceColor.WHITE, PieceKind.PAWN),
    val fen: String = START_FEN,
    val problem: PositionProblem? = null,
    val importedMoves: List<String> = emptyList(),
    val importError: Boolean = false
) {
    val playable: Boolean get() = problem == null
}

class BoardEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BoardEditorUiState())
    val uiState: StateFlow<BoardEditorUiState> = _uiState.asStateFlow()

    init {
        setPieces(PositionEditor.piecesFromFen(START_FEN).orEmpty(), PieceColor.WHITE)
    }

    fun previewState(): GameState? {
        val fen = _uiState.value.fen
        val game = ChessGame()
        return runCatching {
            game.loadFen(fen)
            game.state()
        }.getOrNull()
    }

    fun selectBrush(piece: Piece?) {
        _uiState.value = _uiState.value.copy(brush = piece)
    }

    fun onSquareTap(square: Square) {
        val state = _uiState.value
        val pieces = state.pieces.toMutableMap()
        val brush = state.brush
        if (brush == null || pieces[square] == brush) {
            pieces.remove(square)
        } else {
            pieces[square] = brush
        }
        setPieces(pieces, state.sideToMove)
    }

    fun setSideToMove(color: PieceColor) {
        setPieces(_uiState.value.pieces, color)
    }

    fun clearBoard() {
        setPieces(emptyMap(), PieceColor.WHITE)
    }

    fun resetToStart() {
        setPieces(PositionEditor.piecesFromFen(START_FEN).orEmpty(), PieceColor.WHITE)
    }

    fun importText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(importError = true)
            return
        }
        val pieces = PositionEditor.piecesFromFen(text)
        if (pieces != null && pieces.isNotEmpty()) {
            setPieces(pieces, PositionEditor.sideToMoveFromFen(text))
            _uiState.value = _uiState.value.copy(importedMoves = emptyList(), importError = false)
            return
        }
        val moves = PgnReader.uciMoves(text)
        if (moves.isEmpty()) {
            _uiState.value = _uiState.value.copy(importError = true)
            return
        }
        _uiState.value = _uiState.value.copy(importedMoves = moves, importError = false)
    }

    fun clearImport() {
        _uiState.value = _uiState.value.copy(importedMoves = emptyList(), importError = false)
    }

    private fun setPieces(pieces: Map<Square, Piece>, sideToMove: PieceColor) {
        val validity = PositionEditor.validate(pieces, sideToMove)
        _uiState.value = _uiState.value.copy(
            pieces = pieces,
            sideToMove = sideToMove,
            fen = when (validity) {
                is PositionValidity.Valid -> validity.fen
                is PositionValidity.Invalid -> PositionEditor.buildFen(pieces, sideToMove)
            },
            problem = (validity as? PositionValidity.Invalid)?.reason
        )
    }
}
