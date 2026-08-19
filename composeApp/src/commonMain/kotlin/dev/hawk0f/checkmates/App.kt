package dev.hawk0f.checkmates

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
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
import dev.hawk0f.checkmates.ui.online.OnlineLobbyScreen
import dev.hawk0f.checkmates.ui.profile.ProfileScreen
import dev.hawk0f.checkmates.ui.profile.ReplayHolder
import dev.hawk0f.checkmates.ui.replay.ReplayScreen
import dev.hawk0f.checkmates.ui.settings.SettingsScreen
import dev.hawk0f.checkmates.ui.theme.AppTheme
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
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            val navController = rememberNavController()
            LaunchedEffect(Unit) {
                DeepLinkHandler.pendingCode.collect { code ->
                    if (code != null) {
                        DeepLinkHandler.consume()
                        navController.navigate(OnlineLobbyRoute(prefillCode = code))
                    }
                }
            }
            NavHost(navController = navController, startDestination = HomeRoute) {
                composable<HomeRoute> {
                    HomeScreen(
                        onPassAndPlay = { navController.navigate(HotseatGameRoute) },
                        onPlayOnline = { navController.navigate(OnlineLobbyRoute()) },
                        onPlayBluetooth = { navController.navigate(BleLobbyRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onOpenProfile = { navController.navigate(ProfileRoute) }
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
                        navController.popBackStack()
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
                composable<RemoteGameRoute> {
                    val session = GameSessionHolder.current
                    if (session == null) {
                        navController.popBackStack()
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
