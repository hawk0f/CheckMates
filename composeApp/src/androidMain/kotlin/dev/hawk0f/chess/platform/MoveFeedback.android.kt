package dev.hawk0f.chess.platform

import android.media.AudioManager
import android.media.ToneGenerator

private val toneGenerator by lazy {
    runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull()
}

actual fun playMoveSound(capture: Boolean) {
    val tone = if (capture) ToneGenerator.TONE_PROP_NACK else ToneGenerator.TONE_PROP_ACK
    toneGenerator?.startTone(tone, 40)
}
