package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.DeepLinkHandler
import dev.hawk0f.checkmates.net.lichess.LichessChallenge
import dev.hawk0f.checkmates.net.lichess.LichessOngoingGame
import dev.hawk0f.checkmates.platform.rememberOpenUrl
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.profile.pieceDrawable
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PeopleIcon
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.PlayIcon
import dev.hawk0f.checkmates.ui.theme.PuzzleIcon
import dev.hawk0f.checkmates.ui.theme.ScreenIcon
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import org.jetbrains.compose.resources.painterResource
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.lichess_accept
import dev.hawk0f.checkmates.resources.lichess_also_playing
import dev.hawk0f.checkmates.resources.lichess_casual
import dev.hawk0f.checkmates.resources.lichess_challenges_you
import dev.hawk0f.checkmates.resources.lichess_decline
import dev.hawk0f.checkmates.resources.lichess_game_id
import dev.hawk0f.checkmates.resources.lichess_log_out
import dev.hawk0f.checkmates.resources.lichess_nav_players
import dev.hawk0f.checkmates.resources.lichess_nav_play
import dev.hawk0f.checkmates.resources.lichess_nav_puzzles
import dev.hawk0f.checkmates.resources.lichess_nav_watch
import dev.hawk0f.checkmates.resources.lichess_new_game
import dev.hawk0f.checkmates.resources.lichess_no_games_running
import dev.hawk0f.checkmates.resources.lichess_no_rated_games
import dev.hawk0f.checkmates.resources.lichess_offline
import dev.hawk0f.checkmates.resources.lichess_opponent_fallback
import dev.hawk0f.checkmates.resources.lichess_rated
import dev.hawk0f.checkmates.resources.lichess_resume
import dev.hawk0f.checkmates.resources.lichess_someone
import dev.hawk0f.checkmates.resources.lichess_stream_live
import dev.hawk0f.checkmates.resources.lichess_their_turn
import dev.hawk0f.checkmates.resources.lichess_title
import dev.hawk0f.checkmates.resources.lichess_tournaments
import dev.hawk0f.checkmates.resources.lichess_tournaments_subtitle
import dev.hawk0f.checkmates.resources.lichess_unrated
import dev.hawk0f.checkmates.resources.lichess_vs_opponent
import dev.hawk0f.checkmates.resources.lichess_waiting_suffix
import dev.hawk0f.checkmates.resources.lichess_your_move_suffix
import dev.hawk0f.checkmates.resources.lichess_your_turn
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_settings
import dev.hawk0f.checkmates.resources.a11y_switch_service

