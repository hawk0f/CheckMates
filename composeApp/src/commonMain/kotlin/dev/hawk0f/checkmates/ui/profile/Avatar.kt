package dev.hawk0f.checkmates.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.piece_bb
import dev.hawk0f.checkmates.resources.piece_bk
import dev.hawk0f.checkmates.resources.piece_bn
import dev.hawk0f.checkmates.resources.piece_bp
import dev.hawk0f.checkmates.resources.piece_bq
import dev.hawk0f.checkmates.resources.piece_br
import dev.hawk0f.checkmates.resources.piece_wb
import dev.hawk0f.checkmates.resources.piece_wk
import dev.hawk0f.checkmates.resources.piece_wn
import dev.hawk0f.checkmates.resources.piece_wp
import dev.hawk0f.checkmates.resources.piece_wq
import dev.hawk0f.checkmates.resources.piece_wr
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val pieceAvatarCodes = listOf("wk", "wq", "wr", "wb", "wn", "wp", "bk", "bq", "br", "bb", "bn", "bp")

val emojiAvatarChoices = listOf(
    "😎", "🤠", "🦅", "🦊", "🐺", "🦁", "🐯", "🐸",
    "🐙", "🦉", "🐴", "🦄", "🐲", "👑", "⚡", "🔥",
    "🌟", "🎯", "🚀", "🧊", "🃏", "🎩", "🧠", "💣"
)

fun pieceDrawable(code: String): DrawableResource = when (code) {
    "wk" -> Res.drawable.piece_wk
    "wq" -> Res.drawable.piece_wq
    "wr" -> Res.drawable.piece_wr
    "wb" -> Res.drawable.piece_wb
    "wn" -> Res.drawable.piece_wn
    "wp" -> Res.drawable.piece_wp
    "bk" -> Res.drawable.piece_bk
    "bq" -> Res.drawable.piece_bq
    "br" -> Res.drawable.piece_br
    "bb" -> Res.drawable.piece_bb
    "bn" -> Res.drawable.piece_bn
    else -> Res.drawable.piece_bp
}

@Composable
fun AvatarBadge(kind: String, value: String, size: Dp, fontSize: TextUnit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (kind == "emoji") {
            Text(text = value, fontSize = fontSize)
        } else {
            Image(
                painter = painterResource(pieceDrawable(value)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.72f)
            )
        }
    }
}
