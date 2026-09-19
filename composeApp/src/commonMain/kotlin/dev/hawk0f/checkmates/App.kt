package dev.hawk0f.checkmates

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import dev.hawk0f.checkmates.platform.SystemBarsAppearance
import dev.hawk0f.checkmates.ui.theme.appUsesDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.net.configuredHttpClient
import dev.hawk0f.checkmates.platform.CrashStorage
import dev.hawk0f.checkmates.platform.currentPushToken
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.CrashUploader
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.engine.EngineLevel
import dev.hawk0f.checkmates.shared.engine.EngineStyle
import dev.hawk0f.checkmates.ui.ble.BleLobbyScreen
import dev.hawk0f.checkmates.ui.computer.ComputerSetupScreen
import dev.hawk0f.checkmates.ui.correspondence.CorrespondenceScreen
import dev.hawk0f.checkmates.ui.game.GameMode
import dev.hawk0f.checkmates.ui.game.GameScreen
import dev.hawk0f.checkmates.ui.history.GameHistoryScreen
import dev.hawk0f.checkmates.ui.home.HomeScreen
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PositionEditor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.ui.editor.BoardEditorScreen
import dev.hawk0f.checkmates.ui.friends.FriendsScreen
import dev.hawk0f.checkmates.ui.leaderboard.LeaderboardScreen
import dev.hawk0f.checkmates.ui.openings.OpeningDrillScreen
import dev.hawk0f.checkmates.ui.openings.OpeningsScreen
import dev.hawk0f.checkmates.ui.puzzle.PuzzleScreen
import dev.hawk0f.checkmates.ui.online.OnlineLobbyScreen
import dev.hawk0f.checkmates.ui.profile.ProfileScreen
import dev.hawk0f.checkmates.ui.profile.ReplayHolder
import dev.hawk0f.checkmates.ui.replay.ReplayScreen
import dev.hawk0f.checkmates.ui.settings.SettingsScreen
import dev.hawk0f.checkmates.ui.theme.AppTheme
import dev.hawk0f.checkmates.ui.theme.MaxContentWidth
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class HotseatGameRoute(val startFen: String? = null)

@Serializable
object ComputerSetupRoute

@Serializable
data class ComputerGameRoute(
    val level: Int,
    val playsWhite: Boolean,
    val style: String = EngineStyle.BALANCED.name,
    val startFen: String? = null
)

@Serializable
data class OnlineLobbyRoute(val prefillCode: String? = null)

@Serializable
object BleLobbyRoute

@Serializable
object RemoteGameRoute

@Serializable
data class EditorRoute(val startFen: String? = null)

@Serializable
object FriendsRoute

@Serializable
data class CorrespondenceRoute(val gameId: Long? = null)

@Serializable
object OpeningsRoute

@Serializable
data class OpeningDrillRoute(val lineId: String)

@Serializable
object PuzzleRoute

@Serializable
object LeaderboardRoute

@Serializable
object SettingsRoute

@Serializable
object ProfileRoute

@Serializable
object GameHistoryRoute

@Serializable
object ReplayRoute

