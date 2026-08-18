package dev.hawk0f.chess

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.hawk0f.chess.session.GameSessionHolder
import dev.hawk0f.chess.ui.game.GameMode
import dev.hawk0f.chess.ui.game.GameScreen
import dev.hawk0f.chess.ui.home.HomeScreen
import dev.hawk0f.chess.ui.online.OnlineLobbyScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object HotseatGameRoute

@Serializable
data class OnlineLobbyRoute(val prefillCode: String? = null)

@Serializable
object RemoteGameRoute

@Composable
fun App() {
    MaterialTheme {
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
                        onPlayOnline = { navController.navigate(OnlineLobbyRoute()) }
                    )
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
