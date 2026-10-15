package com.example.fitpick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Orange = Color(0xFFFF6B35)
val Pink = Color(0xFFFF3D77)
val Purple = Color(0xFF7C4DFF)
val Teal = Color(0xFF00C9A7)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7A45),
    onPrimary = Color(0xFF1F0A00),
    primaryContainer = Color(0xFF5A2A12),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFA68BFF),
    tertiary = Color(0xFF2DE0B6),
    background = Color(0xFF0F1020),
    onBackground = Color(0xFFF2F2FA),
    surface = Color(0xFF1C1D36),
    onSurface = Color(0xFFF2F2FA),
    surfaceVariant = Color(0xFF2A2C50),
    onSurfaceVariant = Color(0xFFC9CAE6),
    outline = Color(0xFF6E70A0),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE8531F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D2),
    onPrimaryContainer = Color(0xFF3A1100),
    secondary = Color(0xFF6A43E0),
    tertiary = Color(0xFF00A98B),
    background = Color(0xFFFFF4EC),
    onBackground = Color(0xFF241A17),
    surface = Color.White,
    onSurface = Color(0xFF241A17),
    surfaceVariant = Color(0xFFFFE9DC),
    onSurfaceVariant = Color(0xFF5A4A44),
    outline = Color(0xFFB59C92),
    error = Color(0xFFD32F2F)
)

@Composable
fun FitPickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme())
        listOf(Color(0xFF241352), Color(0xFF0F1020), Color(0xFF0A2E36))
    else
        listOf(Color(0xFFFFD9C2), Color(0xFFFFF4EC), Color(0xFFE4DCFF))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors))) { content() }
}
