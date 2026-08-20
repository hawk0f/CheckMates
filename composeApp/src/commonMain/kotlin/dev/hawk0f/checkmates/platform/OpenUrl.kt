package dev.hawk0f.checkmates.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberOpenUrl(): (String) -> Unit
