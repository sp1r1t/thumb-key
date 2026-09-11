package com.suave.s12.ui.engine

import android.content.Context
import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.RecyclerView
import com.suave.s12.R

/**
 * Hosts [EmojiPickerView] under a theme the picker actually reads, then tints its chrome
 * (category labels and tab icons) to the keyboard's Compose colors.
 *
 * Category headers use `android:textColorPrimary`. Tab icons use `colorControlNormal` /
 * `colorAccent`. The IME's AppCompat theme is light, which is why those labels showed up
 * black on a dark keyboard before this wrapper existed.
 */
internal fun createThemedEmojiPicker(
    context: Context,
    darkKeyboard: Boolean,
    @ColorInt text: Int,
    @ColorInt icon: Int,
    @ColorInt accent: Int,
): EmojiPickerView {
    val themed =
        ContextThemeWrapper(
            context,
            if (darkKeyboard) R.style.EmojiPickerDark else R.style.EmojiPickerLight,
        )
    return EmojiPickerView(themed).also { picker ->
        picker.tintChrome(text = text, icon = icon, accent = accent)
    }
}

private fun View.tintChrome(
    @ColorInt text: Int,
    @ColorInt icon: Int,
    @ColorInt accent: Int,
) {
    val iconTint =
        ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(accent, icon),
        )

    fun tintThis(view: View) {
        when (view.id) {
            androidx.emoji2.emojipicker.R.id.category_name,
            androidx.emoji2.emojipicker.R.id.emoji_picker_empty_category_view,
            -> if (view is TextView) view.setTextColor(text)
            androidx.emoji2.emojipicker.R.id.emoji_picker_header_icon,
            -> if (view is ImageView) view.imageTintList = iconTint
            androidx.emoji2.emojipicker.R.id.emoji_picker_header_underline,
            -> view.backgroundTintList = iconTint
        }
    }

    fun tintTree(view: View) {
        tintThis(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) tintTree(view.getChildAt(i))
        }
    }

    fun hook(view: View) {
        tintThis(view)
        if (view is RecyclerView) {
            view.addOnChildAttachStateChangeListener(
                object : RecyclerView.OnChildAttachStateChangeListener {
                    override fun onChildViewAttachedToWindow(child: View) = tintTree(child)

                    override fun onChildViewDetachedFromWindow(child: View) = Unit
                },
            )
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) hook(view.getChildAt(i))
        }
    }

    hook(this)
}
