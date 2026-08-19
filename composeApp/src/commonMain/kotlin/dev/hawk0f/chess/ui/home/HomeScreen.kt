package dev.hawk0f.chess.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hawk0f.chess.resources.Res
import dev.hawk0f.chess.resources.piece_wn
import dev.hawk0f.chess.session.AuthManager
import dev.hawk0f.chess.shared.protocol.ProfileResponse
import dev.hawk0f.chess.ui.profile.AvatarBadge
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    onPassAndPlay: () -> Unit = {},
    onPlayOnline: () -> Unit = {},
    onPlayBluetooth: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val profile by AuthManager.profile.collectAsStateWithLifecycle()
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.piece_wn),
                contentDescription = null,
                modifier = Modifier.size(76.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chess",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Play with a friend, anywhere",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeCard(
                glyph = "♟",
                title = "Pass & Play",
                subtitle = "Two players, one device",
                onClick = onPassAndPlay
            )
            ModeCard(
                glyph = "🌐",
                title = "Play Online",
                subtitle = "Invite by link, code or QR",
                onClick = onPlayOnline
            )
            ModeCard(
                glyph = "📡",
                title = "Play via Bluetooth",
                subtitle = "Nearby, no internet needed",
                onClick = onPlayBluetooth
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        ProfileEntry(profile, onOpenProfile)

        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenSettings) {
                Text("Settings")
            }
            TextButton(onClick = { showAbout = true }) {
                Text("About")
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About") },
            text = {
                Text(
                    "Chess for two players — online and over Bluetooth.\n\n" +
                        "Chess pieces: \"cburnett\" set by Colin M.L. Burnett, " +
                        "CC BY-SA 3.0, via lichess.org.\n\n" +
                        "Chess rules engine: kchesslib (Apache-2.0)."
                )
            },
            confirmButton = {
                Button(onClick = { showAbout = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ProfileEntry(profile: ProfileResponse?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile != null) {
                AvatarBadge(profile.avatarKind, profile.avatarValue, size = 40.dp, fontSize = 20.sp)
                Column {
                    Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Profile & game history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 20.sp)
                }
                Column {
                    Text("Sign in", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Keep your games and stats",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(glyph: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = glyph, fontSize = 24.sp)
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
