package com.mimo.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val DarkBackground = Color(0xFF121214)
val DarkSurface = Color(0xFF161618)
val DarkCard = Color(0xFF1C1C1F)
val DarkCardHover = Color(0xFF222225)

val Primary = Color(0xFFD0BCFF)
val OnPrimary = Color(0xFF381E72)
val PrimaryContainer = Color(0xFF352A5C)
val OnPrimaryContainer = Color(0xFFEADDFF)

val Secondary = Color(0xFFCCC2DC)
val OnSecondary = Color(0xFF332D41)
val SecondaryContainer = Color(0xFF3D3850)
val OnSecondaryContainer = Color(0xFFE8DEF8)

val Tertiary = Color(0xFFEFB8C8)
val OnTertiary = Color(0xFF492532)
val TertiaryContainer = Color(0xFF4A2A38)
val OnTertiaryContainer = Color(0xFFFFD8E4)

val Error = Color(0xFFF2B8B5)
val OnError = Color(0xFF601410)
val ErrorContainer = Color(0xFF5C1A18)
val OnErrorContainer = Color(0xFFF9DEDC)

val SurfaceVariant = Color(0xFF1E1E22)
val OnSurfaceVariant = Color(0xFFB0AAB6)
val Outline = Color(0xFF6B6572)
val OutlineVariant = Color(0xFF2A2A2E)

val TextPrimary = Color(0xFFDDD8E0)
val TextSecondary = Color(0xFFA09AA8)
val TextMuted = Color(0xFF6E6878)

val AccentRed = Color(0xFFF2B8B5)
val AccentGreen = Color(0xFFA8DAB5)
val AccentOrange = Color(0xFFFFCC80)

@Composable
fun MiMoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    userDarkMode: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isDark = userDarkMode ?: darkTheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceVariant,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            outlineVariant = OutlineVariant
        )
        else -> lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
