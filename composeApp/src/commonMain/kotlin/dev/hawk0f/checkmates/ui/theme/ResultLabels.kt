package dev.hawk0f.checkmates.ui.theme

import androidx.compose.runtime.Composable
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.reason_checkmate
import dev.hawk0f.checkmates.resources.reason_disconnection
import dev.hawk0f.checkmates.resources.reason_draw_agreed
import dev.hawk0f.checkmates.resources.reason_fifty_move
import dev.hawk0f.checkmates.resources.reason_insufficient_material
import dev.hawk0f.checkmates.resources.reason_repetition
import dev.hawk0f.checkmates.resources.reason_resignation
import dev.hawk0f.checkmates.resources.reason_stalemate
import dev.hawk0f.checkmates.resources.reason_timeout
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun reasonResource(reason: GameOverReason): StringResource = when (reason) {
    GameOverReason.CHECKMATE -> Res.string.reason_checkmate
    GameOverReason.STALEMATE -> Res.string.reason_stalemate
    GameOverReason.DRAW_AGREED -> Res.string.reason_draw_agreed
    GameOverReason.RESIGNATION -> Res.string.reason_resignation
    GameOverReason.INSUFFICIENT_MATERIAL -> Res.string.reason_insufficient_material
    GameOverReason.REPETITION -> Res.string.reason_repetition
    GameOverReason.FIFTY_MOVE -> Res.string.reason_fifty_move
    GameOverReason.TIMEOUT -> Res.string.reason_timeout
    GameOverReason.DISCONNECTION -> Res.string.reason_disconnection
}

@Composable
fun reasonLabel(reason: GameOverReason): String = stringResource(reasonResource(reason))
