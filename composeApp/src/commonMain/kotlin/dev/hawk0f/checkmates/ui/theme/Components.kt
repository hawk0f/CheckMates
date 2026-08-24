package dev.hawk0f.checkmates.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

enum class PillTone {
    INK,
    ACCENT,
    LEAF,
    SOFT,
    BAND
}

val MaxContentWidth = 640.dp

@Composable
private fun toneColors(tone: PillTone, enabled: Boolean): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    val pair = when (tone) {
        PillTone.INK -> scheme.inverseSurface to scheme.inverseOnSurface
        PillTone.ACCENT -> scheme.primary to scheme.onPrimary
        PillTone.LEAF -> scheme.tertiary to scheme.onTertiary
        PillTone.SOFT -> scheme.surfaceVariant to scheme.onSurface
        PillTone.BAND -> accents.band to accents.onBand
    }
    return if (enabled) pair else scheme.surfaceVariant to scheme.outline
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.INK,
    enabled: Boolean = true,
    compact: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    val (container, content) = toneColors(tone, enabled)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 18.dp else 22.dp,
                vertical = if (compact) 12.dp else 16.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
            color = content,
            textAlign = TextAlign.Center,
            fontFamily = displayFamily()
        )
        trailing?.invoke()
    }
}

@Composable
fun SelectPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        tone = if (selected) PillTone.LEAF else PillTone.SOFT,
        compact = true
    )
}

@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) container else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.outline,
        modifier = modifier
    )
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    corner: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    SoftCard(
        modifier = modifier,
        container = if (accent) scheme.primary else scheme.surfaceContainer,
        corner = 20.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (accent) scheme.onPrimary else scheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (accent) scheme.onPrimary else scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.outline
            )
        },
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            cursorColor = scheme.primary
        )
    )
}

@Composable
fun ChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    SoftCard(
        modifier = modifier,
        container = if (selected) scheme.primary else scheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) scheme.onPrimary else scheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) scheme.onPrimary.copy(alpha = 0.85f) else scheme.onSurfaceVariant
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.onPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    CheckIcon(color = scheme.primary, size = 13.dp)
                }
            } else {
                ChevronIcon(
                    direction = ChevronDirection.RIGHT,
                    color = scheme.outline,
                    size = 18.dp
                )
            }
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
fun OutlineTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceContainer)
            .border(
                border = BorderStroke(2.dp, if (selected) scheme.primary else Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun InitialsBadge(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    container: Color? = null,
    contentColor: Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container ?: scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(2).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor ?: scheme.onSurfaceVariant
        )
    }
}

@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(scheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) scheme.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) scheme.onSurface else scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WinRateBar(
    white: Int,
    draws: Int,
    black: Int,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val scheme = MaterialTheme.colorScheme
    val total = (white + draws + black).coerceAtLeast(1)
    Row(
        modifier = modifier.height(height).clip(RoundedCornerShape(999.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (white > 0) {
            Box(
                modifier = Modifier
                    .weight(white.toFloat() / total)
                    .fillMaxHeight()
                    .background(scheme.surfaceBright)
            )
        }
        if (draws > 0) {
            Box(
                modifier = Modifier
                    .weight(draws.toFloat() / total)
                    .fillMaxHeight()
                    .background(scheme.outline)
            )
        }
        if (black > 0) {
            Box(
                modifier = Modifier
                    .weight(black.toFloat() / total)
                    .fillMaxHeight()
                    .background(scheme.inverseSurface)
            )
        }
    }
}

@Composable
fun CodeChip(text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(scheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
