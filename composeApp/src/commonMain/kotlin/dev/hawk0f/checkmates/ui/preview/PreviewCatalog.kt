package dev.hawk0f.checkmates.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.ui.theme.AppTheme

internal class PreviewSpec(
    val id: String,
    val width: Dp = 360.dp,
    val capturable: Boolean = true,
    val content: @Composable () -> Unit
)

internal val previewSpecs: List<PreviewSpec> = componentPreviewSpecs + dialogPreviewSpecs + boardPreviewSpecs

internal fun previewState(vararg moves: String): GameState = ChessGame().apply {
    for (uci in moves) {
        applyUci(uci)
    }
}.state()

@Composable
internal fun PreviewFrame(spec: PreviewSpec) {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(spec.width)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                spec.content()
            }
        }
    }
}
