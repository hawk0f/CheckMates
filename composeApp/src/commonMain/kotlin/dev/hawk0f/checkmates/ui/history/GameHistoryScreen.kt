package dev.hawk0f.checkmates.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hawk0f.checkmates.platform.formatDate
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.history_all
import dev.hawk0f.checkmates.resources.history_draws
import dev.hawk0f.checkmates.resources.history_empty
import dev.hawk0f.checkmates.resources.history_local
import dev.hawk0f.checkmates.resources.history_losses
import dev.hawk0f.checkmates.resources.history_average_moves
import dev.hawk0f.checkmates.resources.history_games
import dev.hawk0f.checkmates.resources.history_top_opening
import dev.hawk0f.checkmates.resources.history_unknown_opening
import dev.hawk0f.checkmates.resources.history_win_rate
import dev.hawk0f.checkmates.resources.history_remote
import dev.hawk0f.checkmates.resources.history_search
import dev.hawk0f.checkmates.resources.history_title
import dev.hawk0f.checkmates.resources.history_wins
import dev.hawk0f.checkmates.resources.home_computer
import dev.hawk0f.checkmates.resources.mode_nearby
import dev.hawk0f.checkmates.resources.mode_online
import dev.hawk0f.checkmates.resources.mode_pass_and_play
import dev.hawk0f.checkmates.resources.profile_game_summary
import dev.hawk0f.checkmates.resources.profile_players
import dev.hawk0f.checkmates.session.AuthManager
import dev.hawk0f.checkmates.session.LocalGameHistoryStore
import dev.hawk0f.checkmates.session.OpeningProgressStore
import dev.hawk0f.checkmates.session.mergeGameHistory
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.opening.OpeningBook
import dev.hawk0f.checkmates.shared.opening.OpeningLine
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.SelectPill
import dev.hawk0f.checkmates.ui.theme.SoftTextField
import dev.hawk0f.checkmates.ui.theme.StatTile
import dev.hawk0f.checkmates.ui.theme.reasonLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

enum class HistoryModeFilter {
    ALL,
    LOCAL,
    REMOTE
}

enum class HistoryResultFilter {
    ALL,
    WINS,
    DRAWS,
    LOSSES
}

data class GameHistoryUiState(
    val games: List<GameHistoryItem> = emptyList(),
    val loading: Boolean = false
)

data class GameHistoryStats(
    val totalGames: Int,
    val personalGames: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winRate: Int,
    val averageMoves: Int,
    val topOpening: String?
)

class GameHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameHistoryUiState(LocalGameHistoryStore.games()))
    val uiState: StateFlow<GameHistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val local = LocalGameHistoryStore.games()
        val token = AuthManager.token
        _uiState.value = GameHistoryUiState(local, loading = token != null)
        if (token == null) {
            return
        }
        viewModelScope.launch {
            val remote = runCatching { AuthManager.api.gamesHistory(token).games }.getOrNull()
            _uiState.value = GameHistoryUiState(
                games = if (remote == null) local else mergeGameHistory(remote, LocalGameHistoryStore.games()),
                loading = false
            )
        }
    }
}

fun filterGameHistory(
    games: List<GameHistoryItem>,
    query: String,
    mode: HistoryModeFilter,
    result: HistoryResultFilter
): List<GameHistoryItem> {
    val normalizedQuery = query.trim()
    return games.filter { game ->
        val matchesQuery = normalizedQuery.isEmpty() ||
            game.whiteName.contains(normalizedQuery, ignoreCase = true) ||
            game.blackName.contains(normalizedQuery, ignoreCase = true)
        val matchesMode = when (mode) {
            HistoryModeFilter.ALL -> true
            HistoryModeFilter.LOCAL -> game.mode == "hotseat" || game.mode == "computer"
            HistoryModeFilter.REMOTE -> game.mode == "online" || game.mode == "ble"
        }
        val matchesResult = when (result) {
            HistoryResultFilter.ALL -> true
            HistoryResultFilter.WINS -> game.myColor != null && game.winner == game.myColor
            HistoryResultFilter.DRAWS -> game.winner == null
            HistoryResultFilter.LOSSES -> game.myColor != null && game.winner != null && game.winner != game.myColor
        }
        matchesQuery && matchesMode && matchesResult
    }
}

