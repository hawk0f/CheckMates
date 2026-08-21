package dev.hawk0f.checkmates.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.platform.formatDate
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftTextField
import dev.hawk0f.checkmates.ui.about.AboutDialog
import dev.hawk0f.checkmates.ui.theme.StatTile

@Composable
fun ProfileScreen(
    onOpenReplay: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProfile = profile
    var showAbout by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (currentProfile == null) {
            AuthContent(uiState, viewModel, onBack)
        } else {
            LoggedInContent(currentProfile, uiState, viewModel, onOpenReplay, onBack)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            PillButton(
                text = "Settings",
                onClick = onOpenSettings,
                tone = PillTone.SOFT,
                compact = true
            )
            PillButton(
                text = "About",
                onClick = { showAbout = true },
                tone = PillTone.SOFT,
                compact = true
            )
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Something went wrong", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun ColumnScopeAuthHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall)
        CircleButton(onClick = onBack) {
            ChevronIcon(
                direction = ChevronDirection.LEFT,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColumnScope.AuthContent(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    var registering by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    ColumnScopeAuthHeader(title = "Your profile", onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Sign in to keep your games, stats and replays on the server.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SelectPill(
                text = "Sign in",
                selected = !registering,
                onClick = { registering = false }
            )
            SelectPill(
                text = "Register",
                selected = registering,
                onClick = { registering = true }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftTextField(
                value = login,
                onValueChange = { login = it.take(20) },
                placeholder = "Login",
                modifier = Modifier.fillMaxWidth()
            )
            SoftTextField(
                value = password,
                onValueChange = { password = it.take(64) },
                placeholder = "Password",
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (registering) {
                SoftTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(40) },
                    placeholder = "Display name",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (uiState.busy) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            PillButton(
                text = if (registering) "Create account" else "Sign in",
                onClick = {
                    if (registering) {
                        viewModel.register(login, password, displayName)
                    } else {
                        viewModel.login(login, password)
                    }
                },
                enabled = login.length >= 3 && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ColumnScope.LoggedInContent(
    profile: ProfileResponse,
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onOpenReplay: () -> Unit,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    var editingName by remember { mutableStateOf(false) }
    var pickingAvatar by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .offset(x = 78.dp, y = (-64).dp)
                .clip(CircleShape)
                .background(accents.band)
        )
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 20.dp, top = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable { pickingAvatar = true }) {
                    AvatarBadge(profile.avatarKind, profile.avatarValue, size = 76.dp, fontSize = 32.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.displayName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "@${profile.login} · ${uiState.history.size} games",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                CircleButton(onClick = onBack) {
                    ChevronIcon(direction = ChevronDirection.LEFT, color = scheme.onSurfaceVariant)
                }
            }
        }
    }

    val decided = uiState.history.filter { it.myColor != null }
    val wins = decided.count { it.winner != null && it.winner == it.myColor }
    val losses = decided.count { it.winner != null && it.winner != it.myColor }
    val draws = uiState.history.count { it.winner == null }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            value = wins.toString(),
            label = "Won",
            accent = true,
            modifier = Modifier.weight(1f)
        )
        StatTile(value = draws.toString(), label = "Drawn", modifier = Modifier.weight(1f))
        StatTile(value = losses.toString(), label = "Lost", modifier = Modifier.weight(1f))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        PillButton(
            text = "Edit name",
            onClick = { editingName = true },
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = "Log out",
            onClick = viewModel::logout,
            tone = PillTone.SOFT,
            compact = true
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(start = 26.dp, end = 26.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Recent games")
        if (!uiState.historyLoaded) {
            CircularProgressIndicator(color = scheme.primary)
        } else if (uiState.history.isEmpty()) {
            Text(
                text = "No games yet. Finish a game and it lands here.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(uiState.history, key = { it.id }) { item ->
                    HistoryRow(item) {
                        ReplayHolder.current = item
                        onOpenReplay()
                    }
                    Hairline()
                }
            }
        }
    }

    if (editingName) {
        var name by remember { mutableStateOf(profile.displayName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Display name", style = MaterialTheme.typography.titleLarge) },
            text = {
                SoftTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    placeholder = "Display name",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                PillButton(
                    text = "Save",
                    onClick = {
                        editingName = false
                        viewModel.updateDisplayName(name)
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = "Cancel",
                    onClick = { editingName = false },
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
        )
    }

    if (pickingAvatar) {
        AvatarPickerDialog(
            onPick = { kind, value ->
                pickingAvatar = false
                viewModel.updateAvatar(kind, value)
            },
            onDismiss = { pickingAvatar = false }
        )
    }
}

@Composable
private fun HistoryRow(item: GameHistoryItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val won = item.winner != null && item.winner == item.myColor
    val drawn = item.winner == null
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    when {
                        drawn -> scheme.outlineVariant
                        won -> accents.bandStrong
                        else -> scheme.primary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (drawn) "D" else if (won) "W" else "L",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.whiteName} — ${item.blackName}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${modeLabel(item.mode)} · ${reasonLabel(item.reason)} · " +
                    "${item.uciHistory.size} moves",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Text(
            text = formatDate(item.finishedAtMillis),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.outline
        )
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    "online" -> "Online"
    "ble" -> "Nearby"
    "hotseat" -> "Pass & Play"
    else -> mode
}

private fun reasonLabel(reason: GameOverReason): String = when (reason) {
    GameOverReason.CHECKMATE -> "Checkmate"
    GameOverReason.STALEMATE -> "Stalemate"
    GameOverReason.DRAW_AGREED -> "Draw agreed"
    GameOverReason.RESIGNATION -> "Resignation"
    GameOverReason.INSUFFICIENT_MATERIAL -> "Insufficient material"
    GameOverReason.REPETITION -> "Repetition"
    GameOverReason.FIFTY_MOVE -> "Fifty-move rule"
    GameOverReason.TIMEOUT -> "Time out"
    GameOverReason.DISCONNECTION -> "Disconnection"
}

@Composable
private fun AvatarPickerDialog(onPick: (String, String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose avatar", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Pieces")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pieceAvatarCodes) { code ->
                        Box(modifier = Modifier.clickable { onPick("piece", code) }) {
                            AvatarBadge("piece", code, size = 40.dp, fontSize = 20.sp)
                        }
                    }
                }
                SectionLabel("Emoji")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emojiAvatarChoices) { emoji ->
                        Box(modifier = Modifier.clickable { onPick("emoji", emoji) }) {
                            AvatarBadge("emoji", emoji, size = 40.dp, fontSize = 20.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            PillButton(text = "Cancel", onClick = onDismiss, tone = PillTone.SOFT, compact = true)
        }
    )
}
