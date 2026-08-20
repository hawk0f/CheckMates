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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.DeepLinkHandler
import dev.hawk0f.checkmates.platform.rememberOpenUrl
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.SoftTextField

@Composable
fun LichessLobbyScreen(
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: LichessLobbyViewModel = viewModel { LichessLobbyViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingAuth by DeepLinkHandler.pendingLichessAuth.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()

    LaunchedEffect(pendingAuth) {
        val callback = pendingAuth ?: return@LaunchedEffect
        DeepLinkHandler.consumeLichessAuth()
        viewModel.onAuthCallback(callback.code, callback.state, callback.error)
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step is LichessStep.GameReady) {
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
            Column {
                Text("Lichess", style = MaterialTheme.typography.displaySmall)
                uiState.username?.let {
                    SectionLabel(it, color = LocalAppAccents.current.bandStrong)
                }
            }
            CircleButton(onClick = onBack) {
                CloseIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val step = uiState.step
        when {
            step is LichessStep.Seeking -> WaitingPanel(
                title = "Looking for a ${step.label} game",
                note = "Lichess pairs you with a player of similar rating",
                onCancel = viewModel::cancelWaiting,
                modifier = Modifier.weight(1f)
            )

            step is LichessStep.Waiting -> WaitingPanel(
                title = step.label,
                note = "The game opens as soon as it starts",
                onCancel = viewModel::cancelWaiting,
                modifier = Modifier.weight(1f)
            )

            step is LichessStep.Authorizing -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            uiState.username == null -> SignInContent(
                onSignIn = { openUrl(viewModel.startLogin()) },
                modifier = Modifier.weight(1f)
            )

            else -> PlayContent(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }

    (uiState.step as? LichessStep.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Lichess", style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun SignInContent(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
    ) {
        SoftCard(container = accents.pageAlt, corner = 26.dp) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionLabel("Play the world", color = accents.bandStrong)
                Text(
                    text = "Use your lichess account",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Quick pairing, friend challenges and Stockfish, " +
                        "played on lichess.org with your own rating and history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        PillButton(
            text = "Sign in with Lichess",
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "The browser opens for authorization. Engine help is forbidden there — play yourself.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayContent(
    uiState: LichessLobbyUiState,
    viewModel: LichessLobbyViewModel,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (uiState.incoming.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel("Challenges")
                for (challenge in uiState.incoming) {
                    SoftCard(container = accents.pageAlt, corner = 22.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(challenge.from, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = challenge.timeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                PillButton(
                                    text = "Accept",
                                    onClick = { viewModel.acceptChallenge(challenge.id) },
                                    tone = PillTone.ACCENT,
                                    compact = true
                                )
                                PillButton(
                                    text = "Decline",
                                    onClick = { viewModel.declineChallenge(challenge.id) },
                                    tone = PillTone.SOFT,
                                    compact = true
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.ongoing.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel("Your games")
                for (game in uiState.ongoing) {
                    PillButton(
                        text = "Continue vs ${game.opponent}",
                        onClick = { viewModel.resume(game.gameId) },
                        tone = PillTone.BAND,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionLabel("Quick pairing")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                for (option in QUICK_CLOCKS) {
                    SelectPill(
                        text = option.label,
                        selected = uiState.quickClock == option,
                        onClick = { viewModel.onQuickClockChange(option) }
                    )
                }
            }
            PillButton(
                text = "Find opponent",
                onClick = viewModel::seek,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Rapid and classical only — lichess keeps faster pools for its own apps.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.outline
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionLabel("Clock for friends and Stockfish")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                for (option in FRIEND_CLOCKS) {
                    SelectPill(
                        text = option.label,
                        selected = uiState.friendClock == option,
                        onClick = { viewModel.onFriendClockChange(option) }
                    )
                }
            }
            SoftTextField(
                value = uiState.friendName,
                onValueChange = viewModel::onFriendNameChange,
                placeholder = "Lichess username",
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = "Send challenge",
                onClick = viewModel::challengeFriend,
                tone = PillTone.ACCENT,
                enabled = uiState.friendName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionLabel("Stockfish level")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                for (level in AI_LEVELS) {
                    SelectPill(
                        text = level.toString(),
                        selected = uiState.aiLevel == level,
                        onClick = { viewModel.onAiLevelChange(level) }
                    )
                }
            }
            PillButton(
                text = "Play the computer",
                onClick = viewModel::playComputer,
                tone = PillTone.SOFT,
                modifier = Modifier.fillMaxWidth()
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

@Composable
private fun WaitingPanel(
    title: String,
    note: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(50))
                .background(scheme.primary)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PillButton(text = "Cancel", onClick = onCancel, tone = PillTone.SOFT, compact = true)
    }
}
