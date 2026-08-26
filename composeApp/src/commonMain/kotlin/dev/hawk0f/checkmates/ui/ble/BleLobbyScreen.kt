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
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.ble_connecting
import dev.hawk0f.checkmates.resources.ble_default_host_name
import dev.hawk0f.checkmates.resources.ble_discoverable_as
import dev.hawk0f.checkmates.resources.ble_error_title
import dev.hawk0f.checkmates.resources.ble_find_a_host
import dev.hawk0f.checkmates.resources.ble_find_subtitle
import dev.hawk0f.checkmates.resources.ble_host_a_game
import dev.hawk0f.checkmates.resources.ble_host_subtitle
import dev.hawk0f.checkmates.resources.ble_nearby
import dev.hawk0f.checkmates.resources.ble_play_nearby
import dev.hawk0f.checkmates.resources.ble_role
import dev.hawk0f.checkmates.resources.ble_scanning
import dev.hawk0f.checkmates.resources.ble_stop
import dev.hawk0f.checkmates.resources.ble_tap_to_connect
import dev.hawk0f.checkmates.resources.ble_waiting_for_friend
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.common_you
import dev.hawk0f.checkmates.resources.common_your_name
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_close

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

    BleLobbyContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onHost = requestHostPermissions,
        onScan = requestScanPermissions,
        onStopHosting = viewModel::stopHosting,
        onConnect = viewModel::connectTo,
        onDismissError = viewModel::dismissError,
        onClose = {
            viewModel.stopScan()
            viewModel.stopHosting()
            onBack()
        }
    )
}

@Composable
internal fun BleLobbyContent(
    uiState: BleLobbyUiState,
    onNameChange: (String) -> Unit,
    onHost: () -> Unit,
    onScan: () -> Unit,
    onStopHosting: () -> Unit,
    onConnect: (DiscoveredHost) -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.ble_play_nearby), style = MaterialTheme.typography.displaySmall)
            CircleButton(
                onClick = onClose,
                contentDescription = stringResource(Res.string.a11y_close)
            ) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.common_you))
                SoftTextField(
                    value = uiState.playerName,
                    onValueChange = onNameChange,
                    placeholder = stringResource(Res.string.common_your_name),
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
                        text = stringResource(
                            Res.string.ble_discoverable_as,
                            uiState.playerName.ifBlank { stringResource(Res.string.ble_default_host_name) }
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(Res.string.ble_waiting_for_friend),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    PillButton(
                        text = stringResource(Res.string.ble_stop),
                        onClick = onStopHosting,
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
                    Text(stringResource(Res.string.ble_connecting), style = MaterialTheme.typography.titleMedium)
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        SectionLabel(stringResource(Res.string.ble_role))
                        ChoiceCard(
                            title = stringResource(Res.string.ble_host_a_game),
                            subtitle = stringResource(Res.string.ble_host_subtitle),
                            selected = false,
                            onClick = onHost,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ChoiceCard(
                            title = stringResource(Res.string.ble_find_a_host),
                            subtitle = stringResource(Res.string.ble_find_subtitle),
                            selected = false,
                            onClick = onScan,
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
                                text = stringResource(Res.string.ble_scanning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    if (uiState.hosts.isNotEmpty()) {
                        SectionLabel(stringResource(Res.string.ble_nearby))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            items(uiState.hosts, key = { it.id }) { host ->
                                ChoiceCard(
                                    title = host.label,
                                    subtitle = stringResource(Res.string.ble_tap_to_connect),
                                    selected = false,
                                    onClick = { onConnect(host) },
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
            onDismissRequest = onDismissError,
            title = { Text(stringResource(Res.string.ble_error_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = onDismissError, compact = true)
            }
        )
    }
}
