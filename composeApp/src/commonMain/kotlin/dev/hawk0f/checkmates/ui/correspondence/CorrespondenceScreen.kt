package dev.hawk0f.checkmates.ui.correspondence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.common_ok
import dev.hawk0f.checkmates.resources.correspondence_empty
import dev.hawk0f.checkmates.resources.correspondence_finished
import dev.hawk0f.checkmates.resources.correspondence_new
import dev.hawk0f.checkmates.resources.correspondence_moves
import dev.hawk0f.checkmates.resources.correspondence_plies
import dev.hawk0f.checkmates.resources.correspondence_refresh
import dev.hawk0f.checkmates.resources.correspondence_their_turn
import dev.hawk0f.checkmates.resources.correspondence_title
import dev.hawk0f.checkmates.resources.correspondence_your_turn
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.SanFormatter
import dev.hawk0f.checkmates.shared.protocol.CorrespondenceGame
import dev.hawk0f.checkmates.ui.game.BoardBox
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.game.PromotionDialog
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CorrespondenceScreen(
    initialGameId: Long? = null,
    onBack: () -> Unit,
    viewModel: CorrespondenceViewModel = viewModel { CorrespondenceViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.selectedGame
    LaunchedEffect(initialGameId, state.games) {
        if (selected == null && initialGameId != null) {
            state.games.find { it.id == initialGameId }?.let(viewModel::open)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.correspondence_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(
                onClick = if (selected == null) onBack else viewModel::closeGame,
                contentDescription = stringResource(Res.string.a11y_back)
            ) {
                ChevronIcon(ChevronDirection.LEFT, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected != null) {
            GameContent(selected, state, viewModel)
        } else if (state.loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionLabel(stringResource(Res.string.correspondence_title))
                if (state.games.isEmpty()) {
                    Text(stringResource(Res.string.correspondence_empty))
                }
                state.games.forEach { game ->
                    CorrespondenceRow(game, Modifier.clickable { viewModel.open(game) })
                }
                SectionLabel(stringResource(Res.string.correspondence_new))
                state.friends.forEach { friend ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                        PillButton(
                            text = stringResource(Res.string.correspondence_new),
                            onClick = { viewModel.create(friend) },
                            enabled = !state.working,
                            compact = true,
                            tone = PillTone.SOFT
                        )
                    }
                }
            }
        }
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(Res.string.correspondence_title)) },
            text = { Text(error) },
            confirmButton = {
                PillButton(stringResource(Res.string.common_ok), viewModel::dismissError, compact = true)
            }
        )
    }
    state.pendingPromotion?.let {
        PromotionDialog(
            color = state.board.sideToMove,
            onChoose = viewModel::choosePromotion,
            onDismiss = viewModel::dismissPromotion
        )
    }
}

@Composable
private fun GameContent(game: CorrespondenceGame, state: CorrespondenceUiState, viewModel: CorrespondenceViewModel) {
    val myId = AuthManager.profile.value?.id
    val myColor = if (myId == game.whiteUserId) PieceColor.WHITE else PieceColor.BLACK
    val status = when {
        game.reason != null -> stringResource(Res.string.correspondence_finished)
        game.sideToMove == myColor -> stringResource(Res.string.correspondence_your_turn)
        else -> stringResource(Res.string.correspondence_their_turn)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CorrespondenceRow(game)
        Text(status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        BoardBox(Modifier.fillMaxWidth()) { boardModifier ->
            ChessBoard(
                gameState = state.board,
                selected = state.selectedSquare,
                legalTargets = state.legalTargets,
                flipped = myColor == PieceColor.BLACK,
                onSquareTap = viewModel::onSquareTap,
                interactive = game.reason == null && game.sideToMove == myColor && !state.working,
                modifier = boardModifier
            )
        }
        if (game.uciHistory.isNotEmpty()) {
            SectionLabel(stringResource(Res.string.correspondence_moves))
            Text(
                text = SanFormatter.sanMoves(game.uciHistory).mapIndexed { index, move ->
                    if (index % 2 == 0) "${index / 2 + 1}. $move" else move
                }.joinToString("  "),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        PillButton(
            text = stringResource(Res.string.correspondence_refresh),
            onClick = viewModel::refresh,
            enabled = !state.working,
            modifier = Modifier.fillMaxWidth(),
            tone = PillTone.SOFT
        )
    }
}

@Composable
private fun CorrespondenceRow(game: CorrespondenceGame, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("${game.whiteName} — ${game.blackName}", style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.correspondence_plies, game.uciHistory.size), style = MaterialTheme.typography.bodySmall)
    }
}
