package com.mimo.mobile.ui.adaptive

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun rememberWindowWidthDp(): Int {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp
}

val Int.isExpandedWidth: Boolean get() = this >= 600
val Int.isMediumWidth: Boolean get() = this in 400..599

@Composable
fun AdaptivePadding(
    content: @Composable () -> Unit
) {
    val widthDp = rememberWindowWidthDp()
    val horizontalPadding = when {
        widthDp.isExpandedWidth -> 32.dp
        widthDp.isMediumWidth -> 24.dp
        else -> 16.dp
    }
    Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        content()
    }
}

@Composable
fun AdaptiveMaxWidth(
    content: @Composable () -> Unit
) {
    val widthDp = rememberWindowWidthDp()
    val maxWidth = when {
        widthDp.isExpandedWidth -> 600.dp
        widthDp.isMediumWidth -> 480.dp
        else -> 9999.dp
    }
    Box(modifier = Modifier.widthIn(max = maxWidth)) {
        content()
    }
}
