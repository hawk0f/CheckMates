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
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.common_cancel
import dev.hawk0f.checkmates.resources.common_none
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.common_you
import dev.hawk0f.checkmates.resources.common_your_name
import dev.hawk0f.checkmates.resources.lobby_clock
import dev.hawk0f.checkmates.resources.lobby_colours_drawn_at_random
import dev.hawk0f.checkmates.resources.lobby_could_not_start
import dev.hawk0f.checkmates.resources.lobby_create_game
import dev.hawk0f.checkmates.resources.lobby_quick_pair
import dev.hawk0f.checkmates.resources.lobby_searching
import dev.hawk0f.checkmates.resources.lobby_searching_detail
import dev.hawk0f.checkmates.resources.lobby_friend_joins_with_code
import dev.hawk0f.checkmates.resources.lobby_game_code
import dev.hawk0f.checkmates.resources.lobby_invite_a_friend
import dev.hawk0f.checkmates.resources.lobby_invite_subtitle
import dev.hawk0f.checkmates.resources.lobby_join_game
import dev.hawk0f.checkmates.resources.lobby_join_subtitle
import dev.hawk0f.checkmates.resources.lobby_join_with_code
import dev.hawk0f.checkmates.resources.lobby_new_game
import dev.hawk0f.checkmates.resources.lobby_opponent
import dev.hawk0f.checkmates.resources.lobby_qr_content_description
import dev.hawk0f.checkmates.resources.lobby_scan_qr
import dev.hawk0f.checkmates.resources.lobby_share_invite
import dev.hawk0f.checkmates.resources.lobby_share_message
import dev.hawk0f.checkmates.resources.lobby_waiting_for_opponent
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_close

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
                text = stringResource(Res.string.common_cancel),
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

    OnlineLobbyContent(
        uiState = uiState,
        joining = joining,
        onJoiningChange = { joining = it },
        onNameChange = viewModel::onNameChange,
        onCodeChange = viewModel::onCodeChange,
        onTimeControlChange = viewModel::onTimeControlChange,
        onCreateGame = viewModel::createGame,
        onJoinGame = { viewModel.joinGame() },
        onQuickPair = viewModel::quickPair,
        onCancelWaiting = viewModel::cancelWaiting,
        onCancelSearch = viewModel::cancelSearch,
        onDismissError = viewModel::dismissError,
        onScan = { scanning = true },
        onBack = onBack
    )
}

@Composable
internal fun OnlineLobbyContent(
    uiState: OnlineLobbyUiState,
    joining: Boolean,
    onJoiningChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onTimeControlChange: (TimeControl?) -> Unit,
    onCreateGame: () -> Unit,
    onJoinGame: () -> Unit,
    onQuickPair: () -> Unit,
    onCancelWaiting: () -> Unit,
    onCancelSearch: () -> Unit,
    onDismissError: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.lobby_new_game), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_close)) {
                CloseIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val step = uiState.step
        when {
            step is LobbyStep.WaitingForOpponent -> WaitingContent(
                step = step,
                onCancel = onCancelWaiting,
                modifier = Modifier.weight(1f)
            )

            step is LobbyStep.Searching -> SearchingContent(
                step = step,
                onCancel = onCancelSearch,
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
                joining = joining,
                onJoiningChange = onJoiningChange,
                onNameChange = onNameChange,
                onCodeChange = onCodeChange,
                onTimeControlChange = onTimeControlChange,
                onCreateGame = onCreateGame,
                onJoinGame = onJoinGame,
                onQuickPair = onQuickPair,
                onScan = onScan,
                modifier = Modifier.weight(1f)
            )
        }
    }

    (uiState.step as? LobbyStep.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(Res.string.lobby_could_not_start), style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = onDismissError, compact = true)
            }
        )
    }
}

@Composable
private fun SetupContent(
    uiState: OnlineLobbyUiState,
    joining: Boolean,
    onJoiningChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onTimeControlChange: (TimeControl?) -> Unit,
    onCreateGame: () -> Unit,
    onJoinGame: () -> Unit,
    onQuickPair: () -> Unit,
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
                SectionLabel(stringResource(Res.string.lobby_opponent))
                ChoiceCard(
                    title = stringResource(Res.string.lobby_invite_a_friend),
                    subtitle = stringResource(Res.string.lobby_invite_subtitle),
                    selected = !joining,
                    onClick = { onJoiningChange(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                ChoiceCard(
                    title = stringResource(Res.string.lobby_join_with_code),
                    subtitle = stringResource(Res.string.lobby_join_subtitle),
                    selected = joining,
                    onClick = { onJoiningChange(true) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.common_you))
                SoftTextField(
                    value = uiState.playerName,
                    onValueChange = onNameChange,
                    placeholder = stringResource(Res.string.common_your_name),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (joining) {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    SectionLabel(stringResource(Res.string.lobby_game_code))
                    SoftTextField(
                        value = uiState.codeInput,
                        onValueChange = onCodeChange,
                        placeholder = "ABC234",
                        modifier = Modifier.fillMaxWidth()
                    )
                    PillButton(
                        text = stringResource(Res.string.lobby_scan_qr),
                        onClick = onScan,
                        tone = PillTone.SOFT,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    SectionLabel(stringResource(Res.string.lobby_clock))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        for (choice in clockChoices) {
                            SelectPill(
                                text = choice?.label ?: stringResource(Res.string.common_none),
                                selected = uiState.timeControl == choice,
                                onClick = { onTimeControlChange(choice) }
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
                text = stringResource(if (joining) Res.string.lobby_join_game else Res.string.lobby_create_game),
                onClick = { if (joining) onJoinGame() else onCreateGame() },
                enabled = !joining || uiState.codeInput.length == 6,
                modifier = Modifier.fillMaxWidth()
            )
            if (!joining) {
                PillButton(
                    text = stringResource(Res.string.lobby_quick_pair),
                    onClick = onQuickPair,
                    tone = PillTone.ACCENT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = if (joining) {
                    stringResource(Res.string.lobby_colours_drawn_at_random)
                } else {
                    stringResource(Res.string.lobby_friend_joins_with_code)
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
private fun SearchingContent(
    step: LobbyStep.Searching,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(Res.string.lobby_searching),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(Res.string.lobby_searching_detail, step.queued, step.rating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PillButton(
            text = stringResource(Res.string.common_cancel),
            onClick = onCancel,
            tone = PillTone.INK
        )
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
    val shareMessage = stringResource(Res.string.lobby_share_message, step.joinUrl, step.shortCode)

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        SectionLabel(stringResource(Res.string.lobby_game_code))
        Text(step.shortCode, style = MaterialTheme.typography.displayLarge)
        SoftCard(container = accents.pageAlt, corner = 24.dp) {
            Image(
                painter = rememberQrCodePainter(step.joinUrl),
                contentDescription = stringResource(Res.string.lobby_qr_content_description, step.joinUrl),
                modifier = Modifier.padding(14.dp).size(190.dp)
            )
        }
        Text(
            text = step.joinUrl,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        PillButton(
            text = stringResource(Res.string.lobby_share_invite),
            onClick = {
                shareText(shareMessage)
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
                text = stringResource(Res.string.lobby_waiting_for_opponent),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        PillButton(
            text = stringResource(Res.string.common_cancel),
            onClick = onCancel,
            tone = PillTone.SOFT,
            compact = true
        )
    }
}
