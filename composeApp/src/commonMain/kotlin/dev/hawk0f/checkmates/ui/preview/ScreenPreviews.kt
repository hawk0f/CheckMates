package dev.hawk0f.checkmates.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import dev.hawk0f.checkmates.session.PuzzlePersistence
import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import dev.hawk0f.checkmates.ui.computer.ComputerSetupScreen
import dev.hawk0f.checkmates.ui.editor.BoardEditorScreen
import dev.hawk0f.checkmates.ui.editor.BoardEditorViewModel
import dev.hawk0f.checkmates.ui.flow.FlowPickerScreen
import dev.hawk0f.checkmates.ui.home.HomeScreen
import dev.hawk0f.checkmates.ui.home.HomeViewModel
import dev.hawk0f.checkmates.ui.openings.OpeningsScreen
import dev.hawk0f.checkmates.ui.puzzle.PuzzleScreen
import dev.hawk0f.checkmates.ui.puzzle.PuzzleViewModel
import dev.hawk0f.checkmates.ui.replay.ReplayScreen
import dev.hawk0f.checkmates.ui.settings.SettingsScreen

private const val PREVIEW_NOW_MILLIS = 1_736_942_400_000L

private object PreviewPuzzleStore : PuzzlePersistence {
    override fun loadProgress(): Map<String, PuzzleProgress> = emptyMap()

    override fun saveProgress(progress: Map<String, PuzzleProgress>) = Unit

    override fun loadRating(): Int = 1450

    override fun saveRating(rating: Int) = Unit

    override fun loadStreak(): Int = 4

    override fun saveStreak(streak: Int) = Unit
}

private val previewHistoryItem = GameHistoryItem(
    id = 1,
    mode = "online",
    myColor = PieceColor.WHITE,
    whiteName = "hawk0f",
    blackName = "Anna",
    winner = PieceColor.WHITE,
    reason = GameOverReason.CHECKMATE,
    uciHistory = listOf("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"),
    finishedAtMillis = PREVIEW_NOW_MILLIS
)

internal val homeScreenSpec = PreviewSpec("screen-home", fillsScreen = true) {
    HomeScreen(viewModel = remember { HomeViewModel() })
}

internal val flowPickerScreenSpec = PreviewSpec("screen-flow-picker", fillsScreen = true) {
    FlowPickerScreen(onChosen = {})
}

internal val computerSetupScreenSpec = PreviewSpec("screen-computer-setup", fillsScreen = true) {
    ComputerSetupScreen(onStart = { _, _ -> }, onBack = {})
}

internal val openingsScreenSpec = PreviewSpec("screen-openings", fillsScreen = true) {
    OpeningsScreen(onOpenLine = {}, onBack = {})
}

internal val settingsScreenSpec = PreviewSpec("screen-settings", fillsScreen = true) {
    SettingsScreen(onBack = {})
}

internal val boardEditorScreenSpec = PreviewSpec("screen-board-editor", capturable = false, fillsScreen = true) {
    BoardEditorScreen(
        onPlayHotseat = {},
        onPlayComputer = {},
        onOpenImportedGame = {},
        onBack = {},
        viewModel = remember { BoardEditorViewModel() }
    )
}

internal val puzzleScreenSpec = PreviewSpec("screen-puzzle", fillsScreen = true) {
    PuzzleScreen(
        onBack = {},
        viewModel = remember {
            PuzzleViewModel(store = PreviewPuzzleStore, now = { PREVIEW_NOW_MILLIS })
        }
    )
}

internal val replayScreenSpec = PreviewSpec("screen-replay", capturable = false, fillsScreen = true) {
    ReplayScreen(item = previewHistoryItem, onBack = {})
}

internal val screenPreviewSpecs = listOf(
    homeScreenSpec,
    flowPickerScreenSpec,
    computerSetupScreenSpec,
    openingsScreenSpec,
    settingsScreenSpec,
    boardEditorScreenSpec,
    puzzleScreenSpec,
    replayScreenSpec
)

@Preview
@Composable
internal fun HomeScreenPreview() = PreviewFrame(homeScreenSpec)

@Preview
@Composable
internal fun FlowPickerScreenPreview() = PreviewFrame(flowPickerScreenSpec)

@Preview
@Composable
internal fun ComputerSetupScreenPreview() = PreviewFrame(computerSetupScreenSpec)

@Preview
@Composable
internal fun OpeningsScreenPreview() = PreviewFrame(openingsScreenSpec)

@Preview
@Composable
internal fun SettingsScreenPreview() = PreviewFrame(settingsScreenSpec)

@Preview
@Composable
internal fun BoardEditorScreenPreview() = PreviewFrame(boardEditorScreenSpec)

@Preview
@Composable
internal fun PuzzleScreenPreview() = PreviewFrame(puzzleScreenSpec)

@Preview
@Composable
internal fun ReplayScreenPreview() = PreviewFrame(replayScreenSpec)
