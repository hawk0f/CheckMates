package dev.hawk0f.checkmates.ui.computer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.a11y_back
import dev.hawk0f.checkmates.resources.computer_level
import dev.hawk0f.checkmates.resources.computer_level_label
import dev.hawk0f.checkmates.resources.computer_side_black
import dev.hawk0f.checkmates.resources.computer_side_label
import dev.hawk0f.checkmates.resources.computer_side_random
import dev.hawk0f.checkmates.resources.computer_side_white
import dev.hawk0f.checkmates.resources.computer_start
import dev.hawk0f.checkmates.resources.computer_title
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.engine.EngineLevel
import dev.hawk0f.checkmates.ui.theme.ChevronDirection
import dev.hawk0f.checkmates.ui.theme.ChevronIcon
import dev.hawk0f.checkmates.ui.theme.CircleButton
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.ui.theme.SelectPill
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource

@Composable
fun ComputerSetupScreen(
    onStart: (EngineLevel, PieceColor) -> Unit,
    onBack: () -> Unit
) {
    var level by remember { mutableStateOf(EngineLevel.DEFAULT) }
    var side by remember { mutableStateOf<PieceColor?>(PieceColor.WHITE) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 20.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(stringResource(Res.string.computer_title), style = MaterialTheme.typography.displaySmall)
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
                .padding(horizontal = 26.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.computer_level_label))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    for (option in EngineLevel.entries) {
                        SelectPill(
                            text = option.id.toString(),
                            selected = option == level,
                            onClick = { level = option }
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.computer_level, level.id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionLabel(stringResource(Res.string.computer_side_label))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SelectPill(
                        text = stringResource(Res.string.computer_side_white),
                        selected = side == PieceColor.WHITE,
                        onClick = { side = PieceColor.WHITE }
                    )
                    SelectPill(
                        text = stringResource(Res.string.computer_side_black),
                        selected = side == PieceColor.BLACK,
                        onClick = { side = PieceColor.BLACK }
                    )
                    SelectPill(
                        text = stringResource(Res.string.computer_side_random),
                        selected = side == null,
                        onClick = { side = null }
                    )
                }
            }

            PillButton(
                text = stringResource(Res.string.computer_start),
                onClick = {
                    val color = side ?: if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
                    onStart(level, color)
                },
                tone = PillTone.ACCENT,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
