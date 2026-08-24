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
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.common_cancel
import dev.hawk0f.checkmates.resources.lichess_title
import dev.hawk0f.checkmates.resources.seek_ai_unrated_note
import dev.hawk0f.checkmates.resources.seek_casual
import dev.hawk0f.checkmates.resources.seek_challenge_subtitle
import dev.hawk0f.checkmates.resources.seek_challenges_out
import dev.hawk0f.checkmates.resources.lichess_speed_note
import dev.hawk0f.checkmates.resources.seek_clock_days
import dev.hawk0f.checkmates.resources.seek_clock_label
import dev.hawk0f.checkmates.resources.seek_clock_limit_increment
import dev.hawk0f.checkmates.resources.seek_close
import dev.hawk0f.checkmates.resources.seek_create_seek
import dev.hawk0f.checkmates.resources.seek_game_opens_note
import dev.hawk0f.checkmates.resources.seek_looking_for
import dev.hawk0f.checkmates.resources.seek_open_challenge_body
import dev.hawk0f.checkmates.resources.seek_open_challenge_instead
import dev.hawk0f.checkmates.resources.seek_open_challenge_title
import dev.hawk0f.checkmates.resources.seek_open_link
import dev.hawk0f.checkmates.resources.seek_play_stockfish
import dev.hawk0f.checkmates.resources.seek_pool_pace_note
import dev.hawk0f.checkmates.resources.seek_rated
import dev.hawk0f.checkmates.resources.seek_rating_any
import dev.hawk0f.checkmates.resources.seek_rating_range
import dev.hawk0f.checkmates.resources.seek_rating_spread
import dev.hawk0f.checkmates.resources.seek_rating_unknown
import dev.hawk0f.checkmates.resources.seek_send_challenge
import dev.hawk0f.checkmates.resources.seek_share
import dev.hawk0f.checkmates.resources.seek_share_message
import dev.hawk0f.checkmates.resources.seek_stockfish_level
import dev.hawk0f.checkmates.resources.seek_stockfish_note
import dev.hawk0f.checkmates.resources.seek_stream_note
import dev.hawk0f.checkmates.resources.seek_tab_friend
import dev.hawk0f.checkmates.resources.seek_tab_seek
import dev.hawk0f.checkmates.resources.seek_tab_stockfish
import dev.hawk0f.checkmates.resources.seek_title
import dev.hawk0f.checkmates.resources.seek_username_placeholder
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.a11y_close

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
            Text(stringResource(Res.string.seek_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_close)) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        val step = uiState.step
        when (step) {
            is LichessStep.Seeking -> WaitingPanel(
                title = stringResource(Res.string.seek_looking_for, step.label),
                note = stringResource(Res.string.seek_stream_note),
                onCancel = viewModel::cancelWaiting,
                modifier = Modifier.weight(1f)
            )

            is LichessStep.Waiting -> WaitingPanel(
                title = step.label,
                note = stringResource(Res.string.seek_game_opens_note),
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
            title = { Text(stringResource(Res.string.lichess_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(failed.message) },
            confirmButton = {
                PillButton(text = stringResource(Res.string.common_ok), onClick = viewModel::dismissError, compact = true)
            }
        )
    }

    uiState.openChallengeUrl?.let { url ->
        val shareMessage = stringResource(Res.string.seek_share_message, url)
        AlertDialog(
            onDismissRequest = viewModel::dismissOpenChallenge,
            title = { Text(stringResource(Res.string.seek_open_challenge_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.seek_open_challenge_body))
                    Text(text = url, style = MaterialTheme.typography.titleSmall)
                }
            },
            confirmButton = {
                PillButton(
                    text = stringResource(Res.string.seek_share),
                    onClick = {
                        shareText(shareMessage)
                        viewModel.dismissOpenChallenge()
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = stringResource(Res.string.seek_close),
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
                options = listOf(
                    stringResource(Res.string.seek_tab_seek),
                    stringResource(Res.string.seek_tab_friend),
                    stringResource(Res.string.seek_tab_stockfish)
                ),
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
                    SectionLabel(stringResource(Res.string.seek_clock_label), color = accents.bandStrong)
                    val option = if (poolMode) uiState.poolClock else uiState.directClock
                    CodeChip(
                        text = if (option.isCorrespondence) {
                            stringResource(Res.string.seek_clock_days, option.days ?: 0)
                        } else {
                            stringResource(
                                Res.string.seek_clock_limit_increment,
                                option.limitSeconds,
                                option.incrementSeconds
                            )
                        }
                    )
                }
                Text(
                    text = stringResource(Res.string.lichess_speed_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.bandStrong
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    val options = (if (poolMode) POOL_CLOCKS else DIRECT_CLOCKS)
                        .filter { it.isPlayableOverBoardApi }
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
                        text = stringResource(Res.string.seek_pool_pace_note),
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
                        text = stringResource(Res.string.seek_ai_unrated_note),
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
                                    stringResource(Res.string.seek_rating_any)
                                } else {
                                    stringResource(
                                        Res.string.seek_rating_range,
                                        rating - uiState.ratingSpread,
                                        rating + uiState.ratingSpread
                                    )
                                }
                            } ?: stringResource(Res.string.seek_rating_unknown)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        for (spread in RATING_SPREADS) {
                            SelectPill(
                                text = if (spread <= 0) {
                                    stringResource(Res.string.seek_rating_any)
                                } else {
                                    stringResource(Res.string.seek_rating_spread, spread)
                                },
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
                        placeholder = stringResource(Res.string.seek_username_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    PillButton(
                        text = stringResource(Res.string.seek_open_challenge_instead),
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
                            Text(stringResource(Res.string.seek_stockfish_level), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = stringResource(Res.string.seek_stockfish_note),
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
                            text = stringResource(Res.string.seek_challenges_out, uiState.outgoing.size),
                            color = accents.bandStrong
                        )
                        CodeChip("GET /api/challenge")
                    }
                    for (challenge in uiState.outgoing) {
                        ListRow(
                            title = challenge.destUser?.label ?: stringResource(Res.string.seek_open_link),
                            subtitle = stringResource(
                                Res.string.seek_challenge_subtitle,
                                challenge.timeControl?.label.orEmpty(),
                                stringResource(
                                    if (challenge.rated) Res.string.seek_rated else Res.string.seek_casual
                                )
                            ),
                            leading = { InitialsBadge(text = challenge.destUser?.label ?: "?") },
                            trailing = {
                                PillButton(
                                    text = stringResource(Res.string.common_cancel),
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
                    SeekMode.POOL -> stringResource(Res.string.seek_create_seek)
                    SeekMode.FRIEND -> stringResource(Res.string.seek_send_challenge)
                    SeekMode.AI -> stringResource(Res.string.seek_play_stockfish)
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
        PillButton(text = stringResource(Res.string.common_cancel), onClick = onCancel, tone = PillTone.SOFT, compact = true)
    }
}
