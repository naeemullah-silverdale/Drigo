package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.util.ThemeMode

@Immutable
data class DrigoCustomColors(
    val brandPurple: Color = DrigoBrandPurple,
    val brandMagenta: Color = DrigoBrandMagentaBg,
    val brandGreen: Color = InDriveLimeGreen,
    val cardBackground: Color,
    val cardBorder: Color,
    val inputBackground: Color,
    val dividerColor: Color,
    val success: Color = DrigoSuccess,
    val warning: Color = DrigoWarning,
    val error: Color = DrigoError,
    val info: Color = DrigoInfo,
    val mapOverlayBg: Color,
    val bottomSheetBg: Color,
    val isDark: Boolean
)

val LocalDrigoColors = staticCompositionLocalOf {
    DrigoCustomColors(
        cardBackground = SurfaceLight,
        cardBorder = OutlineVariantLight,
        inputBackground = SurfaceVariantLight,
        dividerColor = OutlineVariantLight,
        mapOverlayBg = SurfaceLight.copy(alpha = 0.96f),
        bottomSheetBg = SurfaceLight,
        isDark = false
    )
}

val MaterialTheme.drigoColors: DrigoCustomColors
    @Composable
    get() = LocalDrigoColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

@Composable
fun DrigoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val customColors = if (darkTheme) {
        DrigoCustomColors(
            brandPurple = DrigoBrandPurple,
            brandMagenta = DrigoBrandMagentaBg,
            brandGreen = SecondaryDark,
            cardBackground = SurfaceDark,
            cardBorder = OutlineVariantDark,
            inputBackground = SurfaceVariantDark,
            dividerColor = OutlineVariantDark,
            success = DrigoSuccess,
            warning = DrigoWarning,
            error = ErrorDark,
            info = DrigoInfo,
            mapOverlayBg = SurfaceDark.copy(alpha = 0.96f),
            bottomSheetBg = SurfaceDark,
            isDark = true
        )
    } else {
        DrigoCustomColors(
            brandPurple = DrigoBrandPurple,
            brandMagenta = DrigoBrandMagentaBg,
            brandGreen = SecondaryLight,
            cardBackground = SurfaceLight,
            cardBorder = OutlineVariantLight,
            inputBackground = SurfaceVariantLight,
            dividerColor = OutlineVariantLight,
            success = DrigoSuccess,
            warning = DrigoWarning,
            error = ErrorLight,
            info = DrigoInfo,
            mapOverlayBg = SurfaceLight.copy(alpha = 0.96f),
            bottomSheetBg = SurfaceLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalDrigoColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun DrigoTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    DrigoTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
