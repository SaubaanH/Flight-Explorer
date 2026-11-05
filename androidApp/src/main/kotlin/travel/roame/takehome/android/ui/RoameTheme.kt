package travel.roame.takehome.android.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RoameColors {
    val Pink = Color(0xFFE83A6F)
    val PinkSoft = Color(0xFFFFE6EE)
    val PinkDeep = Color(0xFFC9184A)
    val Coral = Color(0xFFFF8B5C)
    val Ink = Color(0xFF1A1A1A)
    val Muted = Color(0xFF6B6B6B)
    val Hairline = Color(0xFFE8E6E3)
    val Canvas = Color(0xFFF6F4F1)
    val Surface = Color(0xFFFFFFFF)
    val Offline = Color(0xFFFFF1E0)
    val ErrorSoft = Color(0xFFFFE8EC)

    val CtaGradient = Brush.horizontalGradient(listOf(Coral, Pink))
}

object RoameShapes {
    val Pill = RoundedCornerShape(999.dp)
    val Card = RoundedCornerShape(20.dp)
    val Panel = RoundedCornerShape(24.dp)
    val Chip = RoundedCornerShape(14.dp)
}

private val RoameTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = RoameColors.Ink,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = RoameColors.Ink,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = RoameColors.Ink,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = RoameColors.Ink,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = RoameColors.Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = RoameColors.Ink,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = RoameColors.Ink,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = RoameColors.Muted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = RoameColors.Ink,
    ),
)

private val RoameColorScheme = lightColorScheme(
    primary = RoameColors.Pink,
    onPrimary = Color.White,
    primaryContainer = RoameColors.PinkSoft,
    onPrimaryContainer = RoameColors.PinkDeep,
    secondary = RoameColors.Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8DE),
    onSecondaryContainer = Color(0xFF7A2E10),
    tertiary = RoameColors.PinkDeep,
    background = RoameColors.Canvas,
    onBackground = RoameColors.Ink,
    surface = RoameColors.Surface,
    onSurface = RoameColors.Ink,
    surfaceVariant = Color(0xFFF0EEEA),
    onSurfaceVariant = RoameColors.Muted,
    outline = RoameColors.Hairline,
    error = RoameColors.PinkDeep,
    errorContainer = RoameColors.ErrorSoft,
)

@Composable
fun RoameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoameColorScheme,
        typography = RoameTypography,
        content = content,
    )
}
