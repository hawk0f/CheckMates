package dev.hawk0f.chess.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onPassAndPlay: () -> Unit = {},
    onPlayOnline: () -> Unit = {},
    onPlayBluetooth: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Chess", style = MaterialTheme.typography.displayMedium)
        Button(onClick = onPassAndPlay, modifier = Modifier.fillMaxWidth()) {
            Text("Pass & Play")
        }
        Button(onClick = onPlayOnline, modifier = Modifier.fillMaxWidth()) {
            Text("Play Online")
        }
        Button(onClick = onPlayBluetooth, modifier = Modifier.fillMaxWidth()) {
            Text("Play via Bluetooth")
        }
    }
}
