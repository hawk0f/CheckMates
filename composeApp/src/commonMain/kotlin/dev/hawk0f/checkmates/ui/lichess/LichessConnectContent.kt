package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.ui.profile.pieceDrawable
import org.jetbrains.compose.resources.painterResource
import dev.hawk0f.checkmates.ui.theme.CheckIcon
import dev.hawk0f.checkmates.ui.theme.LocalAppAccents
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel

@Composable
fun LichessConnectContent(
    onSignIn: () -> Unit,
    onOpenPuzzle: () -> Unit,
    onOpenWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accents = LocalAppAccents.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(accents.pageAlt),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(pieceDrawable("wn")),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp)
                )
            }
            Text(
                text = "Play with your\nLichess account",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = "Your games, rating and puzzle history stay on lichess.org. " +
                    "We only ask for what the screens need.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(scheme.inverseSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionLabel("Token scopes requested", color = scheme.inverseOnSurface.copy(alpha = 0.6f))
            for ((scope, purpose) in LichessAuth.SCOPE_DETAILS) {
                ScopeRow(scope = scope, purpose = purpose, granted = true)
            }
            for ((scope, purpose) in LichessAuth.SCOPE_DECLINED) {
                ScopeRow(scope = scope, purpose = purpose, granted = false)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Open without an account", color = accents.bandStrong)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PillButton("Puzzle", onOpenPuzzle, tone = PillTone.SOFT, compact = true)
                PillButton("Watch", onOpenWatch, tone = PillTone.SOFT, compact = true)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                text = "Continue on lichess.org",
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Authorization code + PKCE · no password ever leaves Lichess. " +
                    "Engine help is banned there — play yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ScopeRow(scope: String, purpose: String, granted: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val onDark = scheme.inverseOnSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (granted) scheme.secondary else onDark.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (granted) {
                CheckIcon(color = scheme.onSecondary, size = 13.dp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = scope,
                style = MaterialTheme.typography.titleSmall,
                color = if (granted) onDark else onDark.copy(alpha = 0.45f)
            )
            Text(
                text = purpose,
                style = MaterialTheme.typography.bodySmall,
                color = onDark.copy(alpha = if (granted) 0.6f else 0.35f)
            )
        }
    }
}
