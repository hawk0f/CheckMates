package dev.hawk0f.checkmates.platform

import androidx.compose.runtime.Composable

@Composable
expect fun QrScannerView(
    onResult: (String) -> Unit,
    onPermissionDenied: () -> Unit
)
