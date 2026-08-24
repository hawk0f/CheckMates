package dev.hawk0f.checkmates

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import dev.hawk0f.checkmates.platform.SystemBarsAppearance
import dev.hawk0f.checkmates.ui.theme.appUsesDarkTheme
import dev.hawk0f.checkmates.ui.lichess.WatchSurface
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.hawk0f.checkmates.session.AppFlow
import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.net.configuredHttpClient
import dev.hawk0f.checkmates.platform.CrashStorage
import dev.hawk0f.checkmates.platform.currentPushToken
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.CrashUploader
import dev.hawk0f.checkmates.session.FlowManager
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.engine.EngineLevel
import dev.hawk0f.checkmates.ui.ble.BleLobbyScreen
import dev.hawk0f.checkmates.ui.computer.ComputerSetupScreen
import dev.hawk0f.checkmates.ui.flow.FlowPickerScreen
import dev.hawk0f.checkmates.ui.game.GameMode
import dev.hawk0f.checkmates.ui.game.GameScreen
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
import dev.hawk0f.checkmates.ui.lichess.LichessHomeScreen
import dev.hawk0f.checkmates.ui.lichess.LichessArenasScreen
import dev.hawk0f.checkmates.ui.lichess.LichessExplorerScreen
import dev.hawk0f.checkmates.ui.lichess.LichessPlayersScreen
import dev.hawk0f.checkmates.ui.lichess.LichessReviewScreen
import dev.hawk0f.checkmates.ui.lichess.LichessWatchScreen
import dev.hawk0f.checkmates.ui.lichess.LichessPuzzleScreen
import dev.hawk0f.checkmates.ui.lichess.LichessSeekScreen
import dev.hawk0f.checkmates.ui.online.OnlineLobbyScreen
import dev.hawk0f.checkmates.ui.profile.ProfileScreen
import dev.hawk0f.checkmates.ui.profile.ReplayHolder
import dev.hawk0f.checkmates.ui.replay.ReplayScreen
import dev.hawk0f.checkmates.ui.settings.SettingsScreen
import dev.hawk0f.checkmates.ui.theme.AppTheme
import dev.hawk0f.checkmates.ui.theme.MaxContentWidth
import kotlinx.serialization.Serializable

@Serializable
object FlowPickerRoute

@Serializable
object HomeRoute

@Serializable
data class HotseatGameRoute(val startFen: String? = null)

@Serializable
object ComputerSetupRoute

@Serializable
data class ComputerGameRoute(val level: Int, val playsWhite: Boolean, val startFen: String? = null)

@Serializable
data class OnlineLobbyRoute(val prefillCode: String? = null)

@Serializable
object BleLobbyRoute

@Serializable
object LichessHomeRoute

@Serializable
object LichessSeekRoute

@Serializable
object LichessPuzzleRoute

@Serializable
data class LichessExplorerRoute(val fen: String? = null)

@Serializable
object LichessWatchRoute

@Serializable
object LichessArenasRoute

@Serializable
object LichessPlayersRoute

@Serializable
data class LichessReviewRoute(val gameId: String)

@Serializable
object RemoteGameRoute

@Serializable
data class EditorRoute(val startFen: String? = null)

