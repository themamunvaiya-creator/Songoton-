package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = BurnishedGold,
    onSecondary = Color.White,
    secondaryContainer = BurnishedGoldContainer,
    onSecondaryContainer = OnBurnishedGoldContainer,
    tertiary = DeepTealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = DeepTealContainer,
    onTertiaryContainer = OnDeepTealContainer,
    background = WarmIvoryBackground,
    onBackground = OnWarmSurface,
    surface = WarmSurface,
    onSurface = OnWarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline,
    error = ExpenseCrimson,
    onError = Color.White,
    errorContainer = ExpenseContainer,
    onErrorContainer = ExpenseCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = OnDarkEmerald,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = EmeraldContainer,
    secondary = DarkGoldSecondary,
    onSecondary = OnDarkGold,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = BurnishedGoldContainer,
    tertiary = DeepTealContainer,
    onTertiary = OnDeepTealContainer,
    background = DarkForestBackground,
    onBackground = DarkTextPrimary,
    surface = DarkForestSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = Color(0xFFF97066),
    onError = Color(0xFF550D08),
    errorContainer = Color(0xFF7A1912),
    onErrorContainer = Color(0xFFFEE4E2)
)

val OrgShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SongotonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OrgShapes,
        content = content
    )
}
