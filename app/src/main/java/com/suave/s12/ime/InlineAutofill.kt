package com.suave.s12.ime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.suave.s12.utils.TAG
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
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
     * AOSP injects an empty InlineSuggestionsResponse on every [onStartInput] before the real
     * fill. Returning false from [android.inputmethodservice.InputMethodService.onInlineSuggestionsResponse]
     * tells the framework the IME is not interested, which cancels the session and drops
     * Bitwarden's later chips. Keep waiting; only time out to [INLINE_STATUS_EMPTY] if nothing
     * arrives.
     */
    fun offerEmptyResponse(): Boolean {
        if (_suggestions.value.isNotEmpty()) {
            return true
        }
        if (_status.value != INLINE_STATUS_WAIT) {
            return true
        }
        val waitSeq = sequence.get()
        scope.launch {
            delay(INLINE_EMPTY_GRACE_MS)
            setterGuard.withLock {
                if (sequence.get() == waitSeq &&
                    _status.value == INLINE_STATUS_WAIT &&
                    _suggestions.value.isEmpty()
                ) {
                    waitingAt.set(-1)
                    _status.value = INLINE_STATUS_EMPTY
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
        val heightPx = inflateHeightPx.get().coerceAtLeast(1)
        scope.launch {
            val size = Size(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx)
            val slots = arrayOfNulls<InflatedInlineSuggestion>(raw.size)
            val latch = java.util.concurrent.CountDownLatch(raw.size)
            val executor = ContextCompat.getMainExecutor(context)
            raw.forEachIndexed { index, suggestion ->
                inflateSuggestion(context, suggestion, size, executor) { view ->
                    if (view != null) {
                        slots[index] = InflatedInlineSuggestion(view, suggestion.info.isPinned)
                    }
                    latch.countDown()
                }
            }
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
            val inflated = slots.filterNotNull().sortedBy { it.pinned }
            val fillable = inflated.count { !it.pinned }
            val pinned = inflated.count { it.pinned }
            val label = inlineChipStatus(raw.size, inflated.size)
            Log.d(
                TAG,
                "inline inflate raw=${raw.size} inflated=${inflated.size} " +
                    "fillable=$fillable pinned=$pinned status=$label",
            )
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
    specCount: Int = INLINE_SUGGESTION_SPEC_COUNT,
): InlineSuggestionsRequest {
    val extrasVersions = UiVersions.getVersions(uiExtras)
    val style = inlineSuggestionStyleBundle(context)
    val styleVersions = UiVersions.getVersions(style)
    if (extrasVersions.isNotEmpty() && !extrasVersions.contains(UiVersions.INLINE_UI_VERSION_1)) {
        Log.w(TAG, "inline ui extras versions=$extrasVersions omit v1")
    }
    val min =
        inlinePresentationMinSize(
            dp(context, INLINE_PRESENTATION_MIN_WIDTH_DP),
            dp(context, INLINE_PRESENTATION_MIN_HEIGHT_DP),
        )
    val maxHeight = max(heightPx, dp(context, INLINE_PRESENTATION_MAX_HEIGHT_DP))
    val max = inlinePresentationMaxSize(maxHeight, context.resources.displayMetrics.widthPixels)
    val count = maxCount.coerceAtLeast(1)
    val specs =
        List(specCount.coerceAtLeast(1)) {
            InlinePresentationSpec.Builder(min, max).setStyle(style).build()
        }
    Log.d(
        TAG,
        "inline suggestions request maxCount=$count specCount=${specs.size} " +
            "min=${min.width}x${min.height} max=${max.width}x${max.height} " +
            "styleVersions=$styleVersions extrasVersions=$extrasVersions " +
            "hostChange=n",
    )
    val builder =
        InlineSuggestionsRequest.Builder(specs)
            .setMaxSuggestionCount(count)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setInlineTooltipPresentationSpec(specs.last())
    }
    return builder.build()
}

internal fun inlinePresentationMaxWidthPx(screenWidthPx: Int): Int =
    max(
        INLINE_PRESENTATION_MAX_WIDTH_PX,
        screenWidthPx * 2 / 3,
    ).coerceAtLeast(1)

internal fun inlinePresentationMinSize(
    minWidthPx: Int,
    minHeightPx: Int,
): Size = Size(minWidthPx.coerceAtLeast(1), minHeightPx.coerceAtLeast(1))

internal fun inlinePresentationMaxSize(
    heightPx: Int,
    screenWidthPx: Int,
): Size = Size(inlinePresentationMaxWidthPx(screenWidthPx), heightPx.coerceAtLeast(1))

internal const val INLINE_SUGGESTION_MAX_COUNT = 6
internal const val INLINE_SUGGESTION_SPEC_COUNT = 6
internal const val INLINE_PRESENTATION_MIN_WIDTH_DP = 32
internal const val INLINE_PRESENTATION_MIN_HEIGHT_DP = 8
internal const val INLINE_PRESENTATION_MAX_HEIGHT_DP = 48
internal const val INLINE_PRESENTATION_MAX_WIDTH_PX = 740
internal const val INLINE_EMPTY_GRACE_MS = 1_500L
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
            .createWithResource(
                context,
                androidx.autofill.R.drawable.autofill_inline_suggestion_chip_background,
            ).setTint(bg)
    val padH = dp(context, 8)
    val titleMargin = dp(context, 4)
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
                    .setLayoutMargin(titleMargin, 0, titleMargin, 0)
                    .setTextColor(fg)
                    .setTextSize(14f)
                    .build(),
            ).setSubtitleStyle(
                TextViewStyle
                    .Builder()
                    .setLayoutMargin(titleMargin, 0, titleMargin, 0)
                    .setTextColor(muted)
                    .setTextSize(12f)
                    .build(),
            ).build()
    return UiVersions.newStylesBuilder().addStyle(style).build()
}

@RequiresApi(Build.VERSION_CODES.R)
private fun inflateSuggestion(
    context: Context,
    suggestion: InlineSuggestion,
    size: Size,
    executor: java.util.concurrent.Executor,
    callback: (View?) -> Unit,
) {
    try {
        suggestion.inflate(context, size, executor, callback)
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "inline inflate size=${size.width}x${size.height} rejected, retry wrap", e)
        try {
            suggestion.inflate(
                context,
                Size(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                executor,
                callback,
            )
        } catch (retry: RuntimeException) {
            Log.w(TAG, "inline suggestion inflate failed", retry)
            callback(null)
        }
    } catch (e: RuntimeException) {
        Log.w(TAG, "inline suggestion inflate failed", e)
        callback(null)
    }
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
