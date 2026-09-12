package com.suave.keyboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    // Recompose when the active palette is edited in place (same themeColor id).
    val themeRevision by ThemeRegistry.revision.collectAsState()

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

    val named =
        if (ThemeRegistry.isDynamic(themeId)) {
            null
        } else {
            ThemeRegistry.byId(ctx, themeId)
        }
    // Keep the collector live; revision bumps recompose this theme without a settings change.
    @Suppress("UNUSED_VARIABLE")
    val trackedRevision = themeRevision

    val colorPair =
        if (named == null) {
            dynamicPair
        } else {
            named.schemes
        }

    val systemDark = isSystemInDarkTheme()
    val useDark =
        when (themeMode) {
            ThemeMode.System -> systemDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

    val colors = if (useDark) colorPair.second else colorPair.first
    val extras =
        when {
            named == null -> if (useDark) SemanticExtras.SoftDark else SemanticExtras.SoftLight
            useDark -> named.darkExtras
            else -> named.lightExtras
        }

    CompositionLocalProvider(LocalSemanticExtras provides extras) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
