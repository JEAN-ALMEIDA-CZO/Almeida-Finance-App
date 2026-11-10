package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GoldDarkFinancialColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF090C12),
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldLight,
    secondary = GoldSecondary,
    onSecondary = Color(0xFF090C12),
    tertiary = GoldLight,
    onTertiary = Color(0xFF090C12),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorderGold,
    error = ExpenseRed,
    onError = Color.White
)

val DarkFinancialColorScheme = GoldDarkFinancialColorScheme
val LightFinancialColorScheme = GoldDarkFinancialColorScheme

@Composable
fun FinanGestTheme(
    darkTheme: Boolean = true, // Default to gorgeous Gold & Dark luxury experience
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GoldDarkFinancialColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    FinanGestTheme(darkTheme = darkTheme, content = content)
}
