package dev.hawk0f.checkmates.ui.replay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hawk0f.checkmates.shared.engine.AnalysisSummary
import dev.hawk0f.checkmates.shared.engine.GameAnalyzer
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.session.PuzzlePersistence
import dev.hawk0f.checkmates.session.PuzzleStore
import dev.hawk0f.checkmates.session.ReplayAnnotationPersistence
import dev.hawk0f.checkmates.session.ReplayAnnotationStore
import dev.hawk0f.checkmates.shared.puzzle.Puzzle
import dev.hawk0f.checkmates.shared.puzzle.PuzzleTheme
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReplayAnalysisUiState(
    val running: Boolean = false,
    val analysedPlies: Int = 0,
    val totalPlies: Int = 0,
    val summary: AnalysisSummary? = null,
    val savedPuzzlePlies: Set<Int> = emptySet(),
    val annotations: Map<Int, String> = emptyMap()
)

enum class PracticeResult {
    THINKING,
    TRY_AGAIN,
    CORRECT
}

data class ReplayPracticeUiState(
    val activePly: Int? = null,
    val gameState: GameState? = null,
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val result: PracticeResult = PracticeResult.THINKING
)

class ReplayAnalysisViewModel(
    private val analyzer: GameAnalyzer = GameAnalyzer(),
    private val analysisContext: CoroutineContext = Dispatchers.Default,
    private val puzzleStore: PuzzlePersistence = PuzzleStore,
    private val annotationStore: ReplayAnnotationPersistence = ReplayAnnotationStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplayAnalysisUiState())
    val uiState: StateFlow<ReplayAnalysisUiState> = _uiState.asStateFlow()

    private val _practiceState = MutableStateFlow(ReplayPracticeUiState())
    val practiceState: StateFlow<ReplayPracticeUiState> = _practiceState.asStateFlow()

    private var job: Job? = null
    private var generation = 0
    private var practiceGame: ChessGame? = null
    private var expectedMove: String? = null

    fun loadAnnotations(gameId: Long) {
        _uiState.value = _uiState.value.copy(annotations = annotationStore.annotations(gameId))
    }

    fun saveAnnotation(gameId: Long, ply: Int, text: String) {
        if (ply < 0) {
            return
        }
        annotationStore.save(gameId, ply, text)
        _uiState.value = _uiState.value.copy(annotations = annotationStore.annotations(gameId))
    }

    fun analyse(uciHistory: List<String>, startFen: String? = null) {
        if (_uiState.value.running || uciHistory.isEmpty()) {
            return
        }
        generation++
        val current = generation
        _uiState.value = _uiState.value.copy(
            running = true,
            analysedPlies = 0,
            totalPlies = uciHistory.size,
            summary = null
        )
        job = viewModelScope.launch {
            try {
                val summary = withContext(analysisContext) {
                    analyzer.analyse(
                        uciHistory = uciHistory,
                        startFen = startFen,
                        shouldContinue = { current == generation }
                    ) { done ->
                        if (current == generation) {
                            _uiState.value = _uiState.value.copy(analysedPlies = done)
                        }
                    }
                }
                if (current == generation) {
                    _uiState.value = _uiState.value.copy(running = false, summary = summary)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (current == generation) {
                    _uiState.value = _uiState.value.copy(
                        running = false,
                        analysedPlies = 0,
                        totalPlies = 0,
                        summary = null
                    )
                }
            }
        }
    }

    fun cancel() {
        generation++
        job?.cancel()
        job = null
        _uiState.value = _uiState.value.copy(
            running = false,
            analysedPlies = 0,
            totalPlies = 0,
            summary = null
        )
    }

    fun startPractice(uciHistory: List<String>, startFen: String? = null, ply: Int, bestMove: String) {
        if (ply !in uciHistory.indices) {
            return
        }
        val game = ChessGame()
        startFen?.let(game::loadFen)
        for (uci in uciHistory.take(ply)) {
            if (game.applyUci(uci) !is MoveOutcome.Applied) {
                return
            }
        }
        practiceGame = game
        expectedMove = bestMove.lowercase()
        _practiceState.value = ReplayPracticeUiState(
            activePly = ply,
            gameState = game.state()
        )
    }

    fun stopPractice() {
        practiceGame = null
        expectedMove = null
        _practiceState.value = ReplayPracticeUiState()
    }

    fun onPracticeSquareTap(square: Square) {
        val game = practiceGame ?: return
        val state = _practiceState.value
        if (state.result == PracticeResult.CORRECT) {
            return
        }
        val selected = state.selected
        if (selected == null) {
            val targets = game.legalDestinations(square)
            if (targets.isNotEmpty()) {
                _practiceState.value = state.copy(selected = square, legalTargets = targets)
            }
            return
        }
        if (selected == square) {
            _practiceState.value = state.copy(selected = null, legalTargets = emptySet())
            return
        }
        if (square !in state.legalTargets) {
            val targets = game.legalDestinations(square)
            _practiceState.value = state.copy(
                selected = square.takeIf { targets.isNotEmpty() },
                legalTargets = targets
            )
            return
        }
        val baseMove = selected.toUci() + square.toUci()
        val expected = expectedMove ?: return
        val candidate = if (expected.startsWith(baseMove)) expected else baseMove
        if (candidate != expected) {
            _practiceState.value = state.copy(
                selected = null,
                legalTargets = emptySet(),
                result = PracticeResult.TRY_AGAIN
            )
            return
        }
        val outcome = game.applyUci(candidate)
        if (outcome is MoveOutcome.Applied) {
            _practiceState.value = state.copy(
                gameState = outcome.state,
                selected = null,
                legalTargets = emptySet(),
                result = PracticeResult.CORRECT
            )
        }
    }

    fun savePersonalPuzzle(
        gameId: Long,
        uciHistory: List<String>,
        startFen: String? = null,
        ply: Int,
        bestMove: String,
        centipawnLoss: Int
    ) {
        if (ply !in uciHistory.indices || bestMove.isBlank()) {
            return
        }
        val game = ChessGame()
        startFen?.let(game::loadFen)
        for (uci in uciHistory.take(ply)) {
            if (game.applyUci(uci) !is MoveOutcome.Applied) {
                return
            }
        }
        val puzzle = Puzzle(
            id = "personal-$gameId-$ply",
            fen = game.fen(),
            solution = listOf(bestMove.lowercase()),
            rating = (1200 + centipawnLoss).coerceIn(800, 2400),
            theme = PuzzleTheme.HANGING_PIECE
        )
        puzzleStore.savePersonalPuzzle(puzzle)
        _uiState.value = _uiState.value.copy(savedPuzzlePlies = _uiState.value.savedPuzzlePlies + ply)
    }

    override fun onCleared() {
        cancel()
        stopPractice()
    }
}
