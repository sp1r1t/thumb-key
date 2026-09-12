package com.suave.s12.ui.components.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.suave.s12.R
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.ui.engine.playHaptic
import kotlinx.coroutines.delay
import me.zhanghai.compose.preference.Preference

/** Wait this long after press before repeating, so a tap stays a single step. */
internal const val STEPPER_REPEAT_DELAY_MS = 400L

/** Pause between repeated steps once hold-repeat has started. */
internal const val STEPPER_REPEAT_INTERVAL_MS = 80L

/**
 * Preference row with minus/plus buttons for an integer setting. One tap moves [step]; holding
 * a button past [STEPPER_REPEAT_DELAY_MS] repeats so a wide range (key height, swipe length)
 * is still reachable without a slider. A threshold buzz is optional via [vibrateOnRepeat].
 */
@Composable
fun IntStepperPreference(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    step: Int = 1,
    vibrateOnRepeat: Boolean = true,
    repeatHapticType: HapticType = HapticType.KEYBOARD_TAP,
) {
    Preference(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        widgetContainer = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RepeatingIconButton(
                    onClick = { nextStepperValue(value, -step, valueRange)?.let(onValueChange) },
                    enabled = enabled && value > valueRange.first,
                    vibrateOnRepeat = vibrateOnRepeat,
                    repeatHapticType = repeatHapticType,
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.decrease_value),
                )
                RepeatingIconButton(
                    onClick = { nextStepperValue(value, step, valueRange)?.let(onValueChange) },
                    enabled = enabled && value < valueRange.last,
                    vibrateOnRepeat = vibrateOnRepeat,
                    repeatHapticType = repeatHapticType,
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.increase_value),
                )
            }
        },
    )
}

/** Next value after [delta], or null when already at the end of [range] (so we do not rewrite). */
internal fun nextStepperValue(
    value: Int,
    delta: Int,
    range: IntRange,
): Int? {
    val next = (value + delta).coerceIn(range)
    return next.takeIf { it != value }
}

@Composable
private fun RepeatingIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    vibrateOnRepeat: Boolean,
    repeatHapticType: HapticType,
    imageVector: ImageVector,
    contentDescription: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val onClickState = rememberUpdatedState(onClick)
    val vibrateOnRepeatState = rememberUpdatedState(vibrateOnRepeat)
    val repeatHapticTypeState = rememberUpdatedState(repeatHapticType)
    var repeating by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) {
            repeating = false
            return@LaunchedEffect
        }
        repeating = false
        delay(STEPPER_REPEAT_DELAY_MS)
        repeating = true
        if (vibrateOnRepeatState.value) {
            view.playHaptic(repeatHapticTypeState.value)
        }
        while (true) {
            onClickState.value()
            delay(STEPPER_REPEAT_INTERVAL_MS)
        }
    }

    IconButton(
        onClick = { if (!repeating) onClickState.value() },
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}
