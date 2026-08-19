package dev.hawk0f.checkmates.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound

actual fun playMoveSound(capture: Boolean) {
    AudioServicesPlaySystemSound(if (capture) 1057u else 1104u)
}
