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
import dev.hawk0f.checkmates.ui.theme.StatTile
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.common_cancel
import dev.hawk0f.checkmates.resources.mode_nearby
import dev.hawk0f.checkmates.resources.mode_online
import dev.hawk0f.checkmates.resources.mode_pass_and_play
import dev.hawk0f.checkmates.resources.profile_avatar_emoji
import dev.hawk0f.checkmates.resources.profile_avatar_pieces
import dev.hawk0f.checkmates.resources.profile_choose_avatar
import dev.hawk0f.checkmates.resources.profile_create_account
import dev.hawk0f.checkmates.resources.profile_display_name_placeholder
import dev.hawk0f.checkmates.resources.profile_display_name_title
import dev.hawk0f.checkmates.resources.profile_edit_name
import dev.hawk0f.checkmates.resources.profile_error_title
import dev.hawk0f.checkmates.resources.profile_game_summary
import dev.hawk0f.checkmates.resources.profile_handle_and_games
import dev.hawk0f.checkmates.resources.profile_log_out
import dev.hawk0f.checkmates.resources.profile_login_placeholder
import dev.hawk0f.checkmates.resources.profile_no_games
import dev.hawk0f.checkmates.resources.profile_password_placeholder
import dev.hawk0f.checkmates.resources.profile_players
import dev.hawk0f.checkmates.resources.profile_recent_games
import dev.hawk0f.checkmates.resources.profile_register
import dev.hawk0f.checkmates.resources.profile_save
import dev.hawk0f.checkmates.resources.profile_sign_in
import dev.hawk0f.checkmates.resources.profile_sign_in_pitch
import dev.hawk0f.checkmates.resources.profile_stat_drawn
import dev.hawk0f.checkmates.resources.profile_stat_lost
import dev.hawk0f.checkmates.resources.profile_stat_won
import dev.hawk0f.checkmates.resources.profile_title
import dev.hawk0f.checkmates.ui.theme.reasonLabel
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_back

@Composable
fun ProfileScreen(
    onOpenReplay: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProfile = profile

    Column(modifier = Modifier.fillMaxSize()) {
        if (currentProfile == null) {
            AuthContent(uiState, viewModel, onBack)
        } else {
            LoggedInContent(currentProfile, uiState, viewModel, onOpenReplay, onBack)
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.profile_error_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = viewModel::dismissError, compact = true)
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
        CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
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

    ColumnScopeAuthHeader(title = stringResource(Res.string.profile_title), onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = stringResource(Res.string.profile_sign_in_pitch),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SelectPill(
                text = stringResource(Res.string.profile_sign_in),
                selected = !registering,
                onClick = { registering = false }
            )
            SelectPill(
                text = stringResource(Res.string.profile_register),
                selected = registering,
                onClick = { registering = true }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftTextField(
                value = login,
                onValueChange = { login = it.take(20) },
                placeholder = stringResource(Res.string.profile_login_placeholder),
                modifier = Modifier.fillMaxWidth()
            )
            SoftTextField(
                value = password,
                onValueChange = { password = it.take(64) },
                placeholder = stringResource(Res.string.profile_password_placeholder),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (registering) {
                SoftTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(40) },
                    placeholder = stringResource(Res.string.profile_display_name_placeholder),
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
                text = stringResource(
                    if (registering) Res.string.profile_create_account else Res.string.profile_sign_in
                ),
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
                        text = stringResource(
                            Res.string.profile_handle_and_games,
                            profile.login,
                            uiState.history.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
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
            label = stringResource(Res.string.profile_stat_won),
            accent = true,
            modifier = Modifier.weight(1f)
        )
        StatTile(value = draws.toString(), label = stringResource(Res.string.profile_stat_drawn), modifier = Modifier.weight(1f))
        StatTile(value = losses.toString(), label = stringResource(Res.string.profile_stat_lost), modifier = Modifier.weight(1f))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        PillButton(
            text = stringResource(Res.string.profile_edit_name),
            onClick = { editingName = true },
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.profile_log_out),
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
        SectionLabel(stringResource(Res.string.profile_recent_games))
        if (!uiState.historyLoaded) {
            CircularProgressIndicator(color = scheme.primary)
        } else if (uiState.history.isEmpty()) {
            Text(
                text = stringResource(Res.string.profile_no_games),
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
            title = { Text(stringResource(Res.string.profile_display_name_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                SoftTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    placeholder = stringResource(Res.string.profile_display_name_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                PillButton(
                    text = stringResource(Res.string.profile_save),
                    onClick = {
                        editingName = false
                        viewModel.updateDisplayName(name)
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = stringResource(Res.string.common_cancel),
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
                text = if (drawn) {
                    "D"
                } else if (won) {
                    "W"
                } else {
                    "L"
                },
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.profile_players, item.whiteName, item.blackName),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    Res.string.profile_game_summary,
                    modeLabel(item.mode),
                    reasonLabel(item.reason),
                    item.uciHistory.size
                ),
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

@Composable
private fun modeLabel(mode: String): String = when (mode) {
    "online" -> stringResource(Res.string.mode_online)
    "ble" -> stringResource(Res.string.mode_nearby)
    "hotseat" -> stringResource(Res.string.mode_pass_and_play)
    else -> mode
}

@Composable
private fun AvatarPickerDialog(onPick: (String, String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_choose_avatar), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel(stringResource(Res.string.profile_avatar_pieces))
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
                SectionLabel(stringResource(Res.string.profile_avatar_emoji))
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
            PillButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss, tone = PillTone.SOFT, compact = true)
        }
    )
}
