package dev.hawk0f.checkmates.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import dev.hawk0f.checkmates.net.lichess.LichessChallenge
import dev.hawk0f.checkmates.net.lichess.LichessClock
import dev.hawk0f.checkmates.net.lichess.LichessExplorerMove
import dev.hawk0f.checkmates.net.lichess.LichessExplorerPosition
import dev.hawk0f.checkmates.net.lichess.LichessLeaderboardUser
import dev.hawk0f.checkmates.net.lichess.LichessOngoingGame
import dev.hawk0f.checkmates.net.lichess.LichessOpponent
import dev.hawk0f.checkmates.net.lichess.LichessPerf
import dev.hawk0f.checkmates.net.lichess.LichessRating
import dev.hawk0f.checkmates.net.lichess.LichessTournament
import dev.hawk0f.checkmates.net.lichess.LichessUserRef
import dev.hawk0f.checkmates.session.PuzzlePersistence
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.protocol.GameSpeed
import dev.hawk0f.checkmates.shared.protocol.LeaderboardEntry
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.shared.protocol.RatingEntry
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import dev.hawk0f.checkmates.ui.ble.BleLobbyContent
import dev.hawk0f.checkmates.ui.ble.BleLobbyStep
import dev.hawk0f.checkmates.ui.ble.BleLobbyUiState
import dev.hawk0f.checkmates.ui.computer.ComputerSetupScreen
import dev.hawk0f.checkmates.ui.editor.BoardEditorScreen
import dev.hawk0f.checkmates.ui.editor.BoardEditorViewModel
import dev.hawk0f.checkmates.ui.flow.FlowPickerScreen
import dev.hawk0f.checkmates.ui.friends.FriendsContent
import dev.hawk0f.checkmates.ui.friends.FriendsUiState
import dev.hawk0f.checkmates.ui.game.GameMode
import dev.hawk0f.checkmates.ui.game.GameScreen
import dev.hawk0f.checkmates.ui.game.GameViewModel
import dev.hawk0f.checkmates.ui.home.HomeScreen
import dev.hawk0f.checkmates.ui.home.HomeViewModel
import dev.hawk0f.checkmates.ui.leaderboard.LeaderboardContent
import dev.hawk0f.checkmates.ui.leaderboard.LeaderboardUiState
import dev.hawk0f.checkmates.ui.lichess.LichessArenasContent
import dev.hawk0f.checkmates.ui.lichess.LichessArenasUiState
import dev.hawk0f.checkmates.ui.lichess.LichessExplorerContent
import dev.hawk0f.checkmates.ui.lichess.LichessExplorerUiState
import dev.hawk0f.checkmates.ui.lichess.LichessHomeContent
import dev.hawk0f.checkmates.ui.lichess.LichessHomeUiState
import dev.hawk0f.checkmates.ui.lichess.LichessPlayersContent
import dev.hawk0f.checkmates.ui.lichess.LichessPlayersUiState
import dev.hawk0f.checkmates.ui.lichess.LichessPuzzleContent
import dev.hawk0f.checkmates.ui.lichess.LichessPuzzleUiState
import dev.hawk0f.checkmates.ui.lichess.LichessReviewContent
import dev.hawk0f.checkmates.ui.lichess.LichessReviewUiState
import dev.hawk0f.checkmates.ui.lichess.LichessSeekContent
import dev.hawk0f.checkmates.ui.lichess.LichessSeekUiState
import dev.hawk0f.checkmates.ui.lichess.LichessWatchContent
import dev.hawk0f.checkmates.ui.lichess.LichessWatchUiState
import dev.hawk0f.checkmates.ui.lichess.WatchPlayer
import dev.hawk0f.checkmates.ui.online.LobbyStep
import dev.hawk0f.checkmates.ui.online.OnlineLobbyContent
import dev.hawk0f.checkmates.ui.online.OnlineLobbyUiState
import dev.hawk0f.checkmates.ui.openings.OpeningDrillScreen
import dev.hawk0f.checkmates.ui.openings.OpeningDrillViewModel
import dev.hawk0f.checkmates.ui.openings.OpeningsScreen
import dev.hawk0f.checkmates.ui.profile.ProfileContent
import dev.hawk0f.checkmates.ui.profile.ProfileUiState
import dev.hawk0f.checkmates.ui.puzzle.PuzzleScreen
import dev.hawk0f.checkmates.ui.puzzle.PuzzleViewModel
import dev.hawk0f.checkmates.ui.replay.ReplayScreen
import dev.hawk0f.checkmates.ui.settings.SettingsScreen

