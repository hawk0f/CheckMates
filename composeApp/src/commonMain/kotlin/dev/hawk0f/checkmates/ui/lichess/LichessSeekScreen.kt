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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SegmentedTabs
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftTextField

@Composable
fun LichessSeekScreen(
    onGameReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: LichessSeekViewModel = viewModel { LichessSeekViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val shareText = rememberShareText()

    LaunchedEffect(uiState.step) {
        if (uiState.step is LichessStep.GameReady) {
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
            Text("Find a game", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        val step = uiState.step
        when (step) {
            is LichessStep.Seeking -> WaitingPanel(
                title = "Looking for a ${step.label} game",
                note = "The seek stays open while this request streams. Closing it cancels.",
                onCancel = viewModel::cancelWaiting,
                modifier = Modifier.weight(1f)
            )

            is LichessStep.Waiting -> WaitingPanel(
                title = step.label,
                note = "The game opens as soon as it starts",
                onCancel = viewModel::cancelWaiting,
                modifier = Modifier.weight(1f)
            )

            else -> SetupContent(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }

    (uiState.step as? LichessStep.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Lichess", style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissError, compact = true)
            }
        )
    }

    uiState.openChallengeUrl?.let { url ->
        AlertDialog(
            onDismissRequest = viewModel::dismissOpenChallenge,
            title = { Text("Open challenge", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anyone who opens this link plays you. Blitz is allowed here.")
                    Text(text = url, style = MaterialTheme.typography.titleSmall)
                }
            },
            confirmButton = {
                PillButton(
                    text = "Share",
                    onClick = {
                        shareText("Play me on lichess: $url")
                        viewModel.dismissOpenChallenge()
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = "Close",
                    onClick = viewModel::dismissOpenChallenge,
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
        )
    }
}

@Composable
private fun SetupContent(
    uiState: LichessSeekUiState,
    viewModel: LichessSeekViewModel,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SegmentedTabs(
                options = listOf("Seek", "Friend", "Stockfish"),
                selectedIndex = uiState.mode.ordinal,
                onSelect = { viewModel.onModeChange(SeekMode.entries[it]) },
                modifier = Modifier.fillMaxWidth()
            )

            val poolMode = uiState.mode == SeekMode.POOL
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("time · increment", color = accents.bandStrong)
                    val option = if (poolMode) uiState.poolClock else uiState.directClock
                    CodeChip(
                        text = if (option.isCorrespondence) {
                            "days ${option.days}"
                        } else {
                            "${option.limitSeconds}s + ${option.incrementSeconds}s"
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    val options = if (poolMode) POOL_CLOCKS else DIRECT_CLOCKS
                    for (option in options) {
                        SelectPill(
                            text = option.label,
                            selected = option == if (poolMode) uiState.poolClock else uiState.directClock,
                            onClick = {
                                if (poolMode) {
                                    viewModel.onPoolClockChange(option)
                                } else {
                                    viewModel.onDirectClockChange(option)
                                }
                            }
                        )
                    }
                }
                if (poolMode) {
                    Text(
                        text = "Pool needs rapid pace: minutes + 40 x increment must reach 480.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.outline
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("rated", color = accents.bandStrong)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SelectPill(
                        text = "false",
                        selected = !uiState.rated,
                        onClick = { viewModel.onRatedChange(false) }
                    )
                    SelectPill(
                        text = "true",
                        selected = uiState.rated,
                        onClick = { viewModel.onRatedChange(true) }
                    )
                }
                if (uiState.mode == SeekMode.AI) {
                    Text(
                        text = "/challenge/ai is always unrated, the flag is ignored there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.outline
                    )
                }
            }

            if (poolMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel("ratingRange", color = accents.bandStrong)
                        CodeChip(
                            text = uiState.myRating?.let { rating ->
                                if (uiState.ratingSpread <= 0) {
                                    "any"
                                } else {
                                    "${rating - uiState.ratingSpread} – ${rating + uiState.ratingSpread}"
                                }
                            } ?: "unknown rating"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        for (spread in RATING_SPREADS) {
                            SelectPill(
                                text = if (spread <= 0) "any" else "±$spread",
                                selected = uiState.ratingSpread == spread,
                                onClick = { viewModel.onRatingSpreadChange(spread) }
                            )
                        }
                    }
                }
            }

            if (uiState.mode == SeekMode.FRIEND) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("username", color = accents.bandStrong)
                    SoftTextField(
                        value = uiState.friendName,
                        onValueChange = viewModel::onFriendNameChange,
                        placeholder = "Lichess username",
                        modifier = Modifier.fillMaxWidth()
                    )
                    PillButton(
                        text = "Open challenge link instead",
                        onClick = viewModel::createOpenChallenge,
                        tone = PillTone.SOFT,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.mode == SeekMode.AI) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(scheme.inverseSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.aiLevel.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.inverseOnSurface
                            )
                        }
                        Column {
                            Text("Stockfish level", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "1–8 · /challenge/ai, always unrated",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        for (level in AI_LEVELS) {
                            SelectPill(
                                text = level.toString(),
                                selected = uiState.aiLevel == level,
                                onClick = { viewModel.onAiLevelChange(level) }
                            )
                        }
                    }
                }
            }

            if (uiState.outgoing.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel(
                            text = "Challenges out · ${uiState.outgoing.size}",
                            color = accents.bandStrong
                        )
                        CodeChip("GET /api/challenge")
                    }
                    for (challenge in uiState.outgoing) {
                        ListRow(
                            title = challenge.destUser?.label ?: "Open link",
                            subtitle = "${challenge.timeControl?.label.orEmpty()} · " +
                                if (challenge.rated) "rated" else "casual",
                            leading = { InitialsBadge(text = challenge.destUser?.label ?: "?") },
                            trailing = {
                                PillButton(
                                    text = "Cancel",
                                    onClick = { viewModel.cancelChallenge(challenge.id) },
                                    tone = PillTone.SOFT,
                                    compact = true
                                )
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            PillButton(
                text = when (uiState.mode) {
                    SeekMode.POOL -> "Create seek"
                    SeekMode.FRIEND -> "Send challenge"
                    SeekMode.AI -> "Play Stockfish"
                },
                onClick = viewModel::start,
                enabled = uiState.mode != SeekMode.FRIEND || uiState.friendName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = when (uiState.mode) {
                    SeekMode.POOL -> "POST /api/board/seek — colours drawn at random"
                    SeekMode.FRIEND -> "POST /api/challenge/{user}"
                    SeekMode.AI -> "POST /api/challenge/ai"
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WaitingPanel(
    title: String,
    note: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(scheme.primary)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PillButton(text = "Cancel", onClick = onCancel, tone = PillTone.SOFT, compact = true)
    }
}
