package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.DeepLinkHandler
import dev.hawk0f.checkmates.net.lichess.LichessChallenge
import dev.hawk0f.checkmates.net.lichess.LichessOngoingGame
import dev.hawk0f.checkmates.platform.rememberOpenUrl
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.PlayIcon
import dev.hawk0f.checkmates.ui.theme.SectionLabel

@Composable
fun LichessHomeScreen(
    onGameReady: () -> Unit,
    onOpenSeek: () -> Unit,
    onOpenPuzzle: () -> Unit,
    onOpenWatch: () -> Unit,
    onOpenArenas: () -> Unit,
    onOpenExplorer: () -> Unit,
    onOpenPlayers: () -> Unit,
    onBack: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("Lichess", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        if (uiState.username == null) {
            LichessConnectContent(
                onSignIn = { openUrl(viewModel.startLogin()) },
                onOpenPuzzle = onOpenPuzzle,
                onOpenWatch = onOpenWatch,
                modifier = Modifier.weight(1f).padding(top = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AccountRow(
                    username = uiState.username.orEmpty(),
                    ratings = uiState.ratings,
                    streaming = uiState.streaming
                )

                val live = uiState.ongoing.firstOrNull { it.isMyTurn } ?: uiState.ongoing.firstOrNull()
                if (live != null) {
                    LiveGameCard(game = live, onResume = { viewModel.openGame(live.gameId) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    PillButton("Seek", onOpenSeek, tone = PillTone.ACCENT, compact = true)
                    PillButton("Puzzle", onOpenPuzzle, tone = PillTone.SOFT, compact = true)
                    PillButton("Watch", onOpenWatch, tone = PillTone.SOFT, compact = true)
                    PillButton("Arenas", onOpenArenas, tone = PillTone.SOFT, compact = true)
                    PillButton("Explorer", onOpenExplorer, tone = PillTone.SOFT, compact = true)
                    PillButton("Players", onOpenPlayers, tone = PillTone.SOFT, compact = true)
                }

                Hairline()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("Playing · ${uiState.ongoing.size}", color = accents.bandStrong)
                    SectionLabel(
                        text = "${uiState.incoming.size} challenges in",
                        color = accents.bandStrong
                    )
                }

                for (challenge in uiState.incoming) {
                    ChallengeRow(
                        challenge = challenge,
                        onAccept = { viewModel.acceptChallenge(challenge.id) },
                        onDecline = { viewModel.declineChallenge(challenge.id) }
                    )
                }

                for (game in uiState.ongoing) {
                    ListRow(
                        title = "${game.opponent?.label ?: "Opponent"}" +
                            (game.opponent?.rating?.let { " · $it" } ?: ""),
                        subtitle = gameSubtitle(game),
                        leading = {
                            InitialsBadge(
                                text = game.opponent?.label ?: "?",
                                container = if (game.isMyTurn) accents.band else scheme.surfaceVariant,
                                contentColor = if (game.isMyTurn) accents.onBand else scheme.onSurfaceVariant
                            )
                        },
                        trailing = {
                            Text(
                                text = formatLeft(game.secondsLeft),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (game.isMyTurn) scheme.primary else scheme.onSurfaceVariant
                            )
                        },
                        onClick = { viewModel.openGame(game.gameId) }
                    )
                }

                if (uiState.ongoing.isEmpty() && uiState.incoming.isEmpty()) {
                    Text(
                        text = "No games running. Create a seek or challenge a friend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }

                PillButton(
                    text = "Log out of Lichess",
                    onClick = viewModel::logout,
                    tone = PillTone.SOFT,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Lichess", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun AccountRow(username: String, ratings: List<String>, streaming: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsBadge(
                text = username,
                container = accents.band,
                contentColor = accents.onBand
            )
            Column {
                Text(text = username, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = ratings.joinToString(" · ").ifEmpty { "no rated games yet" },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(scheme.surfaceVariant)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (streaming) accents.positive else scheme.outline)
            )
            Text(
                text = if (streaming) "Stream live" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LiveGameCard(game: LichessOngoingGame, onResume: () -> Unit) {
    val accents = LocalAppAccents.current
    val boardState = remember(game.fen) {
        val chess = ChessGame()
        game.fen?.let { chess.loadFen(it) }
        chess.state()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(accents.band)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SectionLabel(
                    text = "${game.speed.orEmpty()} ${game.variant?.name.orEmpty()} · " +
                        if (game.rated) "rated" else "casual",
                    color = accents.bandStrong
                )
                Text(
                    text = "vs. ${game.opponent?.label ?: "Opponent"}",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            CodeChip("gameId ${game.gameId}")
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(18.dp))) {
            ChessBoard(
                gameState = boardState,
                selected = null,
                legalTargets = emptySet(),
                flipped = game.color == "black",
                onSquareTap = {},
                showCoordinates = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatLeft(game.secondsLeft) +
                    if (game.isMyTurn) " · your move" else " · waiting",
                style = MaterialTheme.typography.titleSmall
            )
            PillButton(
                text = "Resume",
                onClick = onResume,
                tone = PillTone.INK,
                compact = true,
                trailing = { PlayIcon(color = MaterialTheme.colorScheme.inverseOnSurface, size = 14.dp) }
            )
        }
    }
}

@Composable
private fun ChallengeRow(
    challenge: LichessChallenge,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ListRow(
        title = challenge.challenger?.label ?: "Someone",
        subtitle = "${challenge.timeControl?.label.orEmpty()} · " +
            if (challenge.rated) "rated" else "casual",
        leading = { InitialsBadge(text = challenge.challenger?.label ?: "?") },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PillButton("Accept", onAccept, tone = PillTone.ACCENT, compact = true)
                PillButton("No", onDecline, tone = PillTone.SOFT, compact = true)
            }
        },
        modifier = Modifier.background(scheme.surfaceContainer, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp)
    )
}

private fun gameSubtitle(game: LichessOngoingGame): String {
    val speed = game.speed?.replaceFirstChar { it.uppercase() }.orEmpty()
    val turn = if (game.isMyTurn) "your turn" else "their turn"
    val stake = if (game.rated) "rated" else "unrated"
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