private const val PREVIEW_NOW_MILLIS = 1_736_942_400_000L

private object PreviewPuzzleStore : PuzzlePersistence {
    override fun loadProgress(): Map<String, PuzzleProgress> = emptyMap()

    override fun saveProgress(progress: Map<String, PuzzleProgress>) = Unit

    override fun loadRating(): Int = 1450

    override fun saveRating(rating: Int) = Unit

    override fun loadStreak(): Int = 4

    override fun saveStreak(streak: Int) = Unit
}

private val previewHistoryItem = GameHistoryItem(
    id = 1,
    mode = "online",
    myColor = PieceColor.WHITE,
    whiteName = "hawk0f",
    blackName = "Anna",
    winner = PieceColor.WHITE,
    reason = GameOverReason.CHECKMATE,
    uciHistory = listOf("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"),
    finishedAtMillis = PREVIEW_NOW_MILLIS
)

internal val homeScreenSpec = PreviewSpec("screen-home", fillsScreen = true) {
    HomeScreen(viewModel = remember { HomeViewModel() })
}

internal val flowPickerScreenSpec = PreviewSpec("screen-flow-picker", fillsScreen = true) {
    FlowPickerScreen(onChosen = {})
}

internal val computerSetupScreenSpec = PreviewSpec("screen-computer-setup", fillsScreen = true) {
    ComputerSetupScreen(onStart = { _, _ -> }, onBack = {})
}

internal val openingsScreenSpec = PreviewSpec("screen-openings", fillsScreen = true) {
    OpeningsScreen(onOpenLine = {}, onBack = {})
}

internal val settingsScreenSpec = PreviewSpec("screen-settings", fillsScreen = true) {
    SettingsScreen(onBack = {})
}

internal val boardEditorScreenSpec = PreviewSpec("screen-board-editor", capturable = false, fillsScreen = true) {
    BoardEditorScreen(
        onPlayHotseat = {},
        onPlayComputer = {},
        onOpenImportedGame = {},
        onBack = {},
        viewModel = remember { BoardEditorViewModel() }
    )
}

internal val puzzleScreenSpec = PreviewSpec("screen-puzzle", fillsScreen = true) {
    PuzzleScreen(
        onBack = {},
        viewModel = remember {
            PuzzleViewModel(store = PreviewPuzzleStore, now = { PREVIEW_NOW_MILLIS })
        }
    )
}

internal val replayScreenSpec = PreviewSpec("screen-replay", capturable = false, fillsScreen = true) {
    ReplayScreen(item = previewHistoryItem, onBack = {})
}

private val previewFriends = listOf(
    FriendSummary(userId = 7, displayName = "Anna", login = "anna", online = true),
    FriendSummary(userId = 8, displayName = "Boris", login = "boris"),
    FriendSummary(userId = 9, displayName = "Clara", login = "clara", online = true)
)

private val previewProfile = ProfileResponse(
    id = 42,
    login = "hawk0f",
    displayName = "hawk0f",
    avatarKind = "piece",
    avatarValue = "wq",
    createdAtMillis = PREVIEW_NOW_MILLIS
)

private val previewLichessOngoing = LichessOngoingGame(
    gameId = "abcd1234",
    color = "white",
    isMyTurn = true,
    lastMove = "e2e4",
    opponent = LichessOpponent(username = "penguin", rating = 1712),
    perf = "blitz",
    rated = true,
    secondsLeft = 174,
    speed = "blitz"
)

private val previewLichessChallenge = LichessChallenge(
    id = "ch1",
    status = "created",
    challenger = LichessUserRef(username = "rookiebot", rating = 1560),
    rated = false,
    speed = "rapid"
)

