package dev.hawk0f.checkmates.ui.lichess

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill

@Composable
fun LichessWatchScreen(
    onReview: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LichessWatchViewModel = viewModel { LichessWatchViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("Watch", style = MaterialTheme.typography.displaySmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(scheme.surfaceVariant)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.live) accents.positive else scheme.outline)
                    )
                    Text(
                        text = "tv/feed",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                CircleButton(onClick = onBack) {
                    CloseIcon(color = scheme.onSurfaceVariant)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                for (channel in uiState.channels) {
                    SelectPill(
                        text = channel.replaceFirstChar { it.uppercase() },
                        selected = uiState.channel == channel,
                        onClick = { viewModel.watch(channel) }
                    )
                }
            }

            val topPlayer = if (uiState.flipped) uiState.white else uiState.black
            val bottomPlayer = if (uiState.flipped) uiState.black else uiState.white
            PlayerLine(player = topPlayer, alignEnd = false)

            uiState.gameState?.let { state ->
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    ChessBoard(
                        gameState = state,
                        selected = null,
                        legalTargets = emptySet(),
                        flipped = uiState.flipped,
                        onSquareTap = {},
                        showCoordinates = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PlayerLine(player = bottomPlayer, alignEnd = true)

            uiState.gameId?.let { id ->
                PillButton(
                    text = "Import this game to review",
                    onClick = { onReview(id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.broadcasts.isNotEmpty()) {
                SectionLabel("Live broadcasts", color = accents.bandStrong)
                for (broadcast in uiState.broadcasts) {
                    ListRow(
                        title = broadcast.tour.name ?: broadcast.tour.id,
                        subtitle = broadcast.rounds.firstOrNull { it.ongoing }?.name
                            ?: "${broadcast.rounds.size} rounds",
                        leading = { InitialsBadge(text = broadcast.tour.name ?: "B") }
                    )
                }
            }

            ListRow(
                title = "Streamers live · ${uiState.streamerCount}",
                subtitle = "/api/streamer/live",
                leading = { InitialsBadge(text = "SF") },
                trailing = { CodeChip("live") }
            )
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Watch", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }
}

@Composable
private fun PlayerLine(player: WatchPlayer?, alignEnd: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = listOfNotNull(player?.title, player?.rating?.toString())
                    .joinToString(" · ")
                    .ifEmpty { "—" },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = player?.name ?: "Waiting for a game",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = player?.seconds?.let { seconds ->
                "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
            } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            color = if (alignEnd) scheme.primary else scheme.onSurface
        )
    }
}
