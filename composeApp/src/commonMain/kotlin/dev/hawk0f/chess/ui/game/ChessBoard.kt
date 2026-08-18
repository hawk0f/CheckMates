package dev.hawk0f.chess.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import dev.hawk0f.chess.resources.Res
import dev.hawk0f.chess.resources.piece_bb
import dev.hawk0f.chess.resources.piece_bk
import dev.hawk0f.chess.resources.piece_bn
import dev.hawk0f.chess.resources.piece_bp
import dev.hawk0f.chess.resources.piece_bq
import dev.hawk0f.chess.resources.piece_br
import dev.hawk0f.chess.resources.piece_wb
import dev.hawk0f.chess.resources.piece_wk
import dev.hawk0f.chess.resources.piece_wn
import dev.hawk0f.chess.resources.piece_wp
import dev.hawk0f.chess.resources.piece_wq
import dev.hawk0f.chess.resources.piece_wr
import dev.hawk0f.chess.shared.domain.GameState
import org.jetbrains.compose.resources.painterResource
import dev.hawk0f.chess.shared.domain.Piece
import dev.hawk0f.chess.shared.domain.PieceColor
import dev.hawk0f.chess.shared.domain.PieceKind
import dev.hawk0f.chess.shared.domain.Square

private val lightSquare = Color(0xFFF0D9B5)
private val darkSquare = Color(0xFFB58863)
private val selectedTint = Color(0x8020A0F0)
private val lastMoveTint = Color(0x66CDD26A)
private val checkTint = Color(0x80E5605D)
private val legalDot = Color(0x59000000)

@Composable
fun ChessBoard(
    gameState: GameState,
    selected: Square?,
    legalTargets: Set<Square>,
    flipped: Boolean,
    onSquareTap: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.aspectRatio(1f)) {
        val ranks = if (flipped) 0..7 else 7 downTo 0
        for (rank in ranks) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val files = if (flipped) 7 downTo 0 else 0..7
                for (file in files) {
                    val square = Square.of(file, rank)
                    BoardCell(
                        square = square,
                        piece = gameState.pieces[square],
                        isSelected = square == selected,
                        isLegalTarget = square in legalTargets,
                        isLastMove = gameState.lastMove?.let { square == it.first || square == it.second } == true,
                        isCheckedKing = gameState.inCheck &&
                            gameState.pieces[square] == Piece(gameState.sideToMove, PieceKind.KING),
                        onTap = { onSquareTap(square) },
                        modifier = Modifier.weight(1f).fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    square: Square,
    piece: Piece?,
    isSelected: Boolean,
    isLegalTarget: Boolean,
    isLastMove: Boolean,
    isCheckedKing: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val base = if ((square.file + square.rank) % 2 == 0) darkSquare else lightSquare
    val overlay = when {
        isSelected -> selectedTint
        isCheckedKing -> checkTint
        isLastMove -> lastMoveTint
        else -> Color.Transparent
    }
    Box(
        modifier = modifier.background(base).background(overlay).clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (piece != null) {
            PieceGlyph(piece)
        }
        if (isLegalTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize(if (piece == null) 0.3f else 0.85f)
                    .clip(CircleShape)
                    .background(if (piece == null) legalDot else Color.Transparent)
            )
        }
    }
}

@Composable
private fun PieceGlyph(piece: Piece) {
    val resource = when (piece.color to piece.kind) {
        PieceColor.WHITE to PieceKind.KING -> Res.drawable.piece_wk
        PieceColor.WHITE to PieceKind.QUEEN -> Res.drawable.piece_wq
        PieceColor.WHITE to PieceKind.ROOK -> Res.drawable.piece_wr
        PieceColor.WHITE to PieceKind.BISHOP -> Res.drawable.piece_wb
        PieceColor.WHITE to PieceKind.KNIGHT -> Res.drawable.piece_wn
        PieceColor.WHITE to PieceKind.PAWN -> Res.drawable.piece_wp
        PieceColor.BLACK to PieceKind.KING -> Res.drawable.piece_bk
        PieceColor.BLACK to PieceKind.QUEEN -> Res.drawable.piece_bq
        PieceColor.BLACK to PieceKind.ROOK -> Res.drawable.piece_br
        PieceColor.BLACK to PieceKind.BISHOP -> Res.drawable.piece_bb
        PieceColor.BLACK to PieceKind.KNIGHT -> Res.drawable.piece_bn
        else -> Res.drawable.piece_bp
    }
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(0.92f)
    )
}
