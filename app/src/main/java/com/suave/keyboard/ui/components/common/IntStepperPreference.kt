package com.suave.keyboard.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suave.keyboard.R
import com.suave.keyboard.engine.feedback.HapticType
import com.suave.keyboard.ui.engine.playHaptic
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
 *
 * Layout is stacked: title (and optional summary) on top, then a right-aligned control
 * cluster (optional reset, then the - / value / + pill) on the row below so long titles
 * are not squeezed beside the controls.
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
    onReset: (() -> Unit)? = null,
    resetTo: Int? = null,
) {
    Preference(
        title = title,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        icon = icon,
        summary = {
            Column(modifier = Modifier.fillMaxWidth()) {
                summary?.invoke()
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = if (summary != null) 8.dp else 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreferenceControlCluster {
                        if (onReset != null) {
                            PreferenceResetButton(
                                onClick = onReset,
                                enabled = enabled,
                                resetToLabel = resetTo?.toString(),
                            )
                        }
                        StepperPill(
                            value = value,
                            enabled = enabled,
                            canDecrease = value > valueRange.first,
                            canIncrease = value < valueRange.last,
                            vibrateOnRepeat = vibrateOnRepeat,
                            repeatHapticType = repeatHapticType,
                            onDecrease = {
                                nextStepperValue(value, -step, valueRange)?.let(onValueChange)
                            },
                            onIncrease = {
                                nextStepperValue(value, step, valueRange)?.let(onValueChange)
                            },
                        )
                    }
                }
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
private fun StepperPill(
    value: Int,
    enabled: Boolean,
    canDecrease: Boolean,
    canIncrease: Boolean,
    vibrateOnRepeat: Boolean,
    repeatHapticType: HapticType,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(percent = 50),
                )
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        RepeatingStepperButton(
            onClick = onDecrease,
            enabled = enabled && canDecrease,
            vibrateOnRepeat = vibrateOnRepeat,
            repeatHapticType = repeatHapticType,
            imageVector = Icons.Outlined.Remove,
            contentDescription = stringResource(R.string.decrease_value),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        Text(
            text = value.toString(),
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        RepeatingStepperButton(
            onClick = onIncrease,
            enabled = enabled && canIncrease,
            vibrateOnRepeat = vibrateOnRepeat,
            repeatHapticType = repeatHapticType,
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.increase_value),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun RepeatingStepperButton(
    onClick: () -> Unit,
    enabled: Boolean,
    vibrateOnRepeat: Boolean,
    repeatHapticType: HapticType,
    imageVector: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color,
    disabledContentColor: Color,
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

    FilledIconButton(
        onClick = { if (!repeating) onClickState.value() },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}
