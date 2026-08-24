package dev.hawk0f.checkmates.ui.lichess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.ui.profile.pieceDrawable
import org.jetbrains.compose.resources.painterResource
import dev.hawk0f.checkmates.ui.theme.CheckIcon
import dev.hawk0f.checkmates.ui.theme.PillButton
import dev.hawk0f.checkmates.ui.theme.PillTone
import dev.hawk0f.checkmates.ui.theme.SectionLabel
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.connect_headline
import dev.hawk0f.checkmates.resources.connect_open_without_account
import dev.hawk0f.checkmates.resources.connect_password_note
import dev.hawk0f.checkmates.resources.connect_permissions_label
import dev.hawk0f.checkmates.resources.connect_pitch
import dev.hawk0f.checkmates.resources.connect_puzzle
import dev.hawk0f.checkmates.resources.connect_sign_in
import dev.hawk0f.checkmates.resources.connect_watch
import org.jetbrains.compose.resources.stringResource

private val invertFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

@Composable
fun LichessConnectContent(
    onSignIn: () -> Unit,
    onOpenPuzzle: () -> Unit,
    onOpenWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(scheme.inverseSurface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(pieceDrawable("wn")),
                    contentDescription = null,
                    colorFilter = invertFilter,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = stringResource(Res.string.connect_headline),
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = stringResource(Res.string.connect_pitch),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(stringResource(Res.string.connect_permissions_label), color = scheme.outline)
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                for ((title, detail) in LichessAuth.PERMISSIONS_GRANTED) {
                    PermissionRow(title = title, detail = detail, granted = true)
                }
                for ((title, detail) in LichessAuth.PERMISSIONS_DECLINED) {
                    PermissionRow(title = title, detail = detail, granted = false)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(Res.string.connect_open_without_account), color = scheme.outline)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PillButton(stringResource(Res.string.connect_puzzle), onOpenPuzzle, tone = PillTone.SOFT, compact = true)
                PillButton(stringResource(Res.string.connect_watch), onOpenWatch, tone = PillTone.SOFT, compact = true)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                text = stringResource(Res.string.connect_sign_in),
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(Res.string.connect_password_note),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionRow(title: String, detail: String, granted: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (granted) scheme.surfaceVariant else scheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (granted) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(scheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                CheckIcon(color = scheme.onTertiary, size = 14.dp)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .border(2.dp, scheme.outlineVariant, CircleShape)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (granted) scheme.onSurface else scheme.onSurfaceVariant
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) scheme.onSurfaceVariant else scheme.outline
            )
        }
    }
}
