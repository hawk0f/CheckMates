package dev.hawk0f.checkmates.ui.game

import dev.hawk0f.checkmates.shared.domain.Piece
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.MoveOutcome
import dev.hawk0f.checkmates.shared.engine.EngineLevel
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.hawk0f.checkmates.session.HotseatGamePersistence
import dev.hawk0f.checkmates.session.SavedHotseatGame
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.session.ActiveGameSession
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.protocol.GameMessage
import dev.hawk0f.checkmates.shared.transport.GameTransport
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSavedGames(var stored: SavedHotseatGame? = null) : HotseatGamePersistence {
        var cleared = 0

        override fun load(): SavedHotseatGame? = stored

        override fun save(game: SavedHotseatGame) {
            stored = game
        }

        override fun clear() {
            stored = null
            cleared++
        }
    }

    private fun hotseat(savedGames: HotseatGamePersistence = FakeSavedGames()) =
        GameViewModel(GameMode.Hotseat, savedGames)

    private fun GameViewModel.tap(vararg squares: String) {
        for (square in squares) {
            onSquareTap(Square.fromUci(square))
        }
    }

    private fun GameViewModel.play(vararg moves: String) {
        for (move in moves) {
            tap(move.substring(0, 2), move.substring(2, 4))
        }
    }

    @Test
    fun tappingOwnRookCastlesKingside() {
        val viewModel = hotseat()
        viewModel.play("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5")
        viewModel.tap("e1")
        val targets = viewModel.uiState.value.legalTargets
        assertTrue(Square.fromUci("h1") in targets, "rook square should be offered as a castling target")
        assertTrue(Square.fromUci("g1") in targets)
        viewModel.tap("h1")
        val pieces = viewModel.uiState.value.gameState.pieces
        assertEquals(Piece(PieceColor.WHITE, PieceKind.KING), pieces[Square.fromUci("g1")])
        assertEquals(Piece(PieceColor.WHITE, PieceKind.ROOK), pieces[Square.fromUci("f1")])
        assertNull(pieces[Square.fromUci("h1")])
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "e1g1"), viewModel.uiState.value.gameState.uciHistory)
    }

    @Test
    fun tappingOwnRookCastlesQueenside() {
        val viewModel = hotseat()
        viewModel.play("d2d4", "d7d5", "b1c3", "b8c6", "c1f4", "c8f5", "d1d2", "d8d7")
        viewModel.tap("e1", "a1")
        val pieces = viewModel.uiState.value.gameState.pieces
        assertEquals(Piece(PieceColor.WHITE, PieceKind.KING), pieces[Square.fromUci("c1")])
        assertEquals(Piece(PieceColor.WHITE, PieceKind.ROOK), pieces[Square.fromUci("d1")])
        assertNull(pieces[Square.fromUci("a1")])
    }

    @Test
    fun tappingTheSelectedPieceClearsSelection() {
        val viewModel = hotseat()
        viewModel.tap("e2")
        assertEquals(Square.fromUci("e2"), viewModel.uiState.value.selected)
        assertTrue(viewModel.uiState.value.legalTargets.isNotEmpty())
        viewModel.tap("e2")
        assertNull(viewModel.uiState.value.selected)
        assertTrue(viewModel.uiState.value.legalTargets.isEmpty())
        assertTrue(viewModel.uiState.value.gameState.uciHistory.isEmpty())
    }

    @Test
    fun selectingAnotherOwnPieceMovesTheSelection() {
        val viewModel = hotseat()
        viewModel.tap("e2", "d2")
        assertEquals(Square.fromUci("d2"), viewModel.uiState.value.selected)
    }

    @Test
    fun tappingEmptySquareClearsSelection() {
        val viewModel = hotseat()
        viewModel.tap("e2", "h6")
        assertNull(viewModel.uiState.value.selected)
        assertTrue(viewModel.uiState.value.gameState.uciHistory.isEmpty())
    }

    @Test
    fun opponentPiecesAreNotSelectableOnTheirOwn() {
        val viewModel = hotseat()
        viewModel.tap("e7")
        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun lastMoveIsExposedForHighlighting() {
        val viewModel = hotseat()
        viewModel.play("e2e4")
        assertEquals(
            Square.fromUci("e2") to Square.fromUci("e4"),
            viewModel.uiState.value.gameState.lastMove
        )
    }

    @Test
    fun promotionIsDeferredToTheDialogAndAppliesTheChosenPiece() {
        val viewModel = hotseat()
        viewModel.play("e2e4", "d7d5", "e4d5", "c7c6", "d5c6", "g8f6", "c6b7", "e7e6")
        viewModel.tap("b7", "a8")
        assertEquals(
            Square.fromUci("b7") to Square.fromUci("a8"),
            viewModel.uiState.value.pendingPromotion
        )
        assertEquals(8, viewModel.uiState.value.gameState.uciHistory.size)
        viewModel.onPromotionChosen(PieceKind.KNIGHT)
        assertEquals(
            Piece(PieceColor.WHITE, PieceKind.KNIGHT),
            viewModel.uiState.value.gameState.pieces[Square.fromUci("a8")]
        )
        assertEquals("b7a8n", viewModel.uiState.value.gameState.uciHistory.last())
    }

    @Test
    fun takebackRewindsOneMoveInHotseat() {
        val viewModel = hotseat()
        viewModel.play("e2e4", "e7e5")
        viewModel.offerTakeback()
        assertEquals(listOf("e2e4"), viewModel.uiState.value.gameState.uciHistory)
        assertEquals(PieceColor.BLACK, viewModel.uiState.value.gameState.sideToMove)
    }

    @Test
    fun hotseatMovesArePersistedAfterEveryPly() {
        val savedGames = FakeSavedGames()
        val viewModel = hotseat(savedGames)
        viewModel.play("e2e4", "e7e5")
        assertEquals(listOf("e2e4", "e7e5"), savedGames.stored?.uciHistory)
    }

    @Test
    fun aSavedHotseatGameIsRestoredOnTheNextViewModel() {
        val savedGames = FakeSavedGames()
        hotseat(savedGames).play("e2e4", "e7e5", "g1f3")
        val restored = hotseat(savedGames)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), restored.uiState.value.gameState.uciHistory)
        assertEquals(PieceColor.BLACK, restored.uiState.value.gameState.sideToMove)
    }

    @Test
    fun theClockOfTheSideToMoveLosesTheTimeSpentAwayFromTheApp() {
        val savedGames = FakeSavedGames()
        hotseat(savedGames).play("e2e4")
        val saved = savedGames.stored!!
        savedGames.stored = saved.copy(
            timeControl = TimeControl(300, 0),
            whiteMillis = 300_000,
            blackMillis = 300_000,
            savedAtMillis = saved.savedAtMillis - 10_000
        )
        val restored = hotseat(savedGames).uiState.value
        assertEquals(300_000, restored.whiteMillis)
        assertTrue(restored.blackMillis!! in 289_000..291_000, "black clock: ${restored.blackMillis}")
    }

    @Test
    fun aFinishedHotseatGameIsNotKept() {
        val savedGames = FakeSavedGames()
        hotseat(savedGames).play("f2f3", "e7e5", "g2g4", "d8h4")
        assertNull(savedGames.stored)
    }

    @Test
    fun anIllegalSavedHistoryIsDiscarded() {
        val savedGames = FakeSavedGames(
            SavedHotseatGame(
                uciHistory = listOf("e2e4", "e7e5", "e4e5"),
                timeControl = null,
                whiteMillis = null,
                blackMillis = null,
                savedAtMillis = 0,
                seriesWhiteWins = 0,
                seriesBlackWins = 0,
                seriesDraws = 0
            )
        )
        val restored = hotseat(savedGames)
        assertTrue(restored.uiState.value.gameState.uciHistory.isEmpty())
        assertNull(savedGames.stored)
    }

    private class FakeTransport : GameTransport {
        override val incoming: Flow<GameMessage> = MutableSharedFlow()
        override val connectionState = MutableStateFlow<TransportConnectionState>(TransportConnectionState.Connected)
        val sent = mutableListOf<GameMessage>()

        override suspend fun send(message: GameMessage) {
            sent += message
        }

        override suspend fun close() = Unit
    }

    private class RemoteFixture(myColor: PieceColor, kind: String = "lichess") {
        val transport = FakeTransport()
        val session = ActiveGameSession(transport, kind = kind)

        init {
            session.myColor.value = myColor
        }

        val moves: List<String> get() = transport.sent.filterIsInstance<GameMessage.MakeMove>().map { it.uci }

        val premovePlans: List<List<String>> get() = transport.sent.filterIsInstance<GameMessage.SetPremoves>().map { it.uciMoves }

        fun opponentPlays(uci: String, fenAfter: String, moveNumber: Int) {
            session.messages.tryEmit(GameMessage.MoveApplied(uci, fenAfter, moveNumber))
        }

        fun resync(fen: String) {
            session.messages.tryEmit(GameMessage.Resync(fen, emptyList(), drawOfferPending = false))
        }
    }

    private fun remote(fixture: RemoteFixture): GameViewModel {
        val viewModel = GameViewModel(GameMode.Remote(fixture.session))
        dispatcher.scheduler.runCurrent()
        return viewModel
    }

    @Test
    fun premovesQueueWhileItIsNotMyTurn() {
        val fixture = RemoteFixture(PieceColor.BLACK)
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        viewModel.tap("g8", "f6")
        assertEquals(listOf("e7e5", "g8f6"), viewModel.uiState.value.premoves)
        assertTrue(fixture.moves.isEmpty())
        assertEquals(
            Piece(PieceColor.BLACK, PieceKind.PAWN),
            viewModel.uiState.value.premoveState?.pieces?.get(Square.fromUci("e5"))
        )
    }

    @Test
    fun theFirstPremoveIsSentAsSoonAsTheTurnArrives() {
        val fixture = RemoteFixture(PieceColor.BLACK)
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        viewModel.tap("g8", "f6")
        fixture.opponentPlays("e2e4", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("e7e5"), fixture.moves)
        assertEquals(listOf("g8f6"), viewModel.uiState.value.premoves)
    }

    @Test
    fun chainedPremovesOfTheSamePieceSurviveTheInFlightMove() {
        val fixture = RemoteFixture(PieceColor.BLACK)
        val viewModel = remote(fixture)
        viewModel.tap("g8", "f6")
        viewModel.tap("f6", "g4")
        assertEquals(listOf("g8f6", "f6g4"), viewModel.uiState.value.premoves)
        fixture.opponentPlays("e2e4", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("g8f6"), fixture.moves)
        assertEquals(listOf("f6g4"), viewModel.uiState.value.premoves)
        fixture.opponentPlays("g8f6", "rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 1 2", 2)
        dispatcher.scheduler.runCurrent()
        fixture.opponentPlays("d2d4", "rnbqkb1r/pppppppp/5n2/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 3", 3)
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("g8f6", "f6g4"), fixture.moves)
    }

    @Test
    fun aPremoveTheLiveBoardRejectsDropsTheWholeQueue() {
        val fixture = RemoteFixture(PieceColor.BLACK)
        val viewModel = remote(fixture)
        fixture.resync("rnbqkbnr/pppp1ppp/4p3/3P4/8/8/PPP1PPPP/RNBQKBNR w KQkq - 0 3")
        dispatcher.scheduler.runCurrent()
        viewModel.tap("e6", "d5")
        viewModel.tap("f8", "b4")
        assertEquals(listOf("e6d5", "f8b4"), viewModel.uiState.value.premoves)
        fixture.opponentPlays("d5d6", "rnbqkbnr/pppp1ppp/3Pp3/8/8/8/PPP1PPPP/RNBQKBNR b KQkq - 0 3", 3)
        dispatcher.scheduler.runCurrent()
        assertTrue(fixture.moves.isEmpty())
        assertTrue(viewModel.uiState.value.premoves.isEmpty())
    }

    @Test
    fun tappingAnEmptySquareCancelsTheQueue() {
        val fixture = RemoteFixture(PieceColor.BLACK)
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        viewModel.tap("a4")
        assertTrue(viewModel.uiState.value.premoves.isEmpty())
        assertNull(viewModel.uiState.value.premoveState)
    }

    @Test
    fun onlinePremovesAreDelegatedToTheServer() {
        val fixture = RemoteFixture(PieceColor.BLACK, kind = "online")
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        viewModel.tap("g8", "f6")
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(listOf("e7e5"), listOf("e7e5", "g8f6")), fixture.premovePlans)
        fixture.opponentPlays("e2e4", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", 1)
        dispatcher.scheduler.runCurrent()
        assertTrue(fixture.moves.isEmpty())
        assertEquals(listOf("e7e5", "g8f6"), viewModel.uiState.value.premoves)
        fixture.opponentPlays("e7e5", "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", 2)
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("g8f6"), viewModel.uiState.value.premoves)
    }

    @Test
    fun aQueueTheClientDropsIsAlsoClearedOnTheServer() {
        val fixture = RemoteFixture(PieceColor.BLACK, kind = "online")
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(listOf("e7e5")), fixture.premovePlans)
        fixture.resync("rnbqkbnr/pppp1ppp/8/4p3/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 2")
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.premoves.isEmpty())
        assertEquals(listOf(listOf("e7e5"), emptyList()), fixture.premovePlans)
    }

    @Test
    fun aServerRejectionClearsTheOnlinePremoveQueue() {
        val fixture = RemoteFixture(PieceColor.BLACK, kind = "online")
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        fixture.session.messages.tryEmit(GameMessage.PremovesDropped("ILLEGAL_MOVE"))
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.premoves.isEmpty())
        assertNull(viewModel.uiState.value.premoveState)
    }

    @Test
    fun anOldServerFallsBackToClientSidePremoves() {
        val fixture = RemoteFixture(PieceColor.BLACK, kind = "online")
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        dispatcher.scheduler.runCurrent()
        fixture.session.messages.tryEmit(GameMessage.ProtocolError("BAD_MESSAGE", "cannot parse message"))
        dispatcher.scheduler.runCurrent()
        fixture.opponentPlays("e2e4", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("e7e5"), fixture.moves)
    }

    @Test
    fun premovesAreDroppedWhenTheGameEnds() {
        val fixture = RemoteFixture(PieceColor.BLACK)
        val viewModel = remote(fixture)
        viewModel.tap("e7", "e5")
        fixture.session.messages.tryEmit(GameMessage.GameOver(GameOverReason.RESIGNATION, PieceColor.BLACK))
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.premoves.isEmpty())
    }

    @Test
    fun theComputerRepliesAfterMyMove() {
        val viewModel = GameViewModel(
            GameMode.Computer(level = EngineLevel.ONE, myColor = PieceColor.WHITE),
            FakeSavedGames(),
            dispatcher
        )
        dispatcher.scheduler.runCurrent()
        viewModel.tap("e2", "e4")
        repeat(6) { dispatcher.scheduler.runCurrent() }
        val history = viewModel.uiState.value.gameState.uciHistory
        assertEquals("e2e4", history.first())
        assertEquals(2, history.size, "the engine should have answered")
        assertEquals(PieceColor.WHITE, viewModel.uiState.value.gameState.sideToMove)
    }

    @Test
    fun tapsAreIgnoredWhileTheComputerIsOnMove() {
        val viewModel = GameViewModel(
            GameMode.Computer(level = EngineLevel.ONE, myColor = PieceColor.BLACK),
            FakeSavedGames(),
            dispatcher
        )
        dispatcher.scheduler.runCurrent()
        viewModel.tap("e2", "e4")
        assertTrue(viewModel.uiState.value.gameState.uciHistory.size <= 1)
        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun aHintProposesALegalMove() {
        val viewModel = GameViewModel(GameMode.Hotseat, FakeSavedGames(), dispatcher)
        dispatcher.scheduler.runCurrent()
        viewModel.requestHint()
        repeat(6) { dispatcher.scheduler.runCurrent() }
        val hint = viewModel.uiState.value.hint
        assertNotNull(hint)
        val probe = ChessGame()
        assertTrue(probe.applyUci(hint!!) is MoveOutcome.Applied)
    }
}
