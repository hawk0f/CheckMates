package dev.hawk0f.checkmates.ui.about

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.hawk0f.checkmates.ui.theme.PillButton

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "CheckMates — chess for two players, online, on Lichess or nearby.\n\n" +
                    "Chess pieces: \"cburnett\" set by Colin M.L. Burnett, " +
                    "CC BY-SA 3.0, via lichess.org.\n\n" +
                    "Chess rules engine: kchesslib (Apache-2.0).\n\n" +
                    "Type: Caprasimo and Figtree, SIL Open Font License."
            )
        },
        confirmButton = {
            PillButton(text = "OK", onClick = onDismiss, compact = true)
        }
    )
}
