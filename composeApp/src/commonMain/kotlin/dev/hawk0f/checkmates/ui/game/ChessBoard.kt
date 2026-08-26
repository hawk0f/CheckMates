package dev.hawk0f.checkmates.ui.game

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.hawk0f.checkmates.resources.a11y_black_bishop
import dev.hawk0f.checkmates.resources.a11y_black_king
import dev.hawk0f.checkmates.resources.a11y_black_knight
import dev.hawk0f.checkmates.resources.a11y_black_pawn
import dev.hawk0f.checkmates.resources.a11y_black_queen
import dev.hawk0f.checkmates.resources.a11y_black_rook
import dev.hawk0f.checkmates.resources.a11y_square_empty
import dev.hawk0f.checkmates.resources.a11y_square_with_piece
import dev.hawk0f.checkmates.resources.a11y_state_king_in_check
import dev.hawk0f.checkmates.resources.a11y_state_last_move
import dev.hawk0f.checkmates.resources.a11y_state_legal_move
import dev.hawk0f.checkmates.resources.a11y_state_selected
import dev.hawk0f.checkmates.resources.a11y_white_bishop
import dev.hawk0f.checkmates.resources.a11y_white_king
import dev.hawk0f.checkmates.resources.a11y_white_knight
import dev.hawk0f.checkmates.resources.a11y_white_pawn
import dev.hawk0f.checkmates.resources.a11y_white_queen
import dev.hawk0f.checkmates.resources.a11y_white_rook
import org.jetbrains.compose.resources.stringResource
import dev.hawk0f.checkmates.resources.a11y_state_premove

private val selectedTint = Color(0x8020A0F0)
private val lastMoveTint = Color(0x73E8B54A)
private val checkTint = Color(0x80E5605D)
private val premoveTint = Color(0x736C63C8)
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
    interactive: Boolean = true,
    premoveSquares: Set<Square> = emptySet(),
    rotatedColor: PieceColor? = null,
    rotateAllPieces: Boolean = false
) {
    var boardSizePx by remember { mutableStateOf(0) }
    var dragFrom by remember(gameState.fen) { mutableStateOf<Square?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var droppedOn by remember { mutableStateOf<Square?>(null) }
    val selectedSquare = rememberUpdatedState(selected)
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
                                        if (square != selectedSquare.value) {
                                            onSquareTap(square)
                                        }
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
                            piece = gameState.pieces[square],
                            isSelected = square == selected,
                            isLegalTarget = square in legalTargets,
                            isLastMove = gameState.lastMove?.let { square == it.first || square == it.second } == true,
                            isPremove = square in premoveSquares,
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
                    val animatedOffset = animateIntOffsetAsState(
                        targetValue = squareOffset,
                        animationSpec = if (landedInstantly) snap() else tween(durationMillis = 180)
                    )
                    if (tracked.square != dragFrom) {
                        Box(
                            modifier = Modifier
                                .offset { if (landedInstantly) squareOffset else animatedOffset.value }
                                .size(cellDp),
                            contentAlignment = Alignment.Center
                        ) {
                            PieceGlyph(
                                tracked.piece,
                                rotated = rotateAllPieces || tracked.piece.color == rotatedColor
                            )
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
                    PieceGlyph(piece, rotated = rotateAllPieces || piece.color == rotatedColor)
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
    isPremove: Boolean,
    isCheckedKing: Boolean,
    showFileLabel: Boolean,
    showRankLabel: Boolean,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val boardColors = LocalBoardColors.current
    val hasPiece = piece != null
    val squareLabel = squareDescription(square, piece)
    val stateLabel = squareStateDescription(isSelected, isLegalTarget, isLastMove, isPremove, isCheckedKing)
    val isDarkSquare = (square.file + square.rank) % 2 == 0
    val plain = if (isDarkSquare) boardColors.darkSquare else boardColors.lightSquare
    val highlight = if (isDarkSquare) boardColors.darkHighlight else boardColors.lightHighlight
    val base = if (isLastMove && highlight != null) highlight else plain
    val labelColor = if (isDarkSquare) boardColors.lightSquare else boardColors.darkSquare
    val lastMoveOverlay = if (isLastMove) lastMoveTint else Color.Transparent
    val overlay = when {
        isSelected -> selectedTint
        isPremove -> premoveTint
        isCheckedKing -> checkTint
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .background(base)
            .background(lastMoveOverlay)
            .background(overlay)
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .semantics {
                contentDescription = squareLabel
                stateLabel?.let { stateDescription = it }
            },
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
            if (hasPiece) {
                Box(modifier = Modifier.fillMaxSize(0.88f).border(3.dp, legalDot, CircleShape))
            } else {
                Box(modifier = Modifier.fillMaxSize(0.3f).clip(CircleShape).background(legalDot))
            }
        }
    }
}

@Composable
private fun PieceGlyph(piece: Piece, rotated: Boolean = false) {
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
        modifier = Modifier
            .fillMaxSize(0.92f)
            .then(if (rotated) Modifier.rotate(180f) else Modifier)
    )
}

@Composable
private fun squareDescription(square: Square, piece: Piece?): String {
    val name = square.toUci()
    if (piece == null) {
        return stringResource(Res.string.a11y_square_empty, name)
    }
    val pieceName = stringResource(
        when (piece.color to piece.kind) {
            PieceColor.WHITE to PieceKind.KING -> Res.string.a11y_white_king
            PieceColor.WHITE to PieceKind.QUEEN -> Res.string.a11y_white_queen
            PieceColor.WHITE to PieceKind.ROOK -> Res.string.a11y_white_rook
            PieceColor.WHITE to PieceKind.BISHOP -> Res.string.a11y_white_bishop
            PieceColor.WHITE to PieceKind.KNIGHT -> Res.string.a11y_white_knight
            PieceColor.WHITE to PieceKind.PAWN -> Res.string.a11y_white_pawn
            PieceColor.BLACK to PieceKind.KING -> Res.string.a11y_black_king
            PieceColor.BLACK to PieceKind.QUEEN -> Res.string.a11y_black_queen
            PieceColor.BLACK to PieceKind.ROOK -> Res.string.a11y_black_rook
            PieceColor.BLACK to PieceKind.BISHOP -> Res.string.a11y_black_bishop
            PieceColor.BLACK to PieceKind.KNIGHT -> Res.string.a11y_black_knight
            else -> Res.string.a11y_black_pawn
        }
    )
    return stringResource(Res.string.a11y_square_with_piece, name, pieceName)
}

@Composable
private fun squareStateDescription(
    isSelected: Boolean,
    isLegalTarget: Boolean,
    isLastMove: Boolean,
    isPremove: Boolean,
    isCheckedKing: Boolean
): String? = when {
    isCheckedKing -> stringResource(Res.string.a11y_state_king_in_check)
    isSelected -> stringResource(Res.string.a11y_state_selected)
    isPremove -> stringResource(Res.string.a11y_state_premove)
    isLegalTarget -> stringResource(Res.string.a11y_state_legal_move)
    isLastMove -> stringResource(Res.string.a11y_state_last_move)
    else -> null
}
