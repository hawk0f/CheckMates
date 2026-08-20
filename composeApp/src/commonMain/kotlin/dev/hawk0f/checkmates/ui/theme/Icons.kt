package dev.hawk0f.checkmates.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ChevronDirection {
    LEFT,
    RIGHT
}

@Composable
fun ChevronIcon(
    direction: ChevronDirection,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    doubled: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * 0.15f, cap = StrokeCap.Round)
        val w = this.size.width
        val h = this.size.height
        val tips = if (doubled) listOf(0.36f, 0.78f) else listOf(0.66f)
        for (tip in tips) {
            val path = Path()
            if (direction == ChevronDirection.LEFT) {
                path.moveTo(w * tip, h * 0.16f)
                path.lineTo(w * (tip - 0.32f), h * 0.5f)
                path.lineTo(w * tip, h * 0.84f)
            } else {
                path.moveTo(w * (1f - tip), h * 0.16f)
                path.lineTo(w * (1f - tip + 0.32f), h * 0.5f)
                path.lineTo(w * (1f - tip), h * 0.84f)
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.14f
        val inset = this.size.minDimension * 0.2f
        drawLine(
            color = color,
            start = Offset(inset, inset),
            end = Offset(this.size.width - inset, this.size.height - inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(this.size.width - inset, inset),
            end = Offset(inset, this.size.height - inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path()
        path.moveTo(this.size.width * 0.18f, this.size.height * 0.55f)
        path.lineTo(this.size.width * 0.42f, this.size.height * 0.78f)
        path.lineTo(this.size.width * 0.84f, this.size.height * 0.24f)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = this.size.minDimension * 0.17f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun PlayIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path()
        path.moveTo(this.size.width * 0.24f, this.size.height * 0.12f)
        path.lineTo(this.size.width * 0.88f, this.size.height * 0.5f)
        path.lineTo(this.size.width * 0.24f, this.size.height * 0.88f)
        path.close()
        drawPath(path = path, color = color)
    }
}

@Composable
fun PauseIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val barWidth = this.size.width * 0.22f
        val top = this.size.height * 0.14f
        val height = this.size.height * 0.72f
        drawRect(
            color = color,
            topLeft = Offset(this.size.width * 0.22f, top),
            size = Size(barWidth, height)
        )
        drawRect(
            color = color,
            topLeft = Offset(this.size.width * 0.56f, top),
            size = Size(barWidth, height)
        )
    }
}

@Composable
fun FlagIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.14f
        val poleX = this.size.width * 0.26f
        drawLine(
            color = color,
            start = Offset(poleX, this.size.height * 0.12f),
            end = Offset(poleX, this.size.height * 0.9f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val path = Path()
        path.moveTo(poleX, this.size.height * 0.16f)
        path.lineTo(this.size.width * 0.84f, this.size.height * 0.16f)
        path.lineTo(this.size.width * 0.7f, this.size.height * 0.38f)
        path.lineTo(this.size.width * 0.84f, this.size.height * 0.6f)
        path.lineTo(poleX, this.size.height * 0.6f)
        path.close()
        drawPath(path = path, color = color)
    }
}

@Composable
fun PlusIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.14f
        val inset = this.size.minDimension * 0.18f
        drawLine(
            color = color,
            start = Offset(this.size.width / 2f, inset),
            end = Offset(this.size.width / 2f, this.size.height - inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(inset, this.size.height / 2f),
            end = Offset(this.size.width - inset, this.size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