@Composable
fun App() {
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            LaunchedEffect(Unit) {
                CrashStorage.installHandler()
                val client = configuredHttpClient()
                try {
                    val api = ApiClient(client)
                    runCatching { CrashUploader.uploadPending(api) }
                    val authToken = AuthManager.token
                    if (authToken != null) {
                        val pushToken = runCatching { currentPushToken() }.getOrNull()
                        if (pushToken != null) {
                            runCatching { api.savePushToken(authToken, pushToken) }
                        }
                    }
                } finally {
                    client.close()
                }
            }
            LaunchedEffect(Unit) {
                DeepLinkHandler.pendingCorrespondenceId.collect { gameId ->
                    if (gameId != null) {
                        DeepLinkHandler.consumeCorrespondence()
                        navController.navigate(CorrespondenceRoute(gameId))
                    }
                }
            }
            LaunchedEffect(Unit) {
                DeepLinkHandler.pendingCode.collect { code ->
                    if (code != null) {
                        DeepLinkHandler.consume()
                        navController.navigate(OnlineLobbyRoute(prefillCode = code))
                    }
                }
            }
            SystemBarsAppearance(darkIcons = !appUsesDarkTheme())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .safeDrawingPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onPassAndPlay = { navController.navigate(HotseatGameRoute()) },
                            onPlayOnline = { navController.navigate(OnlineLobbyRoute()) },
                            onPlayBluetooth = { navController.navigate(BleLobbyRoute) },
                            onPlayComputer = { navController.navigate(ComputerSetupRoute) },
                            onOpenPuzzles = { navController.navigate(PuzzleRoute) },
                            onOpenEditor = { navController.navigate(EditorRoute()) },
                            onOpenOpenings = { navController.navigate(OpeningsRoute) },
                            onOpenFriends = { navController.navigate(FriendsRoute) },
                            onOpenCorrespondence = { navController.navigate(CorrespondenceRoute()) },
                            onOpenLeaderboard = { navController.navigate(LeaderboardRoute) },
                            onOpenProfile = { navController.navigate(ProfileRoute) },
                            onOpenHistory = { navController.navigate(GameHistoryRoute) },
                            onOpenReplay = { item ->
                                ReplayHolder.current = item
                                navController.navigate(ReplayRoute)
                            },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onResumeGame = { navController.navigate(RemoteGameRoute) }
                        )
                    }
                    composable<FriendsRoute> {
                        FriendsScreen(
                            onGameReady = {
                                navController.navigate(RemoteGameRoute) {
                                    popUpTo<HomeRoute>()
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<CorrespondenceRoute> { entry ->
                        val route = entry.toRoute<CorrespondenceRoute>()
                        CorrespondenceScreen(
                            initialGameId = route.gameId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<OpeningsRoute> {
                        OpeningsScreen(
                            onOpenLine = { lineId -> navController.navigate(OpeningDrillRoute(lineId)) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<OpeningDrillRoute> { entry ->
                        val route = entry.toRoute<OpeningDrillRoute>()
                        OpeningDrillScreen(
                            lineId = route.lineId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<PuzzleRoute> {
                        PuzzleScreen(onBack = { navController.popBackStack() })
                    }
                    composable<LeaderboardRoute> {
                        LeaderboardScreen(onBack = { navController.popBackStack() })
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<ProfileRoute> {
                        ProfileScreen(
                            onOpenReplay = { navController.navigate(ReplayRoute) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<GameHistoryRoute> {
                        GameHistoryScreen(
                            onOpenReplay = { item ->
                                ReplayHolder.current = item
                                navController.navigate(ReplayRoute)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<ReplayRoute> {
                        val item = ReplayHolder.current
                        if (item == null) {
                            LaunchedEffect(Unit) {
                                navController.popBackStack(ReplayRoute, inclusive = true)
                            }
                        } else {
                            ReplayScreen(item = item, onBack = { navController.popBackStack() })
                        }
                    }
                    composable<ComputerSetupRoute> {
                        ComputerSetupScreen(
                            onStart = { level, color, style ->
                                navController.navigate(
                                    ComputerGameRoute(
                                        level = level.id,
                                        playsWhite = color == PieceColor.WHITE,
                                        style = style.name
                                    )
                                ) {
                                    popUpTo<ComputerSetupRoute> {
                                        inclusive = true
                                    }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<ComputerGameRoute> { entry ->
                        val route = entry.toRoute<ComputerGameRoute>()
                        GameScreen(
                            mode = GameMode.Computer(
                                level = EngineLevel.byId(route.level),
                                myColor = if (route.playsWhite) PieceColor.WHITE else PieceColor.BLACK,
                                style = EngineStyle.byName(route.style)
                            ),
                            onExit = { navController.popBackStack() },
                            startFen = route.startFen
                        )
                    }
                    composable<HotseatGameRoute> { entry ->
                        val route = entry.toRoute<HotseatGameRoute>()
                        GameScreen(
                            mode = GameMode.Hotseat,
                            onExit = { navController.popBackStack() },
                            startFen = route.startFen
                        )
                    }
                    composable<EditorRoute> { entry ->
                        val route = entry.toRoute<EditorRoute>()
                        BoardEditorScreen(
                            onPlayHotseat = { fen -> navController.navigate(HotseatGameRoute(fen)) },
                            onPlayComputer = { fen ->
                                navController.navigate(
                                    ComputerGameRoute(
                                        level = EngineLevel.DEFAULT.id,
                                        playsWhite = PositionEditor.sideToMoveFromFen(fen) == PieceColor.WHITE,
                                        startFen = fen
                                    )
                                )
                            },
                            onOpenImportedGame = { imported ->
                                ReplayHolder.current = GameHistoryItem(
                                    id = 0,
                                    mode = "import",
                                    myColor = PieceColor.WHITE,
                                    whiteName = imported.whiteName,
                                    blackName = imported.blackName,
                                    winner = imported.winner,
                                    reason = GameOverReason.RESIGNATION,
                                    uciHistory = imported.uciMoves,
                                    finishedAtMillis = 0,
                                    startFen = imported.startFen
                                )
                                navController.navigate(ReplayRoute)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<OnlineLobbyRoute> { entry ->
                        val route = entry.toRoute<OnlineLobbyRoute>()
                        OnlineLobbyScreen(
                            prefillCode = route.prefillCode,
                            onGameReady = {
                                navController.navigate(RemoteGameRoute) {
                                    popUpTo<HomeRoute>()
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<BleLobbyRoute> {
                        BleLobbyScreen(
                            onGameReady = {
                                navController.navigate(RemoteGameRoute) {
                                    popUpTo<HomeRoute>()
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<RemoteGameRoute> {
                        val session = GameSessionHolder.current
                        if (session == null) {
                            LaunchedEffect(Unit) {
                                navController.popBackStack(RemoteGameRoute, inclusive = true)
                            }
                        } else {
                            GameScreen(
                                mode = GameMode.Remote(session),
                                onExit = {
                                    GameSessionHolder.detach()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
