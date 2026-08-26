package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.watch_live_broadcasts
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.watch_import_to_review
import dev.hawk0f.checkmates.resources.watch_rounds_count
import dev.hawk0f.checkmates.resources.watch_streamers_live
import dev.hawk0f.checkmates.resources.watch_title
import dev.hawk0f.checkmates.resources.watch_waiting_for_game
import org.jetbrains.compose.resources.stringResource

internal val WatchSurface = Color(0xFF201E1D)
private val watchInk = Color(0xFFF5EAD8)
private val watchMuted = Color(0xFFA19786)
private val watchFaint = Color(0xFF82796A)
private val watchAccent = Color(0xFFC67139)
private val watchClock = Color(0xFFF6A06B)
private val watchChip = Color(0x1FF5EAD8)
private val watchLine = Color(0x24F5EAD8)
private val watchOnAccent = Color(0xFFFFF2EB)

@Composable
fun LichessWatchScreen(
    onReview: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LichessWatchViewModel = viewModel { LichessWatchViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LichessWatchContent(
        uiState = uiState,
        onWatch = viewModel::watch,
        onDismissError = viewModel::dismissError,
        onReview = onReview,
        onBack = onBack
    )
}

@Composable
internal fun LichessWatchContent(
    uiState: LichessWatchUiState,
    onWatch: (String) -> Unit,
    onDismissError: () -> Unit,
    onReview: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(WatchSurface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stringResource(Res.string.watch_title),
                style = MaterialTheme.typography.displaySmall,
                color = watchInk
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(watchChip)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.live) watchAccent else watchFaint)
                    )
                    Text(
                        text = "tv/feed",
                        style = MaterialTheme.typography.labelSmall,
                        color = watchInk
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(watchChip)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    CloseIcon(color = watchInk)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (channel in uiState.channels) {
                    ChannelChip(
                        label = channel.replaceFirstChar { it.uppercase() },
                        selected = uiState.channel == channel,
                        onClick = { onWatch(channel) }
                    )
                }
            }

            val topPlayer = if (uiState.flipped) uiState.white else uiState.black
            val bottomPlayer = if (uiState.flipped) uiState.black else uiState.white
            PlayerLine(player = topPlayer, active = false)

            uiState.gameState?.let { state ->
                val flipped = uiState.flipped
                BoardBox(modifier = Modifier.fillMaxWidth()) { boardModifier ->
                    ChessBoard(
                        gameState = state,
                        selected = null,
                        legalTargets = emptySet(),
                        flipped = flipped,
                        onSquareTap = {},
                        interactive = false,
                        showCoordinates = false,
                        modifier = boardModifier.clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            PlayerLine(player = bottomPlayer, active = true)

            if (uiState.broadcasts.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.watch_live_broadcasts),
                    style = MaterialTheme.typography.labelSmall,
                    color = watchFaint
                )
                for (broadcast in uiState.broadcasts) {
                    WatchRow(
                        initials = broadcast.tour.name ?: "B",
                        initialsColor = watchClock,
                        title = broadcast.tour.name ?: broadcast.tour.id,
                        subtitle = broadcast.rounds.firstOrNull { it.ongoing }?.name
                            ?: stringResource(Res.string.watch_rounds_count, broadcast.rounds.size)
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(watchLine))
                }
            }

            WatchRow(
                initials = "SF",
                initialsColor = Color(0xFFAEBF92),
                title = stringResource(Res.string.watch_streamers_live, uiState.streamerCount),
                subtitle = "/api/streamer/live"
            )

            uiState.gameId?.let { id ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .border(1.5.dp, watchLine, CircleShape)
                        .clickable { onReview(id) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.watch_import_to_review),
                        style = MaterialTheme.typography.titleMedium,
                        color = watchInk,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    uiState.error?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(Res.string.watch_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = onDismissError, compact = true)
            }
        )
    }
}

@Composable
private fun ChannelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) watchOnAccent else Color(0xFFDCD3C4),
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) watchAccent else watchChip)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    )
}

@Composable
private fun PlayerLine(player: WatchPlayer?, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = listOfNotNull(player?.title, player?.rating?.toString())
                    .joinToString(" · ")
                    .ifEmpty { "—" },
                style = MaterialTheme.typography.labelSmall,
                color = watchMuted
            )
            Text(
                text = player?.name ?: stringResource(Res.string.watch_waiting_for_game),
                style = MaterialTheme.typography.titleLarge,
                color = watchInk
            )
        }
        Text(
            text = player?.seconds?.let { seconds ->
                "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
            } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            color = if (active) watchClock else watchFaint
        )
    }
}

@Composable
private fun WatchRow(
    initials: String,
    initialsColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(watchChip),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = initialsColor
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = watchInk)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = watchMuted
            )
        }
        ChevronIcon(direction = ChevronDirection.RIGHT, color = watchFaint, size = 16.dp)
    }
}
