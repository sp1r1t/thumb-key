package com.suave.s12.ui.engine

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.suave.s12.IMEService
import com.suave.s12.ime.InflatedInlineSuggestion
import kotlin.math.roundToInt

@Composable
fun InlineSuggestionStrip(
    ime: IMEService,
    heightDp: Int,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val suggestions by ime.inlineAutofill.suggestions.collectAsState()
    if (suggestions.isEmpty()) return
    InlineSuggestionStripContent(suggestions = suggestions, heightDp = heightDp)
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun InlineSuggestionStripContent(
    suggestions: List<InflatedInlineSuggestion>,
    heightDp: Int,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(scrollState)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        suggestions.forEach { suggestion ->
            key(System.identityHashCode(suggestion.view)) {
                var chipPos by remember { mutableStateOf(IntOffset.Zero) }
                AndroidView(
                    factory = { _ ->
                        (suggestion.view.parent as? ViewGroup)?.removeView(suggestion.view)
                        suggestion.view.apply {
                            ViewCompat.setNestedScrollingEnabled(this, true)
                            if (this is InlineContentView) {
                                isZOrderedOnTop = true
                            }
                        }
                    },
                    update = { view ->
                        if (view is InlineContentView) {
                            view.isZOrderedOnTop = true
                        }
                        val xMin = scrollState.value
                        val xMax = scrollState.value + scrollState.viewportSize
                        view.clipBounds =
                            Rect(
                                (xMin - chipPos.x).coerceAtLeast(0),
                                0,
                                (xMax - chipPos.x).coerceAtMost(view.width).coerceAtLeast(0),
                                view.height,
                            )
                        view.visibility =
                            if (view.clipBounds?.isEmpty != false) {
                                View.INVISIBLE
                            } else {
                                View.VISIBLE
                            }
                    },
                    modifier =
                        Modifier
                            .height((heightDp - 8).coerceAtLeast(1).dp)
                            .onGloballyPositioned {
                                val position = it.positionInParent()
                                chipPos =
                                    IntOffset(
                                        position.x.roundToInt(),
                                        position.y.roundToInt(),
                                    )
                            },
                )
            }
        }
    }
}
