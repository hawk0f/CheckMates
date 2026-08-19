package dev.hawk0f.chess.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.hawk0f.chess.platform.playMoveSound
import dev.hawk0f.chess.platform.rememberShareText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.chess.shared.domain.GameOverReason
import dev.hawk0f.chess.shared.domain.GameResult
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.domain.PieceKind
import dev.hawk0f.chess.shared.protocol.TimeControl
import dev.hawk0f.chess.shared.transport.TransportConnectionState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewModel.isRemote) {
            RemoteStatusBar(uiState)
        }
        Text(
            text = when {
                gameState.result != null -> resultText(gameState.result!!)
                viewModel.isRemote && uiState.myColor == gameState.sideToMove -> "Your move"
                viewModel.isRemote -> "Opponent's move"
                gameState.sideToMove == PieceColor.WHITE -> "White to move"
                else -> "Black to move"
            },
            style = MaterialTheme.typography.titleLarge
        )
        if (uiState.seriesMyWins + uiState.seriesOpponentWins + uiState.seriesDraws > 0) {
            Text(
                text = seriesText(uiState, viewModel.isRemote),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BoardSideRow(uiState, sideColor = bottomColor.opposite, capturedFrom = bottomColor)
        ChessBoard(
            gameState = gameState,
            selected = uiState.selected,
            legalTargets = uiState.legalTargets,
            flipped = uiState.myColor == PieceColor.BLACK,
            onSquareTap = viewModel::onSquareTap,
            modifier = Modifier.weight(1f)
        )
        BoardSideRow(uiState, sideColor = bottomColor, capturedFrom = bottomColor.opposite)
        if (uiState.drawOfferIncoming) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Draw offered")
                Button(onClick = viewModel::acceptDraw) {
                    Text("Accept")
                }
                OutlinedButton(onClick = viewModel::declineDraw) {
                    Text("Decline")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::resign, enabled = gameState.result == null) {
                Text("Resign")
            }
            if (viewModel.isRemote) {
                OutlinedButton(
                    onClick = viewModel::offerDraw,
                    enabled = gameState.result == null && !uiState.drawOfferOutgoing
                ) {
                    Text(if (uiState.drawOfferOutgoing) "Draw offered" else "Offer draw")
                }
            } else {
                OutlinedButton(onClick = viewModel::newGame) {
                    Text("New Game")
                }
            }
            OutlinedButton(onClick = onExit) {
                Text("Exit")
            }
        }
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

    gameState.result?.let { result ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game over") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(resultText(result))
                    if (uiState.rematchOfferIncoming) {
                        Text(
                            "${uiState.opponentName ?: "Opponent"} wants a rematch",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                when {
                    !viewModel.isRemote -> Button(onClick = viewModel::newGame) {
                        Text("New Game")
                    }
                    viewModel.supportsRematch && uiState.rematchOfferIncoming ->
                        Button(onClick = viewModel::acceptRematch) {
                            Text("Accept rematch")
                        }
                    viewModel.supportsRematch ->
                        Button(
                            onClick = viewModel::offerRematch,
                            enabled = !uiState.rematchOfferOutgoing
                        ) {
                            Text(if (uiState.rematchOfferOutgoing) "Rematch offered" else "Rematch")
                        }
                    else -> {}
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (viewModel.isRemote && uiState.rematchOfferIncoming) {
                        TextButton(onClick = viewModel::declineRematch) {
                            Text("Decline")
                        }
                    }
                    val shareText = rememberShareText()
                    TextButton(onClick = { shareText(viewModel.buildPgn()) }) {
                        Text("PGN")
                    }
                    TextButton(onClick = onExit) {
                        Text("Exit")
                    }
                }
            }
        )
    }
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
        title = { Text("Time control") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (choice in timeControlChoices) {
                    OutlinedButton(
                        onClick = { onPick(choice) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(choice?.label ?: "No clock")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun BoardSideRow(uiState: GameUiState, sideColor: PieceColor, capturedFrom: PieceColor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            CapturedRow(uiState.gameState, capturedFrom = capturedFrom)
        }
        val millis = if (sideColor == PieceColor.WHITE) uiState.whiteMillis else uiState.blackMillis
        if (millis != null) {
            ClockBadge(
                millis = millis,
                active = uiState.gameState.result == null && uiState.gameState.sideToMove == sideColor
            )
        }
    }
}

@Composable
private fun ClockBadge(millis: Long, active: Boolean) {
    val totalSeconds = millis / 1000
    val text = if (totalSeconds < 20) {
        val tenths = (millis % 1000) / 100
        "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}.$tenths"
    } else {
        "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        color = when {
            millis <= 20_000 && active -> MaterialTheme.colorScheme.error
            active -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun RemoteStatusBar(uiState: GameUiState) {
    val opponent = uiState.opponentName ?: "Opponent"
    val status = when {
        uiState.connectionState is TransportConnectionState.Reconnecting -> "reconnecting…"
        uiState.connectionState is TransportConnectionState.Closed -> "connection lost"
        !uiState.opponentConnected -> "$opponent reconnecting…"
        else -> null
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "You (${colorName(uiState.myColor)}) vs $opponent",
            style = MaterialTheme.typography.titleMedium
        )
        if (status != null) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun colorName(color: PieceColor?): String = when (color) {
    PieceColor.WHITE -> "White"
    PieceColor.BLACK -> "Black"
    null -> "…"
}

private fun seriesText(uiState: GameUiState, isRemote: Boolean): String {
    val left = if (isRemote) "You" else "White"
    val right = if (isRemote) uiState.opponentName ?: "Opponent" else "Black"
    val base = "Series: $left ${uiState.seriesMyWins} – ${uiState.seriesOpponentWins} $right"
    return if (uiState.seriesDraws > 0) "$base (${uiState.seriesDraws} drawn)" else base
}

@Composable
private fun PromotionDialog(
    color: PieceColor,
    onChoose: (PieceKind) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote to") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((kind, label) in listOf(
                    PieceKind.QUEEN to if (color == PieceColor.WHITE) "♕" else "♛",
                    PieceKind.ROOK to if (color == PieceColor.WHITE) "♖" else "♜",
                    PieceKind.BISHOP to if (color == PieceColor.WHITE) "♗" else "♝",
                    PieceKind.KNIGHT to if (color == PieceColor.WHITE) "♘" else "♞"
                )) {
                    Button(onClick = { onChoose(kind) }) {
                        Text(label, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        },
        confirmButton = {}
    )
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
private fun CapturedRow(gameState: dev.hawk0f.chess.shared.domain.GameState, capturedFrom: PieceColor) {
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
        return
    }
    val glyphs = captured.joinToString("") { kind ->
        when (kind) {
            PieceKind.QUEEN -> if (capturedFrom == PieceColor.WHITE) "♕" else "♛"
            PieceKind.ROOK -> if (capturedFrom == PieceColor.WHITE) "♖" else "♜"
            PieceKind.BISHOP -> if (capturedFrom == PieceColor.WHITE) "♗" else "♝"
            PieceKind.KNIGHT -> if (capturedFrom == PieceColor.WHITE) "♘" else "♞"
            else -> if (capturedFrom == PieceColor.WHITE) "♙" else "♟"
        }
    }
    Text(text = glyphs, style = MaterialTheme.typography.titleMedium)
}

private fun resultText(result: GameResult): String {
    val winner = when (result.winner) {
        PieceColor.WHITE -> "White wins"
        PieceColor.BLACK -> "Black wins"
        null -> "Draw"
    }
    val reason = when (result.reason) {
        GameOverReason.CHECKMATE -> "checkmate"
        GameOverReason.STALEMATE -> "stalemate"
        GameOverReason.DRAW_AGREED -> "agreement"
        GameOverReason.RESIGNATION -> "resignation"
        GameOverReason.INSUFFICIENT_MATERIAL -> "insufficient material"
        GameOverReason.REPETITION -> "threefold repetition"
        GameOverReason.FIFTY_MOVE -> "fifty-move rule"
        GameOverReason.TIMEOUT -> "time out"
        GameOverReason.DISCONNECTION -> "disconnection"
    }
    return "$winner — $reason"
}
