package dev.hawk0f.checkmates.ui.replay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.PgnBuilder
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.ui.game.ChessBoard

@Composable
fun ReplayScreen(item: GameHistoryItem, onBack: () -> Unit) {
    var moveIndex by remember { mutableIntStateOf(item.uciHistory.size) }
    val gameState by remember {
        derivedStateOf {
            val game = ChessGame()
            for (uci in item.uciHistory.take(moveIndex)) {
                game.applyUci(uci)
            }
            game.state()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "${item.whiteName} vs ${item.blackName}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            if (moveIndex == 0) {
                "Start position"
            } else {
                "Move $moveIndex of ${item.uciHistory.size}: ${item.uciHistory[moveIndex - 1]}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ChessBoard(
            gameState = gameState,
            selected = null,
            legalTargets = emptySet(),
            flipped = item.myColor == PieceColor.BLACK,
            onSquareTap = {},
            modifier = Modifier.weight(1f).align(Alignment.CenterHorizontally)
        )

        Slider(
            value = moveIndex.toFloat(),
            onValueChange = { moveIndex = it.toInt().coerceIn(0, item.uciHistory.size) },
            valueRange = 0f..item.uciHistory.size.toFloat(),
            steps = (item.uciHistory.size - 1).coerceAtLeast(0)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { moveIndex = 0 }, enabled = moveIndex > 0) {
                Text("|<")
            }
            OutlinedButton(onClick = { moveIndex -= 1 }, enabled = moveIndex > 0) {
                Text("<")
            }
            OutlinedButton(
                onClick = { moveIndex += 1 },
                enabled = moveIndex < item.uciHistory.size
            ) {
                Text(">")
            }
            OutlinedButton(
                onClick = { moveIndex = item.uciHistory.size },
                enabled = moveIndex < item.uciHistory.size
            ) {
                Text(">|")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val shareText = rememberShareText()
            OutlinedButton(
                onClick = {
                    shareText(
                        PgnBuilder.build(
                            whiteName = item.whiteName,
                            blackName = item.blackName,
                            winner = item.winner,
                            reason = item.reason,
                            uciHistory = item.uciHistory,
                            dateMillis = item.finishedAtMillis
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Share PGN")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
        }
    }
}
