package dev.hawk0f.chess.ble

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBlePermissionRequester(onResult: (Boolean) -> Unit): () -> Unit
