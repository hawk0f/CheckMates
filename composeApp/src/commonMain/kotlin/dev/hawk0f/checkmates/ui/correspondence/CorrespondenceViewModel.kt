package dev.hawk0f.checkmates.ui.correspondence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.protocol.CorrespondenceGame
import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CorrespondenceUiState(
    val games: List<CorrespondenceGame> = emptyList(),
    val friends: List<FriendSummary> = emptyList(),
    val selectedGame: CorrespondenceGame? = null,
    val board: GameState = ChessGame().state(),
    val selectedSquare: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val pendingPromotion: Pair<Square, Square>? = null,
    val loading: Boolean = true,
    val working: Boolean = false,
    val error: String? = null
)

class CorrespondenceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CorrespondenceUiState())
    val uiState: StateFlow<CorrespondenceUiState> = _uiState.asStateFlow()
    private var chessGame = ChessGame()

    init {
        refresh()
    }

    fun refresh() {
        val token = AuthManager.token ?: run {
            _uiState.value = _uiState.value.copy(loading = false)
            return
        }
        viewModelScope.launch {
            try {
                val games = AuthManager.api.correspondenceGames(token).games
                val friends = AuthManager.api.friends(token).friends
                val selected = _uiState.value.selectedGame?.id?.let { id -> games.find { it.id == id } }
                _uiState.value = _uiState.value.copy(games = games, friends = friends, loading = false, error = null)
                selected?.let(::open)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun create(friend: FriendSummary) {
        val token = AuthManager.token ?: return
        if (_uiState.value.working) {
            return
        }
        _uiState.value = _uiState.value.copy(working = true, error = null)
        viewModelScope.launch {
            try {
                val game = AuthManager.api.createCorrespondenceGame(token, friend.userId)
                _uiState.value = _uiState.value.copy(working = false, games = listOf(game) + _uiState.value.games)
                open(game)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(working = false, error = e.message)
            }
        }
    }

    fun open(game: CorrespondenceGame) {
        chessGame = ChessGame()
        game.uciHistory.forEach { chessGame.applyUci(it) }
        _uiState.value = _uiState.value.copy(
            selectedGame = game,
            board = chessGame.state(),
            selectedSquare = null,
            legalTargets = emptySet()
        )
    }

    fun closeGame() {
        _uiState.value = _uiState.value.copy(selectedGame = null, selectedSquare = null, legalTargets = emptySet())
    }

    fun onSquareTap(square: Square) {
        val game = _uiState.value.selectedGame ?: return
        val profile = AuthManager.profile.value ?: return
        val myColor = if (profile.id == game.whiteUserId) {
            dev.hawk0f.checkmates.shared.domain.PieceColor.WHITE
        } else {
            dev.hawk0f.checkmates.shared.domain.PieceColor.BLACK
        }
        if (game.reason != null || game.sideToMove != myColor || _uiState.value.working) {
            return
        }
        val selected = _uiState.value.selectedSquare
        if (selected == null) {
            val targets = chessGame.legalDestinations(square)
            _uiState.value = _uiState.value.copy(
                selectedSquare = square.takeIf { targets.isNotEmpty() },
                legalTargets = targets
            )
            return
        }
        if (square == selected) {
            _uiState.value = _uiState.value.copy(selectedSquare = null, legalTargets = emptySet())
            return
        }
        if (square !in _uiState.value.legalTargets) {
            val targets = chessGame.legalDestinations(square)
            _uiState.value = _uiState.value.copy(
                selectedSquare = square.takeIf { targets.isNotEmpty() },
                legalTargets = targets
            )
            return
        }
        var uci = selected.toUci() + square.toUci()
        if (chessGame.isPromotionMove(selected, square)) {
            _uiState.value = _uiState.value.copy(pendingPromotion = selected to square)
            return
        }
        submitMove(game.id, uci)
    }

    fun choosePromotion(kind: PieceKind) {
        val game = _uiState.value.selectedGame ?: return
        val (from, to) = _uiState.value.pendingPromotion ?: return
        val letter = when (kind) {
            PieceKind.QUEEN -> "q"
            PieceKind.ROOK -> "r"
            PieceKind.BISHOP -> "b"
            PieceKind.KNIGHT -> "n"
            else -> return
        }
        _uiState.value = _uiState.value.copy(pendingPromotion = null)
        submitMove(game.id, from.toUci() + to.toUci() + letter)
    }

    fun dismissPromotion() {
        _uiState.value = _uiState.value.copy(pendingPromotion = null)
    }

    private fun submitMove(gameId: Long, uci: String) {
        val token = AuthManager.token ?: return
        _uiState.value = _uiState.value.copy(working = true, selectedSquare = null, legalTargets = emptySet())
        viewModelScope.launch {
            try {
                val updated = AuthManager.api.makeCorrespondenceMove(token, gameId, uci)
                _uiState.value = _uiState.value.copy(
                    games = _uiState.value.games.map { if (it.id == updated.id) updated else it },
                    working = false
                )
                open(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(working = false, error = e.message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
