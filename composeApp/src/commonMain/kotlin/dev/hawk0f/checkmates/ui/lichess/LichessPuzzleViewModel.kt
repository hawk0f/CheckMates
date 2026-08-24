package dev.hawk0f.checkmates.ui.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.net.lichess.LichessPuzzle
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PgnReader
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.Square
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PuzzleFeedback {
    NONE,
    CORRECT,
    WRONG,
    SOLVED
}

data class LichessPuzzleUiState(
    val loading: Boolean = true,
    val puzzleId: String? = null,
    val rating: Int? = null,
    val themes: List<String> = emptyList(),
    val gameState: GameState? = null,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val sideToMove: PieceColor = PieceColor.WHITE,
    val flipped: Boolean = false,
    val feedback: PuzzleFeedback = PuzzleFeedback.NONE,
    val lastMoveSan: String? = null,
    val movesLeft: Int = 0,
    val streak: Int = 0,
    val myPuzzleRating: Int? = null,
    val angle: String? = null,
    val error: String? = null
)

val PUZZLE_ANGLES = listOf(
    null to "Mixed",
    "mateIn2" to "mateIn2",
    "sacrifice" to "sacrifice",
    "endgame" to "endgame",
    "kingsideAttack" to "kingsideAttack"
)

class LichessPuzzleViewModel : ViewModel() {

    private val api = LichessAuth.api
    private val _uiState = MutableStateFlow(LichessPuzzleUiState())
    val uiState: StateFlow<LichessPuzzleUiState> = _uiState.asStateFlow()

    private var game = ChessGame()
    private var solution = emptyList<String>()
    private var solvedIndex = 0

    init {
        loadDaily()
        loadDashboard()
    }

    fun loadDaily() {
        load { api.dailyPuzzle() }
    }

    fun loadNext(angle: String? = _uiState.value.angle) {
        _uiState.value = _uiState.value.copy(angle = angle)
        load { api.nextPuzzle(LichessAuth.token, angle) }
    }

    fun onSquareTap(square: Square) {
        val current = _uiState.value
        if (current.feedback == PuzzleFeedback.SOLVED || current.gameState == null) {
            return
        }
        val selected = current.selected
        when {
            selected == square -> _uiState.value = current.copy(selected = null, legalTargets = emptySet())
            selected != null && square in current.legalTargets -> {
                tryMove(selected, game.castlingRookSquares(selected)[square] ?: square)
            }

            game.pieceAt(square)?.color == game.sideToMove() -> {
                _uiState.value = current.copy(
                    selected = square,
                    legalTargets = game.legalDestinations(square) + game.castlingRookSquares(square).keys
                )
            }

            else -> _uiState.value = current.copy(selected = null, legalTargets = emptySet())
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun tryMove(from: Square, to: Square) {
        val expected = solution.getOrNull(solvedIndex) ?: return
        val uci = "${from.toUci()}${to.toUci()}"
        val matches = expected.startsWith(uci)
        if (!matches) {
            _uiState.value = _uiState.value.copy(
                feedback = PuzzleFeedback.WRONG,
                selected = null,
                legalTargets = emptySet(),
                streak = 0
            )
            return
        }
        applySolutionMove(expected)
        solvedIndex++
        val remaining = solution.size - solvedIndex
        if (remaining <= 0) {
            _uiState.value = _uiState.value.copy(
                gameState = game.state(),
                selected = null,
                legalTargets = emptySet(),
                feedback = PuzzleFeedback.SOLVED,
                lastMoveSan = expected,
                movesLeft = 0,
                streak = _uiState.value.streak + 1,
                sideToMove = game.sideToMove()
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            feedback = PuzzleFeedback.CORRECT,
            lastMoveSan = expected,
            movesLeft = remaining,
            sideToMove = game.sideToMove()
        )
        viewModelScope.launch {
            delay(500)
            val reply = solution.getOrNull(solvedIndex) ?: return@launch
            applySolutionMove(reply)
            solvedIndex++
            _uiState.value = _uiState.value.copy(
                gameState = game.state(),
                movesLeft = solution.size - solvedIndex,
                sideToMove = game.sideToMove(),
                feedback = if (solvedIndex >= solution.size) {
                    PuzzleFeedback.SOLVED
                } else {
                    PuzzleFeedback.CORRECT
                }
            )
        }
    }

    private fun applySolutionMove(uci: String) {
        if (game.applyUci(uci) is MoveOutcome.Illegal) {
            _uiState.value = _uiState.value.copy(error = "puzzle move $uci did not fit the position")
        }
    }

    private fun load(request: suspend () -> LichessPuzzle) {
        _uiState.value = _uiState.value.copy(loading = true, feedback = PuzzleFeedback.NONE)
        viewModelScope.launch {
            runCatching { request() }
                .onSuccess { install(it) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.message ?: "could not load a puzzle"
                    )
                }
        }
    }

    private fun install(puzzle: LichessPuzzle) {
        val history = PgnReader.uciMoves(puzzle.game.pgn)
        val upTo = if (puzzle.puzzle.initialPly > 0) {
            history.take(puzzle.puzzle.initialPly + 1)
        } else {
            history
        }
        game = ChessGame()
        for (uci in upTo) {
            game.applyUci(uci)
        }
        solution = puzzle.puzzle.solution
        solvedIndex = 0
        val side = game.sideToMove()
        _uiState.value = _uiState.value.copy(
            loading = false,
            puzzleId = puzzle.puzzle.id,
            rating = puzzle.puzzle.rating,
            themes = puzzle.puzzle.themes,
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            sideToMove = side,
            flipped = side == PieceColor.BLACK,
            feedback = PuzzleFeedback.NONE,
            lastMoveSan = null,
            movesLeft = solution.size,
            error = null
        )
    }

    private fun loadDashboard() {
        val token = LichessAuth.token ?: return
        viewModelScope.launch {
            runCatching { api.puzzleDashboard(token, 30) }.onSuccess { dashboard ->
                _uiState.value = _uiState.value.copy(
                    myPuzzleRating = dashboard.global?.performance?.takeIf { it > 0 }
                )
            }
        }
    }
}
