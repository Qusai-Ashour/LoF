package com.leapoffaith.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary             = Gold,
    onPrimary           = NavyBackground,
    primaryContainer    = GoldDim,
    onPrimaryContainer  = TextPrimary,
    secondary           = GreenDone,
    onSecondary         = NavyBackground,
    background          = NavyBackground,
    onBackground        = TextPrimary,
    surface             = NavyCard,
    onSurface           = TextPrimary,
    surfaceVariant      = NavyCardLight,
    onSurfaceVariant    = TextSecondary,
    error               = RedMissed,
    onError             = TextPrimary,
    outline             = GreyEmpty
)

private val LightColorScheme = lightColorScheme(
    primary             = ForestGreen,
    onPrimary           = LightCard,
    primaryContainer    = MintAccent,
    onPrimaryContainer  = MossGreen,
    secondary           = LeafGreen,
    onSecondary         = LightCard,
    background          = LightBackground,
    onBackground        = LightTextPrimary,
    surface             = LightCard,
    onSurface           = LightTextPrimary,
    surfaceVariant      = LightCardVariant,
    onSurfaceVariant    = LightTextSecondary,
    error               = LightRedMissed,
    onError             = LightCard,
    outline             = LightGreyEmpty
)

@Composable
fun LeapOfFaithTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = LoFTypography,
        content     = content
    )
}
