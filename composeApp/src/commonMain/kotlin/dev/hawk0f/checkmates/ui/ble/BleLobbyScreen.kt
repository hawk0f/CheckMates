package dev.hawk0f.checkmates.ui.ble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import dev.hawk0f.checkmates.ui.theme.ChoiceCard
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SoftTextField

@Composable
fun BleLobbyScreen(
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: BleLobbyViewModel = viewModel { BleLobbyViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("Play nearby", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = {
                viewModel.stopScan()
                onBack()
            }) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel("You")
                SoftTextField(
                    value = uiState.playerName,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Your name",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (uiState.step) {
                BleLobbyStep.Hosting -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = scheme.primary)
                    Text(
                        text = "Discoverable as \"${uiState.playerName.ifBlank { "Host" }}\"",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Waiting for a friend to connect over Bluetooth",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    PillButton(
                        text = "Stop",
                        onClick = viewModel::stopHosting,
                        tone = PillTone.SOFT,
                        compact = true
                    )
                }

                BleLobbyStep.Connecting -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = scheme.primary)
                    Text("Connecting…", style = MaterialTheme.typography.titleMedium)
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        SectionLabel("Role")
                        ChoiceCard(
                            title = "Host a game",
                            subtitle = "Your device becomes discoverable",
                            selected = false,
                            onClick = requestHostPermissions,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ChoiceCard(
                            title = "Find a host",
                            subtitle = "Scan for a nearby device",
                            selected = false,
                            onClick = requestScanPermissions,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (uiState.step is BleLobbyStep.Scanning) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = scheme.primary,
                                modifier = Modifier.padding(2.dp)
                            )
                            Text(
                                text = "Scanning for hosts…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    if (uiState.hosts.isNotEmpty()) {
                        SectionLabel("Nearby")
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            items(uiState.hosts, key = { it.id }) { host ->
                                ChoiceCard(
                                    title = host.label,
                                    subtitle = "Tap to connect",
                                    selected = false,
                                    onClick = { viewModel.connectTo(host) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    (uiState.step as? BleLobbyStep.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Bluetooth error", style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}
