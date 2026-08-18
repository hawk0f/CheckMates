package dev.hawk0f.chess.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberShareText(): (String) -> Unit
