package dev.hawk0f.checkmates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.hawk0f.checkmates.ui.preview.PreviewGallery

class PreviewGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreviewGallery(onExit = { finish() })
        }
    }
}
