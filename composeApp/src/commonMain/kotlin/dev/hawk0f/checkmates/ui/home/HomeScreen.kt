package dev.hawk0f.checkmates.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.GameSessionHolder
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.protocol.ProfileResponse
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.profile.AvatarBadge
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.home_all_games
import dev.hawk0f.checkmates.resources.home_both_players
import dev.hawk0f.checkmates.resources.flow_lichess_name
import dev.hawk0f.checkmates.resources.home_computer
import dev.hawk0f.checkmates.resources.flow_switch_to
import dev.hawk0f.checkmates.resources.home_moves_with_mode
import dev.hawk0f.checkmates.resources.home_nearby
import dev.hawk0f.checkmates.resources.editor_title
import dev.hawk0f.checkmates.resources.friends_title
import dev.hawk0f.checkmates.resources.openings_title
import dev.hawk0f.checkmates.resources.leaderboard_title
import dev.hawk0f.checkmates.resources.puzzles_title
import dev.hawk0f.checkmates.resources.home_new_game
import dev.hawk0f.checkmates.resources.home_no_finished_games
import dev.hawk0f.checkmates.resources.home_online_in_progress
import dev.hawk0f.checkmates.resources.home_opponent_fallback
import dev.hawk0f.checkmates.resources.home_pass_and_play
import dev.hawk0f.checkmates.resources.home_ready_to_play
import dev.hawk0f.checkmates.resources.home_recent_games
import dev.hawk0f.checkmates.resources.home_result_draw_short
import dev.hawk0f.checkmates.resources.home_result_loss_short
import dev.hawk0f.checkmates.resources.home_result_win_short
import dev.hawk0f.checkmates.resources.home_resume
import dev.hawk0f.checkmates.resources.home_start
import dev.hawk0f.checkmates.resources.home_tap_to_keep_playing
import dev.hawk0f.checkmates.resources.home_two_players_one_device
import dev.hawk0f.checkmates.resources.home_vs_opponent
import dev.hawk0f.checkmates.resources.home_white_moves_first
import dev.hawk0f.checkmates.resources.home_your_move
import dev.hawk0f.checkmates.resources.mode_nearby
import dev.hawk0f.checkmates.resources.mode_online
import dev.hawk0f.checkmates.resources.mode_pass_and_play
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_profile
import dev.hawk0f.checkmates.resources.a11y_settings
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun HomeScreen(
    onPassAndPlay: () -> Unit = {},
    onPlayOnline: () -> Unit = {},
    onPlayBluetooth: () -> Unit = {},
    onPlayComputer: () -> Unit = {},
    onOpenPuzzles: () -> Unit = {},
    onOpenEditor: () -> Unit = {},
    onOpenOpenings: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onSwitchFlow: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onResumeGame: () -> Unit = {},
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val profile by AuthManager.profile.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val liveSession = GameSessionHolder.current

    Column(modifier = Modifier.fillMaxSize()) {
        HeroCard(
            profile = profile,
            opponentName = liveSession?.opponentName?.value,
            hasLiveGame = liveSession != null,
            onOpenProfile = onOpenProfile,
            onOpenSettings = onOpenSettings,
            onPrimaryAction = if (liveSession != null) onResumeGame else onPassAndPlay
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeRail(
                onPlayOnline = onPlayOnline,
                onPassAndPlay = onPassAndPlay,
                onPlayBluetooth = onPlayBluetooth,
                onPlayComputer = onPlayComputer,
                onOpenPuzzles = onOpenPuzzles,
                onOpenEditor = onOpenEditor,
                onOpenOpenings = onOpenOpenings,
                onOpenFriends = onOpenFriends,
                onOpenLeaderboard = onOpenLeaderboard,
                onSwitchFlow = onSwitchFlow
            )
            if (profile != null && recent.isNotEmpty()) {
                Hairline()
                RecentSection(recent, onOpenProfile)
            }
        }
    }
}

