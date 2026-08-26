package dev.hawk0f.checkmates.ui.game

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.hawk0f.checkmates.shared.domain.ChessGame
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.domain.Square
import dev.hawk0f.checkmates.ui.theme.AppTheme
import dev.hawk0f.checkmates.ui.theme.DarkModePreference
import dev.hawk0f.checkmates.ui.theme.ThemeManager
import dev.hawk0f.checkmates.ui.theme.ThemePalette
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [36],
    qualifiers = "w411dp-h891dp-normal-long-notround-any-560dpi-keyshidden-nonav"
)
class ChessBoardScreenshotTest {

    @Before
    fun pinTheme() {
        ThemeManager.selectPalette(ThemePalette.SAGE)
        ThemeManager.selectDarkMode(DarkModePreference.LIGHT)
    }

    private fun stateAfter(vararg moves: String) = ChessGame().apply {
        for (uci in moves) {
            applyUci(uci)
        }
    }.state()

    @Test
    fun startingPosition() {
        captureRoboImage("screenshots/board-start.png") {
            AppTheme {
                ChessBoard(
                    gameState = stateAfter(),
                    selected = null,
                    legalTargets = emptySet(),
                    flipped = false,
                    onSquareTap = {},
                    interactive = false,
                    modifier = Modifier.size(360.dp)
                )
            }
        }
    }

    @Test
    fun selectedPieceWithLegalTargets() {
        captureRoboImage("screenshots/board-selection.png") {
            AppTheme {
                ChessBoard(
                    gameState = stateAfter("e2e4", "e7e5"),
                    selected = Square.fromUci("g1"),
                    legalTargets = setOf(Square.fromUci("f3"), Square.fromUci("e2"), Square.fromUci("h3")),
                    flipped = false,
                    onSquareTap = {},
                    interactive = true,
                    modifier = Modifier.size(360.dp)
                )
            }
        }
    }

    @Test
    fun flippedBoardInCheck() {
        captureRoboImage("screenshots/board-flipped-check.png") {
            AppTheme {
                ChessBoard(
                    gameState = stateAfter("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"),
                    selected = null,
                    legalTargets = emptySet(),
                    flipped = true,
                    onSquareTap = {},
                    interactive = false,
                    modifier = Modifier.size(360.dp)
                )
            }
        }
    }

    @Test
    fun boardRendersForBothColours() {
        for (color in listOf(PieceColor.WHITE, PieceColor.BLACK)) {
            captureRoboImage("screenshots/board-${color.name.lowercase()}.png") {
                AppTheme {
                    ChessBoard(
                        gameState = stateAfter("d2d4", "d7d5"),
                        selected = null,
                        legalTargets = emptySet(),
                        flipped = color == PieceColor.BLACK,
                        onSquareTap = {},
                        interactive = false,
                        modifier = Modifier.size(360.dp)
                    )
                }
            }
        }
    }
}
