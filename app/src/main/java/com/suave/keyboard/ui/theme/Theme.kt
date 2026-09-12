package com.suave.keyboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.suave.keyboard.db.AppSettings
import com.suave.keyboard.db.DEFAULT_THEME_COLOR
import com.suave.keyboard.utils.ThemeMode

@Composable
fun SuaveTheme(
    settings: AppSettings?,
    content: @Composable () -> Unit,
) {
    val themeMode = ThemeMode.entries[settings?.theme ?: 0]
    val themeId = settings?.themeColor ?: DEFAULT_THEME_COLOR

    val ctx = LocalContext.current
    ThemeRegistry.ensureLoaded(ctx)
    ThemeStore.get(ctx).loadIntoRegistry()

    val android12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Dynamic schemes crash on lower than android 12
    val dynamicPair =
        if (android12OrLater) {
            Pair(dynamicLightColorScheme(ctx), dynamicDarkColorScheme(ctx))
        } else {
            ThemeRegistry.byId(NamedTheme.DEFAULT_ID).schemes
        }

    val colorPair =
        if (ThemeRegistry.isDynamic(themeId)) {
            dynamicPair
        } else {
            ThemeRegistry.colorSchemes(ctx, themeId)
        }

    val systemTheme =
        if (!isSystemInDarkTheme()) {
            colorPair.first
        } else {
            colorPair.second
        }

    val colors =
        when (themeMode) {
            ThemeMode.System -> systemTheme
            ThemeMode.Light -> colorPair.first
            ThemeMode.Dark -> colorPair.second
        }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
