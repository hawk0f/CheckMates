package dev.hawk0f.checkmates.ui.game

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import dev.hawk0f.checkmates.shared.domain.GameState
import dev.hawk0f.checkmates.shared.domain.Piece
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.PieceKind
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.ui.theme.LocalBoardColors
import org.jetbrains.compose.resources.painterResource

private val selectedTint = Color(0x8020A0F0)
private val lastMoveTint = Color(0x66CDD26A)
private val checkTint = Color(0x80E5605D)
private val legalDot = Color(0x59000000)

private data class TrackedPiece(val id: Int, val piece: Piece, val square: Square)

private class PieceTracker {
    private var nextId = 0
    private var current: List<TrackedPiece> = emptyList()
    private var lastHistorySize = -1

    fun update(state: GameState): List<TrackedPiece> {
        if (state.uciHistory.size < lastHistorySize) {
            current = emptyList()
        }
        lastHistorySize = state.uciHistory.size
        val unmatched = state.pieces.toMutableMap()
        val result = mutableListOf<TrackedPiece>()
        val leftoverOld = mutableListOf<TrackedPiece>()
        for (old in current) {
            if (unmatched[old.square] == old.piece) {
                result.add(old)
                unmatched.remove(old.square)
            } else {
                leftoverOld.add(old)
            }
        }
        state.lastMove?.let { (from, to) ->
            val movedOld = leftoverOld.find { it.square == from }
            val landed = unmatched[to]
            if (movedOld != null && landed != null) {
                result.add(TrackedPiece(movedOld.id, landed, to))
                unmatched.remove(to)
                leftoverOld.remove(movedOld)
            }
        }
        for ((square, piece) in unmatched.toList()) {
            val match = leftoverOld.find { it.piece == piece }
            if (match != null) {
                result.add(TrackedPiece(match.id, piece, square))
                unmatched.remove(square)
                leftoverOld.remove(match)
            }
        }
        for ((square, piece) in unmatched) {
            result.add(TrackedPiece(nextId++, piece, square))
        }
        current = result
        return result
    }
}

val DefaultMaxBoardSize = 520.dp

@Composable
fun BoardBox(
    modifier: Modifier = Modifier,
    maxSize: Dp = DefaultMaxBoardSize,
    board: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val available = if (constraints.hasBoundedHeight) {
            minOf(maxWidth, maxHeight)
        } else {
            maxWidth
        }
        board(Modifier.size(minOf(available, maxSize)))
    }
}

@Composable
fun ChessBoard(
    gameState: GameState,
    selected: Square?,
    legalTargets: Set<Square>,
    flipped: Boolean,
    onSquareTap: (Square) -> Unit,
    modifier: Modifier = Modifier,
    showCoordinates: Boolean = true,
    interactive: Boolean = true
) {
    var boardSizePx by remember { mutableStateOf(0) }
    var dragFrom by remember(gameState.fen) { mutableStateOf<Square?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var droppedOn by remember { mutableStateOf<Square?>(null) }
    val tracker = remember { PieceTracker() }
    val trackedPieces = remember(gameState) { tracker.update(gameState) }

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

    LaunchedEffect(droppedOn) {
        if (droppedOn != null) {
            withFrameNanos { }
            withFrameNanos { }
            droppedOn = null
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { boardSizePx = it.width }
            .then(
                if (!interactive) {
                    Modifier
                } else {
                    Modifier.pointerInput(flipped, gameState.fen) {
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
                                    droppedOn = target
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
                }
            )
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
                            hasPiece = gameState.pieces[square] != null,
                            isSelected = square == selected,
                            isLegalTarget = square in legalTargets,
                            isLastMove = gameState.lastMove?.let { square == it.first || square == it.second } == true,
                            isCheckedKing = gameState.inCheck &&
                                gameState.pieces[square] == Piece(gameState.sideToMove, PieceKind.KING),
                            showFileLabel = showCoordinates && rank == (if (flipped) 7 else 0),
                            showRankLabel = showCoordinates && file == (if (flipped) 7 else 0),
                            onTap = if (interactive) {
                                { onSquareTap(square) }
                            } else {
                                null
                            },
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                    }
                }
            }
        }
        if (boardSizePx > 0) {
            val cell = boardSizePx / 8
            val cellDp = with(LocalDensity.current) { cell.toDp() }
            trackedPieces.forEach { tracked ->
                key(tracked.id) {
                    val column = if (flipped) 7 - tracked.square.file else tracked.square.file
                    val row = if (flipped) tracked.square.rank else 7 - tracked.square.rank
                    val landedInstantly = droppedOn != null && tracked.square == droppedOn
                    val squareOffset = IntOffset(column * cell, row * cell)
                    val animatedOffset by animateIntOffsetAsState(
                        targetValue = squareOffset,
                        animationSpec = if (landedInstantly) snap() else tween(durationMillis = 180)
                    )
                    val renderedOffset = if (landedInstantly) squareOffset else animatedOffset
                    if (tracked.square != dragFrom) {
                        Box(
                            modifier = Modifier
                                .offset { renderedOffset }
                                .size(cellDp),
                            contentAlignment = Alignment.Center
                        ) {
                            PieceGlyph(tracked.piece)
                        }
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
    hasPiece: Boolean,
    isSelected: Boolean,
    isLegalTarget: Boolean,
    isLastMove: Boolean,
    isCheckedKing: Boolean,
    showFileLabel: Boolean,
    showRankLabel: Boolean,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val boardColors = LocalBoardColors.current
    val isDarkSquare = (square.file + square.rank) % 2 == 0
    val plain = if (isDarkSquare) boardColors.darkSquare else boardColors.lightSquare
    val highlight = if (isDarkSquare) boardColors.darkHighlight else boardColors.lightHighlight
    val base = if (isLastMove && highlight != null) highlight else plain
    val labelColor = if (isDarkSquare) boardColors.lightSquare else boardColors.darkSquare
    val overlay = when {
        isSelected -> selectedTint
        isCheckedKing -> checkTint
        isLastMove && highlight == null -> lastMoveTint
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .background(base)
            .background(overlay)
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
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
        if (isLegalTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize(if (hasPiece) 0.85f else 0.3f)
                    .clip(CircleShape)
                    .background(if (hasPiece) Color.Transparent else legalDot)
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
