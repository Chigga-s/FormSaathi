package com.formsaathi.UI

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FormSathiPurple = Color(0xFF39277A)
private val FormSathiOrange = Color(0xFFFF8A00)

private val LightColorScheme = lightColorScheme(
    primary = FormSathiPurple,
    secondary = FormSathiOrange,

    background = Color.White,
    surface = Color.White,

    onPrimary = Color.White,
    onSecondary = Color.White,

    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9A7FF),
    secondary = Color(0xFFFFB45C),

    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),

    onPrimary = Color(0xFF21134F),
    onSecondary = Color.Black,

    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun FormSaathiTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        },
        content = content
    )
}