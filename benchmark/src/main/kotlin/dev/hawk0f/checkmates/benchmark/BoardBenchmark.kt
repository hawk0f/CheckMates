package dev.hawk0f.checkmates.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @OptIn(ExperimentalMacrobenchmarkApi::class)
    @Test
    fun dragPieces() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 8,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Ignore(),
        setupBlock = {
            launchToBoard()
        }
    ) {
        device.playOpeningMoves()
    }
}
