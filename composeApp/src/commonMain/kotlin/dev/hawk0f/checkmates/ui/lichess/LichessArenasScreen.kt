package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import dev.hawk0f.checkmates.net.lichess.LichessTournament
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.CloseIcon
import dev.hawk0f.checkmates.ui.theme.CodeChip
import dev.hawk0f.checkmates.ui.theme.Hairline
import dev.hawk0f.checkmates.ui.theme.InitialsBadge
import dev.hawk0f.checkmates.ui.theme.ListRow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel

@Composable
fun LichessArenasScreen(
    onBack: () -> Unit,
    viewModel: LichessArenasViewModel = viewModel { LichessArenasViewModel() }
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
            Text("Tournaments", style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack) {
                CloseIcon(color = scheme.onSurfaceVariant)
            }
        }

        if (uiState.loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = scheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.featured?.let { arena ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(accents.band)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                SectionLabel(
                                    text = arena.secondsToFinish?.let { "Started · ${it / 60} min left" }
                                        ?: "Started",
                                    color = accents.bandStrong
                                )
                                Text(
                                    text = arena.fullName ?: arena.id,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = clockLabel(arena) + " · ${arena.nbPlayers} players",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            uiState.myRank?.let { CodeChip("#$it") }
                        }
                        PillButton(
                            text = if (uiState.joined.contains(arena.id)) "Joined" else "Join arena",
                            onClick = { viewModel.join(arena.id) },
                            tone = PillTone.INK,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (uiState.standing.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Standing", color = accents.bandStrong)
                        for (player in uiState.standing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = player.rank.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = player.score.toString() +
                                        if (player.sheet?.fire == true) " · x2" else "",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }

                if (uiState.starting.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel("Starting soon", color = accents.bandStrong)
                        for (arena in uiState.starting) {
                            ListRow(
                                title = arena.fullName ?: arena.id,
                                subtitle = startsLabel(arena),
                                leading = { InitialsBadge(text = clockLabel(arena)) },
                                trailing = {
                                    PillButton(
                                        text = if (uiState.joined.contains(arena.id)) "In" else "Join",
                                        onClick = { viewModel.join(arena.id) },
                                        tone = PillTone.SOFT,
                                        compact = true
                                    )
                                }
                            )
                            Hairline()
                        }
                    }
                }

                if (uiState.teams.isNotEmpty()) {
                    ListRow(
                        title = "Your teams · ${uiState.teams.size}",
                        subtitle = uiState.teams.take(2).mapNotNull { it.name }.joinToString(", "),
                        leading = { InitialsBadge(text = "TM") }
                    )
                }
            }
        }
    }

    uiState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text("Tournaments", style = MaterialTheme.typography.titleLarge) },
            text = { Text(message) },
            confirmButton = {
                PillButton(text = "OK", onClick = viewModel::dismissMessage, compact = true)
            }
        )
    }
}

private fun clockLabel(arena: LichessTournament): String {
    val limit = arena.clock?.limit ?: 0
    val increment = arena.clock?.increment ?: 0
    return "${limit / 60}+$increment"
}

private fun startsLabel(arena: LichessTournament): String {
    val starts = arena.secondsToStart?.let { "in ${it / 60} min" } ?: "soon"
    val stake = if (arena.rated) "rated" else "casual"
    val length = arena.minutes?.let { "$it min" }.orEmpty()
    return listOf(starts, stake, length).filter { it.isNotBlank() }.joinToString(" · ")
}
