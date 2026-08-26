package dev.hawk0f.checkmates.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.platform.playMoveSound
import dev.hawk0f.checkmates.platform.rememberShareText
import dev.hawk0f.checkmates.shared.domain.GameResult
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.SanFormatter
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.protocol.ClockMode
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
import dev.hawk0f.checkmates.ui.theme.SegmentedPills
import dev.hawk0f.checkmates.ui.theme.SoftTextField
import dev.hawk0f.checkmates.ui.theme.SoftCard
import dev.hawk0f.checkmates.ui.theme.StatTile
import org.jetbrains.compose.resources.painterResource
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.game_accept
import dev.hawk0f.checkmates.resources.game_accept_rematch
import dev.hawk0f.checkmates.resources.game_advantage
import dev.hawk0f.checkmates.resources.game_advantage_level
import dev.hawk0f.checkmates.resources.game_advantage_mine
import dev.hawk0f.checkmates.resources.game_advantage_theirs
import dev.hawk0f.checkmates.resources.game_allow
import dev.hawk0f.checkmates.resources.game_analysis
import dev.hawk0f.checkmates.resources.game_black_to_move
import dev.hawk0f.checkmates.resources.game_chat_empty
import dev.hawk0f.checkmates.resources.game_chat_line
import dev.hawk0f.checkmates.resources.game_chat_placeholder
import dev.hawk0f.checkmates.resources.game_chat_send
import dev.hawk0f.checkmates.resources.game_chat_title
import dev.hawk0f.checkmates.resources.game_claim_win
import dev.hawk0f.checkmates.resources.clock_mode_bronstein
import dev.hawk0f.checkmates.resources.clock_mode_delay
import dev.hawk0f.checkmates.resources.clock_mode_fischer
import dev.hawk0f.checkmates.resources.game_clock_title
import dev.hawk0f.checkmates.resources.game_close
import dev.hawk0f.checkmates.resources.game_connection_lost
import dev.hawk0f.checkmates.resources.game_decline
import dev.hawk0f.checkmates.resources.game_draw_offered
import dev.hawk0f.checkmates.resources.game_exit
import dev.hawk0f.checkmates.resources.game_keep_playing
import dev.hawk0f.checkmates.resources.game_move_number
import dev.hawk0f.checkmates.resources.game_move_played
import dev.hawk0f.checkmates.resources.game_moves_played
import dev.hawk0f.checkmates.resources.game_moves_with_count
import dev.hawk0f.checkmates.resources.game_new_game
import dev.hawk0f.checkmates.resources.game_no_clock
import dev.hawk0f.checkmates.resources.game_no_moves_were_played
import dev.hawk0f.checkmates.resources.game_no_moves_yet
import dev.hawk0f.checkmates.resources.game_opening_move
import dev.hawk0f.checkmates.resources.game_opponent_fallback
import dev.hawk0f.checkmates.resources.game_opponent_gone
import dev.hawk0f.checkmates.resources.game_opponent_gone_countdown
import dev.hawk0f.checkmates.resources.game_opponent_reconnecting
import dev.hawk0f.checkmates.resources.game_promote_to
import dev.hawk0f.checkmates.resources.game_reconnecting
import dev.hawk0f.checkmates.resources.game_rematch
import dev.hawk0f.checkmates.resources.game_rematch_offered
import dev.hawk0f.checkmates.resources.game_resign
import dev.hawk0f.checkmates.resources.game_resign_local_body
import dev.hawk0f.checkmates.resources.game_resign_remote_body
import dev.hawk0f.checkmates.resources.game_resign_title
import dev.hawk0f.checkmates.resources.game_result_and_move
import dev.hawk0f.checkmates.resources.rating_changed
import dev.hawk0f.checkmates.resources.game_result_black_won
import dev.hawk0f.checkmates.resources.game_result_draw
import dev.hawk0f.checkmates.resources.game_result_white_won
import dev.hawk0f.checkmates.resources.game_result_you_lost
import dev.hawk0f.checkmates.resources.game_result_you_won
import dev.hawk0f.checkmates.resources.game_series_local
import dev.hawk0f.checkmates.resources.game_series_remote
import dev.hawk0f.checkmates.resources.game_side_black
import dev.hawk0f.checkmates.resources.game_side_white
import dev.hawk0f.checkmates.resources.game_stream_live
import dev.hawk0f.checkmates.resources.game_stream_reconnecting
import dev.hawk0f.checkmates.resources.game_takeback_requested
import dev.hawk0f.checkmates.resources.game_the_game
import dev.hawk0f.checkmates.resources.game_their_move
import dev.hawk0f.checkmates.resources.game_wants_rematch
import dev.hawk0f.checkmates.resources.game_white_to_move
import dev.hawk0f.checkmates.resources.game_you
import dev.hawk0f.checkmates.resources.game_your_move
import dev.hawk0f.checkmates.ui.theme.reasonLabel
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.a11y_chat
import dev.hawk0f.checkmates.resources.a11y_new_game
import dev.hawk0f.checkmates.resources.a11y_offer_draw
import dev.hawk0f.checkmates.resources.a11y_resign
import dev.hawk0f.checkmates.resources.a11y_takeback
import dev.hawk0f.checkmates.resources.game_back_to_live
import dev.hawk0f.checkmates.resources.game_viewing_ply
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.resources.a11y_hint
import dev.hawk0f.checkmates.resources.computer_opponent
import dev.hawk0f.checkmates.resources.computer_thinking
import dev.hawk0f.checkmates.resources.game_clear_premoves
import dev.hawk0f.checkmates.resources.game_hint_move
import dev.hawk0f.checkmates.resources.game_premoves_queued
import dev.hawk0f.checkmates.shared.domain.Square
import androidx.compose.ui.unit.Dp
import dev.hawk0f.checkmates.resources.a11y_first_move
import dev.hawk0f.checkmates.resources.a11y_last_move
import dev.hawk0f.checkmates.resources.a11y_next_move
import dev.hawk0f.checkmates.resources.a11y_previous_move
import dev.hawk0f.checkmates.resources.game_moves_browse
import dev.hawk0f.checkmates.ui.theme.LocalBoardColors