internal val leaderboardScreenSpec = PreviewSpec("screen-leaderboard", fillsScreen = true) {
    LeaderboardContent(
        uiState = LeaderboardUiState(
            speed = GameSpeed.BLITZ,
            entries = listOf(
                LeaderboardEntry(userId = 1, displayName = "Magnus", rating = 2882, games = 412, provisional = false),
                LeaderboardEntry(userId = 2, displayName = "Anna", rating = 2140, games = 96, provisional = false),
                LeaderboardEntry(userId = 3, displayName = "Boris", rating = 1804, games = 12, provisional = true)
            ),
            myRatings = listOf(
                RatingEntry(speed = GameSpeed.BLITZ, rating = 1842, deviation = 60, games = 74, provisional = false),
                RatingEntry(speed = GameSpeed.RAPID, rating = 1710, deviation = 110, games = 8, provisional = true)
            ),
            loading = false
        ),
        onSelectSpeed = {},
        onBack = {}
    )
}

internal val friendsScreenSpec = PreviewSpec("screen-friends", capturable = false, fillsScreen = true) {
    FriendsContent(
        uiState = FriendsUiState(
            friends = previewFriends.take(2),
            recentOpponents = previewFriends.drop(2),
            nameInput = "clara",
            loading = false
        ),
        signedIn = true,
        onNameChange = {},
        onAddFriend = {},
        onChallenge = {},
        onChallengeRecent = {},
        onRemoveFriend = {},
        onDismissError = {},
        onBack = {}
    )
}

internal val bleLobbyScreenSpec = PreviewSpec("screen-ble-lobby", capturable = false, fillsScreen = true) {
    BleLobbyContent(
        uiState = BleLobbyUiState(playerName = "hawk0f", step = BleLobbyStep.Hosting),
        onNameChange = {},
        onHost = {},
        onScan = {},
        onStopHosting = {},
        onConnect = {},
        onDismissError = {},
        onClose = {}
    )
}

internal val onlineLobbySetupScreenSpec = PreviewSpec("screen-online-lobby", capturable = false, fillsScreen = true) {
    OnlineLobbyContent(
        uiState = OnlineLobbyUiState(playerName = "hawk0f"),
        joining = false,
        onJoiningChange = {},
        onNameChange = {},
        onCodeChange = {},
        onTimeControlChange = {},
        onCreateGame = {},
        onJoinGame = {},
        onQuickPair = {},
        onCancelWaiting = {},
        onCancelSearch = {},
        onDismissError = {},
        onScan = {},
        onBack = {}
    )
}

internal val onlineLobbyWaitingScreenSpec = PreviewSpec("screen-online-lobby-waiting", fillsScreen = true) {
    OnlineLobbyContent(
        uiState = OnlineLobbyUiState(
            playerName = "hawk0f",
            step = LobbyStep.WaitingForOpponent(
                shortCode = "ABC234",
                joinUrl = "https://chess.hawk0f.icu/j/ABC234"
            )
        ),
        joining = false,
        onJoiningChange = {},
        onNameChange = {},
        onCodeChange = {},
        onTimeControlChange = {},
        onCreateGame = {},
        onJoinGame = {},
        onQuickPair = {},
        onCancelWaiting = {},
        onCancelSearch = {},
        onDismissError = {},
        onScan = {},
        onBack = {}
    )
}

internal val profileAuthScreenSpec = PreviewSpec("screen-profile-auth", capturable = false, fillsScreen = true) {
    ProfileContent(
        profile = null,
        uiState = ProfileUiState(),
        onLogin = { _, _ -> },
        onRegister = { _, _, _ -> },
        onLogout = {},
        onUpdateDisplayName = {},
        onUpdateAvatar = { _, _ -> },
        onDismissError = {},
        onOpenReplay = {},
        onBack = {}
    )
}

internal val profileAccountScreenSpec = PreviewSpec("screen-profile-account", capturable = false, fillsScreen = true) {
    ProfileContent(
        profile = previewProfile,
        uiState = ProfileUiState(history = listOf(previewHistoryItem), historyLoaded = true),
        onLogin = { _, _ -> },
        onRegister = { _, _, _ -> },
        onLogout = {},
        onUpdateDisplayName = {},
        onUpdateAvatar = { _, _ -> },
        onDismissError = {},
        onOpenReplay = {},
        onBack = {}
    )
}

