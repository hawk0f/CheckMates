package dev.hawk0f.chess.ble

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val blePermissions = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_ADVERTISE
)

@Composable
actual fun rememberBlePermissionRequester(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        onResult(grants.values.all { it })
    }
    return remember(context) {
        {
            val allGranted = blePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                onResult(true)
            } else {
                launcher.launch(blePermissions)
            }
        }
    }
}
