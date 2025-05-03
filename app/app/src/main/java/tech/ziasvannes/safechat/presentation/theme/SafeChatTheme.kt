package tech.ziasvannes.safechat.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes

// Define the color palette for the app
object SafeChatColors {
    // Primary brand colors
    val Primary = Color(0xFF2196F3)
    val PrimaryVariant = Color(0xFF0D47A1)
    val OnPrimary = Color.White

    // Secondary accent colors
    val Secondary = Color(0xFF03DAC5)
    val SecondaryVariant = Color(0xFF018786)
    val OnSecondary = Color.Black

    // Background and surface colors for dark theme
    val BackgroundDark = Color(0xFF121212)
    val SurfaceDark = Color(0xFF1E1E1E)
    val OnBackgroundDark = Color.White
    val OnSurfaceDark = Color.White

    // Background and surface colors for light theme
    val BackgroundLight = Color.White
    val SurfaceLight = Color(0xFFF5F5F5)
    val OnBackgroundLight = Color.Black
    val OnSurfaceLight = Color.Black

    // Error colors
    val Error = Color(0xFFB00020)
    val OnError = Color.White
    
    // Status colors
    val Online = Color(0xFF4CAF50)
    val Away = Color(0xFFFFEB3B)
    val Offline = Color(0xFF9E9E9E)
    
    // Encryption status colors
    val Encrypted = Color(0xFF4CAF50)
    val NotEncrypted = Color(0xFFB00020)
    val KeyExchangeInProgress = Color(0xFFFFEB3B)
}

// Dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = SafeChatColors.Primary,
    onPrimary = SafeChatColors.OnPrimary,
    primaryContainer = SafeChatColors.PrimaryVariant,
    onPrimaryContainer = SafeChatColors.OnPrimary,
    secondary = SafeChatColors.Secondary,
    onSecondary = SafeChatColors.OnSecondary,
    secondaryContainer = SafeChatColors.SecondaryVariant,
    onSecondaryContainer = SafeChatColors.OnSecondary,
    background = SafeChatColors.BackgroundDark,
    onBackground = SafeChatColors.OnBackgroundDark,
    surface = SafeChatColors.SurfaceDark,
    onSurface = SafeChatColors.OnSurfaceDark,
    error = SafeChatColors.Error,
    onError = SafeChatColors.OnError
)

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = SafeChatColors.Primary,
    onPrimary = SafeChatColors.OnPrimary,
    primaryContainer = SafeChatColors.PrimaryVariant,
    onPrimaryContainer = SafeChatColors.OnPrimary,
    secondary = SafeChatColors.Secondary,
    onSecondary = SafeChatColors.OnSecondary,
    secondaryContainer = SafeChatColors.SecondaryVariant,
    onSecondaryContainer = SafeChatColors.OnSecondary,
    background = SafeChatColors.BackgroundLight,
    onBackground = SafeChatColors.OnBackgroundLight,
    surface = SafeChatColors.SurfaceLight,
    onSurface = SafeChatColors.OnSurfaceLight,
    error = SafeChatColors.Error,
    onError = SafeChatColors.OnError
)

// Define the typography for the app
val SafeChatTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

// Define shape definitions for UI components
val SafeChatShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

/**
 * Applies the SafeChat app theme to its content, selecting dark or light mode based on the system setting or an explicit parameter.
 *
 * Wraps the provided composable content in a Material3 theme using SafeChat's color palette, typography, and shapes for consistent styling across the app.
 *
 * @param darkTheme If true, applies the dark theme; if false, applies the light theme. Defaults to the system's dark theme setting.
 * @param content The composable content to which the theme will be applied.
 */
@Composable
fun SafeChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SafeChatTypography,
        shapes = SafeChatShapes,
        content = content
    )
}