internal val gameHotseatScreenSpec = PreviewSpec("screen-game-hotseat", fillsScreen = true) {
    GameScreen(
        mode = GameMode.Hotseat,
        onExit = {},
        viewModel = remember {
            GameViewModel(mode = GameMode.Hotseat).apply {
                selectTimeControl(null)
                for (uci in listOf("e2e4", "e7e5", "g1f3", "b8c6")) {
                    onSquareTap(Square.fromUci(uci.take(2)))
                    onSquareTap(Square.fromUci(uci.drop(2)))
                }
            }
        }
    )
}

internal val openingDrillScreenSpec = PreviewSpec("screen-opening-drill", fillsScreen = true) {
    OpeningDrillScreen(
        lineId = "italian",
        onBack = {},
        viewModel = remember { OpeningDrillViewModel("italian") }
    )
}

internal val lichessHomeSignedOutScreenSpec = PreviewSpec("screen-lichess-home-signed-out", fillsScreen = true) {
    LichessHomeContent(
        uiState = LichessHomeUiState(),
        onSignIn = {},
        onOpenGame = {},
        onAcceptChallenge = {},
        onDeclineChallenge = {},
        onLogout = {},
        onDismissError = {},
        onOpenSeek = {},
        onOpenPuzzle = {},
        onOpenWatch = {},
        onOpenArenas = {},
        onOpenExplorer = {},
        onOpenPlayers = {},
        onOpenSettings = {},
        onSwitchFlow = {}
    )
}

internal val lichessHomeSignedInScreenSpec = PreviewSpec("screen-lichess-home", fillsScreen = true) {
    LichessHomeContent(
        uiState = LichessHomeUiState(
            username = "hawk0f",
            ratings = listOf("blitz 1842", "rapid 1710"),
            streaming = true,
            ongoing = listOf(previewLichessOngoing),
            incoming = listOf(previewLichessChallenge)
        ),
        onSignIn = {},
        onOpenGame = {},
        onAcceptChallenge = {},
        onDeclineChallenge = {},
        onLogout = {},
        onDismissError = {},
        onOpenSeek = {},
        onOpenPuzzle = {},
        onOpenWatch = {},
        onOpenArenas = {},
        onOpenExplorer = {},
        onOpenPlayers = {},
        onOpenSettings = {},
        onSwitchFlow = {}
    )
}

internal val lichessSeekScreenSpec = PreviewSpec("screen-lichess-seek", fillsScreen = true) {
    LichessSeekContent(
        uiState = LichessSeekUiState(myRating = 1842),
        onModeChange = {},
        onPoolClockChange = {},
        onDirectClockChange = {},
        onRatedChange = {},
        onRatingSpreadChange = {},
        onFriendNameChange = {},
        onAiLevelChange = {},
        onCreateOpenChallenge = {},
        onCancelChallenge = {},
        onStart = {},
        onCancelWaiting = {},
        onDismissError = {},
        onDismissOpenChallenge = {},
        onBack = {}
    )
}

internal val lichessPlayersScreenSpec = PreviewSpec("screen-lichess-players", capturable = false, fillsScreen = true) {
    LichessPlayersContent(
        uiState = LichessPlayersUiState(
            loading = false,
            query = "pen",
            suggestions = listOf("penguin", "pendulum"),
            online = listOf(LichessUserRef(username = "penguin", rating = 1712, online = true)),
            offline = listOf(LichessUserRef(username = "walrus", rating = 1490)),
            leaderboard = listOf(
                LichessLeaderboardUser(
                    id = "magnus",
                    username = "DrNykterstein",
                    title = "GM",
                    perfs = mapOf("blitz" to LichessRating(rating = 3105, progress = 12))
                )
            )
        ),
        onQueryChange = {},
        onChallenge = {},
        onCreateOpenChallenge = {},
        onDismissMessage = {},
        onBack = {}
    )
}

