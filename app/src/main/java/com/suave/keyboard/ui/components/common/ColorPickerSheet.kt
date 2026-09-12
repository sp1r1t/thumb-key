package com.suave.keyboard.ui.components.common

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suave.keyboard.R
import com.suave.keyboard.ui.theme.json.parseArgbHex
import com.suave.keyboard.ui.theme.json.toArgbHex
import kotlin.math.roundToInt

/**
 * Bottom sheet to edit a theme color: SV square, hue slider, and hex field.
 * Confirms with [onConfirm] as ARGB hex (`#AARRGGBB`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    role: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialColor = runCatching { parseArgbHex(initialHex) }.getOrElse { Color.Gray }
    val initialHsv = remember(initialColor) { initialColor.toHsv() }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(initialColor.toArgbHex()) }
    val alpha = remember(initialColor) { initialColor.alpha }

    fun applyHsv(
        h: Float,
        s: Float,
        v: Float,
    ) {
        hue = h
        saturation = s
        value = v
        hexText = hsvToColor(h, s, v, alpha).toArgbHex()
    }

    fun applyHex(raw: String) {
        hexText = raw
        val parsed = runCatching { parseArgbHex(raw) }.getOrNull() ?: return
        val hsv = parsed.toHsv()
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val current = hsvToColor(hue, saturation, value, alpha)

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_color_picker_title, role),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(current)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { applyHex(it) },
                    label = { Text(stringResource(R.string.theme_color_hex)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = runCatching { parseArgbHex(hexText) }.isFailure,
                )
            }
            SaturationValueBox(
                hue = hue,
                saturation = saturation,
                value = value,
                onChange = { s, v -> applyHsv(hue, s, v) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
            )
            Text(
                text = stringResource(R.string.theme_color_hue),
                style = MaterialTheme.typography.labelMedium,
            )
            HueSlider(
                hue = hue,
                onHueChange = { applyHsv(it, saturation, value) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val parsed = runCatching { parseArgbHex(hexText) }.getOrNull()
                        if (parsed != null) {
                            onConfirm(parsed.toArgbHex())
                        }
                    },
                    enabled = runCatching { parseArgbHex(hexText) }.isSuccess,
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColor = hsvToColor(hue, 1f, 1f, 1f)
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(hue) {
                    detectTapGestures { pos ->
                        onChange(
                            (pos.x / size.width).coerceIn(0f, 1f),
                            (1f - pos.y / size.height).coerceIn(0f, 1f),
                        )
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onChange(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            (1f - change.position.y / size.height).coerceIn(0f, 1f),
                        )
                    }
                },
    ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                val x = saturation * size.width
                val y = (1f - value) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = Offset(x, y),
                )
                drawCircle(
                    color = Color.Black,
                    radius = 10.dp.toPx(),
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = hsvToColor(hue, saturation, value, 1f),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spectrum =
        remember {
            listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red,
            )
        }
    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(spectrum)),
        )
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv
}

private fun hsvToColor(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
): Color {
    val argb =
        AndroidColor.HSVToColor(
            (alpha * 255f).roundToInt().coerceIn(0, 255),
            floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f)),
        )
    return Color(argb)
}
