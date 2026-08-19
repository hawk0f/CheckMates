package dev.hawk0f.chess.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import dev.hawk0f.chess.ui.theme.LocalBoardColors

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
    var boardSizePx by remember { mutableStateOf(0) }
    var dragFrom by remember(gameState.fen) { mutableStateOf<Square?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    fun squareAt(position: Offset): Square? {
        if (boardSizePx == 0) {
            return null
        }
        val cell = boardSizePx / 8f
        val column = (position.x / cell).toInt().coerceIn(0, 7)
        val row = (position.y / cell).toInt().coerceIn(0, 7)
        val file = if (flipped) 7 - column else column
        val rank = if (flipped) row else 7 - row
        return Square.of(file, rank)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { boardSizePx = it.width }
            .pointerInput(flipped, gameState.fen) {
                detectDragGestures(
                    onDragStart = { position ->
                        squareAt(position)?.let { square ->
                            if (gameState.pieces[square] != null) {
                                dragFrom = square
                                dragPosition = position
                                onSquareTap(square)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragPosition += dragAmount
                    },
                    onDragEnd = {
                        val from = dragFrom
                        dragFrom = null
                        if (from != null) {
                            squareAt(dragPosition)?.let { target ->
                                if (target != from) {
                                    onSquareTap(target)
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        dragFrom = null
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val ranks = if (flipped) 0..7 else 7 downTo 0
            for (rank in ranks) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val files = if (flipped) 7 downTo 0 else 0..7
                    for (file in files) {
                        val square = Square.of(file, rank)
                        BoardCell(
                            square = square,
                            piece = gameState.pieces[square].takeIf { square != dragFrom },
                            isSelected = square == selected,
                            isLegalTarget = square in legalTargets,
                            isLastMove = gameState.lastMove?.let { square == it.first || square == it.second } == true,
                            isCheckedKing = gameState.inCheck &&
                                gameState.pieces[square] == Piece(gameState.sideToMove, PieceKind.KING),
                            showFileLabel = rank == (if (flipped) 7 else 0),
                            showRankLabel = file == (if (flipped) 7 else 0),
                            onTap = { onSquareTap(square) },
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                    }
                }
            }
        }
        dragFrom?.let { from ->
            gameState.pieces[from]?.let { piece ->
                val cell = boardSizePx / 8
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragPosition.x - cell / 2).toInt(),
                                (dragPosition.y - cell).toInt()
                            )
                        }
                        .size(with(LocalDensity.current) { cell.toDp() })
                ) {
                    PieceGlyph(piece)
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
    showFileLabel: Boolean,
    showRankLabel: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boardColors = LocalBoardColors.current
    val base = if ((square.file + square.rank) % 2 == 0) boardColors.darkSquare else boardColors.lightSquare
    val labelColor = if ((square.file + square.rank) % 2 == 0) boardColors.lightSquare else boardColors.darkSquare
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
        if (showRankLabel) {
            Text(
                text = "${square.rank + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.align(Alignment.TopStart).padding(2.dp)
            )
        }
        if (showFileLabel) {
            Text(
                text = "${'a' + square.file}",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
            )
        }
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