@Composable
fun LichessHomeScreen(
    onGameReady: () -> Unit,
    onOpenSeek: () -> Unit,
    onOpenPuzzle: () -> Unit,
    onOpenWatch: () -> Unit,
    onOpenArenas: () -> Unit,
    onOpenExplorer: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchFlow: () -> Unit,
    viewModel: LichessHomeViewModel = viewModel { LichessHomeViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingAuth by DeepLinkHandler.pendingLichessAuth.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()
    val accents = LocalAppAccents.current
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(pendingAuth) {
        val callback = pendingAuth ?: return@LaunchedEffect
        DeepLinkHandler.consumeLichessAuth()
        viewModel.onAuthCallback(callback.code, callback.state, callback.error)
    }

    LaunchedEffect(uiState.gameReady) {
        if (uiState.gameReady) {
            viewModel.consumeGameReady()
            onGameReady()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.username == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 20.dp, top = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(stringResource(Res.string.lichess_title), style = MaterialTheme.typography.displaySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleButton(
                        onClick = onOpenSettings,
                        contentDescription = stringResource(Res.string.a11y_settings)
                    ) {
                        Text(text = "\u2699", color = scheme.onSurfaceVariant)
                    }
                    CircleButton(
                        onClick = onSwitchFlow,
                        contentDescription = stringResource(Res.string.a11y_switch_service)
                    ) {
                        CloseIcon(color = scheme.onSurfaceVariant)
                    }
                }
            }
            LichessConnectContent(
                onSignIn = { openUrl(viewModel.startLogin()) },
                onOpenPuzzle = onOpenPuzzle,
                onOpenWatch = onOpenWatch,
                modifier = Modifier.weight(1f).padding(top = 12.dp)
            )
        } else {
            val live = uiState.ongoing.firstOrNull { it.isMyTurn } ?: uiState.ongoing.firstOrNull()
            Column(
                modifier = Modifier.fillMaxWidth().background(accents.band).padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountRow(
                    username = uiState.username.orEmpty(),
                    ratings = uiState.ratings,
                    streaming = uiState.streaming,
                    onOpenSettings = onOpenSettings,
                    onSwitchFlow = onSwitchFlow
                )
                if (live != null) {
                    LiveGameBlock(game = live, onResume = { viewModel.openGame(live.gameId) })
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp)
                    .padding(top = 14.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton(
                        text = stringResource(Res.string.lichess_new_game),
                        onClick = onOpenSeek,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(scheme.primaryContainer)
                            .clickable(onClick = onOpenExplorer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(pieceDrawable("wn")),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                for (challenge in uiState.incoming) {
                    ChallengeCard(
                        challenge = challenge,
                        onAccept = { viewModel.acceptChallenge(challenge.id) },
                        onDecline = { viewModel.declineChallenge(challenge.id) }
                    )
                }

                val alsoPlaying = uiState.ongoing.filter { it.gameId != live?.gameId }
                if (alsoPlaying.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionLabel(stringResource(Res.string.lichess_also_playing, alsoPlaying.size))
                        for (game in alsoPlaying) {
                            ListRow(
                                title = (
                                    game.opponent?.label
                                        ?: stringResource(Res.string.lichess_opponent_fallback)
                                    ) +
                                    (game.opponent?.rating?.let { " · $it" } ?: ""),
                                subtitle = gameSubtitle(game),
                                leading = {
                                    InitialsBadge(
                                        text = game.opponent?.label ?: "?",
                                        size = 34.dp,
                                        container = if (game.isMyTurn) accents.band else scheme.surfaceVariant,
                                        contentColor = if (game.isMyTurn) accents.onBand else scheme.onSurfaceVariant
                                    )
                                },
                                trailing = {
                                    Text(
                                        text = formatLeft(game.secondsLeft),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (game.isMyTurn) scheme.primary else scheme.outline
                                    )
                                },
                                onClick = { viewModel.openGame(game.gameId) }
                            )
                        }
                    }
                }

                if (uiState.ongoing.isEmpty() && uiState.incoming.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.lichess_no_games_running),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }

                Hairline()

                ListRow(
                    title = stringResource(Res.string.lichess_tournaments),
                    subtitle = stringResource(Res.string.lichess_tournaments_subtitle),
                    leading = {
                        InitialsBadge(
                            text = "TR",
                            size = 34.dp,
                            container = accents.band,
                            contentColor = accents.onBand
                        )
                    },
                    trailing = {
                        ChevronIcon(direction = ChevronDirection.RIGHT, color = scheme.outline, size = 16.dp)
                    },
                    onClick = onOpenArenas
                )

                PillButton(
                    text = stringResource(Res.string.lichess_log_out),
                    onClick = viewModel::logout,
                    tone = PillTone.SOFT,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            BottomNav(
                onPuzzles = onOpenPuzzle,
                onWatch = onOpenWatch,
                onPlayers = onOpenPlayers
            )
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.lichess_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun AccountRow(
    username: String,
    ratings: List<String>,
    streaming: Boolean,
    onOpenSettings: () -> Unit,
    onSwitchFlow: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsBadge(
                text = username,
                size = 44.dp,
                container = accents.positive,
                contentColor = accents.pageAlt
            )
            Column {
                Text(text = username, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = ratings.joinToString(" · ")
                        .ifEmpty { stringResource(Res.string.lichess_no_rated_games) },
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.positive
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accents.pageAlt)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (streaming) accents.bandStrong else scheme.outline)
                )
                Text(
                    text = stringResource(if (streaming) Res.string.lichess_stream_live else Res.string.lichess_offline),
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.positive
                )
            }
            CircleButton(
                onClick = onOpenSettings,
                size = 36.dp,
                container = accents.pageAlt,
                contentDescription = stringResource(Res.string.a11y_settings)
            ) {
                Text(text = "\u2699", color = accents.positive)
            }
            CircleButton(
                onClick = onSwitchFlow,
                size = 36.dp,
                container = accents.pageAlt,
                contentDescription = stringResource(Res.string.a11y_switch_service)
            ) {
                CloseIcon(color = accents.positive, size = 15.dp)
            }
        }
    }
}

@Composable
private fun LiveGameBlock(game: LichessOngoingGame, onResume: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val boardState = remember(game.fen) {
        val chess = ChessGame()
        game.fen?.let { chess.loadFen(it) }
        chess.state()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel(
                    text = listOfNotNull(
                        game.speed?.replaceFirstChar { it.uppercase() },
                        game.variant?.name?.takeIf { it != "Standard" },
                        stringResource(if (game.rated) Res.string.lichess_rated else Res.string.lichess_casual)
                    ).joinToString(" · "),
                    color = accents.positive
                )
                Text(
                    text = stringResource(
                        Res.string.lichess_vs_opponent,
                        game.opponent?.label ?: stringResource(Res.string.lichess_opponent_fallback)
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            CodeChip(stringResource(Res.string.lichess_game_id, game.gameId))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp)) {
            BoardBox(modifier = Modifier.fillMaxWidth(), maxSize = 340.dp) { boardModifier ->
                ChessBoard(
                    gameState = boardState,
                    selected = null,
                    legalTargets = emptySet(),
                    flipped = game.color == "black",
                    onSquareTap = {},
                    interactive = false,
                    showCoordinates = false,
                    modifier = boardModifier.clip(RoundedCornerShape(24.dp))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatLeft(game.secondsLeft) +
                    stringResource(
                        if (game.isMyTurn) {
                            Res.string.lichess_your_move_suffix
                        } else {
                            Res.string.lichess_waiting_suffix
                        }
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = accents.onBand
            )
            PillButton(
                text = stringResource(Res.string.lichess_resume),
                onClick = onResume,
                tone = PillTone.INK,
                compact = true,
                trailing = { PlayIcon(color = scheme.inverseOnSurface, size = 14.dp) }
            )
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: LichessChallenge,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.primaryContainer)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsBadge(
            text = challenge.challenger?.label ?: "?",
            size = 36.dp,
            container = scheme.primary,
            contentColor = scheme.onPrimary
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = stringResource(
                    Res.string.lichess_challenges_you,
                    challenge.challenger?.label ?: stringResource(Res.string.lichess_someone)
                ),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onPrimaryContainer
            )
            Text(
                text = listOfNotNull(
                    challenge.speed?.replaceFirstChar { it.uppercase() },
                    challenge.timeControl?.label,
                    stringResource(if (challenge.rated) Res.string.lichess_rated else Res.string.lichess_casual)
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PillButton(stringResource(Res.string.lichess_accept), onAccept, tone = PillTone.INK, compact = true)
            PillButton(stringResource(Res.string.lichess_decline), onDecline, tone = PillTone.SOFT, compact = true)
        }
    }
}

@Composable
private fun BottomNav(
    onPuzzles: () -> Unit,
    onWatch: () -> Unit,
    onPlayers: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Hairline()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavItem(
                label = stringResource(Res.string.lichess_nav_play),
                selected = true,
                onClick = {},
                modifier = Modifier.weight(1f)
            ) { color -> PlayIcon(color = color, size = 21.dp) }
            NavItem(
                label = stringResource(Res.string.lichess_nav_puzzles),
                selected = false,
                onClick = onPuzzles,
                modifier = Modifier.weight(1f)
            ) { color -> PuzzleIcon(color = color, size = 21.dp) }
            NavItem(
                label = stringResource(Res.string.lichess_nav_watch),
                selected = false,
                onClick = onWatch,
                modifier = Modifier.weight(1f)
            ) { color -> ScreenIcon(color = color, size = 21.dp) }
            NavItem(
                label = stringResource(Res.string.lichess_nav_players),
                selected = false,
                onClick = onPlayers,
                modifier = Modifier.weight(1f)
            ) { color -> PeopleIcon(color = color, size = 21.dp) }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val color = if (selected) scheme.onPrimaryContainer else scheme.outline
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !selected, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon(color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun gameSubtitle(game: LichessOngoingGame): String {
    val speed = game.speed?.replaceFirstChar { it.uppercase() }.orEmpty()
    val turn = stringResource(if (game.isMyTurn) Res.string.lichess_your_turn else Res.string.lichess_their_turn)
    val stake = stringResource(if (game.rated) Res.string.lichess_rated else Res.string.lichess_unrated)
    return listOf(speed, stake, turn).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatLeft(seconds: Long?): String {
    if (seconds == null) {
        return "—"
    }
    if (seconds >= 86_400) {
        return "${seconds / 86_400}d"
    }
    if (seconds >= 3_600) {
        return "${seconds / 3_600}h"
    }
    val minutes = seconds / 60
    val rest = seconds % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}
