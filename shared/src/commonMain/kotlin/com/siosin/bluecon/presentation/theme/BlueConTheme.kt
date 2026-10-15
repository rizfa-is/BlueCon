package com.siosin.bluecon.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF096B4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF4E4),
    onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF32644E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1ECDD),
    onSecondaryContainer = Color(0xFF102E20),
    tertiary = Color(0xFF805512),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE4B7),
    onTertiaryContainer = Color(0xFF302000),
    background = Color(0xFFF4F8F5),
    onBackground = Color(0xFF18231D),
    surface = Color.White,
    onSurface = Color(0xFF18231D),
    onSurfaceVariant = Color(0xFF4B6054),
    surfaceContainer = Color(0xFFE7F0EB),
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFDEEAE2),
    surfaceContainerHighest = Color(0xFFD7E5DC),
    outline = Color(0xFF6F8175),
    outlineVariant = Color(0xFFCCDCD1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DE4C2),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF145139),
    onPrimaryContainer = Color(0xFFCDF4E4),
    secondary = Color(0xFFAFD3BD),
    onSecondary = Color(0xFF173828),
    secondaryContainer = Color(0xFF304E3E),
    onSecondaryContainer = Color(0xFFD1ECDD),
    tertiary = Color(0xFFE8C07A),
    onTertiary = Color(0xFF442C00),
    tertiaryContainer = Color(0xFF40341B),
    onTertiaryContainer = Color(0xFFF6E1B6),
    background = Color(0xFF0B1511),
    onBackground = Color(0xFFF0F6F3),
    surface = Color(0xFF15241F),
    onSurface = Color(0xFFF0F6F3),
    onSurfaceVariant = Color(0xFFA9BDB6),
    surfaceContainer = Color(0xFF1B2C24),
    surfaceContainerLow = Color(0xFF15241F),
    surfaceContainerHigh = Color(0xFF23352C),
    surfaceContainerHighest = Color(0xFF2B3D33),
    outline = Color(0xFF81958B),
    outlineVariant = Color(0xFF304139),
)

@Composable
fun BlueConTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
