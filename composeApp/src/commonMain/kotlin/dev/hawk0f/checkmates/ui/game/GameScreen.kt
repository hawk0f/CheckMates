package dev.hawk0f.checkmates.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hawk0f.checkmates.net.lichess.LichessChatLine
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.platform.playMoveSound
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.GameResult
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import dev.hawk0f.checkmates.shared.transport.TransportConnectionState
import dev.hawk0f.checkmates.ui.profile.pieceDrawable
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.FlagIcon
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SoftTextField
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.StatTile
import org.jetbrains.compose.resources.painterResource

@Composable
fun GameScreen(
    mode: GameMode,
    onExit: () -> Unit
) {
    val viewModel: GameViewModel = viewModel(key = if (mode is GameMode.Remote) "remote" else "hotseat") {
        GameViewModel(mode)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gameState = uiState.gameState
    val accents = LocalAppAccents.current

    val haptic = LocalHapticFeedback.current
    var lastSeenPieceCount by remember { mutableStateOf(32) }
    LaunchedEffect(gameState.uciHistory.size) {
        if (gameState.uciHistory.isNotEmpty()) {
            val pieceCount = gameState.pieces.size
            playMoveSound(capture = pieceCount < lastSeenPieceCount)
            lastSeenPieceCount = pieceCount
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            lastSeenPieceCount = 32
        }
    }

    val bottomColor = uiState.myColor ?: PieceColor.WHITE
    var confirmingResign by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(accents.pageAlt)) {
        if (gameState.result != null) {
            GameOverPanel(
                uiState = uiState,
                viewModel = viewModel,
                bottomColor = bottomColor,
                onExit = onExit
            )
        } else {
            PlayingPanel(
                uiState = uiState,
                viewModel = viewModel,
                bottomColor = bottomColor,
                onExit = onExit,
                onResignRequest = { confirmingResign = true }
            )
        }
    }

    if (confirmingResign && gameState.result == null) {
        AlertDialog(
            onDismissRequest = { confirmingResign = false },
            title = { Text("Resign the game?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = if (viewModel.isRemote) {
                        "Your opponent takes the win."
                    } else {
                        "${if (gameState.sideToMove == PieceColor.WHITE) "Black" else "White"} takes the win."
                    }
                )
            },
            confirmButton = {
                PillButton(
                    text = "Resign",
                    onClick = {
                        confirmingResign = false
                        viewModel.resign()
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = "Keep playing",
                    onClick = { confirmingResign = false },
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
        )
    }

    if (uiState.showTimePicker && gameState.result == null) {
        TimeControlDialog(onPick = viewModel::selectTimeControl)
    }

    uiState.pendingPromotion?.let {
        PromotionDialog(
            color = gameState.sideToMove,
            onChoose = viewModel::onPromotionChosen,
            onDismiss = viewModel::onPromotionDismissed
        )
    }
}

@Composable
private fun PlayingPanel(
    uiState: GameUiState,
    viewModel: GameViewModel,
    bottomColor: PieceColor,
    onExit: () -> Unit,
    onResignRequest: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val gameState = uiState.gameState

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 20.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            CircleButton(
                onClick = onExit,
                container = scheme.onSurface.copy(alpha = 0.08f)
            ) {
                ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand)
            }
            PlayerBlock(
                uiState = uiState,
                sideColor = bottomColor.opposite,
                alignEnd = true,
                clockSize = false,
                rotated = !viewModel.isRemote
            )
        }

        BoardBox(modifier = Modifier.weight(1f)) { boardModifier ->
            ChessBoard(
                gameState = gameState,
                selected = uiState.selected,
                legalTargets = uiState.legalTargets,
                flipped = uiState.myColor == PieceColor.BLACK,
                onSquareTap = viewModel::onSquareTap,
                modifier = boardModifier
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            PlayerBlock(
                uiState = uiState,
                sideColor = bottomColor,
                alignEnd = false,
                clockSize = true,
                rotated = false
            )
            Column(horizontalAlignment = Alignment.End) {
                SectionLabel(statusLabel(uiState, viewModel.isRemote), color = accents.onBand)
                Text(
                    text = lastMoveLabel(gameState),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
            }
        }

        BottomSheet(
            uiState = uiState,
            viewModel = viewModel,
            onResignRequest = onResignRequest
        )
    }
}

@Composable
private fun BottomSheet(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onResignRequest: () -> Unit
) {
    val lichess = viewModel.lichessTransport
    val takebackIncoming by (lichess?.takebackIncoming ?: MutableStateFlow(false))
        .collectAsStateWithLifecycle()
    val takebackOutgoing by (lichess?.takebackOutgoing ?: MutableStateFlow(false))
        .collectAsStateWithLifecycle()
    val opponentGone by (lichess?.opponentGoneSeconds ?: MutableStateFlow<Int?>(null))
        .collectAsStateWithLifecycle()
    val chatLines by (lichess?.chat ?: MutableStateFlow(emptyList<LichessChatLine>()))
        .collectAsStateWithLifecycle()
    var chatOpen by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    if (chatOpen && lichess != null) {
        AlertDialog(
            onDismissRequest = { chatOpen = false },
            title = { Text("Chat", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatLines.isEmpty()) {
                        Text(
                            text = "No messages yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    for (line in chatLines.takeLast(8)) {
                        Text(
                            text = "${line.author}: ${line.text}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SoftTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it.take(140) },
                        placeholder = "Say something",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                PillButton(
                    text = "Send",
                    onClick = {
                        viewModel.sendChat(chatDraft)
                        chatDraft = ""
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = "Close",
                    onClick = { chatOpen = false },
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
        )
    }

    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(scheme.background)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(scheme.onSurface.copy(alpha = 0.2f))
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SectionLabel("Moves", color = accents.bandStrong)
                val allPairs = allMovePairs(uiState.gameState.uciHistory)
                if (allPairs.isEmpty()) {
                    Text(
                        text = "No moves yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                for (pair in allPairs) {
                    Text(
                        text = pair,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (uiState.drawOfferIncoming) {
            SoftCard(container = accents.band, corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Draw offered",
                        style = MaterialTheme.typography.titleMedium,
                        color = accents.onBand,
                        modifier = Modifier.weight(1f)
                    )
                    PillButton(
                        text = "Accept",
                        onClick = viewModel::acceptDraw,
                        tone = PillTone.INK,
                        compact = true
                    )
                    PillButton(
                        text = "Decline",
                        onClick = viewModel::declineDraw,
                        tone = PillTone.SOFT,
                        compact = true
                    )
                }
            }
        }

        if (takebackIncoming || uiState.takebackOfferIncoming) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Takeback requested",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Allow",
                    onClick = { viewModel.answerTakeback(true) },
                    tone = PillTone.ACCENT,
                    compact = true
                )
                PillButton(
                    text = "No",
                    onClick = { viewModel.answerTakeback(false) },
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
        }

        val goneSeconds = opponentGone
        if (lichess != null && goneSeconds != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goneSeconds > 0) {
                        "Opponent gone — claim victory in ${goneSeconds}s"
                    } else {
                        "Opponent gone"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Claim win",
                    onClick = viewModel::claimVictory,
                    tone = PillTone.INK,
                    compact = true,
                    enabled = goneSeconds == 0
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val pairs = if (expanded) emptyList() else movePairs(uiState.gameState.uciHistory)
                for ((index, pair) in pairs.withIndex()) {
                    Text(
                        text = pair,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == pairs.lastIndex) scheme.onSurface else scheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel.supportsTakeback) {
                    CircleButton(
                        onClick = viewModel::offerTakeback,
                        enabled = !takebackOutgoing &&
                            !uiState.takebackOfferOutgoing &&
                            uiState.gameState.uciHistory.isNotEmpty()
                    ) {
                        Text(
                            text = "↩",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                }
                if (lichess != null) {
                    CircleButton(onClick = { chatOpen = true }) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                }
                if (viewModel.isRemote) {
                    CircleButton(
                        onClick = viewModel::offerDraw,
                        enabled = !uiState.drawOfferOutgoing
                    ) {
                        Text(
                            text = "½",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface
                        )
                    }
                } else {
                    CircleButton(onClick = viewModel::newGame) {
                        Text(
                            text = "↺",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                }
                CircleButton(onClick = onResignRequest) {
                    FlagIcon(color = scheme.onPrimaryContainer, size = 17.dp)
                }
            }
        }

        val connectionNote = connectionNote(uiState)
        if (connectionNote != null) {
            Text(
                text = connectionNote,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }
    }
}

@Composable
private fun GameOverPanel(
    uiState: GameUiState,
    viewModel: GameViewModel,
    bottomColor: PieceColor,
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val result = uiState.gameState.result ?: return
    val shareText = rememberShareText()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SectionLabel(
                text = "${reasonLabel(result.reason)} · move ${(uiState.gameState.uciHistory.size + 1) / 2}",
                color = accents.onBand
            )
            Text(
                text = headlineText(result, uiState.myColor, viewModel.isRemote),
                style = MaterialTheme.typography.displayMedium,
                color = scheme.onBackground
            )
            if (uiState.rematchOfferIncoming) {
                Text(
                    text = "${uiState.opponentName ?: "Opponent"} wants a rematch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary
                )
            }
        }

        BoardBox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            maxSize = 360.dp
        ) { boardModifier ->
            ChessBoard(
                gameState = uiState.gameState,
                selected = null,
                legalTargets = emptySet(),
                flipped = bottomColor == PieceColor.BLACK,
                onSquareTap = {},
                interactive = false,
                modifier = boardModifier.alpha(0.9f).clip(RoundedCornerShape(20.dp)),
                showCoordinates = false
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = "${uiState.gameState.uciHistory.size}",
                    label = "Moves played",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = seriesValue(uiState),
                    label = if (viewModel.isRemote) "You · them · drawn" else "White · black · drawn",
                    modifier = Modifier.weight(1f)
                )
            }
            SectionLabel("The game", color = accents.bandStrong)
            SoftCard(
                container = scheme.background,
                corner = 20.dp,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pairs = allMovePairs(uiState.gameState.uciHistory)
                    if (pairs.isEmpty()) {
                        Text(
                            text = "No moves were played.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        for (pair in pairs) {
                            Text(
                                text = pair,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                !viewModel.isRemote -> PillButton(
                    text = "New game",
                    onClick = viewModel::newGame,
                    modifier = Modifier.weight(1f)
                )

                viewModel.supportsRematch && uiState.rematchOfferIncoming -> PillButton(
                    text = "Accept rematch",
                    onClick = viewModel::acceptRematch,
                    modifier = Modifier.weight(1f)
                )

                viewModel.supportsRematch -> PillButton(
                    text = if (uiState.rematchOfferOutgoing) "Rematch offered" else "Rematch",
                    onClick = viewModel::offerRematch,
                    enabled = !uiState.rematchOfferOutgoing,
                    modifier = Modifier.weight(1f)
                )

                else -> Spacer(modifier = Modifier.weight(1f))
            }
            if (viewModel.isRemote && uiState.rematchOfferIncoming) {
                PillButton(
                    text = "Decline",
                    onClick = viewModel::declineRematch,
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
            PillButton(
                text = "PGN",
                onClick = { shareText(viewModel.buildPgn()) },
                tone = PillTone.SOFT,
                compact = true
            )
            PillButton(
                text = "Exit",
                onClick = onExit,
                tone = PillTone.SOFT,
                compact = true
            )
        }
    }
}

@Composable
private fun PlayerBlock(
    uiState: GameUiState,
    sideColor: PieceColor,
    alignEnd: Boolean,
    clockSize: Boolean,
    rotated: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val millis = if (sideColor == PieceColor.WHITE) uiState.whiteMillis else uiState.blackMillis
    val active = uiState.gameState.result == null && uiState.gameState.sideToMove == sideColor
    val alignment = if (alignEnd) Alignment.End else Alignment.Start
    val isMine = uiState.myColor == null || uiState.myColor == sideColor

    val name = playerName(uiState, sideColor, isMine)

    Column(
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = if (rotated) Modifier.rotate(180f) else Modifier
    ) {
        if (!clockSize) {
            if (millis != null) {
                ClockText(millis = millis, active = active, big = false)
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.bandStrong
                )
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (active) scheme.onSurface else accents.onBand
                )
            }
            CapturedRow(uiState.gameState, capturedFrom = sideColor.opposite)
        } else {
            CapturedRow(uiState.gameState, capturedFrom = sideColor.opposite)
            if (millis != null) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.bandStrong
                )
                ClockText(millis = millis, active = active, big = true)
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.displaySmall,
                    color = if (active) scheme.onSurface else accents.onBand
                )
            }
        }
    }
}

@Composable
private fun ClockText(millis: Long, active: Boolean, big: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val totalSeconds = millis / 1000
    val text = if (totalSeconds < 20) {
        val tenths = (millis % 1000) / 100
        "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}.$tenths"
    } else {
        "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
    }
    Text(
        text = text,
        style = if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
        color = when {
            millis <= 20_000 && active -> scheme.error
            active -> scheme.primary
            else -> accents.onBand
        }
    )
}

private fun playerName(uiState: GameUiState, sideColor: PieceColor, isMine: Boolean): String {
    if (uiState.myColor == null) {
        return if (sideColor == PieceColor.WHITE) "White" else "Black"
    }
    return if (isMine) "You" else uiState.opponentName ?: "Opponent"
}

private fun statusLabel(uiState: GameUiState, isRemote: Boolean): String = when {
    isRemote && uiState.myColor == uiState.gameState.sideToMove -> "Your move"
    isRemote -> "Their move"
    uiState.gameState.sideToMove == PieceColor.WHITE -> "White to move"
    else -> "Black to move"
}

private fun lastMoveLabel(gameState: GameState): String {
    val last = gameState.uciHistory.lastOrNull() ?: return "Opening move"
    return "$last played"
}

private fun connectionNote(uiState: GameUiState): String? = when {
    uiState.connectionState is TransportConnectionState.Reconnecting -> "Reconnecting…"
    uiState.connectionState is TransportConnectionState.Closed -> "Connection lost"
    !uiState.opponentConnected && uiState.myColor != null ->
        "${uiState.opponentName ?: "Opponent"} reconnecting…"

    else -> null
}

private fun allMovePairs(history: List<String>): List<String> {
    val pairs = mutableListOf<String>()
    var index = 0
    while (index < history.size) {
        val number = index / 2 + 1
        val white = history[index]
        val black = history.getOrNull(index + 1)
        pairs.add(if (black != null) "$number. $white  $black" else "$number. $white")
        index += 2
    }
    return pairs
}

private fun movePairs(history: List<String>): List<String> {
    if (history.isEmpty()) {
        return listOf("No moves yet")
    }
    val pairs = mutableListOf<String>()
    var index = 0
    while (index < history.size) {
        val number = index / 2 + 1
        val white = history[index]
        val black = history.getOrNull(index + 1)
        pairs.add(if (black != null) "$number. $white $black" else "$number. $white")
        index += 2
    }
    return pairs.takeLast(2)
}

private fun seriesValue(uiState: GameUiState): String =
    "${uiState.seriesMyWins} · ${uiState.seriesOpponentWins} · ${uiState.seriesDraws}"

private fun headlineText(result: GameResult, myColor: PieceColor?, isRemote: Boolean): String = when {
    result.winner == null -> "Draw"
    isRemote && myColor != null -> if (result.winner == myColor) "You won" else "You lost"
    result.winner == PieceColor.WHITE -> "White won"
    else -> "Black won"
}

private val timeControlChoices = listOf(
    null,
    TimeControl(180, 0),
    TimeControl(180, 2),
    TimeControl(300, 0),
    TimeControl(600, 0),
    TimeControl(900, 10)
)

@Composable
private fun TimeControlDialog(onPick: (TimeControl?) -> Unit) {
    AlertDialog(
        onDismissRequest = { onPick(null) },
        title = { Text("Clock", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (choice in timeControlChoices) {
                    PillButton(
                        text = choice?.label ?: "No clock",
                        onClick = { onPick(choice) },
                        tone = PillTone.SOFT,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun PromotionDialog(
    color: PieceColor,
    onChoose: (PieceKind) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote to", style = MaterialTheme.typography.titleLarge) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (kind in listOf(
                    PieceKind.QUEEN,
                    PieceKind.ROOK,
                    PieceKind.BISHOP,
                    PieceKind.KNIGHT
                )) {
                    SoftCard(
                        container = MaterialTheme.colorScheme.surfaceContainer,
                        corner = 20.dp,
                        onClick = { onChoose(kind) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(pieceDrawable(pieceCode(color, kind))),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun pieceCode(color: PieceColor, kind: PieceKind): String {
    val prefix = if (color == PieceColor.WHITE) "w" else "b"
    val suffix = when (kind) {
        PieceKind.KING -> "k"
        PieceKind.QUEEN -> "q"
        PieceKind.ROOK -> "r"
        PieceKind.BISHOP -> "b"
        PieceKind.KNIGHT -> "n"
        PieceKind.PAWN -> "p"
    }
    return prefix + suffix
}

private val pieceValues = mapOf(
    PieceKind.PAWN to 1,
    PieceKind.KNIGHT to 3,
    PieceKind.BISHOP to 3,
    PieceKind.ROOK to 5,
    PieceKind.QUEEN to 9
)

private val initialCounts = mapOf(
    PieceKind.PAWN to 8,
    PieceKind.KNIGHT to 2,
    PieceKind.BISHOP to 2,
    PieceKind.ROOK to 2,
    PieceKind.QUEEN to 1
)

@Composable
private fun CapturedRow(gameState: GameState, capturedFrom: PieceColor) {
    val remaining = gameState.pieces.values
        .filter { it.color == capturedFrom }
        .groupingBy { it.kind }
        .eachCount()
    val captured = buildList {
        for ((kind, initial) in initialCounts) {
            repeat(initial - (remaining[kind] ?: 0)) {
                add(kind)
            }
        }
    }.sortedByDescending { pieceValues[it] }
    if (captured.isEmpty()) {
        Spacer(modifier = Modifier.height(15.dp))
        return
    }
    Row(
        modifier = Modifier.alpha(0.65f),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (kind in captured.take(10)) {
            Image(
                painter = painterResource(pieceDrawable(pieceCode(capturedFrom, kind))),
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun reasonLabel(reason: GameOverReason): String = when (reason) {
    GameOverReason.CHECKMATE -> "Checkmate"
    GameOverReason.STALEMATE -> "Stalemate"
    GameOverReason.DRAW_AGREED -> "Draw agreed"
    GameOverReason.RESIGNATION -> "Resignation"
    GameOverReason.INSUFFICIENT_MATERIAL -> "Insufficient material"
    GameOverReason.REPETITION -> "Threefold repetition"
    GameOverReason.FIFTY_MOVE -> "Fifty-move rule"
    GameOverReason.TIMEOUT -> "Time out"
    GameOverReason.DISCONNECTION -> "Disconnection"
}
