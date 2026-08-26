package dev.hawk0f.checkmates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.hawk0f.checkmates.ui.preview.PreviewGallery
import dev.hawk0f.checkmates.ui.theme.DarkModePreference
import dev.hawk0f.checkmates.ui.theme.ThemeManager

class PreviewGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val spec = intent.getStringExtra("spec")
        intent.getStringExtra("mode")?.let { ThemeManager.selectDarkMode(DarkModePreference.byId(it)) }
        setContent {
            PreviewGallery(initialSpecId = spec, onExit = { finish() })
        }
    }
}
