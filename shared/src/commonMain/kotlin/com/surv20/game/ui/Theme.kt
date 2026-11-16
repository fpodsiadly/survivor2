package com.surv20.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Placeholder palette (sci-fi, SPEC W40) until the Recraft art sets the final look.
val Night = Color(0xFF0E1B2B)
val Teal = Color(0xFF4FE3C1)
val Amber = Color(0xFFFFB547)

@Composable
fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Teal,
            secondary = Amber,
            background = Night,
            surface = Color(0xFF16263A),
        ),
        content = content,
    )
}