internal val lichessArenasScreenSpec = PreviewSpec("screen-lichess-arenas", fillsScreen = true) {
    LichessArenasContent(
        uiState = LichessArenasUiState(
            loading = false,
            featured = LichessTournament(
                id = "hourly",
                fullName = "Hourly Blitz Arena",
                clock = LichessClock(limit = 180, increment = 2),
                nbPlayers = 1284,
                minutes = 60,
                secondsToFinish = 1800,
                rated = true,
                perf = LichessPerf(key = "blitz", name = "Blitz")
            ),
            starting = listOf(
                LichessTournament(
                    id = "daily",
                    fullName = "Daily Rapid Arena",
                    clock = LichessClock(limit = 600, increment = 0),
                    nbPlayers = 312,
                    minutes = 120,
                    secondsToStart = 900,
                    rated = true,
                    perf = LichessPerf(key = "rapid", name = "Rapid")
                )
            )
        ),
        onJoin = {},
        onDismissMessage = {},
        onBack = {}
    )
}

internal val lichessPuzzleScreenSpec = PreviewSpec("screen-lichess-puzzle", fillsScreen = true) {
    LichessPuzzleContent(
        uiState = LichessPuzzleUiState(
            loading = false,
            puzzleId = "aBcDe",
            rating = 1620,
            themes = listOf("mateIn2", "kingsideAttack"),
            gameState = previewState("e2e4", "e7e5", "g1f3", "b8c6", "f1b5"),
            sideToMove = PieceColor.BLACK,
            flipped = true,
            movesLeft = 3,
            streak = 5,
            myPuzzleRating = 1704
        ),
        onLoadNext = {},
        onLoadDaily = {},
        onSquareTap = {},
        onDismissError = {},
        onBack = {}
    )
}

internal val lichessExplorerScreenSpec = PreviewSpec("screen-lichess-explorer", fillsScreen = true) {
    LichessExplorerContent(
        uiState = LichessExplorerUiState(
            gameState = previewState("e2e4", "c7c5"),
            moves = listOf("e2e4", "c7c5"),
            position = LichessExplorerPosition(
                white = 21_450,
                draws = 8_902,
                black = 19_884,
                moves = listOf(
                    LichessExplorerMove(uci = "g1f3", san = "Nf3", white = 9_120, draws = 4_010, black = 8_330, averageRating = 2412),
                    LichessExplorerMove(uci = "b1c3", san = "Nc3", white = 4_210, draws = 1_902, black = 4_004, averageRating = 2380)
                )
            )
        ),
        onSourceChange = {},
        onPlayMove = {},
        onUndo = {},
        onReset = {},
        onDismissError = {},
        onBack = {}
    )
}

internal val lichessWatchScreenSpec = PreviewSpec("screen-lichess-watch", fillsScreen = true) {
    LichessWatchContent(
        uiState = LichessWatchUiState(
            channels = listOf("best", "blitz", "rapid", "classical", "bullet"),
            channel = "blitz",
            gameId = "abcd1234",
            gameState = previewState("d2d4", "d7d5", "c2c4", "e7e6", "b1c3"),
            white = WatchPlayer(name = "DrNykterstein", title = "GM", rating = 3105, seconds = 128),
            black = WatchPlayer(name = "penguingim1", title = "GM", rating = 2914, seconds = 96),
            live = true,
            streamerCount = 7
        ),
        onWatch = {},
        onDismissError = {},
        onReview = {},
        onBack = {}
    )
}

internal val lichessReviewScreenSpec = PreviewSpec("screen-lichess-review", fillsScreen = true) {
    LichessReviewContent(
        uiState = LichessReviewUiState(
            gameId = "abcd1234",
            loading = false,
            moves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6"),
            moveIndex = 4,
            gameState = previewState("e2e4", "e7e5", "g1f3", "b8c6")
        ),
        onGoTo = {},
        onStep = {},
        onDismissError = {},
        onBack = {}
    )
}

