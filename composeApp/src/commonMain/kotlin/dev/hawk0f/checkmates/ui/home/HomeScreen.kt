package dev.hawk0f.checkmates.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.profile.AvatarBadge
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel

@Composable
fun HomeScreen(
    onPassAndPlay: () -> Unit = {},
    onPlayOnline: () -> Unit = {},
    onPlayBluetooth: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onResumeGame: () -> Unit = {},
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val profile by AuthManager.profile.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    var showAbout by remember { mutableStateOf(false) }
    val liveSession = GameSessionHolder.current

    Column(modifier = Modifier.fillMaxSize()) {
        HeroCard(
            profile = profile,
            opponentName = liveSession?.opponentName?.value,
            hasLiveGame = liveSession != null,
            onOpenProfile = onOpenProfile,
            onPrimaryAction = if (liveSession != null) onResumeGame else onPassAndPlay
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeRail(
                onPlayOnline = onPlayOnline,
                onPassAndPlay = onPassAndPlay,
                onPlayBluetooth = onPlayBluetooth,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings,
                onAbout = { showAbout = true }
            )
            Hairline()
            if (profile == null) {
                SignInRow(onOpenProfile)
            } else {
                RecentSection(recent, onOpenProfile)
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun HeroCard(
    profile: ProfileResponse?,
    opponentName: String?,
    hasLiveGame: Boolean,
    onOpenProfile: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val accents = LocalAppAccents.current
    val scheme = MaterialTheme.colorScheme
    val previewState = remember { ChessGame().state() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accents.band)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable(onClick = onOpenProfile)) {
                if (profile != null) {
                    AvatarBadge(profile.avatarKind, profile.avatarValue, size = 44.dp, fontSize = 20.sp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accents.onBand),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♞",
                            fontSize = 22.sp,
                            color = accents.band
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accents.pageAlt)
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Text(
                    text = if (hasLiveGame) "Your move" else "Ready to play",
                    style = MaterialTheme.typography.labelMedium,
                    color = accents.onBand
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SectionLabel(
                text = if (hasLiveGame) "Online · in progress" else "Pass & Play",
                color = accents.onBand
            )
            Text(
                text = if (hasLiveGame) "vs. ${opponentName ?: "Opponent"}" else "Two players, one device",
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onBackground
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            ChessBoard(
                gameState = previewState,
                selected = null,
                legalTargets = emptySet(),
                flipped = false,
                onSquareTap = {},
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                showCoordinates = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasLiveGame) "Tap to keep playing" else "White moves first",
                style = MaterialTheme.typography.titleMedium,
                color = accents.onBand
            )
            PillButton(
                text = if (hasLiveGame) "Resume" else "Start",
                onClick = onPrimaryAction,
                tone = PillTone.INK,
                compact = true
            )
        }
    }
}

@Composable
private fun ModeRail(
    onPlayOnline: () -> Unit,
    onPassAndPlay: () -> Unit,
    onPlayBluetooth: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PillButton(
                text = "New game",
                onClick = onPlayOnline,
                tone = PillTone.ACCENT,
                compact = true
            )
            PillButton(
                text = "Pass & Play",
                onClick = onPassAndPlay,
                tone = PillTone.SOFT,
                compact = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PillButton(
                text = "Nearby",
                onClick = onPlayBluetooth,
                tone = PillTone.SOFT,
                compact = true
            )
            PillButton(
                text = "Profile",
                onClick = onOpenProfile,
                tone = PillTone.SOFT,
                compact = true
            )
            PillButton(
                text = "Settings",
                onClick = onOpenSettings,
                tone = PillTone.SOFT,
                compact = true
            )
        }
        PillButton(
            text = "About",
            onClick = onAbout,
            tone = PillTone.SOFT,
            compact = true
        )
    }
}

@Composable
private fun SignInRow(onOpenProfile: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Your account")
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Sign in", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Keep your games and stats",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = scheme.outline)
        }
    }
}

@Composable
private fun RecentSection(recent: List<GameHistoryItem>, onOpenProfile: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Recent games")
        if (recent.isEmpty()) {
            Text(
                "No finished games yet — your first result lands here.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        } else {
            for (item in recent) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val won = item.winner != null && item.winner == item.myColor
                    val drawn = item.winner == null
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    drawn -> scheme.surfaceVariant
                                    won -> accents.bandStrong
                                    else -> scheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (drawn) "D" else if (won) "W" else "L",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (drawn) scheme.onSurfaceVariant else scheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opponentLabel(item),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${item.uciHistory.size} moves · ${modeLabel(item.mode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = scheme.outline)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            PillButton(
                text = "All games",
                onClick = onOpenProfile,
                tone = PillTone.SOFT,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun opponentLabel(item: GameHistoryItem): String = when (item.myColor) {
    PieceColor.WHITE -> item.blackName
    PieceColor.BLACK -> item.whiteName
    null -> "${item.whiteName} — ${item.blackName}"
}

private fun modeLabel(mode: String): String = when (mode) {
    "online" -> "Online"
    "ble" -> "Nearby"
    "hotseat" -> "Pass & Play"
    else -> mode
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "CheckMates — chess for two players, online or nearby.\n\n" +
                    "Chess pieces: \"cburnett\" set by Colin M.L. Burnett, " +
                    "CC BY-SA 3.0, via lichess.org.\n\n" +
                    "Chess rules engine: kchesslib (Apache-2.0).\n\n" +
                    "Type: Caprasimo and Figtree, SIL Open Font License."
            )
        },
        confirmButton = {
            PillButton(text = "OK", onClick = onDismiss, compact = true)
        }
    )
}
