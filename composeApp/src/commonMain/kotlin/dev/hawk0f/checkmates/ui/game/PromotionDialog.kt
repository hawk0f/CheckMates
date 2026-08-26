package dev.hawk0f.checkmates.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.game_promote_to
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.ui.profile.pieceDrawable
import dev.hawk0f.checkmates.ui.theme.SoftCard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val promotionChoices = listOf(
    PieceKind.QUEEN,
    PieceKind.ROOK,
    PieceKind.BISHOP,
    PieceKind.KNIGHT
)

internal fun promotionLetter(kind: PieceKind): String? = when (kind) {
    PieceKind.QUEEN -> "q"
    PieceKind.ROOK -> "r"
    PieceKind.BISHOP -> "b"
    PieceKind.KNIGHT -> "n"
    else -> null
}

@Composable
internal fun PromotionDialog(
    color: PieceColor,
    onChoose: (PieceKind) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.game_promote_to), style = MaterialTheme.typography.titleLarge) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (kind in promotionChoices) {
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