@Composable
private fun HeroCard(
    profile: ProfileResponse?,
    opponentName: String?,
    hasLiveGame: Boolean,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val accents = LocalAppAccents.current
    val scheme = MaterialTheme.colorScheme
    val previewState = remember { ChessGame().state() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accents.band)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val profileLabel = stringResource(Res.string.a11y_profile)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenProfile)
                        .semantics {
                            role = Role.Button
                            contentDescription = profileLabel
                        }
                ) {
                    if (profile != null) {
                        AvatarBadge(profile.avatarKind, profile.avatarValue, size = 44.dp, fontSize = 20.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accents.onBand),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♞",
                                fontSize = 22.sp,
                                color = accents.band
                            )
                        }
                    }
                }
                val settingsLabel = stringResource(Res.string.a11y_settings)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accents.pageAlt)
                        .clickable(onClick = onOpenSettings)
                        .semantics {
                            role = Role.Button
                            contentDescription = settingsLabel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚙",
                        fontSize = 18.sp,
                        color = accents.onBand
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accents.pageAlt)
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Text(
                    text = stringResource(
                        if (hasLiveGame) Res.string.home_your_move else Res.string.home_ready_to_play
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = accents.onBand
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SectionLabel(
                text = stringResource(
                    if (hasLiveGame) Res.string.home_online_in_progress else Res.string.home_pass_and_play
                ),
                color = accents.onBand
            )
            Text(
                text = if (hasLiveGame) {
                    stringResource(
                        Res.string.home_vs_opponent,
                        opponentName ?: stringResource(Res.string.home_opponent_fallback)
                    )
                } else {
                    stringResource(Res.string.home_two_players_one_device)
                },
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onBackground
            )
        }

        BoardBox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            maxSize = 420.dp
        ) { boardModifier ->
            ChessBoard(
                gameState = previewState,
                selected = null,
                legalTargets = emptySet(),
                flipped = false,
                onSquareTap = {},
                interactive = false,
                modifier = boardModifier.clip(RoundedCornerShape(24.dp)),
                showCoordinates = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (hasLiveGame) Res.string.home_tap_to_keep_playing else Res.string.home_white_moves_first
                ),
                style = MaterialTheme.typography.titleMedium,
                color = accents.onBand
            )
            PillButton(
                text = stringResource(if (hasLiveGame) Res.string.home_resume else Res.string.home_start),
                onClick = onPrimaryAction,
                tone = PillTone.INK,
                compact = true
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeRail(
    onPlayOnline: () -> Unit,
    onPassAndPlay: () -> Unit,
    onPlayBluetooth: () -> Unit,
    onPlayComputer: () -> Unit,
    onOpenPuzzles: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenOpenings: () -> Unit,
    onOpenFriends: () -> Unit,
    onSwitchFlow: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        PillButton(
            text = stringResource(Res.string.home_new_game),
            onClick = onPlayOnline,
            tone = PillTone.ACCENT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.home_pass_and_play),
            onClick = onPassAndPlay,
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.home_computer),
            onClick = onPlayComputer,
            tone = PillTone.LEAF,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.home_nearby),
            onClick = onPlayBluetooth,
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.puzzles_title),
            onClick = onOpenPuzzles,
            tone = PillTone.LEAF,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.leaderboard_title),
            onClick = onOpenLeaderboard,
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.friends_title),
            onClick = onOpenFriends,
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.openings_title),
            onClick = onOpenOpenings,
            tone = PillTone.LEAF,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.editor_title),
            onClick = onOpenEditor,
            tone = PillTone.SOFT,
            compact = true
        )
        PillButton(
            text = stringResource(Res.string.flow_switch_to, stringResource(Res.string.flow_lichess_name)),
            onClick = onSwitchFlow,
            tone = PillTone.BAND,
            compact = true
        )
    }
}

@Composable
private fun RecentSection(recent: List<GameHistoryItem>, onOpenProfile: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(stringResource(Res.string.home_recent_games))
        if (recent.isEmpty()) {
            Text(
                stringResource(Res.string.home_no_finished_games),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        } else {
            for (item in recent) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val won = item.winner != null && item.winner == item.myColor
                    val drawn = item.winner == null
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    drawn -> scheme.surfaceVariant
                                    won -> accents.bandStrong
                                    else -> scheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                when {
                                    drawn -> Res.string.home_result_draw_short
                                    won -> Res.string.home_result_win_short
                                    else -> Res.string.home_result_loss_short
                                }
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (drawn) scheme.onSurfaceVariant else scheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opponentLabel(item),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(
                                Res.string.home_moves_with_mode,
                                item.uciHistory.size,
                                modeLabel(item.mode)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = scheme.outline)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            PillButton(
                text = stringResource(Res.string.home_all_games),
                onClick = onOpenProfile,
                tone = PillTone.SOFT,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun opponentLabel(item: GameHistoryItem): String = when (item.myColor) {
    PieceColor.WHITE -> item.blackName
    PieceColor.BLACK -> item.whiteName
    null -> stringResource(Res.string.home_both_players, item.whiteName, item.blackName)
}

@Composable
private fun modeLabel(mode: String): String = when (mode) {
    "online" -> stringResource(Res.string.mode_online)
    "ble" -> stringResource(Res.string.mode_nearby)
    "hotseat" -> stringResource(Res.string.mode_pass_and_play)
    else -> mode
}
