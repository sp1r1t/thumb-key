package com.suave.s12.ui.engine

import android.os.Build
import android.view.ViewGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.suave.s12.IMEService
import com.suave.s12.ime.InflatedInlineSuggestion

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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        suggestions.forEach { suggestion ->
            key(System.identityHashCode(suggestion.view)) {
                AndroidView(
                    factory = { _ ->
                        (suggestion.view.parent as? ViewGroup)?.removeView(suggestion.view)
                        suggestion.view
                    },
                    modifier = Modifier.height((heightDp - 8).coerceAtLeast(1).dp),
                )
            }
        }
    }
}