@Serializable
object FriendsRoute

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
                DeepLinkHandler.pendingCode.collect { code ->
                    if (code != null) {
                        DeepLinkHandler.consume()
                        if (FlowManager.current != AppFlow.CHECKMATES) {
                            FlowManager.select(AppFlow.CHECKMATES)
                        }
                        navController.navigate(OnlineLobbyRoute(prefillCode = code))
                    }
                }
            }
            LaunchedEffect(Unit) {
                DeepLinkHandler.pendingLichessAuth.collect { callback ->
                    if (callback != null) {
                        if (FlowManager.current != AppFlow.LICHESS) {
                            FlowManager.select(AppFlow.LICHESS)
                        }
                        navController.navigate(LichessHomeRoute) {
                            launchSingleTop = true
                        }
                    }
                }
            }
            val currentEntry by navController.currentBackStackEntryAsState()
            val darkPage = currentEntry?.destination?.route?.contains("LichessWatchRoute") == true
            SystemBarsAppearance(darkIcons = !darkPage && !appUsesDarkTheme())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (darkPage) WatchSurface else MaterialTheme.colorScheme.background)
                    .safeDrawingPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                val startDestination: Any = remember {
                    when (FlowManager.current) {
                        null -> FlowPickerRoute
                        AppFlow.CHECKMATES -> HomeRoute
                        AppFlow.LICHESS -> LichessHomeRoute
                    }
                }
                val openFlowHome: (AppFlow) -> Unit = { chosen ->
                    FlowManager.select(chosen)
                    val destination: Any = if (chosen == AppFlow.LICHESS) LichessHomeRoute else HomeRoute
                    navController.navigate(destination) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()
                ) {
                    composable<FlowPickerRoute> {
                        FlowPickerScreen(
                            initial = FlowManager.current,
                            onChosen = openFlowHome
                        )
                    }
                    composable<HomeRoute> {
                        HomeScreen(
                            onPassAndPlay = { navController.navigate(HotseatGameRoute()) },
                            onPlayOnline = { navController.navigate(OnlineLobbyRoute()) },
                            onPlayBluetooth = { navController.navigate(BleLobbyRoute) },
                            onPlayComputer = { navController.navigate(ComputerSetupRoute) },
                            onSwitchFlow = { openFlowHome(AppFlow.LICHESS) },
                            onOpenPuzzles = { navController.navigate(PuzzleRoute) },
                            onOpenEditor = { navController.navigate(EditorRoute()) },
                            onOpenOpenings = { navController.navigate(OpeningsRoute) },
                            onOpenFriends = { navController.navigate(FriendsRoute) },
                            onOpenLeaderboard = { navController.navigate(LeaderboardRoute) },
                            onOpenProfile = { navController.navigate(ProfileRoute) },
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
                            onBack = { navController.popBackStack() },
                            onSwitchFlow = { openFlowHome(FlowManager.other()) }
                        )
                    }
                    composable<ProfileRoute> {
                        ProfileScreen(
                            onOpenReplay = { navController.navigate(ReplayRoute) },
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
                            onStart = { level, color ->
                                navController.navigate(
                                    ComputerGameRoute(
                                        level = level.id,
                                        playsWhite = color == PieceColor.WHITE
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
                                myColor = if (route.playsWhite) PieceColor.WHITE else PieceColor.BLACK
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
                            onOpenImportedGame = { moves ->
                                ReplayHolder.current = GameHistoryItem(
                                    id = 0,
                                    mode = "import",
                                    myColor = PieceColor.WHITE,
                                    whiteName = "White",
                                    blackName = "Black",
                                    winner = null,
                                    reason = GameOverReason.RESIGNATION,
                                    uciHistory = moves,
                                    finishedAtMillis = 0
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
                    composable<LichessHomeRoute> {
                        LichessHomeScreen(
                            onGameReady = {
                                navController.navigate(RemoteGameRoute) {
                                    popUpTo<HomeRoute>()
                                }
                            },
                            onOpenSeek = { navController.navigate(LichessSeekRoute) },
                            onOpenPuzzle = { navController.navigate(LichessPuzzleRoute) },
                            onOpenWatch = { navController.navigate(LichessWatchRoute) },
                            onOpenArenas = { navController.navigate(LichessArenasRoute) },
                            onOpenExplorer = { navController.navigate(LichessExplorerRoute()) },
                            onOpenPlayers = { navController.navigate(LichessPlayersRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onSwitchFlow = { openFlowHome(AppFlow.CHECKMATES) }
                        )
                    }
                    composable<LichessPuzzleRoute> {
                        LichessPuzzleScreen(onBack = { navController.popBackStack() })
                    }
                    composable<LichessWatchRoute> {
                        LichessWatchScreen(
                            onReview = { gameId -> navController.navigate(LichessReviewRoute(gameId)) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<LichessArenasRoute> {
                        LichessArenasScreen(onBack = { navController.popBackStack() })
                    }
                    composable<LichessPlayersRoute> {
                        LichessPlayersScreen(onBack = { navController.popBackStack() })
                    }
                    composable<LichessReviewRoute> { entry ->
                        val route = entry.toRoute<LichessReviewRoute>()
                        LichessReviewScreen(
                            gameId = route.gameId,
                            onBack = { navController.popBackStack() },
                            onOpenExplorer = { fen ->
                                navController.navigate(LichessExplorerRoute(fen))
                            }
                        )
                    }
                    composable<LichessExplorerRoute> { entry ->
                        val route = entry.toRoute<LichessExplorerRoute>()
                        LichessExplorerScreen(
                            onBack = { navController.popBackStack() },
                            startFen = route.fen
                        )
                    }
                    composable<LichessSeekRoute> {
                        LichessSeekScreen(
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
                                    GameSessionHolder.clear()
                                    navController.popBackStack()
                                },
                                onOpenReview = { gameId ->
                                    navController.navigate(LichessReviewRoute(gameId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