internal val screenPreviewSpecs = listOf(
    homeScreenSpec,
    flowPickerScreenSpec,
    computerSetupScreenSpec,
    openingsScreenSpec,
    settingsScreenSpec,
    boardEditorScreenSpec,
    puzzleScreenSpec,
    replayScreenSpec,
    leaderboardScreenSpec,
    friendsScreenSpec,
    bleLobbyScreenSpec,
    onlineLobbySetupScreenSpec,
    onlineLobbyWaitingScreenSpec,
    profileAuthScreenSpec,
    profileAccountScreenSpec,
    gameHotseatScreenSpec,
    openingDrillScreenSpec,
    lichessHomeSignedOutScreenSpec,
    lichessHomeSignedInScreenSpec,
    lichessSeekScreenSpec,
    lichessPlayersScreenSpec,
    lichessArenasScreenSpec,
    lichessPuzzleScreenSpec,
    lichessExplorerScreenSpec,
    lichessWatchScreenSpec,
    lichessReviewScreenSpec
)

@Preview
@Composable
internal fun HomeScreenPreview() = PreviewFrame(homeScreenSpec)

@Preview
@Composable
internal fun FlowPickerScreenPreview() = PreviewFrame(flowPickerScreenSpec)

@Preview
@Composable
internal fun ComputerSetupScreenPreview() = PreviewFrame(computerSetupScreenSpec)

@Preview
@Composable
internal fun OpeningsScreenPreview() = PreviewFrame(openingsScreenSpec)

@Preview
@Composable
internal fun SettingsScreenPreview() = PreviewFrame(settingsScreenSpec)

@Preview
@Composable
internal fun BoardEditorScreenPreview() = PreviewFrame(boardEditorScreenSpec)

@Preview
@Composable
internal fun PuzzleScreenPreview() = PreviewFrame(puzzleScreenSpec)

@Preview
@Composable
internal fun ReplayScreenPreview() = PreviewFrame(replayScreenSpec)

@Preview
@Composable
internal fun LeaderboardScreenPreview() = PreviewFrame(leaderboardScreenSpec)

@Preview
@Composable
internal fun FriendsScreenPreview() = PreviewFrame(friendsScreenSpec)

@Preview
@Composable
internal fun BleLobbyScreenPreview() = PreviewFrame(bleLobbyScreenSpec)

@Preview
@Composable
internal fun OnlineLobbySetupScreenPreview() = PreviewFrame(onlineLobbySetupScreenSpec)

@Preview
@Composable
internal fun OnlineLobbyWaitingScreenPreview() = PreviewFrame(onlineLobbyWaitingScreenSpec)

@Preview
@Composable
internal fun ProfileAuthScreenPreview() = PreviewFrame(profileAuthScreenSpec)

@Preview
@Composable
internal fun ProfileAccountScreenPreview() = PreviewFrame(profileAccountScreenSpec)

@Preview
@Composable
internal fun GameHotseatScreenPreview() = PreviewFrame(gameHotseatScreenSpec)

@Preview
@Composable
internal fun OpeningDrillScreenPreview() = PreviewFrame(openingDrillScreenSpec)

@Preview
@Composable
internal fun LichessHomeSignedOutScreenPreview() = PreviewFrame(lichessHomeSignedOutScreenSpec)

@Preview
@Composable
internal fun LichessHomeScreenPreview() = PreviewFrame(lichessHomeSignedInScreenSpec)

@Preview
@Composable
internal fun LichessSeekScreenPreview() = PreviewFrame(lichessSeekScreenSpec)

@Preview
@Composable
internal fun LichessPlayersScreenPreview() = PreviewFrame(lichessPlayersScreenSpec)

@Preview
@Composable
internal fun LichessArenasScreenPreview() = PreviewFrame(lichessArenasScreenSpec)

@Preview
@Composable
internal fun LichessPuzzleScreenPreview() = PreviewFrame(lichessPuzzleScreenSpec)

@Preview
@Composable
internal fun LichessExplorerScreenPreview() = PreviewFrame(lichessExplorerScreenSpec)

@Preview
@Composable
internal fun LichessWatchScreenPreview() = PreviewFrame(lichessWatchScreenSpec)

@Preview
@Composable
internal fun LichessReviewScreenPreview() = PreviewFrame(lichessReviewScreenSpec)
