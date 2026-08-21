package dev.hawk0f.checkmates.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "dev.hawk0f.checkmates"

private const val TIMEOUT_MS = 20_000L
private const val DRAG_STEPS = 24

class BoardGeometry(private val fileX: IntArray, private val rankY: IntArray) {

    fun x(file: Char): Int = fileX[file - 'a']

    fun y(rank: Int): Int = rankY[rank - 1]
}

fun MacrobenchmarkScope.launchToBoard() {
    device.executeShellCommand(
        "pm grant $TARGET_PACKAGE android.permission.POST_NOTIFICATIONS"
    )
    pressHome()
    startActivityAndWait()
    device.openHotseatBoard()
}

fun UiDevice.openHotseatBoard() {
    wait(Until.findObject(By.text("Pass & Play")), TIMEOUT_MS).click()
    wait(Until.findObject(By.text("No clock")), TIMEOUT_MS).click()
    wait(Until.hasObject(By.text("h")), TIMEOUT_MS)
    waitForIdle()
}

fun UiDevice.readBoardGeometry(): BoardGeometry {
    val fileX = IntArray(8) { index ->
        val label = ('a' + index).toString()
        wait(Until.findObject(By.text(label)), TIMEOUT_MS).visibleBounds.centerX()
    }
    val rankY = IntArray(8) { index ->
        val label = (index + 1).toString()
        wait(Until.findObject(By.text(label)), TIMEOUT_MS).visibleBounds.centerY()
    }
    return BoardGeometry(fileX, rankY)
}

fun UiDevice.dragMove(geometry: BoardGeometry, from: String, to: String) {
    swipe(
        geometry.x(from[0]),
        geometry.y(from[1].digitToInt()),
        geometry.x(to[0]),
        geometry.y(to[1].digitToInt()),
        DRAG_STEPS
    )
    waitForIdle()
}

fun UiDevice.playOpeningMoves() {
    val geometry = readBoardGeometry()
    dragMove(geometry, "e2", "e4")
    dragMove(geometry, "e7", "e5")
    dragMove(geometry, "g1", "f3")
    dragMove(geometry, "b8", "c6")
}
