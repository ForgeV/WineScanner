package com.winescanner.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


object WineColors {
    val Background = Color(0xFF14100F)
    val Surface = Color(0xFF1F1917)
    val SurfaceVariant = Color(0xFF2A2220)
    val Burgundy = Color(0xFF7A1F3D)
    val BurgundyLight = Color(0xFFB33A5E)
    val Gold = Color(0xFFC9A24B)
    val Cream = Color(0xFFEDE6E2)
    val CreamMuted = Color(0xFFCBBFB9)
}

private val WineColorScheme = darkColorScheme(
    primary = WineColors.BurgundyLight,
    onPrimary = Color.White,
    primaryContainer = WineColors.Burgundy,
    onPrimaryContainer = Color.White,
    secondary = WineColors.Gold,
    onSecondary = Color(0xFF241B00),
    background = WineColors.Background,
    onBackground = WineColors.Cream,
    surface = WineColors.Surface,
    onSurface = WineColors.Cream,
    surfaceVariant = WineColors.SurfaceVariant,
    onSurfaceVariant = WineColors.CreamMuted,
    error = Color(0xFFCF6679)
)

private val WineTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun WineScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WineColorScheme,
        typography = WineTypography,
        content = content
    )
}
