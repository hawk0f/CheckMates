package dev.hawk0f.chess.ui.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnlineLobbyScreen(
    prefillCode: String?,
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnlineLobbyViewModel = viewModel { OnlineLobbyViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(if (prefillCode != null) 1 else 0) }

    LaunchedEffect(prefillCode) {
        if (prefillCode != null) {
            viewModel.onCodeChange(prefillCode)
        }
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step is LobbyStep.GameReady) {
            viewModel.consumeGameReady()
            onGameReady()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Play Online", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = uiState.playerName,
            onValueChange = viewModel::onNameChange,
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Create") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Join") })
        }

        when (val step = uiState.step) {
            is LobbyStep.WaitingForOpponent -> WaitingContent(step, onCancel = {
                viewModel.cancelWaiting()
            })

            LobbyStep.Working -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }

            else -> if (tab == 0) {
                Button(onClick = viewModel::createGame, modifier = Modifier.fillMaxWidth()) {
                    Text("Create game")
                }
            } else {
                OutlinedTextField(
                    value = uiState.codeInput,
                    onValueChange = viewModel::onCodeChange,
                    label = { Text("Game code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.joinGame() },
                    enabled = uiState.codeInput.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Join game")
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }

    (uiState.step as? LobbyStep.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Error") },
            text = { Text(failed.message) },
            confirmButton = {
                Button(onClick = viewModel::dismissError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun WaitingContent(step: LobbyStep.WaitingForOpponent, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Game code", style = MaterialTheme.typography.titleMedium)
        Text(step.shortCode, style = MaterialTheme.typography.displayMedium)
        Text(step.joinUrl, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
        }
        Text("Waiting for opponent…")
        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
