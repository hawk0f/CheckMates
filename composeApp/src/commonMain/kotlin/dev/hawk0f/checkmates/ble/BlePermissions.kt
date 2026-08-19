package dev.hawk0f.checkmates.ble

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBlePermissionRequester(onResult: (Boolean) -> Unit): () -> Unit