private val SheetPeekHeight = 172.dp
private val RevealSlide = 18.dp

@Composable
fun GameScreen(
    mode: GameMode,
    onExit: () -> Unit,
    onOpenReview: ((String) -> Unit)? = null,
    startFen: String? = null
) {
    val viewModelKey = when (mode) {
        is GameMode.Remote -> "remote"
        is GameMode.Computer -> "computer-${mode.level.id}-${mode.myColor}-${startFen.orEmpty()}"
        GameMode.Hotseat -> "hotseat-${startFen.orEmpty()}"
    }
    val viewModel: GameViewModel = viewModel(key = viewModelKey) {
        GameViewModel(mode = mode, startFen = startFen)
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                onResignRequest = { confirmingResign = true },
                onOpenReview = onOpenReview
            )
        }
    }

    if (confirmingResign && gameState.result == null) {
        AlertDialog(
            onDismissRequest = { confirmingResign = false },
            title = { Text(stringResource(Res.string.game_resign_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = if (viewModel.isRemote) {
                        stringResource(Res.string.game_resign_remote_body)
                    } else {
                        stringResource(
                            Res.string.game_resign_local_body,
                            stringResource(
                                if (gameState.sideToMove == PieceColor.WHITE) {
                                    Res.string.game_side_black
                                } else {
                                    Res.string.game_side_white
                                }
                            )
                        )
                    }
                )
            },
            confirmButton = {
                PillButton(
                    text = stringResource(Res.string.game_resign),
                    onClick = {
                        confirmingResign = false
                        viewModel.resign()
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = stringResource(Res.string.game_keep_playing),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayingPanel(
    uiState: GameUiState,
    viewModel: GameViewModel,
    bottomColor: PieceColor,
    onExit: () -> Unit,
    onResignRequest: () -> Unit,
    onOpenReview: ((String) -> Unit)?
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val gameState = uiState.gameState
    val history = gameState.uciHistory
    var previewPly by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(history.size) {
        previewPly = null
    }
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val expanded = sheetState.targetValue == SheetValue.Expanded
    val offerIncoming = uiState.drawOfferIncoming || uiState.takebackOfferIncoming
    LaunchedEffect(offerIncoming) {
        if (offerIncoming) {
            sheetState.expand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = SheetPeekHeight,
        sheetContainerColor = scheme.surfaceContainer,
        sheetContentColor = scheme.onSurface,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 12.dp,
        containerColor = scheme.background,
        sheetContent = {
            GameSheet(
                uiState = uiState,
                viewModel = viewModel,
                bottomColor = bottomColor,
                expanded = expanded,
                previewPly = previewPly,
                onSelectPly = { ply -> previewPly = ply?.takeIf { it != history.size } },
                onResignRequest = onResignRequest,
                onOpenReview = onOpenReview
            )
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 20.dp, top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleButton(
                        onClick = onExit,
                        container = scheme.onSurface.copy(alpha = 0.08f),
                        contentDescription = stringResource(Res.string.a11y_back)
                    ) {
                        ChevronIcon(direction = ChevronDirection.LEFT, color = accents.onBand)
                    }
                    uiState.connectionState?.let { state ->
                        StreamChip(connected = state is TransportConnectionState.Connected)
                    }
                }
                PlayerBlock(
                    uiState = uiState,
                    sideColor = bottomColor.opposite,
                    alignEnd = true,
                    clockSize = false,
                    rotated = viewModel.isHotseat
                )
            }

            val selected = uiState.selected
            val legalTargets = uiState.legalTargets
            val flipped = uiState.myColor == PieceColor.BLACK
            val previewState = remember(history, previewPly, viewModel.startPositionFen) {
                previewPly?.let { ply ->
                    val replay = ChessGame()
                    viewModel.startPositionFen?.let { replay.loadFen(it) }
                    for (uci in history.take(ply)) {
                        replay.applyUci(uci)
                    }
                    replay.state()
                }
            }
            val hintSquares = remember(uiState.hint) {
                uiState.hint?.takeIf { it.length >= 4 }?.let { uci ->
                    setOf(Square.fromUci(uci.substring(0, 2)), Square.fromUci(uci.substring(2, 4)))
                } ?: emptySet()
            }
            val premoveSquares = remember(uiState.premoves) {
                uiState.premoves.flatMap { uci ->
                    listOf(Square.fromUci(uci.substring(0, 2)), Square.fromUci(uci.substring(2, 4)))
                }.toSet()
            }
            BoardBox(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp),
                maxSize = 560.dp
            ) { boardModifier ->
                ChessBoard(
                    gameState = previewState ?: uiState.premoveState ?: gameState,
                    selected = if (previewState == null) selected else null,
                    legalTargets = if (previewState == null) legalTargets else emptySet(),
                    flipped = flipped,
                    onSquareTap = viewModel::onSquareTap,
                    interactive = previewState == null,
                    premoveSquares = if (previewState == null) premoveSquares + hintSquares else emptySet(),
                    rotatedColor = if (viewModel.isHotseat) bottomColor.opposite else null,
                    modifier = boardModifier
                )
            }
        }
    }
}

@Composable
private fun GameSheet(
    uiState: GameUiState,
    viewModel: GameViewModel,
    bottomColor: PieceColor,
    expanded: Boolean,
    previewPly: Int?,
    onSelectPly: (Int?) -> Unit,
    onResignRequest: () -> Unit,
    onOpenReview: ((String) -> Unit)?
) {
    val lichess = viewModel.lichessTransport
    val takebackIncoming by (lichess?.takebackIncoming ?: MutableStateFlow(false))
        .collectAsStateWithLifecycle()
    val takebackOutgoing by (lichess?.takebackOutgoing ?: MutableStateFlow(false))
        .collectAsStateWithLifecycle()
    val opponentGone by (lichess?.opponentGoneSeconds ?: MutableStateFlow<Int?>(null))
        .collectAsStateWithLifecycle()
    val chatLines = uiState.chat
    var chatOpen by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }

    if (chatOpen && viewModel.supportsChat) {
        AlertDialog(
            onDismissRequest = { chatOpen = false },
            title = { Text(stringResource(Res.string.game_chat_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatLines.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.game_chat_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    for (line in chatLines.takeLast(8)) {
                        Text(
                            text = stringResource(Res.string.game_chat_line, line.author, line.text),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SoftTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it.take(140) },
                        placeholder = stringResource(Res.string.game_chat_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                PillButton(
                    text = stringResource(Res.string.game_chat_send),
                    onClick = {
                        viewModel.sendChat(chatDraft)
                        chatDraft = ""
                    },
                    compact = true
                )
            },
            dismissButton = {
                PillButton(
                    text = stringResource(Res.string.game_close),
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
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MyPlayerRow(
            uiState = uiState,
            bottomColor = bottomColor,
            isRemote = viewModel.isRemote
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lichess != null && onOpenReview != null) {
                PillButton(
                    text = stringResource(Res.string.game_analysis),
                    onClick = { onOpenReview(lichess.gameId) },
                    tone = PillTone.SOFT,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    SheetReveal(visible = !expanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val pairs = movePairs(uiState.gameState.uciHistory)
                            for ((index, pair) in pairs.withIndex()) {
                                Text(
                                    text = pair,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (index == pairs.lastIndex) {
                                        scheme.onSurface
                                    } else {
                                        scheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel.supportsTakeback) {
                    CircleButton(
                        onClick = viewModel::offerTakeback,
                        enabled = !takebackOutgoing &&
                            !uiState.takebackOfferOutgoing &&
                            uiState.gameState.uciHistory.isNotEmpty(),
                        contentDescription = stringResource(Res.string.a11y_takeback)
                    ) {
                        Text(
                            text = "↩",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                }
                if (viewModel.supportsHint) {
                    CircleButton(
                        onClick = viewModel::requestHint,
                        enabled = uiState.gameState.result == null && !uiState.engineThinking,
                        contentDescription = stringResource(Res.string.a11y_hint)
                    ) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                }
                if (viewModel.supportsChat) {
                    CircleButton(
                        onClick = { chatOpen = true },
                        contentDescription = stringResource(Res.string.a11y_chat)
                    ) {
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
                        enabled = !uiState.drawOfferOutgoing,
                        contentDescription = stringResource(Res.string.a11y_offer_draw)
                    ) {
                        Text(
                            text = "½",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface
                        )
                    }
                } else {
                    CircleButton(
                        onClick = viewModel::newGame,
                        contentDescription = stringResource(Res.string.a11y_new_game)
                    ) {
                        Text(
                            text = "↺",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface
                        )
                    }
                }
                CircleButton(
                    onClick = onResignRequest,
                    container = scheme.primaryContainer,
                    contentDescription = stringResource(Res.string.a11y_resign)
                ) {
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

        SheetReveal(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(scheme.outlineVariant.copy(alpha = 0.5f))
                )
                if (uiState.drawOfferIncoming) {
                    SoftCard(container = accents.band, corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.game_draw_offered),
                                style = MaterialTheme.typography.titleMedium,
                                color = accents.onBand,
                                modifier = Modifier.weight(1f)
                            )
                            PillButton(
                                text = stringResource(Res.string.game_accept),
                                onClick = viewModel::acceptDraw,
                                tone = PillTone.INK,
                                compact = true
                            )
                            PillButton(
                                text = stringResource(Res.string.game_decline),
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
                            text = stringResource(Res.string.game_takeback_requested),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = stringResource(Res.string.game_allow),
                            onClick = { viewModel.answerTakeback(true) },
                            tone = PillTone.ACCENT,
                            compact = true
                        )
                        PillButton(
                            text = stringResource(Res.string.game_decline),
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
                                stringResource(Res.string.game_opponent_gone_countdown, goneSeconds)
                            } else {
                                stringResource(Res.string.game_opponent_gone)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = stringResource(Res.string.game_claim_win),
                            onClick = viewModel::claimVictory,
                            tone = PillTone.INK,
                            compact = true,
                            enabled = goneSeconds == 0
                        )
                    }
                }

                if (uiState.engineThinking || uiState.hint != null) {
                    Text(
                        text = if (uiState.engineThinking) {
                            stringResource(Res.string.computer_thinking)
                        } else {
                            stringResource(Res.string.game_hint_move, uiState.hint.orEmpty())
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = accents.onBand
                    )
                }

                if (uiState.premoves.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(
                            text = stringResource(Res.string.game_premoves_queued, uiState.premoves.size),
                            color = accents.bandStrong
                        )
                        PillButton(
                            text = stringResource(Res.string.game_clear_premoves),
                            onClick = viewModel::clearPremoves,
                            tone = PillTone.SOFT,
                            compact = true
                        )
                    }
                }

                AdvantageBar(gameState = uiState.gameState, bottomColor = bottomColor)
                MoveNavRow(
                    historySize = uiState.gameState.uciHistory.size,
                    previewPly = previewPly,
                    onSelectPly = onSelectPly
                )
                MovesGrid(
                    history = uiState.gameState.uciHistory,
                    previewPly = previewPly,
                    onSelectPly = onSelectPly,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 280.dp)
                )
            }
        }
    }
}

@Composable
private fun SheetReveal(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260)
    )
    val slide = with(LocalDensity.current) { RevealSlide.toPx() }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * slide
        }
    ) {
        content()
    }
}

@Composable
private fun StreamChip(connected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 13.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (connected) accents.bandStrong else scheme.error)
        )
        Text(
            text = stringResource(
                if (connected) Res.string.game_stream_live else Res.string.game_stream_reconnecting
            ),
            style = MaterialTheme.typography.labelSmall,
            color = accents.onBand
        )
    }
}

@Composable
private fun MyPlayerRow(
    uiState: GameUiState,
    bottomColor: PieceColor,
    isRemote: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val millis = if (bottomColor == PieceColor.WHITE) uiState.whiteMillis else uiState.blackMillis
    val active = uiState.gameState.result == null && uiState.gameState.sideToMove == bottomColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (millis != null) {
            ClockText(millis = millis, active = active, big = false)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = playerName(uiState, bottomColor, isMine = true),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            SectionLabel(
                text = statusLabel(uiState, isRemote),
                color = if (active) scheme.onPrimaryContainer else accents.bandStrong
            )
        }
        MaterialChip(gameState = uiState.gameState, bottomColor = bottomColor)
        CapturedRow(uiState.gameState, capturedFrom = bottomColor.opposite)
    }
}

@Composable
private fun MoveNavRow(
    historySize: Int,
    previewPly: Int?,
    onSelectPly: (Int?) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val current = previewPly ?: historySize
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(text = stringResource(Res.string.game_moves_browse), modifier = Modifier.weight(1f))
        CircleButton(
            onClick = { onSelectPly(0) },
            size = 38.dp,
            enabled = current > 0,
            contentDescription = stringResource(Res.string.a11y_first_move)
        ) {
            ChevronIcon(direction = ChevronDirection.LEFT, color = scheme.onSurface, size = 16.dp, doubled = true)
        }
        CircleButton(
            onClick = { onSelectPly((current - 1).coerceAtLeast(0)) },
            size = 38.dp,
            enabled = current > 0,
            contentDescription = stringResource(Res.string.a11y_previous_move)
        ) {
            ChevronIcon(direction = ChevronDirection.LEFT, color = scheme.onSurface, size = 16.dp)
        }
        CircleButton(
            onClick = { onSelectPly((current + 1).coerceAtMost(historySize)) },
            size = 38.dp,
            enabled = current < historySize,
            contentDescription = stringResource(Res.string.a11y_next_move)
        ) {
            ChevronIcon(direction = ChevronDirection.RIGHT, color = scheme.onSurface, size = 16.dp)
        }
        CircleButton(
            onClick = { onSelectPly(null) },
            size = 38.dp,
            enabled = previewPly != null,
            contentDescription = stringResource(Res.string.a11y_last_move)
        ) {
            ChevronIcon(direction = ChevronDirection.RIGHT, color = scheme.onSurface, size = 16.dp, doubled = true)
        }
    }
}

@Composable
private fun AdvantageBar(gameState: GameState, bottomColor: PieceColor) {
    val scheme = MaterialTheme.colorScheme
    val balance = materialBalance(gameState)
    val mine = if (bottomColor == PieceColor.WHITE) balance else -balance
    val fraction = (0.5f + mine / 20f).coerceIn(0.08f, 0.92f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel(stringResource(Res.string.game_advantage))
            Text(
                text = when {
                    mine > 0 -> stringResource(Res.string.game_advantage_mine, mine)
                    mine < 0 -> stringResource(Res.string.game_advantage_theirs, -mine)
                    else -> stringResource(Res.string.game_advantage_level)
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimaryContainer
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(scheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(scheme.primary)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(scheme.onTertiaryContainer)
            )
        }
    }
}

@Composable
private fun MovesGrid(
    history: List<String>,
    previewPly: Int?,
    onSelectPly: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val rows = sanMoveRows(history)
    val currentPly = previewPly ?: history.size
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(
                text = if (previewPly != null) {
                    stringResource(Res.string.game_viewing_ply, previewPly, history.size)
                } else {
                    stringResource(Res.string.game_moves_with_count, history.size)
                }
            )
            if (previewPly != null) {
                PillButton(
                    text = stringResource(Res.string.game_back_to_live),
                    onClick = { onSelectPly(null) },
                    tone = PillTone.SOFT,
                    compact = true
                )
            }
        }
        if (rows.isEmpty()) {
            Text(
                text = stringResource(Res.string.game_no_moves_yet),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for ((index, row) in rows.withIndex()) {
                val isLast = index == rows.lastIndex
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.game_move_number, row.number),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLast) scheme.onPrimaryContainer else scheme.outline,
                        modifier = Modifier.width(24.dp)
                    )
                    val whitePly = (row.number - 1) * 2 + 1
                    MoveCell(
                        text = row.white,
                        highlighted = currentPly == whitePly,
                        onClick = { onSelectPly(whitePly) },
                        modifier = Modifier.weight(1f)
                    )
                    MoveCell(
                        text = row.black ?: "…",
                        highlighted = row.black != null && currentPly == whitePly + 1,
                        muted = row.black == null,
                        onClick = if (row.black != null) {
                            { onSelectPly(whitePly + 1) }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveCell(
    text: String,
    highlighted: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    muted: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    if (highlighted) {
        Box(modifier = modifier.then(clickable)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.primaryContainer)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (muted) scheme.outline else scheme.onSurfaceVariant,
            modifier = modifier.then(clickable)
        )
    }
}

private data class MoveRow(val number: Int, val white: String, val black: String?)

private fun sanMoveRows(history: List<String>): List<MoveRow> {
    val san = SanFormatter.sanMoves(history)
    return san.chunked(2).mapIndexed { index, pair ->
        MoveRow(number = index + 1, white = pair.first(), black = pair.getOrNull(1))
    }
}

private fun materialBalance(gameState: GameState): Int {
    var balance = 0
    for (piece in gameState.pieces.values) {
        val value = pieceValues[piece.kind] ?: 0
        balance += if (piece.color == PieceColor.WHITE) value else -value
    }
    return balance
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
                text = stringResource(
                    Res.string.game_result_and_move,
                    reasonLabel(result.reason),
                    (uiState.gameState.uciHistory.size + 1) / 2
                ),
                color = accents.onBand
            )
            Text(
                text = headlineText(result, uiState.myColor, viewModel.isRemote),
                style = MaterialTheme.typography.displayMedium,
                color = scheme.onBackground
            )
            uiState.ratingChange?.let { change ->
                val delta = change.after - change.before
                Text(
                    text = stringResource(
                        Res.string.rating_changed,
                        change.after,
                        if (delta >= 0) "+$delta" else delta.toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (delta >= 0) accents.positive else accents.negative
                )
            }
            if (uiState.rematchOfferIncoming) {
                Text(
                    text = stringResource(
                        Res.string.game_wants_rematch,
                        opponentLabel(uiState)
                    ),
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
                    label = stringResource(Res.string.game_moves_played),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = seriesValue(uiState),
                    label = stringResource(
                        if (viewModel.isRemote) Res.string.game_series_remote else Res.string.game_series_local
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            SectionLabel(stringResource(Res.string.game_the_game), color = accents.bandStrong)
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
                    val pairs = remember(uiState.gameState.uciHistory) {
                        allMovePairs(uiState.gameState.uciHistory)
                    }
                    if (pairs.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.game_no_moves_were_played),
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
                    text = stringResource(Res.string.game_new_game),
                    onClick = viewModel::newGame,
                    modifier = Modifier.weight(1f)
                )

                viewModel.supportsRematch && uiState.rematchOfferIncoming -> PillButton(
                    text = stringResource(Res.string.game_accept_rematch),
                    onClick = viewModel::acceptRematch,
                    modifier = Modifier.weight(1f)
                )

                viewModel.supportsRematch -> PillButton(
                    text = stringResource(
                        if (uiState.rematchOfferOutgoing) {
                            Res.string.game_rematch_offered
                        } else {
                            Res.string.game_rematch
                        }
                    ),
                    onClick = viewModel::offerRematch,
                    enabled = !uiState.rematchOfferOutgoing,
                    modifier = Modifier.weight(1f)
                )

                else -> Spacer(modifier = Modifier.weight(1f))
            }
            if (viewModel.isRemote && uiState.rematchOfferIncoming) {
                PillButton(
                    text = stringResource(Res.string.game_decline),
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
                text = stringResource(Res.string.game_exit),
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

@Composable
private fun opponentLabel(uiState: GameUiState): String {
    uiState.opponentName?.let { return it }
    uiState.computerLevel?.let { return stringResource(Res.string.computer_opponent, it) }
    return stringResource(Res.string.game_opponent_fallback)
}

@Composable
private fun playerName(uiState: GameUiState, sideColor: PieceColor, isMine: Boolean): String {
    if (uiState.myColor == null) {
        return stringResource(
            if (sideColor == PieceColor.WHITE) Res.string.game_side_white else Res.string.game_side_black
        )
    }
    return if (isMine) {
        stringResource(Res.string.game_you)
    } else {
        opponentLabel(uiState)
    }
}

@Composable
private fun statusLabel(uiState: GameUiState, isRemote: Boolean): String = stringResource(
    when {
        isRemote && uiState.myColor == uiState.gameState.sideToMove -> Res.string.game_your_move
        isRemote -> Res.string.game_their_move
        uiState.gameState.sideToMove == PieceColor.WHITE -> Res.string.game_white_to_move
        else -> Res.string.game_black_to_move
    }
)

@Composable
private fun lastMoveLabel(gameState: GameState): String {
    val last = gameState.uciHistory.lastOrNull() ?: return stringResource(Res.string.game_opening_move)
    return stringResource(Res.string.game_move_played, last)
}

@Composable
private fun connectionNote(uiState: GameUiState): String? = when {
    uiState.connectionState is TransportConnectionState.Reconnecting ->
        stringResource(Res.string.game_reconnecting)

    uiState.connectionState is TransportConnectionState.Closed ->
        stringResource(Res.string.game_connection_lost)

    !uiState.opponentConnected && uiState.myColor != null -> stringResource(
        Res.string.game_opponent_reconnecting,
        opponentLabel(uiState)
    )

    else -> null
}

private fun allMovePairs(history: List<String>): List<String> {
    val moves = readableMoves(history)
    val pairs = mutableListOf<String>()
    var index = 0
    while (index < moves.size) {
        val number = index / 2 + 1
        val white = moves[index]
        val black = moves.getOrNull(index + 1)
        pairs.add(if (black != null) "$number. $white  $black" else "$number. $white")
        index += 2
    }
    return pairs
}

@Composable
private fun movePairs(history: List<String>): List<String> {
    if (history.isEmpty()) {
        return listOf(stringResource(Res.string.game_no_moves_yet))
    }
    val moves = remember(history) { readableMoves(history) }
    val pairs = mutableListOf<String>()
    var index = 0
    while (index < moves.size) {
        val number = index / 2 + 1
        val white = moves[index]
        val black = moves.getOrNull(index + 1)
        pairs.add(if (black != null) "$number. $white $black" else "$number. $white")
        index += 2
    }
    return pairs.takeLast(2)
}

private fun readableMoves(history: List<String>): List<String> =
    SanFormatter.sanMoves(history).takeIf { it.size == history.size } ?: history

private fun seriesValue(uiState: GameUiState): String =
    "${uiState.seriesMyWins} · ${uiState.seriesOpponentWins} · ${uiState.seriesDraws}"

@Composable
private fun headlineText(result: GameResult, myColor: PieceColor?, isRemote: Boolean): String = stringResource(
    when {
        result.winner == null -> Res.string.game_result_draw
        isRemote && myColor != null -> if (result.winner == myColor) {
            Res.string.game_result_you_won
        } else {
            Res.string.game_result_you_lost
        }

        result.winner == PieceColor.WHITE -> Res.string.game_result_white_won
        else -> Res.string.game_result_black_won
    }
)

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
    var clockMode by remember { mutableStateOf(ClockMode.FISCHER) }
    AlertDialog(
        onDismissRequest = { onPick(null) },
        title = { Text(stringResource(Res.string.game_clock_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedPills(
                    options = ClockMode.entries.map { option -> clockModeLabel(option) },
                    selectedIndex = ClockMode.entries.indexOf(clockMode),
                    onSelect = { index -> clockMode = ClockMode.entries[index] }
                )
                for (choice in timeControlChoices) {
                    val withMode = choice?.copy(mode = clockMode)
                    PillButton(
                        text = withMode?.label ?: stringResource(Res.string.game_no_clock),
                        onClick = { onPick(withMode) },
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
private fun clockModeLabel(mode: ClockMode): String = stringResource(
    when (mode) {
        ClockMode.FISCHER -> Res.string.clock_mode_fischer
        ClockMode.BRONSTEIN -> Res.string.clock_mode_bronstein
        ClockMode.DELAY -> Res.string.clock_mode_delay
    }
)

@Composable
private fun PromotionDialog(
    color: PieceColor,
    onChoose: (PieceKind) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.game_promote_to), style = MaterialTheme.typography.titleLarge) },
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

private fun capturedPieces(gameState: GameState, capturedFrom: PieceColor): List<PieceKind> {
    val remaining = gameState.pieces.values
        .filter { it.color == capturedFrom }
        .groupingBy { it.kind }
        .eachCount()
    return buildList {
        for ((kind, initial) in initialCounts) {
            repeat(initial - (remaining[kind] ?: 0)) {
                add(kind)
            }
        }
    }.sortedByDescending { pieceValues[it] }
}

@Composable
private fun CapturedRow(
    gameState: GameState,
    capturedFrom: PieceColor,
    modifier: Modifier = Modifier,
    pieceSize: Dp = 17.dp
) {
    val captured = capturedPieces(gameState, capturedFrom)
    if (captured.isEmpty()) {
        Spacer(modifier = modifier.height(pieceSize))
        return
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LocalBoardColors.current.lightSquare)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (kind in captured.take(10)) {
            Image(
                painter = painterResource(pieceDrawable(pieceCode(capturedFrom, kind))),
                contentDescription = null,
                modifier = Modifier.size(pieceSize)
            )
        }
    }
}

@Composable
private fun MaterialChip(gameState: GameState, bottomColor: PieceColor) {
    val balance = materialBalance(gameState)
    val mine = if (bottomColor == PieceColor.WHITE) balance else -balance
    if (mine == 0) {
        return
    }
    val accents = LocalAppAccents.current
    val positive = mine > 0
    Text(
        text = if (positive) "+$mine" else mine.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = if (positive) accents.positive else accents.negative,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
