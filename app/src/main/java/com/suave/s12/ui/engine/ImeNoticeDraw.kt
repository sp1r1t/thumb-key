package com.suave.s12.ui.engine

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suave.s12.IMEService
import com.suave.s12.ImeNotice
import kotlinx.coroutines.delay

internal data class ImeNoticeDraw(
    val text: String,
    val alpha: Float,
    val background: Color,
    val foreground: Color,
)

@Composable
internal fun rememberImeNoticeDraw(ime: IMEService): ImeNoticeDraw? {
    val notice by ime.notice.collectAsState()
    var fading by remember { mutableStateOf<ImeNotice?>(null) }
    val visible = notice != null
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "imeNotice",
        finishedListener = { if (!visible) fading = null },
    )
    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        fading = current
        delay(1100)
        ime.clearNotice(current.seq)
    }
    val text = (notice ?: fading)?.text ?: return null
    if (alpha == 0f && !visible) return null
    val colors = MaterialTheme.colorScheme
    return ImeNoticeDraw(
        text = text,
        alpha = alpha,
        background = colors.inverseSurface,
        foreground = colors.inverseOnSurface,
    )
}

/**
 * Paints the IME notice over the keyboard without introducing a layout node. A composed
 * Text overlay stays a hit target even at graphicsLayer alpha 0, so DOWN can land on a
 * key (haptic) and UP get stolen, or the pill can swallow a left swipe for g.
 */
internal fun Modifier.drawImeNotice(
    notice: ImeNoticeDraw?,
    textMeasurer: TextMeasurer,
): Modifier {
    if (notice == null || notice.alpha <= 0f) return this
    return drawWithContent {
        drawContent()
        val topPad = 8.dp.toPx()
        val hPad = 16.dp.toPx()
        val vPad = 8.dp.toPx()
        val maxTextWidth = (size.width - 2 * hPad).toInt().coerceAtLeast(0)
        val layout =
            textMeasurer.measure(
                text = notice.text,
                style =
                    TextStyle(
                        color = notice.foreground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxTextWidth),
            )
        val pillWidth = layout.size.width + 2 * hPad
        val pillHeight = layout.size.height + 2 * vPad
        val left = (size.width - pillWidth) / 2f
        val top = topPad
        drawRoundRect(
            color = notice.background.copy(alpha = notice.background.alpha * notice.alpha),
            topLeft = Offset(left, top),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f),
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(left + hPad, top + vPad),
            alpha = notice.alpha,
        )
    }
}
