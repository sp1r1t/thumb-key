package com.suave.s12.ime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InlineSuggestion
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.content.ContextCompat
import com.suave.s12.R
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class InflatedInlineSuggestion(
    val view: View,
    val pinned: Boolean,
)

/**
 * Hosts Autofill Framework inline chips (Bitwarden, Google, 1Password, and any other service
 * the user picked in system settings). The IME never sees passwords: it only inflates the
 * RemoteViews the service already rendered.
 */
class InlineAutofillHost {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val setterGuard = Mutex()
    private val sequence = AtomicInteger(0)
    private val inflateHeightPx = AtomicInteger(0)
    private val _suggestions = MutableStateFlow<List<InflatedInlineSuggestion>>(emptyList())
    val suggestions: StateFlow<List<InflatedInlineSuggestion>> = _suggestions.asStateFlow()
    private val _status = MutableStateFlow(INLINE_STATUS_IDLE)
    val status: StateFlow<String> = _status.asStateFlow()

    fun pickTopFillable(): InflatedInlineSuggestion? = pickTopFillable(_suggestions.value)

    fun acceptTop(): Boolean {
        val view = pickTopFillable()?.view ?: return false
        view.post { view.performClick() }
        return true
    }

    fun markWaiting(heightPx: Int) {
        inflateHeightPx.set(heightPx.coerceAtLeast(1))
        _status.value = INLINE_STATUS_WAIT
    }

    fun clear() {
        val id = sequence.incrementAndGet()
        _status.value = INLINE_STATUS_IDLE
        scope.launch {
            setterGuard.withLock {
                if (sequence.get() == id) {
                    _suggestions.value = emptyList()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun show(
        context: Context,
        raw: List<InlineSuggestion>,
    ) {
        if (raw.isEmpty()) {
            // AOSP sends an empty ping on every onStartInput. Do not tear down chips we already
            // inflated; only record empty when nothing is on the strip yet.
            if (_suggestions.value.isEmpty()) {
                _status.value = INLINE_STATUS_EMPTY
            }
            return
        }
        val id = sequence.incrementAndGet()
        val heightPx = inflateHeightPx.get().coerceAtLeast(1)
        scope.launch {
            val size = Size(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx)
            val slots = arrayOfNulls<InflatedInlineSuggestion>(raw.size)
            val latch = java.util.concurrent.CountDownLatch(raw.size)
            val executor = ContextCompat.getMainExecutor(context)
            raw.forEachIndexed { index, suggestion ->
                try {
                    suggestion.inflate(context, size, executor) { view ->
                        if (view != null) {
                            slots[index] = InflatedInlineSuggestion(view, suggestion.info.isPinned)
                        }
                        latch.countDown()
                    }
                } catch (_: RuntimeException) {
                    latch.countDown()
                }
            }
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
            val inflated = slots.filterNotNull().sortedBy { it.pinned }
            setterGuard.withLock {
                if (sequence.get() == id) {
                    _suggestions.value = inflated
                    _status.value = inlineChipStatus(raw.size, inflated.size)
                }
            }
        }
    }
}

fun pickTopFillable(items: List<InflatedInlineSuggestion>): InflatedInlineSuggestion? =
    pickTopFillable(items) { it.pinned }

fun <T> pickTopFillable(
    items: List<T>,
    pinned: (T) -> Boolean,
): T? = items.firstOrNull { !pinned(it) } ?: items.firstOrNull()

@RequiresApi(Build.VERSION_CODES.R)
fun createInlineSuggestionsRequest(
    context: Context,
    heightPx: Int,
    maxCount: Int = INLINE_SUGGESTION_MAX_COUNT,
): InlineSuggestionsRequest {
    val height = heightPx.coerceAtLeast(1)
    val minWidth = dp(context, 48).coerceAtLeast(1)
    val maxWidth = context.resources.displayMetrics.widthPixels.coerceAtLeast(minWidth)
    val spec =
        InlinePresentationSpec.Builder(Size(minWidth, height), Size(maxWidth, height))
            .setStyle(inlineSuggestionStyleBundle(context))
            .build()
    val specs = List(maxCount.coerceAtLeast(1)) { spec }
    return InlineSuggestionsRequest.Builder(specs)
        .setMaxSuggestionCount(maxCount.coerceAtLeast(1))
        .build()
}

internal const val INLINE_SUGGESTION_MAX_COUNT = 6
internal const val INLINE_STATUS_IDLE = "-"
internal const val INLINE_STATUS_WAIT = "wait"
internal const val INLINE_STATUS_EMPTY = "0"
internal const val INLINE_STATUS_FAIL = "fail"

internal fun inlineChipStatus(
    rawCount: Int,
    inflatedCount: Int,
): String =
    when {
        rawCount <= 0 -> INLINE_STATUS_EMPTY
        inflatedCount <= 0 -> INLINE_STATUS_FAIL
        else -> inflatedCount.toString()
    }

/**
 * Compact debug-bar token: whether this field has Autofill, plus chip state in brackets
 * when it does. Examples: `af=y [6]`, `af=n`, `af=off`, `af=na`.
 */
internal fun formatAutofillDebug(
    sdkAtLeastR: Boolean,
    inlineEnabled: Boolean,
    hasAutofillId: Boolean,
    status: String,
): String {
    if (!sdkAtLeastR) return "af=na"
    if (!inlineEnabled) return "af=off"
    if (!hasAutofillId) return "af=n"
    val state = status.ifEmpty { INLINE_STATUS_IDLE }
    return "af=y [$state]"
}

@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.R)
private fun inlineSuggestionStyleBundle(context: Context): Bundle {
    val night =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val bg = if (night) Color.parseColor("#FF3C3C3C") else Color.parseColor("#FFE8E8E8")
    val fg = if (night) Color.WHITE else Color.parseColor("#FF202124")
    val muted = if (night) Color.parseColor("#B3FFFFFF") else Color.parseColor("#99202124")
    val chip =
        Icon
            .createWithResource(context, R.drawable.inline_suggestion_chip)
            .setTint(bg)
    val padH = dp(context, 8)
    val style =
        InlineSuggestionUi
            .newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackground(chip)
                    .setPadding(0, 0, 0, 0)
                    .build(),
            ).setChipStyle(
                ViewStyle.Builder()
                    .setBackground(chip)
                    .setPadding(padH, 0, padH, 0)
                    .build(),
            ).setStartIconStyle(
                ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build(),
            ).setEndIconStyle(
                ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build(),
            ).setTitleStyle(
                TextViewStyle
                    .Builder()
                    .setTextColor(fg)
                    .setTextSize(14f)
                    .build(),
            ).setSubtitleStyle(
                TextViewStyle
                    .Builder()
                    .setTextColor(muted)
                    .setTextSize(12f)
                    .build(),
            ).build()
    return UiVersions.newStylesBuilder().addStyle(style).build()
}

private fun dp(
    context: Context,
    value: Int,
): Int =
    TypedValue
        .applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics,
        ).toInt()
