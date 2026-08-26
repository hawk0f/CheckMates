package dev.hawk0f.checkmates.ui.openings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.session.OpeningProgressStore
import dev.hawk0f.checkmates.session.OpeningProgressPersistence
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.opening.OpeningBook
import dev.hawk0f.checkmates.shared.opening.OpeningLine
import dev.hawk0f.checkmates.ui.game.promotionLetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DrillStatus {
    PLAYING,
    WRONG_MOVE,
    COMPLETED
}

data class OpeningDrillUiState(
    val line: OpeningLine,
    val gameState: GameState,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val status: DrillStatus = DrillStatus.PLAYING,
    val expectedMove: String? = null,
    val mistakes: Int = 0,
    val bestStreak: Int = 0,
    val pendingPromotion: Pair<Square, Square>? = null
)

class OpeningDrillViewModel(
    lineId: String,
    private val store: OpeningProgressPersistence = OpeningProgressStore,
    private val opponentDelayMillis: Long = 350
) : ViewModel() {

    private val line = OpeningBook.byId(lineId) ?: OpeningBook.lines.first()
    private var game = ChessGame()
    private var opponentJob: Job? = null

    private val _uiState = MutableStateFlow(
        OpeningDrillUiState(
            line = line,
            gameState = game.state(),
            bestStreak = store.bestStreak(line.id)
        )
    )
    val uiState: StateFlow<OpeningDrillUiState> = _uiState.asStateFlow()

    init {
        playOpponentMoveIfNeeded()
    }

    fun restart() {
        opponentJob?.cancel()
        game = ChessGame()
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            status = DrillStatus.PLAYING,
            expectedMove = null,
            mistakes = 0,
            pendingPromotion = null
        )
        playOpponentMoveIfNeeded()
    }

    fun onSquareTap(square: Square) {
        val state = _uiState.value
        if (state.status == DrillStatus.COMPLETED || state.pendingPromotion != null) {
            return
        }
        val selected = state.selected
        if (selected == null) {
            val piece = state.gameState.pieces[square] ?: return
            if (piece.color != state.gameState.sideToMove) {
                return
            }
            _uiState.value = state.copy(
                selected = square,
                legalTargets = game.legalDestinations(square).toSet(),
                status = DrillStatus.PLAYING,
                expectedMove = null
            )
            return
        }
        if (square == selected) {
            _uiState.value = state.copy(selected = null, legalTargets = emptySet())
            return
        }
        if (square !in state.legalTargets) {
            val piece = state.gameState.pieces[square]
            if (piece != null && piece.color == state.gameState.sideToMove) {
                _uiState.value = state.copy(
                    selected = square,
                    legalTargets = game.legalDestinations(square).toSet()
                )
            }
            return
        }
        if (game.isPromotionMove(selected, square)) {
            _uiState.value = state.copy(pendingPromotion = selected to square)
            return
        }
        submit(selected.toUci() + square.toUci())
    }

    fun onPromotionChosen(kind: PieceKind) {
        val (from, to) = _uiState.value.pendingPromotion ?: return
        val letter = promotionLetter(kind) ?: return
        _uiState.value = _uiState.value.copy(pendingPromotion = null)
        submit(from.toUci() + to.toUci() + letter)
    }

    fun onPromotionDismissed() {
        _uiState.value = _uiState.value.copy(pendingPromotion = null, selected = null, legalTargets = emptySet())
    }

    private fun submit(uci: String) {
        val expected = line.moves.getOrNull(game.state().uciHistory.size) ?: return
        if (uci != expected) {
            _uiState.value = _uiState.value.copy(
                selected = null,
                legalTargets = emptySet(),
                status = DrillStatus.WRONG_MOVE,
                expectedMove = expected,
                mistakes = _uiState.value.mistakes + 1
            )
            return
        }
        if (game.applyUci(uci) !is MoveOutcome.Applied) {
            return
        }
        _uiState.value = _uiState.value.copy(
            gameState = game.state(),
            selected = null,
            legalTargets = emptySet(),
            status = DrillStatus.PLAYING,
            expectedMove = null
        )
        playOpponentMoveIfNeeded()
    }

    private fun playOpponentMoveIfNeeded() {
        val played = game.state().uciHistory.size
        if (played >= line.moves.size) {
            complete()
            return
        }
        val nextIsOurs = (played % 2 == 0) == (line.trainedColor == PieceColor.WHITE)
        if (nextIsOurs) {
            return
        }
        opponentJob = viewModelScope.launch {
            delay(opponentDelayMillis)
            if (game.state().uciHistory.size == played && game.applyUci(line.moves[played]) is MoveOutcome.Applied) {
                _uiState.value = _uiState.value.copy(gameState = game.state())
                if (game.state().uciHistory.size >= line.moves.size) {
                    complete()
                }
            }
        }
    }

    private fun complete() {
        val state = _uiState.value
        val streak = if (state.mistakes == 0) state.bestStreak + 1 else 0
        store.saveResult(line.id, mistakes = state.mistakes, streak = streak)
        _uiState.value = state.copy(status = DrillStatus.COMPLETED, bestStreak = streak)
    }
}
