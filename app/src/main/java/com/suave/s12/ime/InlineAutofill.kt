package com.suave.s12.ime

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.View
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
import com.suave.s12.utils.TAG
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
    private val waitingAt = AtomicInteger(-1)
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

    fun markWaiting() {
        waitingAt.set(sequence.get())
        _status.value = INLINE_STATUS_WAIT
    }

    fun clear() {
        val id = sequence.incrementAndGet()
        waitingAt.set(-1)
        _status.value = INLINE_STATUS_IDLE
        scope.launch {
            setterGuard.withLock {
                if (sequence.get() == id) {
                    _suggestions.value = emptyList()
                }
            }
        }
    }

    /**
     * AOSP sends an empty inline response on every input start before the real fill
     * arrives. Applying that ping as af=0 wipes chips and cancels in-flight inflate.
     * Only a fill that arrives while we are still waiting is a real empty result.
     */
    fun offerEmptyResponse(): Boolean {
        if (_status.value != INLINE_STATUS_WAIT || sequence.get() != waitingAt.get()) {
            return false
        }
        waitingAt.set(-1)
        _status.value = INLINE_STATUS_EMPTY
        scope.launch {
            setterGuard.withLock {
                if (_status.value == INLINE_STATUS_EMPTY) {
                    _suggestions.value = emptyList()
                }
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun show(
        context: Context,
        raw: List<InlineSuggestion>,
    ) {
        if (raw.isEmpty()) {
            if (!offerEmptyResponse()) {
                Log.d(TAG, "skip empty inline response status=${_status.value}")
            }
            return
        }
        waitingAt.set(-1)
        val id = sequence.incrementAndGet()
        scope.launch {
            val size = Size(INLINE_INFLATE_WRAP, INLINE_INFLATE_WRAP)
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
                } catch (e: RuntimeException) {
                    Log.w(TAG, "inline suggestion inflate failed at $index", e)
                    latch.countDown()
                }
            }
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
            val inflated =
                slots.filterNotNull().sortedBy { it.pinned }
            val label = inlineChipStatus(raw.size, inflated.size)
            if (inflated.size < raw.size) {
                Log.w(TAG, "inline suggestion inflate $label of ${raw.size}")
            }
            setterGuard.withLock {
                if (sequence.get() == id) {
                    _suggestions.value = inflated
                    _status.value = label
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
    uiExtras: Bundle = Bundle(),
    maxCount: Int = INLINE_SUGGESTION_MAX_COUNT,
): InlineSuggestionsRequest {
    val extrasVersions = UiVersions.getVersions(uiExtras)
    if (extrasVersions.isNotEmpty() && !extrasVersions.contains(UiVersions.INLINE_UI_VERSION_1)) {
        Log.w(TAG, "inline ui extras versions=$extrasVersions omit v1")
    }
    val spec =
        InlinePresentationSpec.Builder(inlinePresentationMinSize(), inlinePresentationMaxSize())
            .setStyle(inlineSuggestionStyleBundle(context))
            .build()
    val specs = List(maxCount.coerceAtLeast(1)) { spec }
    Log.d(
        TAG,
        "inline suggestions request heightPx=${heightPx.coerceAtLeast(1)} " +
            "maxCount=${maxCount.coerceAtLeast(1)} extrasVersions=$extrasVersions",
    )
    return InlineSuggestionsRequest.Builder(specs)
        .setMaxSuggestionCount(maxCount.coerceAtLeast(1))
        .build()
}

internal fun inlinePresentationMinSize(): Size =
    Size(INLINE_PRESENTATION_MIN_PX, INLINE_PRESENTATION_MIN_PX)

internal fun inlinePresentationMaxSize(): Size =
    Size(INLINE_PRESENTATION_MAX_PX, INLINE_PRESENTATION_MAX_PX)

internal const val INLINE_SUGGESTION_MAX_COUNT = 4
internal const val INLINE_PRESENTATION_MIN_PX = 0
internal const val INLINE_PRESENTATION_MAX_PX = Int.MAX_VALUE
internal const val INLINE_INFLATE_WRAP = -2
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

@RequiresApi(Build.VERSION_CODES.R)
private fun inlineSuggestionStyleBundle(context: Context): Bundle {
    val night =
        (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    val bg = if (night) Color.parseColor("#FF3C3C3C") else Color.parseColor("#FFE8E8E8")
    val fg = if (night) Color.WHITE else Color.parseColor("#FF202124")
    val muted = if (night) Color.parseColor("#B3FFFFFF") else Color.parseColor("#99202124")
    val chip = android.graphics.drawable.Icon.createWithResource(context, R.drawable.inline_suggestion_chip)
    val padH = dp(context, 8)
    val style =
        InlineSuggestionUi
            .newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackground(chip)
                    .setBackgroundColor(bg)
                    .setPadding(0, 0, 0, 0)
                    .build(),
            ).setChipStyle(
                ViewStyle.Builder()
                    .setBackground(chip)
                    .setBackgroundColor(bg)
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
