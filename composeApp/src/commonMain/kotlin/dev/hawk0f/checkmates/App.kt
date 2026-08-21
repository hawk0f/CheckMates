package dev.hawk0f.checkmates

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
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
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.ui.ble.BleLobbyScreen
import dev.hawk0f.checkmates.ui.game.GameMode
import dev.hawk0f.checkmates.ui.game.GameScreen
import dev.hawk0f.checkmates.ui.home.HomeScreen
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
object HomeRoute

@Serializable
object HotseatGameRoute

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
object LichessExplorerRoute

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
                DeepLinkHandler.pendingCode.collect { code ->
                    if (code != null) {
                        DeepLinkHandler.consume()
                        navController.navigate(OnlineLobbyRoute(prefillCode = code))
                    }
                }
            }
            LaunchedEffect(Unit) {
                DeepLinkHandler.pendingLichessAuth.collect { callback ->
                    if (callback != null) {
                        navController.navigate(LichessHomeRoute) {
                            launchSingleTop = true
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onPassAndPlay = { navController.navigate(HotseatGameRoute) },
                            onPlayOnline = { navController.navigate(OnlineLobbyRoute()) },
                            onPlayBluetooth = { navController.navigate(BleLobbyRoute) },
                            onPlayLichess = { navController.navigate(LichessHomeRoute) },
                            onOpenProfile = { navController.navigate(ProfileRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onResumeGame = { navController.navigate(RemoteGameRoute) }
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(onBack = { navController.popBackStack() })
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
                    composable<HotseatGameRoute> {
                        GameScreen(
                            mode = GameMode.Hotseat,
                            onExit = { navController.popBackStack() }
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
                            onOpenExplorer = { navController.navigate(LichessExplorerRoute) },
                            onOpenPlayers = { navController.navigate(LichessPlayersRoute) },
                            onBack = { navController.popBackStack() }
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
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<LichessExplorerRoute> {
                        LichessExplorerScreen(onBack = { navController.popBackStack() })
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
