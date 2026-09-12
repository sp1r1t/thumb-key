package com.suave.s12.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.suave.s12.IMEService
import com.suave.s12.R
import com.suave.s12.db.AppSettingsViewModel
import com.suave.s12.db.DEFAULT_KEYBOARD_LAYOUT
import com.suave.s12.db.LayoutsUpdate
import com.suave.s12.legacy.KeyboardLayout

const val TAG = "com.thumbkey"

const val IME_ACTION_CUSTOM_LABEL = EditorInfo.IME_MASK_ACTION + 1

const val ANIMATION_SPEED = 300

@Composable
fun colorVariantToColor(colorVariant: ColorVariant): Color =
    when (colorVariant) {
        ColorVariant.SURFACE -> MaterialTheme.colorScheme.surface
        ColorVariant.SURFACE_VARIANT -> MaterialTheme.colorScheme.surfaceVariant
        ColorVariant.PRIMARY -> MaterialTheme.colorScheme.primary
        ColorVariant.SECONDARY -> MaterialTheme.colorScheme.secondary
        ColorVariant.MUTED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5F)
    }

fun fontSizeVariantToFontSize(
    fontSizeVariant: FontSizeVariant,
    keySize: Dp,
    isUpperCase: Boolean,
): Dp {
    val divFactor =
        when (fontSizeVariant) {
            FontSizeVariant.LARGE -> 2.5f
            FontSizeVariant.SMALL -> 5f
            FontSizeVariant.SMALLEST -> 8f
        }

    // Make uppercase letters slightly smaller
    val upperCaseFactor =
        if (isUpperCase) {
            0.8f
        } else {
            1f
        }

    return keySize.times(upperCaseFactor).div(divFactor)
}

val Dp.toPx get() = (this * Resources.getSystem().displayMetrics.density).value

val Float.pxToSp
    get() =
        TextUnit(
            this / Resources.getSystem().displayMetrics.scaledDensity,
            TextUnitType.Sp,
        )

fun keyboardPositionToAlignment(position: KeyboardPosition): Alignment =
    when (position) {
        KeyboardPosition.Right -> Alignment.BottomEnd
        KeyboardPosition.Center, KeyboardPosition.Split -> Alignment.BottomCenter
        KeyboardPosition.Left -> Alignment.BottomStart
        KeyboardPosition.Dual -> Alignment.BottomStart
    }

/**
 * Avoid capitalizing or switching to shifted mode in certain edit boxes
 */
fun isUriOrEmailOrPasswordField(ime: IMEService): Boolean {
    val inputType = ime.currentInputEditorInfo.inputType and (InputType.TYPE_MASK_VARIATION)
    return listOf(
        InputType.TYPE_TEXT_VARIATION_URI,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_NUMBER_VARIATION_PASSWORD,
    ).contains(inputType) ||
        ime.currentInputEditorInfo.inputType == EditorInfo.TYPE_NULL
}

fun isPasswordField(ime: IMEService): Boolean {
    val inputType = ime.currentInputEditorInfo.inputType and (InputType.TYPE_MASK_VARIATION)
    return listOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_NUMBER_VARIATION_PASSWORD,
    ).contains(inputType)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(
    text: String,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBack: Boolean = true,
) {
    val activity = LocalActivity.current
    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = text,
            )
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = {
                    // If there's no previous destination, finish the activity
                    // This handles the case when navigating directly to a screen via intent
                    if (navController.previousBackStackEntry == null) {
                        activity?.finish()
                    } else {
                        navController.popBackStack()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.settings),
                    )
                }
            }
        },
    )
}

fun openLink(
    url: String,
    ctx: Context,
) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    ctx.startActivity(intent)
}

fun Int.toBool() = this == 1

fun Boolean.toInt() = this.compareTo(false)

/**
 * The layouts there are whats stored in the DB, a string comma set of title index numbers
 */
fun keyboardLayoutsSetFromDbIndexString(layouts: String?): Set<KeyboardLayout> =
    layouts?.split(",")?.map { KeyboardLayout.entries[it.trim().toInt()] }?.toSet()
        ?: setOf(
            KeyboardLayout.entries[DEFAULT_KEYBOARD_LAYOUT],
        )

fun Context.getPackageInfo(): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }

fun Context.getVersionCode(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getPackageInfo().longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo().versionCode
    }

/**
 * The debug and app IME names act strange, so you need to check both
 */
fun Context.getImeNames(): List<String> =
    listOf(
        "$packageName/com.suave.s12.IMEService",
        "$packageName/.IMEService",
    )

fun updateLayouts(
    appSettingsViewModel: AppSettingsViewModel,
    layoutsState: Set<KeyboardLayout>,
) {
    appSettingsViewModel.updateLayouts(
        LayoutsUpdate(
            id = 1,
            // Set the current to the first
            keyboardLayout = layoutsState.first().ordinal,
            keyboardLayouts =
                layoutsState
                    .map { it.ordinal }
                    .joinToString(),
        ),
    )
}
