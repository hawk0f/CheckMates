package dev.hawk0f.checkmates.ui.puzzle

import androidx.lifecycle.ViewModel
import dev.hawk0f.checkmates.session.PuzzlePersistence
import dev.hawk0f.checkmates.session.PuzzleStore
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.puzzle.BundledPuzzles
import dev.hawk0f.checkmates.shared.puzzle.Puzzle
import dev.hawk0f.checkmates.shared.puzzle.PuzzleElo
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import dev.hawk0f.checkmates.shared.puzzle.SpacedRepetition
import dev.hawk0f.checkmates.ui.game.promotionLetter
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PuzzleOutcome {
    UNSOLVED,
    SOLVED,
    FAILED
}

data class PuzzleUiState(
    val puzzle: Puzzle?,
    val gameState: GameState?,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val solverColor: PieceColor = PieceColor.WHITE,
    val outcome: PuzzleOutcome = PuzzleOutcome.UNSOLVED,
    val rating: Int = PuzzleElo.DEFAULT_RATING,
    val ratingDelta: Int = 0,
    val streak: Int = 0,
    val solvedToday: Int = 0,
    val hintSquare: Square? = null,
    val pendingPromotion: Pair<Square, Square>? = null
)

class PuzzleViewModel(
    private val store: PuzzlePersistence = PuzzleStore,
    private val puzzles: List<Puzzle> = BundledPuzzles.all,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ViewModel() {

    private var progress: MutableMap<String, PuzzleProgress> = store.loadProgress().toMutableMap()
    private var game = ChessGame()
    private var solutionIndex = 0

    private val _uiState = MutableStateFlow(
        PuzzleUiState(
            puzzle = null,
            gameState = null,
            rating = store.loadRating(),
            streak = store.loadStreak()
        )
    )
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    init {
        loadNext()
    }

    fun loadNext() {
        val puzzle = SpacedRepetition.nextPuzzle(
            puzzles = puzzles,
            progress = progress,
            playerRating = _uiState.value.rating,
            nowMillis = now(),
            currentId = _uiState.value.puzzle?.id
        )
        if (puzzle == null) {
            _uiState.value = _uiState.value.copy(puzzle = null, gameState = null)
            return
        }
        game = ChessGame()
        game.loadFen(puzzle.fen)
        solutionIndex = 0
        val state = game.state()
        _uiState.value = _uiState.value.copy(
            puzzle = puzzle,
            gameState = state,
            solverColor = state.sideToMove,
            selected = null,
            legalTargets = emptySet(),
            outcome = PuzzleOutcome.UNSOLVED,
            ratingDelta = 0,
            hintSquare = null,
            pendingPromotion = null
        )
    }

    fun onSquareTap(square: Square) {
        val state = _uiState.value
        if (state.outcome != PuzzleOutcome.UNSOLVED || state.gameState == null || state.pendingPromotion != null) {
            return
        }
        val selected = state.selected
        if (selected == null) {
            val piece = state.gameState.pieces[square] ?: return
            if (piece.color != state.gameState.sideToMove) {
                return
            }
            _uiState.value = state.copy(selected = square, legalTargets = game.legalDestinations(square).toSet())
            return
        }
        if (square == selected) {
            _uiState.value = state.copy(selected = null, legalTargets = emptySet())
            return
        }
        if (square !in state.legalTargets) {
            val piece = state.gameState.pieces[square]
            if (piece != null && piece.color == state.gameState.sideToMove) {
                _uiState.value = state.copy(selected = square, legalTargets = game.legalDestinations(square).toSet())
            }
            return
        }
        if (game.isPromotionMove(selected, square)) {
            _uiState.value = state.copy(pendingPromotion = selected to square)
            return
        }
        submit(selected, square, null)
    }

    fun onPromotionChosen(kind: PieceKind) {
        val (from, to) = _uiState.value.pendingPromotion ?: return
        val letter = promotionLetter(kind) ?: return
        _uiState.value = _uiState.value.copy(pendingPromotion = null)
        submit(from, to, letter.first())
    }

    fun onPromotionDismissed() {
        _uiState.value = _uiState.value.copy(pendingPromotion = null, selected = null, legalTargets = emptySet())
    }

    fun showHint() {
        val puzzle = _uiState.value.puzzle ?: return
        val from = puzzle.solution.getOrNull(solutionIndex)?.take(2) ?: return
        _uiState.value = _uiState.value.copy(hintSquare = Square.fromUci(from))
    }

    private fun submit(from: Square, to: Square, promotion: Char?) {
        val puzzle = _uiState.value.puzzle ?: return
        val uci = from.toUci() + to.toUci() + (promotion?.lowercaseChar() ?: "")
        val expected = puzzle.solution.getOrNull(solutionIndex) ?: return
        if (uci != expected) {
            finish(puzzle, solved = false)
            return
        }
        if (game.applyUci(uci) !is MoveOutcome.Applied) {
            return
        }
        solutionIndex++
        val reply = puzzle.solution.getOrNull(solutionIndex)
        if (reply != null && game.applyUci(reply) is MoveOutcome.Applied) {
            solutionIndex++
        }
        if (solutionIndex >= puzzle.solution.size) {
            finish(puzzle, solved = true)
            return
        }
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            hintSquare = null
        )
    }

    private fun finish(puzzle: Puzzle, solved: Boolean) {
        val moment = now()
        val entry = progress[puzzle.id] ?: PuzzleProgress(puzzle.id)
        progress[puzzle.id] = if (solved) {
            SpacedRepetition.onSolved(entry, moment)
        } else {
            SpacedRepetition.onFailed(entry, moment)
        }
        store.saveProgress(progress)

        val before = _uiState.value.rating
        val after = PuzzleElo.update(before, puzzle.rating, solved)
        store.saveRating(after)

        val streak = if (solved) _uiState.value.streak + 1 else 0
        store.saveStreak(streak)

        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            outcome = if (solved) PuzzleOutcome.SOLVED else PuzzleOutcome.FAILED,
            rating = after,
            ratingDelta = after - before,
            streak = streak,
            solvedToday = _uiState.value.solvedToday + if (solved) 1 else 0
        )
    }
}
