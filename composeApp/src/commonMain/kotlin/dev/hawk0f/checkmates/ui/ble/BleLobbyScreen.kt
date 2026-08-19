package dev.hawk0f.checkmates.ui.ble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.hawk0f.checkmates.ble.rememberBlePermissionRequester

@Composable
fun BleLobbyScreen(
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: BleLobbyViewModel = viewModel { BleLobbyViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val requestHostPermissions = rememberBlePermissionRequester { granted ->
        if (granted) {
            viewModel.startHosting()
        }
    }
    val requestScanPermissions = rememberBlePermissionRequester { granted ->
        if (granted) {
            viewModel.startScan()
        }
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step is BleLobbyStep.GameReady) {
            viewModel.consumeGameReady()
            onGameReady()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Play via Bluetooth", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = uiState.playerName,
            onValueChange = viewModel::onNameChange,
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        when (uiState.step) {
            BleLobbyStep.Hosting -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Discoverable as \"${uiState.playerName.ifBlank { "Host" }}\"")
                    Text("Waiting for a friend to connect…")
                    OutlinedButton(onClick = viewModel::stopHosting) {
                        Text("Stop")
                    }
                }
            }

            BleLobbyStep.Connecting -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Connecting…")
                }
            }

            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = requestHostPermissions, modifier = Modifier.weight(1f)) {
                        Text("Host game")
                    }
                    Button(onClick = requestScanPermissions, modifier = Modifier.weight(1f)) {
                        Text("Find host")
                    }
                }
                if (uiState.step is BleLobbyStep.Scanning) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text("Scanning for hosts…")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.hosts, key = { it.id }) { host ->
                        Card(
                            onClick = { viewModel.connectTo(host) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = host.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = {
            viewModel.stopScan()
            onBack()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }

    (uiState.step as? BleLobbyStep.Failed)?.let { failed ->
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
