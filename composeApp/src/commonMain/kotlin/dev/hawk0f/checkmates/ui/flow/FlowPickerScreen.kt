package dev.hawk0f.checkmates.ui.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.flow_checkmates_details
import dev.hawk0f.checkmates.resources.flow_checkmates_name
import dev.hawk0f.checkmates.resources.flow_checkmates_tagline
import dev.hawk0f.checkmates.resources.flow_continue
import dev.hawk0f.checkmates.resources.flow_lichess_details
import dev.hawk0f.checkmates.resources.flow_lichess_name
import dev.hawk0f.checkmates.resources.flow_lichess_tagline
import dev.hawk0f.checkmates.resources.flow_picker_subtitle
import dev.hawk0f.checkmates.resources.flow_picker_title
import dev.hawk0f.checkmates.session.AppFlow
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SoftCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun FlowPickerScreen(
    initial: AppFlow? = null,
    onChosen: (AppFlow) -> Unit
) {
    var selected by remember { mutableStateOf(initial ?: AppFlow.CHECKMATES) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.flow_picker_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.flow_picker_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowOption(
            title = stringResource(Res.string.flow_checkmates_name),
            tagline = stringResource(Res.string.flow_checkmates_tagline),
            details = stringResource(Res.string.flow_checkmates_details),
            selected = selected == AppFlow.CHECKMATES,
            onClick = { selected = AppFlow.CHECKMATES }
        )
        FlowOption(
            title = stringResource(Res.string.flow_lichess_name),
            tagline = stringResource(Res.string.flow_lichess_tagline),
            details = stringResource(Res.string.flow_lichess_details),
            selected = selected == AppFlow.LICHESS,
            onClick = { selected = AppFlow.LICHESS }
        )
        Spacer(modifier = Modifier.height(4.dp))
        PillButton(
            text = stringResource(Res.string.flow_continue),
            onClick = { onChosen(selected) },
            tone = PillTone.ACCENT,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FlowOption(
    title: String,
    tagline: String,
    details: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        container = if (selected) accents.band else scheme.surfaceVariant,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tagline.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accents.onBand else scheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (selected) accents.onBand else scheme.onSurface
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) accents.onBand else scheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}
