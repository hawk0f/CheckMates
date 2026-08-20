package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.net.lichess.LICHESS_BASE_URL
import dev.hawk0f.checkmates.platform.rememberOpenUrl
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel

@Composable
fun LichessPlayersScreen(
    onBack: () -> Unit,
    viewModel: LichessPlayersViewModel = viewModel { LichessPlayersViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val openUrl = rememberOpenUrl()
    val shareText = rememberShareText()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("Players", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        if (uiState.loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = scheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.online.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(
                            text = "Following · online ${uiState.online.size}",
                            color = accents.bandStrong
                        )
                        for (user in uiState.online) {
                            ListRow(
                                title = user.label,
                                subtitle = if (user.playing == true) "playing" else "online",
                                leading = { InitialsBadge(text = user.label) },
                                trailing = {
                                    val gameId = user.playingId
                                    if (gameId != null) {
                                        PillButton(
                                            text = "Watch",
                                            onClick = { openUrl("$LICHESS_BASE_URL/$gameId") },
                                            tone = PillTone.SOFT,
                                            compact = true
                                        )
                                    } else {
                                        PillButton(
                                            text = "Challenge",
                                            onClick = { viewModel.challenge(user.label) },
                                            tone = PillTone.ACCENT,
                                            compact = true
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                if (uiState.offline.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel("Offline · ${uiState.offline.size}", color = accents.bandStrong)
                        for (user in uiState.offline.take(8)) {
                            ListRow(
                                title = user.label,
                                subtitle = "not online now",
                                leading = { InitialsBadge(text = user.label) }
                            )
                            Hairline()
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Leaderboard · rapid", color = accents.bandStrong)
                    for (user in uiState.leaderboard) {
                        ListRow(
                            title = user.username,
                            subtitle = user.title,
                            leading = { InitialsBadge(text = user.username) },
                            trailing = {
                                Text(
                                    text = user.perfs["rapid"]?.rating?.toString() ?: "—",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Open challenge link", color = accents.bandStrong)
                    uiState.openChallengeUrl?.let { url ->
                        CodeChip(url)
                        PillButton(
                            text = "Share link",
                            onClick = { shareText("Play me on lichess: $url") },
                            tone = PillTone.ACCENT,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    PillButton(
                        text = "Create 5+0 link",
                        onClick = viewModel::createOpenChallenge,
                        tone = PillTone.SOFT,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    uiState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text("Players", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissMessage, compact = true)
            }
        )
    }
}
