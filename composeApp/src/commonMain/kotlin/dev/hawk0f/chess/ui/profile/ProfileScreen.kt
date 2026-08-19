package dev.hawk0f.chess.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.chess.platform.formatDate
import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.protocol.GameHistoryItem
import dev.hawk0f.chess.shared.protocol.ProfileResponse

@Composable
fun ProfileScreen(
    onOpenReplay: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val currentProfile = profile
        if (currentProfile == null) {
            AuthContent(uiState, viewModel)
        } else {
            LoggedInContent(currentProfile, uiState, viewModel, onOpenReplay)
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = viewModel::dismissError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AuthContent(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Text("Profile", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Sign in to keep your game history and stats on the server.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    TabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sign in") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Register") })
    }

    OutlinedTextField(
        value = login,
        onValueChange = { login = it.take(20) },
        label = { Text("Login") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it.take(64) },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    if (tab == 1) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it.take(40) },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (uiState.busy) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Button(
            onClick = {
                if (tab == 0) {
                    viewModel.login(login, password)
                } else {
                    viewModel.register(login, password, displayName)
                }
            },
            enabled = login.length >= 3 && password.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (tab == 0) "Sign in" else "Create account")
        }
    }
}

@Composable
private fun ColumnScope.LoggedInContent(
    profile: ProfileResponse,
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onOpenReplay: () -> Unit
) {
    var editingName by remember { mutableStateOf(false) }
    var pickingAvatar by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.clickable { pickingAvatar = true }
        ) {
            AvatarBadge(profile.avatarKind, profile.avatarValue, size = 72.dp, fontSize = 36.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(
                "@${profile.login}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                TextButton(onClick = { editingName = true }) {
                    Text("Edit name")
                }
                TextButton(onClick = viewModel::logout) {
                    Text("Log out")
                }
            }
        }
    }

    StatsRow(uiState.history)

    Text("Game history", style = MaterialTheme.typography.titleMedium)
    if (!uiState.historyLoaded) {
        CircularProgressIndicator()
    } else if (uiState.history.isEmpty()) {
        Text(
            "No games yet. Finish a game and it will show up here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.history, key = { it.id }) { item ->
                HistoryCard(item) {
                    ReplayHolder.current = item
                    onOpenReplay()
                }
            }
        }
    }

    if (editingName) {
        var name by remember { mutableStateOf(profile.displayName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Display name") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    editingName = false
                    viewModel.updateDisplayName(name)
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingName = false }) {
                    Text("Cancel")
                }
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
private fun StatsRow(history: List<GameHistoryItem>) {
    val decided = history.filter { it.myColor != null }
    val wins = decided.count { it.winner != null && it.winner == it.myColor }
    val losses = decided.count { it.winner != null && it.winner != it.myColor }
    val draws = history.count { it.winner == null }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Games", history.size.toString(), Modifier.weight(1f))
        StatCard("Wins", wins.toString(), Modifier.weight(1f))
        StatCard("Draws", draws.toString(), Modifier.weight(1f))
        StatCard("Losses", losses.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HistoryCard(item: GameHistoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${item.whiteName} vs ${item.blackName}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    resultLabel(item),
                    style = MaterialTheme.typography.titleSmall,
                    color = resultColor(item)
                )
            }
            Text(
                "${modeLabel(item.mode)} · ${item.uciHistory.size} moves · ${reasonLabel(item.reason)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatDate(item.finishedAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun resultColor(item: GameHistoryItem) = when {
    item.winner == null || item.myColor == null -> MaterialTheme.colorScheme.onSurfaceVariant
    item.winner == item.myColor -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.error
}

private fun resultLabel(item: GameHistoryItem): String = when {
    item.winner == null -> "Draw"
    item.myColor == null -> if (item.winner == PieceColor.WHITE) "1–0" else "0–1"
    item.winner == item.myColor -> "Won"
    else -> "Lost"
}

private fun modeLabel(mode: String): String = when (mode) {
    "online" -> "Online"
    "ble" -> "Bluetooth"
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
        title = { Text("Choose avatar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Chess pieces", style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pieceAvatarCodes) { code ->
                        Column(
                            modifier = Modifier.clickable { onPick("piece", code) }
                        ) {
                            AvatarBadge("piece", code, size = 40.dp, fontSize = 20.sp)
                        }
                    }
                }
                Text("Emoji", style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emojiAvatarChoices) { emoji ->
                        Column(
                            modifier = Modifier.clickable { onPick("emoji", emoji) }
                        ) {
                            AvatarBadge("emoji", emoji, size = 40.dp, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
