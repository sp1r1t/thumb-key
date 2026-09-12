package com.suave.keyboard.utils

import androidx.annotation.StringRes
import com.suave.keyboard.R

enum class ColorVariant {
    PRIMARY,
    SECONDARY,
    SURFACE,
    SURFACE_VARIANT,
    MUTED,
}

enum class FontSizeVariant {
    LARGE,
    SMALL,
    SMALLEST,
}

enum class ThemeMode(
    @param:StringRes val resId: Int,
) {
    System(R.string.system),
    Light(R.string.light),
    Dark(R.string.dark),
}

/**
 * Legacy enum kept only for title string resources and ordinal->id migration notes.
 * Runtime selection uses [com.suave.keyboard.ui.theme.ThemeRegistry] string ids.
 */
enum class ThemeColor(
    @param:StringRes val resId: Int,
    val id: String,
) {
    Dynamic(R.string.dynamic, "dynamic"),
    Green(R.string.green, "green"),
    Pink(R.string.pink, "pink"),
    Srcery(R.string.srcery, "srcery"),
    Blue(R.string.blue, "blue"),
    Dracula(R.string.dracula, "dracula"),
    Twilight(R.string.twilight, "twilight"),
    HighContrast(R.string.high_contrast, "highContrast"),
    HighContrastColorful(R.string.high_contrast_colorful, "highContrastColorful"),
    Ancom(R.string.ancom, "ancom"),
    Matrix(R.string.matrix, "matrix"),
    Neon(R.string.neon, "neon"),
    /** Soft black-and-white brand theme. */
    Suave(R.string.theme_color_suave, "suave"),
    ;

    companion object {
        fun idFromLegacyOrdinal(ordinal: Int): String =
            entries.getOrNull(ordinal)?.id ?: Suave.id
    }
}

enum class KeyboardPosition(
    @param:StringRes val resId: Int,
) {
    Center(R.string.center),
    Right(R.string.right),
    Left(R.string.left),
    Dual(R.string.dual),
    Split(R.string.split),
}
