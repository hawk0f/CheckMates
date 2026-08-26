package dev.hawk0f.checkmates.ui.preview

import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.hawk0f.checkmates.ui.theme.DarkModePreference
import dev.hawk0f.checkmates.ui.theme.ThemeManager
import dev.hawk0f.checkmates.ui.theme.ThemePalette
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal val previewRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.0001f)
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [36],
    qualifiers = "w411dp-h891dp-normal-long-notround-any-560dpi-keyshidden-nonav"
)
internal abstract class PreviewScreenshotTest(
    private val specs: List<PreviewSpec>,
    private val mode: DarkModePreference
) {

    @Before
    fun applyTheme() {
        ThemeManager.selectPalette(ThemePalette.SAGE)
        ThemeManager.selectDarkMode(mode)
    }

    @Test
    fun previewsMatchTheirGoldens() {
        for (spec in specs.filter { it.capturable }) {
            captureRoboImage(
                filePath = "screenshots/preview-${spec.id}-${mode.id}.png",
                roborazziOptions = previewRoborazziOptions
            ) {
                PreviewFrame(spec)
            }
        }
    }
}

internal class ComponentPreviewLightScreenshotTest : PreviewScreenshotTest(
    componentPreviewSpecs + dialogPreviewSpecs,
    DarkModePreference.LIGHT
)

internal class ComponentPreviewDarkScreenshotTest : PreviewScreenshotTest(
    componentPreviewSpecs + dialogPreviewSpecs,
    DarkModePreference.DARK
)

internal class BoardPreviewLightScreenshotTest : PreviewScreenshotTest(
    boardPreviewSpecs,
    DarkModePreference.LIGHT
)

internal class BoardPreviewDarkScreenshotTest : PreviewScreenshotTest(
    boardPreviewSpecs,
    DarkModePreference.DARK
)

internal class ScreenPreviewLightScreenshotTest : PreviewScreenshotTest(
    screenPreviewSpecs,
    DarkModePreference.LIGHT
)

internal class ScreenPreviewDarkScreenshotTest : PreviewScreenshotTest(
    screenPreviewSpecs,
    DarkModePreference.DARK
)