fun calculateGameHistoryStats(
    games: List<GameHistoryItem>,
    additionalOpenings: List<OpeningLine> = emptyList()
): GameHistoryStats {
    val personal = games.filter { it.myColor != null }
    val wins = personal.count { it.winner == it.myColor }
    val draws = personal.count { it.winner == null }
    val losses = personal.size - wins - draws
    val averageMoves = if (games.isEmpty()) {
        0
    } else {
        games.map { (it.uciHistory.size + 1) / 2.0 }.average().roundToInt()
    }
    val topOpening = games
        .asSequence()
        .filter { it.startFen == null }
        .mapNotNull { OpeningBook.identify(it.uciHistory, additionalOpenings)?.name }
        .groupingBy { it }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key })
        ?.key
    return GameHistoryStats(
        totalGames = games.size,
        personalGames = personal.size,
        wins = wins,
        draws = draws,
        losses = losses,
        winRate = if (personal.isEmpty()) 0 else (wins * 100.0 / personal.size).roundToInt(),
        averageMoves = averageMoves,
        topOpening = topOpening
    )
}

@Composable
fun GameHistoryScreen(
    onOpenReplay: (GameHistoryItem) -> Unit,
    onBack: () -> Unit,
    viewModel: GameHistoryViewModel = viewModel { GameHistoryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(HistoryModeFilter.ALL) }
    var result by remember { mutableStateOf(HistoryResultFilter.ALL) }
    val visibleGames = filterGameHistory(uiState.games, query, mode, result)
    val customOpenings = remember { OpeningProgressStore.customLines() }
    val stats = calculateGameHistoryStats(visibleGames, customOpenings)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.history_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(ChevronDirection.LEFT, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                placeholder = stringResource(Res.string.history_search),
                modifier = Modifier.fillMaxWidth()
            )
            FilterRow(
                labels = listOf(
                    HistoryModeFilter.ALL to stringResource(Res.string.history_all),
                    HistoryModeFilter.LOCAL to stringResource(Res.string.history_local),
                    HistoryModeFilter.REMOTE to stringResource(Res.string.history_remote)
                ),
                selected = mode,
                onSelect = { mode = it }
            )
            FilterRow(
                labels = listOf(
                    HistoryResultFilter.ALL to stringResource(Res.string.history_all),
                    HistoryResultFilter.WINS to stringResource(Res.string.history_wins),
                    HistoryResultFilter.DRAWS to stringResource(Res.string.history_draws),
                    HistoryResultFilter.LOSSES to stringResource(Res.string.history_losses)
                ),
                selected = result,
                onSelect = { result = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    value = stats.totalGames.toString(),
                    label = stringResource(Res.string.history_games),
                    modifier = Modifier.weight(1f),
                    accent = true
                )
                StatTile(
                    value = "${stats.winRate}%",
                    label = stringResource(Res.string.history_win_rate),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = stats.averageMoves.toString(),
                    label = stringResource(Res.string.history_average_moves),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(
                    Res.string.history_top_opening,
                    stats.topOpening ?: stringResource(Res.string.history_unknown_opening)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            uiState.loading && uiState.games.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            visibleGames.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 26.dp)
            ) {
                items(visibleGames, key = { it.id }) { game ->
                    GameHistoryRow(game, onOpenReplay)
                    Hairline()
                }
            }
        }
    }
}

@Composable
private fun <T> FilterRow(labels: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((value, label) in labels) {
            SelectPill(label, value == selected, { onSelect(value) })
        }
    }
}

@Composable
private fun GameHistoryRow(game: GameHistoryItem, onOpenReplay: (GameHistoryItem) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val sharedBoard = game.myColor == null
    val drawn = game.winner == null
    val won = game.myColor != null && game.winner == game.myColor
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenReplay(game) }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    when {
                        sharedBoard || drawn -> scheme.surfaceVariant
                        won -> LocalAppAccents.current.bandStrong
                        else -> scheme.primary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    sharedBoard && game.winner == PieceColor.WHITE -> "1–0"
                    sharedBoard && game.winner == PieceColor.BLACK -> "0–1"
                    drawn -> "D"
                    won -> "W"
                    else -> "L"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (sharedBoard || drawn) scheme.onSurfaceVariant else scheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.profile_players, game.whiteName, game.blackName),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(
                    Res.string.profile_game_summary,
                    historyModeLabel(game.mode),
                    reasonLabel(game.reason),
                    game.uciHistory.size
                ),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Text(formatDate(game.finishedAtMillis), style = MaterialTheme.typography.bodySmall, color = scheme.outline)
    }
}

@Composable
private fun historyModeLabel(mode: String): String = when (mode) {
    "online" -> stringResource(Res.string.mode_online)
    "ble" -> stringResource(Res.string.mode_nearby)
    "hotseat" -> stringResource(Res.string.mode_pass_and_play)
    "computer" -> stringResource(Res.string.home_computer)
    else -> mode
}
