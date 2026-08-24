package dev.hawk0f.checkmates.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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

@Composable
fun PuzzleIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = this.size.minDimension * 0.12f, cap = StrokeCap.Round)
        val path = Path()
        path.moveTo(w * 0.16f, h * 0.32f)
        path.lineTo(w * 0.38f, h * 0.32f)
        path.cubicTo(w * 0.38f, h * 0.14f, w * 0.62f, h * 0.14f, w * 0.62f, h * 0.32f)
        path.lineTo(w * 0.84f, h * 0.32f)
        path.lineTo(w * 0.84f, h * 0.54f)
        path.cubicTo(w * 0.66f, h * 0.54f, w * 0.66f, h * 0.78f, w * 0.84f, h * 0.78f)
        path.lineTo(w * 0.84f, h * 0.86f)
        path.lineTo(w * 0.16f, h * 0.86f)
        path.lineTo(w * 0.16f, h * 0.64f)
        path.cubicTo(w * 0.34f, h * 0.64f, w * 0.34f, h * 0.4f, w * 0.16f, h * 0.4f)
        path.close()
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
fun ScreenIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = this.size.minDimension * 0.12f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.1f, h * 0.28f),
            size = Size(w * 0.8f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.14f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        val antenna = Path()
        antenna.moveTo(w * 0.34f, h * 0.1f)
        antenna.lineTo(w * 0.5f, h * 0.24f)
        antenna.lineTo(w * 0.66f, h * 0.1f)
        drawPath(
            path = antenna,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun PeopleIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = this.size.minDimension * 0.12f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        drawCircle(
            color = color,
            radius = w * 0.16f,
            center = Offset(w * 0.4f, h * 0.32f),
            style = stroke
        )
        val shoulders = Path()
        shoulders.moveTo(w * 0.12f, h * 0.86f)
        shoulders.lineTo(w * 0.12f, h * 0.76f)
        shoulders.cubicTo(w * 0.12f, h * 0.6f, w * 0.68f, h * 0.6f, w * 0.68f, h * 0.76f)
        shoulders.lineTo(w * 0.68f, h * 0.86f)
        drawPath(path = shoulders, color = color, style = stroke)
        val side = Path()
        side.moveTo(w * 0.76f, h * 0.34f)
        side.cubicTo(w * 0.94f, h * 0.42f, w * 0.94f, h * 0.56f, w * 0.76f, h * 0.64f)
        drawPath(path = side, color = color, style = stroke)
    }
}
