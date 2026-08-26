package dev.hawk0f.checkmates.ui.preview

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.ui.game.ChessBoard
import dev.hawk0f.checkmates.ui.game.PromotionDialog
import androidx.compose.ui.tooling.preview.Preview

private val boardSize = Modifier.size(328.dp)

internal val boardStartSpec = PreviewSpec("board-start") {
    ChessBoard(
        gameState = previewState(),
        selected = null,
        legalTargets = emptySet(),
        flipped = false,
        onSquareTap = {},
        interactive = false,
        modifier = boardSize
    )
}

internal val boardSelectionSpec = PreviewSpec("board-selection") {
    ChessBoard(
        gameState = previewState("e2e4", "e7e5"),
        selected = Square.fromUci("g1"),
        legalTargets = setOf(Square.fromUci("f3"), Square.fromUci("e2"), Square.fromUci("h3")),
        flipped = false,
        onSquareTap = {},
        modifier = boardSize
    )
}

internal val boardCheckFlippedSpec = PreviewSpec("board-check-flipped") {
    ChessBoard(
        gameState = previewState("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"),
        selected = null,
        legalTargets = emptySet(),
        flipped = true,
        onSquareTap = {},
        interactive = false,
        modifier = boardSize
    )
}

internal val boardPremoveSpec = PreviewSpec("board-premove") {
    ChessBoard(
        gameState = previewState("d2d4", "d7d5", "c2c4"),
        selected = null,
        legalTargets = emptySet(),
        flipped = true,
        onSquareTap = {},
        premoveSquares = setOf(Square.fromUci("d5"), Square.fromUci("c4")),
        modifier = boardSize
    )
}

internal val boardWithoutCoordinatesSpec = PreviewSpec("board-no-coordinates") {
    ChessBoard(
        gameState = previewState("e2e4", "c7c5", "g1f3"),
        selected = null,
        legalTargets = emptySet(),
        flipped = false,
        onSquareTap = {},
        showCoordinates = false,
        interactive = false,
        modifier = boardSize
    )
}

internal val boardRotatedPiecesSpec = PreviewSpec("board-rotated-pieces") {
    ChessBoard(
        gameState = previewState("e2e4", "e7e5", "g1f3", "b8c6"),
        selected = null,
        legalTargets = emptySet(),
        flipped = false,
        onSquareTap = {},
        interactive = false,
        rotatedColor = PieceColor.BLACK,
        modifier = boardSize
    )
}

internal val promotionWhiteSpec = PreviewSpec("promotion-white") {
    PromotionDialog(color = PieceColor.WHITE, onChoose = {}, onDismiss = {})
}

internal val promotionBlackSpec = PreviewSpec("promotion-black") {
    PromotionDialog(color = PieceColor.BLACK, onChoose = {}, onDismiss = {})
}

internal val boardPreviewSpecs = listOf(
    boardStartSpec,
    boardSelectionSpec,
    boardCheckFlippedSpec,
    boardPremoveSpec,
    boardWithoutCoordinatesSpec,
    boardRotatedPiecesSpec
)

internal val dialogPreviewSpecs = listOf(
    promotionWhiteSpec,
    promotionBlackSpec
)

@Preview
@Composable
internal fun BoardStartPreview() = PreviewFrame(boardStartSpec)

@Preview
@Composable
internal fun BoardSelectionPreview() = PreviewFrame(boardSelectionSpec)

@Preview
@Composable
internal fun BoardCheckFlippedPreview() = PreviewFrame(boardCheckFlippedSpec)

@Preview
@Composable
internal fun BoardPremovePreview() = PreviewFrame(boardPremoveSpec)

@Preview
@Composable
internal fun BoardWithoutCoordinatesPreview() = PreviewFrame(boardWithoutCoordinatesSpec)

@Preview
@Composable
internal fun BoardRotatedPiecesPreview() = PreviewFrame(boardRotatedPiecesSpec)

@Preview
@Composable
internal fun PromotionWhitePreview() = PreviewFrame(promotionWhiteSpec)

@Preview
@Composable
internal fun PromotionBlackPreview() = PreviewFrame(promotionBlackSpec)
