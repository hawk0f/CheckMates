package dev.hawk0f.checkmates.ui.friends

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.friends_add
import dev.hawk0f.checkmates.resources.friends_add_label
import dev.hawk0f.checkmates.resources.friends_challenge
import dev.hawk0f.checkmates.resources.friends_challenge_sent
import dev.hawk0f.checkmates.resources.friends_empty
import dev.hawk0f.checkmates.resources.friends_recent
import dev.hawk0f.checkmates.resources.friends_remove
import dev.hawk0f.checkmates.resources.friends_sign_in
import dev.hawk0f.checkmates.resources.friends_title
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun FriendsScreen(
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: FriendsViewModel = viewModel { FriendsViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signedIn = AuthManager.token != null

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
            Text(stringResource(Res.string.friends_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(
                    direction = ChevronDirection.LEFT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!signedIn) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.friends_sign_in), style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(Res.string.friends_add_label)) },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = stringResource(Res.string.friends_add),
                    onClick = viewModel::addFriend,
                    enabled = uiState.nameInput.isNotBlank() && !uiState.working,
                    tone = PillTone.ACCENT,
                    compact = true
                )
            }

            uiState.challengeCode?.let { code ->
                Text(
                    text = stringResource(Res.string.friends_challenge_sent, code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(stringResource(Res.string.friends_title))
                    if (uiState.friends.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.friends_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    for (friend in uiState.friends) {
                        FriendRow(
                            friend = friend,
                            working = uiState.working,
                            onChallenge = { viewModel.challenge(friend) },
                            onRemove = { viewModel.removeFriend(friend) }
                        )
                    }
                }

                if (uiState.recentOpponents.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(Res.string.friends_recent))
                        for (opponent in uiState.recentOpponents) {
                            FriendRow(
                                friend = opponent,
                                working = uiState.working,
                                onChallenge = { viewModel.challengeRecent(opponent) },
                                onRemove = null
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.friends_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(
                    text = stringResource(Res.string.common_ok),
                    onClick = viewModel::dismissError,
                    compact = true
                )
            }
        )
    }
}

@Composable
private fun FriendRow(
    friend: FriendSummary,
    working: Boolean,
    onChallenge: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            if (friend.login.isNotBlank()) {
                Text(
                    text = "@${friend.login}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        PillButton(
            text = stringResource(Res.string.friends_challenge),
            onClick = onChallenge,
            enabled = !working && friend.userId >= 0,
            tone = PillTone.SOFT,
            compact = true
        )
        if (onRemove != null) {
            PillButton(
                text = stringResource(Res.string.friends_remove),
                onClick = onRemove,
                enabled = !working,
                tone = PillTone.SOFT,
                compact = true
            )
        }
    }
}
