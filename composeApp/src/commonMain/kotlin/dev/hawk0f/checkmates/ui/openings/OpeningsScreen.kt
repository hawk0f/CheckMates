package dev.hawk0f.checkmates.ui.openings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.openings_black_lines
import dev.hawk0f.checkmates.resources.openings_line_moves
import dev.hawk0f.checkmates.resources.openings_title
import dev.hawk0f.checkmates.resources.openings_white_lines
import dev.hawk0f.checkmates.session.OpeningProgressStore
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.opening.OpeningBook
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpeningsScreen(onOpenLine: (String) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.openings_title), style = MaterialTheme.typography.displaySmall)
            CircleButton(onClick = onBack, contentDescription = stringResource(Res.string.a11y_back)) {
                ChevronIcon(
                    direction = ChevronDirection.LEFT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            for (color in listOf(PieceColor.WHITE, PieceColor.BLACK)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(
                        stringResource(
                            if (color == PieceColor.WHITE) {
                                Res.string.openings_white_lines
                            } else {
                                Res.string.openings_black_lines
                            }
                        )
                    )
                    for (line in OpeningBook.forColor(color)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLine(line.id) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(line.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(
                                    Res.string.openings_line_moves,
                                    (line.plies + 1) / 2,
                                    OpeningProgressStore.bestStreak(line.id)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
