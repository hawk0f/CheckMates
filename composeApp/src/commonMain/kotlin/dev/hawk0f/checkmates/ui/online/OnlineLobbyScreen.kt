package dev.hawk0f.checkmates.ui.online

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.platform.QrScannerView
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.ui.theme.ChoiceCard
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.SoftTextField
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

private val clockChoices = listOf(
    null,
    TimeControl(180, 0),
    TimeControl(180, 2),
    TimeControl(300, 0),
    TimeControl(600, 0),
    TimeControl(900, 10)
)

@Composable
fun OnlineLobbyScreen(
    prefillCode: String?,
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnlineLobbyViewModel = viewModel { OnlineLobbyViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var joining by remember { mutableStateOf(prefillCode != null) }
    var scanning by remember { mutableStateOf(false) }

    if (scanning) {
        Box(modifier = Modifier.fillMaxSize()) {
            QrScannerView(
                onResult = { text ->
                    scanning = false
                    viewModel.joinGame(text)
                },
                onPermissionDenied = { scanning = false }
            )
            PillButton(
                text = "Cancel",
                onClick = { scanning = false },
                tone = PillTone.INK,
                modifier = Modifier.align(Alignment.BottomCenter).padding(28.dp)
            )
        }
        return
    }

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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("New game", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack) {
                CloseIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val step = uiState.step
        when {
            step is LobbyStep.WaitingForOpponent -> WaitingContent(
                step = step,
                onCancel = viewModel::cancelWaiting,
                modifier = Modifier.weight(1f)
            )

            step == LobbyStep.Working -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> SetupContent(
                uiState = uiState,
                viewModel = viewModel,
                joining = joining,
                onJoiningChange = { joining = it },
                onScan = { scanning = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    (uiState.step as? LobbyStep.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Could not start", style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun SetupContent(
    uiState: OnlineLobbyUiState,
    viewModel: OnlineLobbyViewModel,
    joining: Boolean,
    onJoiningChange: (Boolean) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel("Opponent")
                ChoiceCard(
                    title = "Invite a friend",
                    subtitle = "Share a link, code or QR",
                    selected = !joining,
                    onClick = { onJoiningChange(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                ChoiceCard(
                    title = "Join with a code",
                    subtitle = "6 characters, or scan a QR",
                    selected = joining,
                    onClick = { onJoiningChange(true) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel("You")
                SoftTextField(
                    value = uiState.playerName,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Your name",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (joining) {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    SectionLabel("Game code")
                    SoftTextField(
                        value = uiState.codeInput,
                        onValueChange = viewModel::onCodeChange,
                        placeholder = "ABC234",
                        modifier = Modifier.fillMaxWidth()
                    )
                    PillButton(
                        text = "Scan QR",
                        onClick = onScan,
                        tone = PillTone.SOFT,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    SectionLabel("Clock")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        for (choice in clockChoices) {
                            SelectPill(
                                text = choice?.label ?: "None",
                                selected = uiState.timeControl == choice,
                                onClick = { viewModel.onTimeControlChange(choice) }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillButton(
                text = if (joining) "Join game" else "Create game",
                onClick = { if (joining) viewModel.joinGame() else viewModel.createGame() },
                enabled = !joining || uiState.codeInput.length == 6,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (joining) {
                    "Colours are drawn at random when you join"
                } else {
                    "Your friend joins with the code or the link"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WaitingContent(
    step: LobbyStep.WaitingForOpponent,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val shareText = rememberShareText()

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        SectionLabel("Game code")
        Text(step.shortCode, style = MaterialTheme.typography.displayLarge)
        SoftCard(container = accents.pageAlt, corner = 24.dp) {
            Image(
                painter = rememberQrCodePainter(step.joinUrl),
                contentDescription = "QR code for ${step.joinUrl}",
                modifier = Modifier.padding(14.dp).size(190.dp)
            )
        }
        Text(
            text = step.joinUrl,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        PillButton(
            text = "Share invite",
            onClick = {
                shareText("Play chess with me! ${step.joinUrl} (code ${step.shortCode})")
            },
            tone = PillTone.ACCENT
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.primary)
            )
            Text(
                text = "Waiting for opponent…",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        PillButton(
            text = "Cancel",
            onClick = onCancel,
            tone = PillTone.SOFT,
            compact = true
        )
    }
}
