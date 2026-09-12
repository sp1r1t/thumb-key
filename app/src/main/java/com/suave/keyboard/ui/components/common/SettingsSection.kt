package com.suave.keyboard.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.suave.keyboard.R
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * A labeled, collapsible group of preference rows, folded by default. Use this instead
 * of a plain divider when a screen has more than one cluster of related settings.
 *
 * Optional [infoText] puts a compact "i" in the section title (same pattern as
 * [SettingTitle]) for arranging/editing help that would crowd the section body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    infoText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var showInfo by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
            )
            if (infoText != null) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.more_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(end = 8.dp)
                            .requiredSize(18.dp)
                            .clickable(role = Role.Button) { showInfo = true },
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription =
                    stringResource(
                        if (expanded) R.string.collapse_section else R.string.expand_section,
                    ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(content = content)
        }
    }

    if (infoText != null && showInfo) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { showInfo = false },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp),
            ) {
                MarkdownText(
                    markdown = infoText,
                    linkColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Settings options scroll in the remaining space. The test-out button stays pinned to
 * the bottom; the text field only takes height while you are actually typing.
 *
 * Scaffold padding already includes the navigation bar (and on some Material3 versions
 * the IME). Adding imePadding() on top of that stacked both insets, leaving a hole
 * between the test field and the keyboard. Take the larger of the two instead.
 */
@Composable
fun SettingsScreenBody(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    settingsScreenBodyPadding(
                        padding = padding,
                        layoutDirection = layoutDirection,
                        imeBottom = imeBottom,
                    ),
                )
                .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            content = content,
        )
        TestOutTextField()
    }
}

internal fun settingsScreenBodyPadding(
    padding: PaddingValues,
    layoutDirection: LayoutDirection,
    imeBottom: Dp,
): PaddingValues =
    PaddingValues(
        start = padding.calculateStartPadding(layoutDirection),
        top = padding.calculateTopPadding(),
        end = padding.calculateEndPadding(layoutDirection),
        bottom = settingsBodyBottomPadding(padding.calculateBottomPadding(), imeBottom),
    )

/** Nav-bar (or IME) from the Scaffold, unioned with the live IME inset - never both stacked. */
internal fun settingsBodyBottomPadding(
    scaffoldBottom: Dp,
    imeBottom: Dp,
): Dp = maxOf(scaffoldBottom, imeBottom)
