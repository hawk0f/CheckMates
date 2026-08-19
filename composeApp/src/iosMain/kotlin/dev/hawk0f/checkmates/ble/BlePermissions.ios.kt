package dev.hawk0f.checkmates.ble

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberBlePermissionRequester(onResult: (Boolean) -> Unit): () -> Unit = remember {
    {
        onResult(true)
    }
}
