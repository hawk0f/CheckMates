package dev.hawk0f.checkmates.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
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
import dev.hawk0f.checkmates.ui.friends.FriendsContent
import dev.hawk0f.checkmates.ui.friends.FriendsUiState
import dev.hawk0f.checkmates.ui.game.GameMode
import dev.hawk0f.checkmates.ui.game.GameScreen
import dev.hawk0f.checkmates.ui.game.GameViewModel
import dev.hawk0f.checkmates.ui.home.HomeScreen
import dev.hawk0f.checkmates.ui.home.HomeViewModel
import dev.hawk0f.checkmates.ui.leaderboard.LeaderboardContent
import dev.hawk0f.checkmates.ui.leaderboard.LeaderboardUiState
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

    override fun loadPersonalPuzzles() = emptyList<dev.hawk0f.checkmates.shared.puzzle.Puzzle>()

    override fun savePersonalPuzzle(puzzle: dev.hawk0f.checkmates.shared.puzzle.Puzzle) = Unit
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

internal val computerSetupScreenSpec = PreviewSpec("screen-computer-setup", fillsScreen = true) {
    ComputerSetupScreen(onStart = { _, _, _ -> }, onBack = {})
}

internal val openingsScreenSpec = PreviewSpec("screen-openings", fillsScreen = true) {
    OpeningsScreen(onOpenLine = {}, onBack = {})
}

internal val settingsScreenSpec = PreviewSpec("screen-settings", fillsScreen = true) {
    SettingsScreen(onBack = {})
}

internal val boardEditorScreenSpec = PreviewSpec("screen-board-editor", fillsScreen = true) {
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

internal val friendsScreenSpec = PreviewSpec("screen-friends", fillsScreen = true) {
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

internal val bleLobbyScreenSpec = PreviewSpec("screen-ble-lobby", fillsScreen = true) {
    BleLobbyContent(
        uiState = BleLobbyUiState(playerName = "hawk0f", step = BleLobbyStep.Hosting),
        onNameChange = {},
        onTimeControlChange = {},
        onHost = {},
        onScan = {},
        onStopHosting = {},
        onConnect = {},
        onDismissError = {},
        onClose = {}
    )
}

internal val onlineLobbySetupScreenSpec = PreviewSpec("screen-online-lobby", fillsScreen = true) {
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

internal val profileAuthScreenSpec = PreviewSpec("screen-profile-auth", fillsScreen = true) {
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

internal val profileAccountScreenSpec = PreviewSpec("screen-profile-account", fillsScreen = true) {
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

internal val screenPreviewSpecs = listOf(
    homeScreenSpec,
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
    openingDrillScreenSpec
)

@Preview
@Composable
internal fun HomeScreenPreview() = PreviewFrame(homeScreenSpec)

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
