package dev.hawk0f.chess.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.hawk0f.chess.shared.domain.GameState
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
    val glyph = when (piece.kind) {
        PieceKind.KING -> "♚"
        PieceKind.QUEEN -> "♛"
        PieceKind.ROOK -> "♜"
        PieceKind.BISHOP -> "♝"
        PieceKind.KNIGHT -> "♞"
        PieceKind.PAWN -> "♟"
    }
    val fill = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF202020)
    val outline = if (piece.color == PieceColor.WHITE) Color(0xFF202020) else Color(0xFF757575)
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = glyph,
            style = TextStyle(
                color = fill,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                shadow = androidx.compose.ui.graphics.Shadow(color = outline, blurRadius = 2f)
            )
        )
    }
}
