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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.suave.s12.R
import kotlinx.coroutines.delay
import me.zhanghai.compose.preference.Preference

private const val INITIAL_REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 70L

/**
 * Preference row with minus/plus buttons for an integer setting. One tap moves [step]; holding
 * a button repeats so a wide range (key height, swipe length) is still reachable without a slider.
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
                    onClick = { onValueChange((value - step).coerceIn(valueRange)) },
                    enabled = enabled && value > valueRange.first,
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.decrease_value),
                )
                RepeatingIconButton(
                    onClick = { onValueChange((value + step).coerceIn(valueRange)) },
                    enabled = enabled && value < valueRange.last,
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.increase_value),
                )
            }
        },
    )
}

@Composable
private fun RepeatingIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    imageVector: ImageVector,
    contentDescription: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var repeated by remember { mutableStateOf(false) }

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect
        repeated = false
        delay(INITIAL_REPEAT_DELAY_MS)
        repeated = true
        while (true) {
            onClick()
            delay(REPEAT_INTERVAL_MS)
        }
    }

    IconButton(
        onClick = { if (!repeated) onClick() },
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}
