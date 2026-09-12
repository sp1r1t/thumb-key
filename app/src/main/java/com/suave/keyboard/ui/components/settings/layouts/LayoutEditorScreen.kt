package com.suave.keyboard.ui.components.settings.layouts

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.suave.keyboard.R
import com.suave.keyboard.SuaveApplication
import com.suave.keyboard.db.AppSettingsViewModel
import com.suave.keyboard.db.ClipboardItem
import com.suave.keyboard.db.LayoutsUpdate
import com.suave.keyboard.engine.gesture.Direction
import com.suave.keyboard.engine.gesture.SlideAxis
import com.suave.keyboard.engine.gesture.Zone
import com.suave.keyboard.engine.intent.CommandId
import com.suave.keyboard.engine.intent.KeyFillRole
import com.suave.keyboard.engine.intent.KeyIntent
import com.suave.keyboard.engine.intent.KeyMapping
import com.suave.keyboard.engine.intent.KeyPosition
import com.suave.keyboard.engine.intent.Layout
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.engine.intent.SlideBehavior
import com.suave.keyboard.engine.intent.layoutRows
import com.suave.keyboard.engine.modifier.ModifierState
import com.suave.keyboard.layout.KeyInsertGap
import com.suave.keyboard.layout.LayerContent
import com.suave.keyboard.layout.LayoutDraftHistory
import com.suave.keyboard.layout.LayoutLayer
import com.suave.keyboard.layout.LayoutPreviewSession
import com.suave.keyboard.layout.LayoutRegistry
import com.suave.keyboard.layout.LayoutResizeMemory
import com.suave.keyboard.layout.NamedLayout
import com.suave.keyboard.layout.S12_CLIPBOARD_LAYER_HEIGHT_ROWS
import com.suave.keyboard.layout.S12_EMOJI_LAYER_HEIGHT_ROWS
import com.suave.keyboard.layout.UserLayoutStore
import com.suave.keyboard.layout.blankLayout
import com.suave.keyboard.layout.blankNamedLayout
import com.suave.keyboard.layout.gridOrEmpty
import com.suave.keyboard.layout.moveKeyToGap
import com.suave.keyboard.layout.moveRow
import com.suave.keyboard.layout.putKey
import com.suave.keyboard.layout.removeKeyAndCompact
import com.suave.keyboard.layout.removeRow
import com.suave.keyboard.layout.replaceGrid
import com.suave.keyboard.layout.resizeRows
import com.suave.keyboard.layout.sameExceptTitle
import com.suave.keyboard.layout.swapKeys
import com.suave.keyboard.layout.withUpdatedIntents
import com.suave.keyboard.ui.components.clipboard.ClipboardHistoryScreen
import com.suave.keyboard.ui.components.common.IntStepperPreference
import com.suave.keyboard.ui.components.common.SettingRow
import com.suave.keyboard.ui.components.common.SettingTitle
import com.suave.keyboard.ui.components.common.SettingsSection
import com.suave.keyboard.ui.components.common.awaitImeSpawned
import com.suave.keyboard.ui.components.common.settingsScreenBodyPadding
import com.suave.keyboard.ui.engine.KeyLegendMark
import com.suave.keyboard.ui.engine.LegendVisibility
import com.suave.keyboard.ui.engine.createThemedEmojiPicker
import com.suave.keyboard.ui.engine.keyLegend
import com.suave.keyboard.utils.SimpleTopAppBar
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceTheme

private const val LAYOUT_AUTO_SAVE_DEBOUNCE_MS = 250L

private val ZONE_ORDER: List<Pair<String, Zone>> =
    listOf(
        "upLeft" to Zone.Directional(Direction.UP_LEFT),
        "up" to Zone.Directional(Direction.UP),
        "upRight" to Zone.Directional(Direction.UP_RIGHT),
        "left" to Zone.Directional(Direction.LEFT),
        "center" to Zone.Center,
        "right" to Zone.Directional(Direction.RIGHT),
        "downLeft" to Zone.Directional(Direction.DOWN_LEFT),
        "down" to Zone.Directional(Direction.DOWN),
        "downRight" to Zone.Directional(Direction.DOWN_RIGHT),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
    editId: String? = null,
    createFrom: String? = null,
) {
    val ctx = LocalContext.current
    val activity = LocalActivity.current
    val app = ctx.applicationContext as SuaveApplication
    val store = app.userLayoutStore
    val scope = rememberCoroutineScope()

    var draft by remember { mutableStateOf<NamedLayout?>(null) }
    var sessionBaseline by remember { mutableStateOf<NamedLayout?>(null) }
    var existedOnOpen by remember { mutableStateOf(false) }
    var persistJob by remember { mutableStateOf<Job?>(null) }
    var blankSetup by remember { mutableStateOf(createFrom == "blank" && editId == null) }
    var blankRowCount by remember { mutableStateOf(4) }
    var blankRowSizes by remember { mutableStateOf(listOf(5, 5, 5, 4)) }
    var selectedLayer by remember { mutableStateOf(LayoutLayer.MAIN) }
    var editingKey by remember { mutableStateOf<KeyPosition?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }
    val history = remember { LayoutDraftHistory() }
    val resizeMemoryByLayer = remember { mutableMapOf<LayoutLayer, LayoutResizeMemory>() }

    fun resizeMemory(layer: LayoutLayer): LayoutResizeMemory =
        resizeMemoryByLayer.getOrPut(layer) { LayoutResizeMemory() }

    fun layoutForDisk(layout: NamedLayout): NamedLayout =
        layout.copy(title = layout.title.ifBlank { "Custom" })

    fun markDirtyAgainstBaseline(layout: NamedLayout) {
        dirty = sessionBaseline == null || layout != sessionBaseline
    }

    fun ensureLayoutEnabled(id: String) {
        val settings = appSettingsViewModel.appSettings.value
        val enabled =
            settings
                ?.keyboardLayouts
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
                .toSet()
        if (id in enabled) return
        appSettingsViewModel.updateLayouts(
            LayoutsUpdate(
                id = 1,
                keyboardLayout = settings?.keyboardLayout ?: id,
                keyboardLayouts = (enabled + id).joinToString(","),
            ),
        )
    }

    fun schedulePersist(layout: NamedLayout) {
        persistJob?.cancel()
        persistJob =
            scope.launch {
                delay(LAYOUT_AUTO_SAVE_DEBOUNCE_MS)
                try {
                    store.save(layoutForDisk(layout))
                    ensureLayoutEnabled(layout.id)
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            ctx,
                            ctx.getString(R.string.layout_action_failed, e.message ?: ""),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
    }

    fun bindPreviewSession(
        layout: NamedLayout,
        tryingOut: Boolean,
    ) {
        val selectedId =
            appSettingsViewModel.appSettings.value?.keyboardLayout ?: LayoutRegistry.DEFAULT_ID
        val activeFallback =
            if (selectedId == layout.id) {
                // Editing the daily-driver layout: keep the open-time snapshot so the IME
                // can fall back to a working keyboard while the draft is half-broken.
                sessionBaseline ?: layout
            } else {
                LayoutRegistry.byId(ctx, selectedId)
            }
        LayoutPreviewSession.bind(
            edited = layout,
            activeFallback = activeFallback,
            useEdited = tryingOut,
        )
    }

    fun beginSession(
        layout: NamedLayout,
        existed: Boolean,
    ) {
        sessionBaseline = layout
        // Keep new layouts on disk from the start so Cancel can revert to this snapshot
        // without a separate Save. Remove unwanted copies from the layouts list.
        existedOnOpen = true
        history.clear()
        dirty = !existed
        bindPreviewSession(layout, tryingOut = false)
        if (!existed) {
            scope.launch {
                try {
                    store.save(layoutForDisk(layout))
                    ensureLayoutEnabled(layout.id)
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            ctx,
                            ctx.getString(R.string.layout_action_failed, e.message ?: ""),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    fun leaveEditor() {
        LayoutPreviewSession.stop()
        if (navController.previousBackStackEntry == null) {
            activity?.finish()
        } else {
            navController.popBackStack()
        }
    }

    fun abortAndLeave() {
        persistJob?.cancel()
        LayoutPreviewSession.stop()
        scope.launch {
            try {
                val current = draft
                if (current != null) {
                    if (!existedOnOpen) {
                        runCatching { store.delete(current.id) }
                    } else {
                        sessionBaseline?.let { store.save(layoutForDisk(it)) }
                    }
                }
            } catch (e: Exception) {
                Toast
                    .makeText(
                        ctx,
                        ctx.getString(R.string.layout_action_failed, e.message ?: ""),
                        Toast.LENGTH_LONG,
                    ).show()
            } finally {
                leaveEditor()
            }
        }
    }

    fun commitDraft(
        next: NamedLayout,
        coalesceTitle: Boolean = false,
    ) {
        val cur = draft
        if (cur == null) {
            draft = next
            markDirtyAgainstBaseline(next)
            schedulePersist(next)
            return
        }
        if (cur == next) return
        if (coalesceTitle) {
            val undoBaseline = history.peekUndo()
            val continuingTitleEdit =
                undoBaseline != null &&
                    undoBaseline.sameExceptTitle(cur) &&
                    cur.sameExceptTitle(next)
            if (!continuingTitleEdit) {
                history.recordBeforeChange(cur)
            }
        } else {
            history.recordBeforeChange(cur)
        }
        draft = next
        markDirtyAgainstBaseline(next)
        schedulePersist(next)
    }

    /** Always mutate the live [draft] grid so gesture handlers cannot commit a stale snapshot. */
    fun commitGridEdit(edit: (Layout) -> Layout) {
        val cur = draft ?: return
        val layer = selectedLayer
        commitDraft(cur.replaceGrid(layer, edit(cur.gridOrEmpty(layer))))
    }

    fun undoDraft() {
        val cur = draft ?: return
        val previous = history.undo(cur) ?: return
        draft = previous
        markDirtyAgainstBaseline(previous)
        schedulePersist(previous)
    }

    fun redoDraft() {
        val cur = draft ?: return
        val next = history.redo(cur) ?: return
        draft = next
        markDirtyAgainstBaseline(next)
        schedulePersist(next)
    }

    LaunchedEffect(editId, createFrom) {
        resizeMemoryByLayer.clear()
        history.clear()
        persistJob?.cancel()
        when {
            editId != null -> {
                val existing = store.get(editId) ?: LayoutRegistry.byId(ctx, editId)
                if (UserLayoutStore.isBuiltinId(existing.id) && existing.id == editId) {
                    // Editing a builtin opens a user copy.
                    val copy =
                        existing.copy(
                            id = UserLayoutStore.newUserId(),
                            title = "${existing.title} copy",
                        )
                    draft = copy
                    beginSession(copy, existed = false)
                } else {
                    draft = existing
                    beginSession(existing, existed = true)
                }
            }
            createFrom == "blank" -> {
                blankSetup = true
            }
            createFrom != null -> {
                val source = store.get(createFrom) ?: LayoutRegistry.byId(ctx, createFrom)
                val copy =
                    source.copy(
                        id = UserLayoutStore.newUserId(),
                        title = "${source.title} copy",
                    )
                draft = copy
                beginSession(copy, existed = false)
            }
        }
    }

    BackHandler(enabled = draft != null && !blankSetup) {
        // Edits already auto-save; Back keeps them. Discard is the explicit revert action.
        leaveEditor()
    }

    if (blankSetup) {
        Scaffold(
            topBar = {
                SimpleTopAppBar(
                    text = stringResource(R.string.layout_start_blank),
                    navController = navController,
                )
            },
        ) { padding ->
            ProvidePreferenceTheme {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                ) {
                    IntStepperPreference(
                        value = blankRowCount,
                        onValueChange = { n ->
                            blankRowCount = n
                            blankRowSizes =
                                List(n) { i -> blankRowSizes.getOrElse(i) { 5 } }
                        },
                        valueRange = 1..8,
                        title = { SettingTitle(text = stringResource(R.string.layout_row_count)) },
                        summary = {
                            Text(stringResource(R.string.layout_row_count_summary, blankRowCount))
                        },
                        onReset = {
                            blankRowCount = 4
                            blankRowSizes = listOf(5, 5, 5, 4)
                        },
                        resetTo = 4,
                    )
                    blankRowSizes.forEachIndexed { index, size ->
                        IntStepperPreference(
                            value = size,
                            onValueChange = { v ->
                                blankRowSizes =
                                    blankRowSizes.toMutableList().also { it[index] = v }
                            },
                            valueRange = 1..LAYOUT_MAX_KEYS_PER_ROW,
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.layout_keys_in_row, index + 1),
                                )
                            },
                            summary = {
                                Text(stringResource(R.string.layout_keys_in_row_summary, size))
                            },
                            onReset = {
                                blankRowSizes =
                                    blankRowSizes.toMutableList().also { it[index] = 5 }
                            },
                            resetTo = 5,
                        )
                    }
                    Button(
                        onClick = {
                            val created =
                                blankNamedLayout(
                                    id = UserLayoutStore.newUserId(),
                                    title = "Custom",
                                    rowSizes = blankRowSizes,
                                )
                            draft = created
                            beginSession(created, existed = false)
                            blankSetup = false
                            schedulePersist(created)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        Text(stringResource(R.string.layout_continue_editing))
                    }
                }
            }
        }
        return
    }

    val layout = draft
    if (layout == null) {
        Scaffold(
            topBar = {
                SimpleTopAppBar(
                    text = stringResource(R.string.layout_editor),
                    navController = navController,
                )
            },
        ) { padding ->
            Text(
                text = loadError ?: stringResource(R.string.layout_loading),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        }
        return
    }

    val grid = layout.gridOrEmpty(selectedLayer)
    val keyBeingEdited = editingKey?.let { grid[it] }
    val previewLayout by LayoutPreviewSession.layout.collectAsState()
    val useEditedLayout by LayoutPreviewSession.useEdited.collectAsState()
    val isPreviewing = previewLayout?.id == layout.id && useEditedLayout

    DisposableEffect(Unit) {
        onDispose { LayoutPreviewSession.stop() }
    }
    LaunchedEffect(layout) {
        if (LayoutPreviewSession.isActive &&
            LayoutPreviewSession.layout.value?.id == layout.id
        ) {
            LayoutPreviewSession.updateIfActive(layout)
        } else {
            bindPreviewSession(layout, tryingOut = false)
        }
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.layout_editor),
                navController = navController,
                onNavigateBack = { leaveEditor() },
                actions = {
                    IconButton(
                        onClick = { undoDraft() },
                        enabled = history.canUndo,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = stringResource(R.string.layout_undo),
                        )
                    }
                    IconButton(
                        onClick = { redoDraft() },
                        enabled = history.canRedo,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = stringResource(R.string.layout_redo),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ProvidePreferenceTheme {
            val layoutDirection = LocalLayoutDirection.current
            val density = LocalDensity.current
            val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            settingsScreenBodyPadding(
                                padding = padding,
                                layoutDirection = layoutDirection,
                                imeBottom = imeBottom,
                            ),
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                ) {
                OutlinedTextField(
                    value = layout.title,
                    onValueChange = {
                        commitDraft(layout.copy(title = it), coalesceTitle = true)
                    },
                    label = { Text(stringResource(R.string.layout_title)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                )
                Text(
                    text = stringResource(R.string.layout_id_label, layout.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                SettingsSection(
                    title = stringResource(R.string.layout_layers),
                    initiallyExpanded = true,
                    infoText = stringResource(R.string.layout_layers_info),
                ) {
                    LayerSwitchAssigner(
                        selected = selectedLayer,
                        onSelect = { selectedLayer = it },
                        namedLayout = layout,
                        grid = grid,
                        onKeyClick = { editingKey = it },
                        onAssignLayerSwitch = { pos, layer, zone ->
                            commitGridEdit { layerGrid ->
                                val mapping = layerGrid.getValue(pos)
                                val intents = mapping.intents.toMutableMap()
                                intents[zone] = KeyIntent.Command(layer.toggleCommandId())
                                val labels = mapping.displayLabels.toMutableMap()
                                labels.remove(zone)
                                layerGrid.putKey(
                                    pos,
                                    mapping
                                        .withUpdatedIntents(intents)
                                        .copy(displayLabels = labels),
                                )
                            }
                        },
                        onSwapKeys = { from, to ->
                            if (from != to) {
                                commitGridEdit { it.swapKeys(from, to) }
                            }
                        },
                        onMoveKeyToGap = { from, gap ->
                            commitGridEdit {
                                it.moveKeyToGap(
                                    from = from,
                                    gap = gap,
                                    maxKeysPerRow = LAYOUT_MAX_KEYS_PER_ROW,
                                )
                            }
                        },
                        onRemoveKey = { pos ->
                            commitGridEdit { it.removeKeyAndCompact(pos) }
                        },
                        onMoveRow = { from, to ->
                            if (from != to) {
                                commitGridEdit { it.moveRow(from, to) }
                            }
                        },
                        onRemoveRow = { rowIndex ->
                            commitGridEdit { it.removeRow(rowIndex) }
                        },
                        onResizeRows = { sizes ->
                            commitGridEdit {
                                it.resizeRows(sizes, resizeMemory(selectedLayer))
                            }
                        },
                        onSeedLayer =
                            if (selectedLayer != LayoutLayer.MAIN && grid.isEmpty()) {
                                {
                                    val seed =
                                        when (selectedLayer) {
                                            LayoutLayer.NUMERIC -> listOf(5, 5, 5, 4)
                                            else -> listOf(4)
                                        }
                                    val cur = draft
                                    if (cur != null) {
                                        commitDraft(
                                            cur.replaceGrid(selectedLayer, blankLayout(seed)),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        onClearLayer =
                            if (selectedLayer != LayoutLayer.MAIN && grid.isNotEmpty()) {
                                {
                                    val cur = draft
                                    if (cur != null) {
                                        commitDraft(
                                            when (selectedLayer) {
                                                LayoutLayer.NUMERIC -> cur.copy(numericLayout = null)
                                                LayoutLayer.EMOJI -> cur.copy(emojiBottomRow = null)
                                                LayoutLayer.CLIPBOARD ->
                                                    cur.copy(clipboardBottomRow = null)
                                                LayoutLayer.MAIN -> cur
                                            },
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.layout_settings),
                    initiallyExpanded = false,
                ) {
                    LayoutLayer.entries.forEach { layer ->
                        // Numbers (and Main) have no content panel above the keys; height
                        // there would only add empty space, so it is not editable.
                        if (layer == LayoutLayer.MAIN || layer == LayoutLayer.NUMERIC) {
                            return@forEach
                        }
                        val layerLabel = layerChipLabel(layer)
                        val defaultHeight = defaultPanelHeightRows(layer)
                        val height = layout.layerHeights[layer] ?: 0
                        IntStepperPreference(
                            value = height,
                            onValueChange = { v ->
                                commitDraft(
                                    layout.copy(
                                        layerHeights =
                                            if (v <= 0) {
                                                layout.layerHeights - layer
                                            } else {
                                                layout.layerHeights + (layer to v)
                                            },
                                    ),
                                )
                            },
                            valueRange = 0..12,
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.layout_layer_height, layerLabel),
                                    infoText = stringResource(R.string.layout_layer_height_info),
                                )
                            },
                            summary = {
                                Text(
                                    if (height <= 0) {
                                        stringResource(R.string.layout_layer_height_default)
                                    } else {
                                        stringResource(R.string.layout_layer_height_summary, height)
                                    },
                                )
                            },
                            onReset = {
                                commitDraft(
                                    layout.copy(
                                        layerHeights = layout.layerHeights + (layer to defaultHeight),
                                    ),
                                )
                            },
                            resetTo = defaultHeight,
                        )
                    }

                    val cycle = layout.spaceMultitapCycle.orEmpty()
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.titleSmall,
                    ) {
                        SettingTitle(
                            text = stringResource(R.string.layout_space_multitap),
                            infoText = stringResource(R.string.layout_space_multitap_info),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    Text(
                        text =
                            if (layout.spaceMultitapCycle == null) {
                                stringResource(R.string.layout_space_multitap_default)
                            } else {
                                stringResource(R.string.layout_space_multitap_custom, cycle.size)
                            },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    cycle.forEachIndexed { index, item ->
                        OutlinedTextField(
                            value = item,
                            onValueChange = { v ->
                                val next = cycle.toMutableList().also { it[index] = v }
                                commitDraft(layout.copy(spaceMultitapCycle = next))
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            singleLine = true,
                            label = { Text(stringResource(R.string.layout_space_multitap_item, index + 1)) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val next = cycle.toMutableList().also { it.removeAt(index) }
                                        commitDraft(
                                            layout.copy(
                                                spaceMultitapCycle = next.ifEmpty { null },
                                            ),
                                        )
                                    },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                commitDraft(
                                    layout.copy(
                                        spaceMultitapCycle = cycle + ", ",
                                    ),
                                )
                            },
                        ) {
                            Text(stringResource(R.string.layout_space_multitap_add))
                        }
                        TextButton(
                            onClick = {
                                commitDraft(layout.copy(spaceMultitapCycle = null))
                            },
                        ) {
                            Text(stringResource(R.string.layout_space_multitap_reset))
                        }
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPreviewing) {
                                LayoutPreviewSession.setUseEdited(false)
                            } else {
                                bindPreviewSession(layout, tryingOut = true)
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (isPreviewing) {
                                    R.string.layout_exit_preview
                                } else {
                                    R.string.layout_try_keyboard
                                },
                            ),
                        )
                    }
                    if (isPreviewing) {
                        Text(
                            text = stringResource(R.string.layout_previewing_banner),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = stringResource(R.string.layout_unsaved_changes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = { abortAndLeave() },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(stringResource(R.string.layout_cancel_edits))
                    }
                }
                }
                if (isPreviewing) {
                    LayoutPreviewTestField()
                }
            }
        }
    }

    if (keyBeingEdited != null && editingKey != null) {
        // Dialogs are siblings of the Scaffold content, so they need their own theme:
        // Preference / ListPreference / IntStepperPreference crash without it.
        ProvidePreferenceTheme {
            KeyEditorDialog(
                position = editingKey!!,
                mapping = keyBeingEdited,
                shiftMappings = layout.shiftMappings,
                capsLockMappings = layout.capsLockMappings,
                onDismiss = { editingKey = null },
                onSave = { updated, shifts, caps ->
                    val pos = editingKey!!
                    val cur = draft
                    if (cur != null) {
                        commitDraft(
                            cur
                                .replaceGrid(
                                    selectedLayer,
                                    cur.gridOrEmpty(selectedLayer).putKey(pos, updated),
                                ).copy(
                                    shiftMappings = shifts,
                                    capsLockMappings = caps,
                                ),
                        )
                    }
                    editingKey = null
                },
            )
        }
    }
}

private sealed class PreviewDrag {
    data class Layer(val layer: LayoutLayer) : PreviewDrag()

    data class Key(val from: KeyPosition) : PreviewDrag()

    data class Row(val from: Int) : PreviewDrag()
}

/** Pinned under the editor while Try on keyboard is active; focuses so the IME opens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayoutPreviewTestField() {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
        keyboardController?.show()
        awaitImeSpawned(density, ime, imeTarget)
        withFrameNanos { }
        bringIntoViewRequester.bringIntoView()
    }

    OutlinedTextField(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .focusRequester(focusRequester),
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(stringResource(R.string.test_out_placeholder)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 1,
        maxLines = 3,
    )
}

@Composable
private fun LayerSwitchAssigner(
    selected: LayoutLayer,
    onSelect: (LayoutLayer) -> Unit,
    namedLayout: NamedLayout,
    grid: Map<KeyPosition, KeyMapping>,
    onKeyClick: (KeyPosition) -> Unit,
    onAssignLayerSwitch: (KeyPosition, LayoutLayer, Zone) -> Unit,
    onSwapKeys: (KeyPosition, KeyPosition) -> Unit,
    onMoveKeyToGap: (KeyPosition, KeyInsertGap) -> Unit,
    onRemoveKey: (KeyPosition) -> Unit,
    onMoveRow: (Int, Int) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onResizeRows: (List<Int>) -> Unit,
    onSeedLayer: (() -> Unit)? = null,
    onClearLayer: (() -> Unit)? = null,
) {
    var drag by remember { mutableStateOf<PreviewDrag?>(null) }
    var dragRootPos by remember { mutableStateOf(Offset.Zero) }
    var hoverKey by remember { mutableStateOf<KeyPosition?>(null) }
    var hoverZone by remember { mutableStateOf<Zone>(Zone.Center) }
    var hoverRow by remember { mutableStateOf<Int?>(null) }
    var hoverGap by remember { mutableStateOf<KeyInsertGap?>(null) }
    var hoverTrash by remember { mutableStateOf(false) }
    val keyBounds = remember { mutableStateMapOf<KeyPosition, Rect>() }
    val rowBounds = remember { mutableStateMapOf<Int, Rect>() }
    var trashBounds by remember { mutableStateOf(Rect.Zero) }
    var hostOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    val legendVisibility = remember { LegendVisibility() }
    val legendModifierState = remember { ModifierState.NONE }
    val density = LocalDensity.current
    val minZoneTargetPx = with(density) { LAYER_ZONE_DROP_MIN_SIZE.toPx() }

    fun dropTargetRect(keyRect: Rect): Rect {
        val width = maxOf(keyRect.width, minZoneTargetPx)
        val height = maxOf(keyRect.height, minZoneTargetPx)
        val center = keyRect.center
        return Rect(
            left = center.x - width / 2f,
            top = center.y - height / 2f,
            right = center.x + width / 2f,
            bottom = center.y + height / 2f,
        )
    }

    fun clearHover() {
        hoverKey = null
        hoverZone = Zone.Center
        hoverRow = null
        hoverGap = null
        hoverTrash = false
    }

    fun findInsertGap(rootPos: Offset, exclude: KeyPosition?): KeyInsertGap? {
        val rowIndex =
            rowBounds.entries
                .firstOrNull { (_, rect) -> rect.contains(rootPos) }
                ?.key
                ?: return null
        val rowKeys = layoutRows(grid).getOrNull(rowIndex) ?: return null
        val keyedRects =
            rowKeys.mapNotNull { pos ->
                val rect = keyBounds[pos] ?: return@mapNotNull null
                pos to rect
            }
        if (keyedRects.isEmpty()) {
            return KeyInsertGap(rowIndex, 0)
        }
        // Crossing into a full row is refused (same-row moves keep the count).
        if (exclude?.row != rowIndex && rowKeys.size >= LAYOUT_MAX_KEYS_PER_ROW) {
            return null
        }
        val x = rootPos.x
        var insertCol = keyedRects.size
        for ((index, pair) in keyedRects.withIndex()) {
            if (x < pair.second.center.x) {
                insertCol = index
                break
            }
        }
        return KeyInsertGap(rowIndex, insertCol)
    }

    fun updateKeyHover(rootPos: Offset, exclude: KeyPosition? = null) {
        hoverTrash = trashBounds != Rect.Zero && trashBounds.contains(rootPos)
        hoverRow = null
        hoverZone = Zone.Center
        if (hoverTrash) {
            hoverKey = null
            hoverGap = null
            return
        }
        hoverKey =
            keyBounds.entries
                .firstOrNull { (pos, rect) -> pos != exclude && rect.contains(rootPos) }
                ?.key
        hoverGap =
            if (hoverKey != null) {
                null
            } else {
                findInsertGap(rootPos, exclude)?.takeUnless { gap ->
                    exclude != null &&
                        exclude.row == gap.row &&
                        (gap.col == exclude.col || gap.col == exclude.col + 1)
                }
            }
    }

    fun updateRowHover(rootPos: Offset, exclude: Int? = null) {
        hoverTrash = trashBounds != Rect.Zero && trashBounds.contains(rootPos)
        hoverKey = null
        hoverGap = null
        hoverZone = Zone.Center
        hoverRow =
            if (hoverTrash) {
                null
            } else {
                rowBounds.entries
                    .firstOrNull { (index, rect) -> index != exclude && rect.contains(rootPos) }
                    ?.key
            }
    }

    fun updateLayerHover(rootPos: Offset) {
        hoverTrash = false
        hoverRow = null
        hoverGap = null
        val current = hoverKey
        if (current != null) {
            val keyRect = keyBounds[current]
            if (keyRect != null) {
                val target = dropTargetRect(keyRect)
                if (target.contains(rootPos)) {
                    hoverZone = zoneAt(rootPos, target)
                    return
                }
            }
        }
        val next =
            keyBounds.entries
                .firstOrNull { (_, rect) -> rect.contains(rootPos) }
                ?.key
        hoverKey = next
        hoverZone =
            if (next != null) {
                zoneAt(rootPos, dropTargetRect(keyBounds.getValue(next)))
            } else {
                Zone.Center
            }
    }

    fun endDrag() {
        when (val current = drag) {
            is PreviewDrag.Layer -> {
                val target = hoverKey
                if (target != null) {
                    onAssignLayerSwitch(target, current.layer, hoverZone)
                }
            }
            is PreviewDrag.Key -> {
                when {
                    hoverTrash -> onRemoveKey(current.from)
                    hoverKey != null && hoverKey != current.from ->
                        onSwapKeys(current.from, hoverKey!!)
                    hoverGap != null -> onMoveKeyToGap(current.from, hoverGap!!)
                    else -> Unit
                }
            }
            is PreviewDrag.Row -> {
                when {
                    hoverTrash -> onRemoveRow(current.from)
                    hoverRow != null && hoverRow != current.from ->
                        onMoveRow(current.from, hoverRow!!)
                    else -> Unit
                }
            }
            null -> Unit
        }
        drag = null
        clearHover()
    }

    LaunchedEffect(grid.keys.toSet()) {
        val alive = grid.keys.toSet()
        keyBounds.keys.filter { it !in alive }.forEach { keyBounds.remove(it) }
        val rowCount = layoutRows(grid).size
        rowBounds.keys.filter { it >= rowCount }.forEach { rowBounds.remove(it) }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    hostOriginInRoot = coords.positionInRoot()
                },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LayerChips(
                selected = selected,
                onSelect = onSelect,
                onDragStart = { layer, rootPos ->
                    drag = PreviewDrag.Layer(layer)
                    dragRootPos = rootPos
                    updateLayerHover(rootPos)
                },
                onDrag = { rootPos ->
                    dragRootPos = rootPos
                    updateLayerHover(rootPos)
                },
                onDragEnd = { endDrag() },
                onDragCancel = {
                    drag = null
                    clearHover()
                },
            )
            val totalRows = namedLayout.heightRows(selected)
            val contentRows = namedLayout.contentRows(selected)
            if (contentRows > 0) {
                LayoutPreviewLayerPanel(
                    content = namedLayout.contentFor(selected),
                    height = PREVIEW_KEY_HEIGHT * contentRows,
                    keyHeight = PREVIEW_KEY_HEIGHT,
                )
            }
            LayoutPreviewGrid(
                layout = grid,
                onKeyClick = onKeyClick,
                onResizeRows = onResizeRows,
                highlightedKey = hoverKey,
                draggingKey = (drag as? PreviewDrag.Key)?.from,
                highlightedRow = hoverRow,
                draggingRow = (drag as? PreviewDrag.Row)?.from,
                showDeleteTarget = drag is PreviewDrag.Key || drag is PreviewDrag.Row,
                deleteTargetHighlighted = hoverTrash,
                onDeleteTargetBoundsInRoot = { rect -> trashBounds = rect },
                onKeyBoundsInRoot = { pos, rect ->
                    if (rect == Rect.Zero) {
                        keyBounds.remove(pos)
                    } else {
                        keyBounds[pos] = rect
                    }
                },
                onRowBoundsInRoot = { index, rect ->
                    if (rect == Rect.Zero) {
                        rowBounds.remove(index)
                    } else {
                        rowBounds[index] = rect
                    }
                },
                onKeyDragStart = { pos, rootPos ->
                    drag = PreviewDrag.Key(pos)
                    dragRootPos = rootPos
                    updateKeyHover(rootPos, exclude = pos)
                },
                onKeyDrag = { rootPos ->
                    val from = (drag as? PreviewDrag.Key)?.from
                    dragRootPos = rootPos
                    updateKeyHover(rootPos, exclude = from)
                },
                onKeyDragEnd = { endDrag() },
                onKeyDragCancel = {
                    drag = null
                    clearHover()
                },
                onRowDragStart = { rowIndex, rootPos ->
                    drag = PreviewDrag.Row(rowIndex)
                    dragRootPos = rootPos
                    updateRowHover(rootPos, exclude = rowIndex)
                },
                onRowDrag = { rootPos ->
                    val from = (drag as? PreviewDrag.Row)?.from
                    dragRootPos = rootPos
                    updateRowHover(rootPos, exclude = from)
                },
                onRowDragEnd = { endDrag() },
                onRowDragCancel = {
                    drag = null
                    clearHover()
                },
            )
            if (onSeedLayer != null) {
                TextButton(
                    onClick = onSeedLayer,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.layout_add_layer_grid))
                }
            }
            if (onClearLayer != null) {
                TextButton(
                    onClick = onClearLayer,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.layout_clear_layer_grid))
                }
            }
        }

        val layerHover = hoverKey
        if (drag is PreviewDrag.Layer && layerHover != null) {
            val keyRect = keyBounds[layerHover]
            if (keyRect != null) {
                val target = dropTargetRect(keyRect)
                val localTopLeft = target.topLeft - hostOriginInRoot
                LayerZoneDropOverlay(
                    highlightedZone = hoverZone,
                    modifier =
                        Modifier
                            .offset {
                                IntOffset(
                                    localTopLeft.x.roundToInt(),
                                    localTopLeft.y.roundToInt(),
                                )
                            }
                            .width(with(density) { target.width.toDp() })
                            .height(with(density) { target.height.toDp() }),
                )
            }
        }


        val gap = hoverGap
        if (drag is PreviewDrag.Key && gap != null) {
            val rowKeys = layoutRows(grid).getOrNull(gap.row)
            val rowRect = rowBounds[gap.row]
            if (rowKeys != null && rowRect != null) {
                val xRoot =
                    when {
                        rowKeys.isEmpty() -> rowRect.center.x
                        gap.col <= 0 -> {
                            keyBounds[rowKeys.first()]?.left ?: rowRect.left
                        }
                        gap.col >= rowKeys.size -> {
                            keyBounds[rowKeys.last()]?.right ?: rowRect.right
                        }
                        else -> {
                            val left = keyBounds[rowKeys[gap.col - 1]]?.right
                            val right = keyBounds[rowKeys[gap.col]]?.left
                            if (left != null && right != null) {
                                (left + right) / 2f
                            } else {
                                null
                            }
                        }
                    }
                if (xRoot != null) {
                    val localX = xRoot - hostOriginInRoot.x
                    val localTop = rowRect.top - hostOriginInRoot.y
                    Box(
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(
                                        (localX - 1.5.dp.toPx()).roundToInt(),
                                        localTop.roundToInt(),
                                    )
                                }
                                .width(3.dp)
                                .height(with(density) { rowRect.height.toDp() })
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp),
                                ),
                    )
                }
            }
        }

        when (val current = drag) {
            is PreviewDrag.Layer -> {
                val local = dragRootPos - hostOriginInRoot
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    modifier =
                        Modifier
                            .offset {
                                IntOffset(
                                    (local.x - 40.dp.toPx()).roundToInt(),
                                    (local.y - 20.dp.toPx()).roundToInt(),
                                )
                            },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = layerChipIcon(current.layer),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = layerChipLabel(current.layer),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            is PreviewDrag.Key -> {
                val mapping = grid[current.from]
                if (mapping != null) {
                    val local = dragRootPos - hostOriginInRoot
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(
                                        (local.x - 28.dp.toPx()).roundToInt(),
                                        (local.y - 28.dp.toPx()).roundToInt(),
                                    )
                                }
                                .width(56.dp)
                                .height(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            val legend =
                                keyLegend(
                                    intent = mapping.intents[Zone.Center],
                                    visibility = legendVisibility,
                                    modifierState = legendModifierState,
                                    shiftMappings = emptyMap(),
                                    displayLabel = mapping.displayLabels[Zone.Center],
                                )
                            if (legend != null) {
                                KeyLegendMark(
                                    legend = legend,
                                    fontSize = 16.sp,
                                    iconSize = 22.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            } else {
                                Text(
                                    text = "·",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
            is PreviewDrag.Row -> {
                val local = dragRootPos - hostOriginInRoot
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    modifier =
                        Modifier
                            .offset {
                                IntOffset(
                                    (local.x - 20.dp.toPx()).roundToInt(),
                                    (local.y - 20.dp.toPx()).roundToInt(),
                                )
                            }
                            .size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun LayerZoneDropOverlay(
    highlightedZone: Zone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            LAYER_ZONE_DROP_GRID.forEach { row ->
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEach { zone ->
                        val selected = zone == highlightedZone
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        RoundedCornerShape(6.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = zoneDropLabel(zone),
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun zoneDropLabel(zone: Zone): String =
    when (zone) {
        Zone.Center -> "*"
        is Zone.Directional ->
            when (zone.direction) {
                Direction.UP_LEFT -> "UL"
                Direction.UP -> "U"
                Direction.UP_RIGHT -> "UR"
                Direction.LEFT -> "L"
                Direction.RIGHT -> "R"
                Direction.DOWN_LEFT -> "DL"
                Direction.DOWN -> "D"
                Direction.DOWN_RIGHT -> "DR"
            }
    }

private fun zoneAt(
    rootPos: Offset,
    rect: Rect,
): Zone {
    val x = ((rootPos.x - rect.left) / rect.width).coerceIn(0f, 0.999f)
    val y = ((rootPos.y - rect.top) / rect.height).coerceIn(0f, 0.999f)
    val col = (x * 3f).toInt()
    val row = (y * 3f).toInt()
    return LAYER_ZONE_DROP_GRID[row][col]
}

private val LAYER_ZONE_DROP_GRID: List<List<Zone>> =
    listOf(
        listOf(
            Zone.Directional(Direction.UP_LEFT),
            Zone.Directional(Direction.UP),
            Zone.Directional(Direction.UP_RIGHT),
        ),
        listOf(
            Zone.Directional(Direction.LEFT),
            Zone.Center,
            Zone.Directional(Direction.RIGHT),
        ),
        listOf(
            Zone.Directional(Direction.DOWN_LEFT),
            Zone.Directional(Direction.DOWN),
            Zone.Directional(Direction.DOWN_RIGHT),
        ),
    )

private val LAYER_ZONE_DROP_MIN_SIZE = 108.dp

@Composable
private fun layerChipLabel(layer: LayoutLayer): String =
    when (layer) {
        LayoutLayer.MAIN -> stringResource(R.string.layout_layer_main)
        LayoutLayer.NUMERIC -> stringResource(R.string.layout_layer_numeric)
        LayoutLayer.EMOJI -> stringResource(R.string.layout_layer_emoji)
        LayoutLayer.CLIPBOARD -> stringResource(R.string.layout_layer_clipboard)
    }

/** S12-style panel defaults: room above the key row for emoji picker / clipboard list. */
private fun defaultPanelHeightRows(layer: LayoutLayer): Int =
    when (layer) {
        LayoutLayer.EMOJI -> S12_EMOJI_LAYER_HEIGHT_ROWS
        LayoutLayer.CLIPBOARD -> S12_CLIPBOARD_LAYER_HEIGHT_ROWS
        LayoutLayer.MAIN, LayoutLayer.NUMERIC -> 0
    }

/** Same icons the live keyboard uses for ABC / Numbers / Emoji / Clipboard toggles. */
private fun layerChipIcon(layer: LayoutLayer): ImageVector =
    when (layer) {
        LayoutLayer.MAIN -> Icons.Outlined.Abc
        LayoutLayer.NUMERIC -> Icons.Outlined.Numbers
        LayoutLayer.EMOJI -> Icons.Outlined.EmojiEmotions
        LayoutLayer.CLIPBOARD -> Icons.Outlined.History
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerChips(
    selected: LayoutLayer,
    onSelect: (LayoutLayer) -> Unit,
    onDragStart: (LayoutLayer, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    // Wrap instead of squeezing the last chip into a tall sliver on narrow screens.
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LayoutLayer.entries.forEach { layer ->
            var coords by remember(layer) { mutableStateOf<LayoutCoordinates?>(null) }
            val clickGate = rememberDragClickGate(layer)
            val onDragStartLatest = rememberUpdatedState(onDragStart)
            val onDragLatest = rememberUpdatedState(onDrag)
            val onDragEndLatest = rememberUpdatedState(onDragEnd)
            val onDragCancelLatest = rememberUpdatedState(onDragCancel)
            val label = layerChipLabel(layer)
            FilterChip(
                selected = selected == layer,
                onClick = {
                    if (!clickGate.consumeSuppressedClick()) {
                        onSelect(layer)
                    }
                },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        imageVector = layerChipIcon(layer),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier =
                    Modifier
                        .onGloballyPositioned { coords = it }
                        .pointerInput(layer) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local ->
                                    clickGate.arm()
                                    val root =
                                        coords?.localToRoot(local) ?: return@detectDragGesturesAfterLongPress
                                    onDragStartLatest.value(layer, root)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val root =
                                        coords?.localToRoot(change.position)
                                            ?: return@detectDragGesturesAfterLongPress
                                    onDragLatest.value(root)
                                },
                                onDragEnd = { onDragEndLatest.value() },
                                onDragCancel = {
                                    clickGate.clear()
                                    onDragCancelLatest.value()
                                },
                            )
                        },
            )
        }
    }
}

/**
 * After a long-press drag, a trailing click may or may not arrive. Arming suppresses that
 * click; if none comes, auto-clear so the next real tap is not eaten.
 */
@Composable
private fun rememberDragClickGate(key: Any? = null): DragClickGate {
    var suppressClick by remember(key) { mutableStateOf(false) }
    var armGeneration by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(armGeneration) {
        if (armGeneration == 0) return@LaunchedEffect
        delay(DRAG_CLICK_SUPPRESS_MS)
        suppressClick = false
    }
    return remember(key) {
        DragClickGate(
            arm = {
                suppressClick = true
                armGeneration += 1
            },
            clear = { suppressClick = false },
            consumeSuppressedClick = {
                if (suppressClick) {
                    suppressClick = false
                    true
                } else {
                    false
                }
            },
        )
    }
}

private class DragClickGate(
    val arm: () -> Unit,
    val clear: () -> Unit,
    val consumeSuppressedClick: () -> Boolean,
)

private const val DRAG_CLICK_SUPPRESS_MS = 400L

@Composable
private fun LayoutPreviewLayerPanel(
    content: LayerContent,
    height: Dp,
    keyHeight: Dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(
                    start = PREVIEW_KEYS_START_INSET,
                    end = PREVIEW_KEYS_END_INSET,
                    bottom = 4.dp,
                )
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        when (content) {
            LayerContent.None -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val bands = (height / keyHeight).toInt().coerceAtLeast(1)
                        repeat(bands) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color =
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.45f,
                                                ),
                                            shape = RoundedCornerShape(4.dp),
                                        ),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.layout_preview_panel_empty),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                    )
                }
            }

            LayerContent.EmojiPicker -> {
                val colorScheme = MaterialTheme.colorScheme
                val pickerText = colorScheme.onSurface.toArgb()
                val pickerIcon = colorScheme.onSurfaceVariant.toArgb()
                val pickerAccent = colorScheme.primary.toArgb()
                val darkKeyboard = colorScheme.surface.luminance() < 0.5f
                key(pickerText, pickerIcon, pickerAccent, darkKeyboard) {
                    AndroidView(
                        factory = { context ->
                            createThemedEmojiPicker(
                                context = context,
                                darkKeyboard = darkKeyboard,
                                text = pickerText,
                                icon = pickerIcon,
                                accent = pickerAccent,
                            ).apply {
                                // Preview only - picking should not type into a field here.
                                setOnEmojiPickedListener { }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            LayerContent.ClipboardHistory -> {
                val samplePinned = stringResource(R.string.layout_preview_clipboard_sample_2)
                val sampleItem = stringResource(R.string.layout_preview_clipboard_sample_1)
                val samples =
                    remember(samplePinned, sampleItem) {
                        listOf(
                            ClipboardItem(id = 1, text = samplePinned, isPinned = true),
                            ClipboardItem(id = 2, text = sampleItem),
                            ClipboardItem(id = 3, text = "https://example.com"),
                        )
                    }
                ClipboardHistoryScreen(
                    clipboardItems = samples,
                    isEnabled = true,
                    onItemClick = {},
                    onItemPaste = {},
                    onItemDelete = {},
                    onItemTogglePin = {},
                    onBack = {},
                    onClearAll = {},
                    onGoToClipboardSettings = {},
                    keyHeight = keyHeight.value,
                    keyPadding = 4,
                    cornerRadius = 6f,
                    vibrateOnTap = false,
                    imagesEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LayoutPreviewGrid(
    layout: Map<KeyPosition, KeyMapping>,
    onKeyClick: (KeyPosition) -> Unit,
    onResizeRows: (List<Int>) -> Unit,
    highlightedKey: KeyPosition? = null,
    draggingKey: KeyPosition? = null,
    highlightedRow: Int? = null,
    draggingRow: Int? = null,
    showDeleteTarget: Boolean = false,
    deleteTargetHighlighted: Boolean = false,
    onDeleteTargetBoundsInRoot: (Rect) -> Unit = {},
    onKeyBoundsInRoot: (KeyPosition, Rect) -> Unit = { _, _ -> },
    onRowBoundsInRoot: (Int, Rect) -> Unit = { _, _ -> },
    onKeyDragStart: (KeyPosition, Offset) -> Unit = { _, _ -> },
    onKeyDrag: (Offset) -> Unit = {},
    onKeyDragEnd: () -> Unit = {},
    onKeyDragCancel: () -> Unit = {},
    onRowDragStart: (Int, Offset) -> Unit = { _, _ -> },
    onRowDrag: (Offset) -> Unit = {},
    onRowDragEnd: () -> Unit = {},
    onRowDragCancel: () -> Unit = {},
) {
    val rows = layoutRows(layout)
    val sizes = rows.map { it.size }
    LaunchedEffect(showDeleteTarget) {
        if (!showDeleteTarget) {
            onDeleteTargetBoundsInRoot(Rect.Zero)
        }
    }
    if (rows.isEmpty()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.layout_preview_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
            PreviewAddRowButton(
                enabled = true,
                onClick = { onResizeRows(listOf(PREVIEW_DEFAULT_KEYS_PER_ROW)) },
            )
        }
        return
    }
    val legendVisibility = remember { LegendVisibility() }
    val legendModifierState = remember { ModifierState.NONE }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            // Fixed row height: columnSpan only widens a key, never makes it taller.
            val rowHighlighted = highlightedRow == rowIndex
            val rowDragging = draggingRow == rowIndex
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_KEY_HEIGHT)
                        .onGloballyPositioned { coords ->
                            val origin = coords.positionInRoot()
                            onRowBoundsInRoot(
                                rowIndex,
                                Rect(
                                    offset = origin,
                                    size =
                                        Size(
                                            coords.size.width.toFloat(),
                                            coords.size.height.toFloat(),
                                        ),
                                ),
                            )
                        }
                        .then(
                            if (rowHighlighted) {
                                Modifier.border(
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                    RoundedCornerShape(8.dp),
                                )
                            } else {
                                Modifier
                            }
                        ),
                horizontalArrangement = Arrangement.spacedBy(PREVIEW_ROW_INNER_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviewRowHandle(
                    dragging = rowDragging,
                    onDragStart = { root -> onRowDragStart(rowIndex, root) },
                    onDrag = onRowDrag,
                    onDragEnd = onRowDragEnd,
                    onDragCancel = onRowDragCancel,
                )
                row.forEach { pos ->
                    key(pos) {
                        val mapping = layout.getValue(pos)
                        LayoutPreviewKey(
                            mapping = mapping,
                            legendVisibility = legendVisibility,
                            legendModifierState = legendModifierState,
                            highlighted = highlightedKey == pos,
                            dragging = draggingKey == pos || rowDragging,
                            onClick = { onKeyClick(pos) },
                            onBoundsInRoot = { rect -> onKeyBoundsInRoot(pos, rect) },
                            onDragStart = { root -> onKeyDragStart(pos, root) },
                            onDrag = onKeyDrag,
                            onDragEnd = onKeyDragEnd,
                            onDragCancel = onKeyDragCancel,
                            modifier =
                                Modifier
                                    .weight(mapping.columnSpan.toFloat())
                                    .fillMaxHeight(),
                        )
                    }
                }
                PreviewAddKeyButton(
                    enabled = row.size < LAYOUT_MAX_KEYS_PER_ROW,
                    onClick = {
                        val next = sizes.toMutableList()
                        next[rowIndex] = row.size + 1
                        onResizeRows(next)
                    },
                )
            }
        }
        if (showDeleteTarget) {
            PreviewDeleteKeyTarget(
                highlighted = deleteTargetHighlighted,
                onBoundsInRoot = onDeleteTargetBoundsInRoot,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            PreviewAddRowButton(
                enabled = rows.size < PREVIEW_MAX_ROWS,
                onClick = {
                    onResizeRows(sizes + sizes.last())
                },
            )
        }
    }
}

@Composable
private fun PreviewRowHandle(
    dragging: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val onDragStartLatest = rememberUpdatedState(onDragStart)
    val onDragLatest = rememberUpdatedState(onDrag)
    val onDragEndLatest = rememberUpdatedState(onDragEnd)
    val onDragCancelLatest = rememberUpdatedState(onDragCancel)
    Icon(
        imageVector = Icons.Outlined.DragHandle,
        contentDescription = stringResource(R.string.layout_row_handle),
        tint =
            if (dragging) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier =
            Modifier
                .size(PREVIEW_ROW_HANDLE_SIZE)
                .onGloballyPositioned { coords = it }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { local ->
                            val root = coords?.localToRoot(local) ?: return@detectDragGesturesAfterLongPress
                            onDragStartLatest.value(root)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val root =
                                coords?.localToRoot(change.position)
                                    ?: return@detectDragGesturesAfterLongPress
                            onDragLatest.value(root)
                        },
                        onDragEnd = { onDragEndLatest.value() },
                        onDragCancel = { onDragCancelLatest.value() },
                    )
                },
    )
}

@Composable
private fun PreviewAddKeyButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(PREVIEW_ADD_KEY_SIZE),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.layout_add_key),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PreviewAddRowButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.layout_add_row))
    }
}

@Composable
private fun PreviewDeleteKeyTarget(
    highlighted: Boolean,
    onBoundsInRoot: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container =
        if (highlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        }
    val content = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = container,
            tonalElevation = if (highlighted) 8.dp else 2.dp,
            shadowElevation = if (highlighted) 8.dp else 2.dp,
            modifier =
                Modifier
                    .size(48.dp)
                    .onGloballyPositioned { coords ->
                        val origin = coords.positionInRoot()
                        onBoundsInRoot(
                            Rect(
                                offset = origin,
                                size =
                                    Size(
                                        coords.size.width.toFloat(),
                                        coords.size.height.toFloat(),
                                    ),
                            ),
                        )
                    },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.layout_drag_delete),
                    tint = content,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private const val PREVIEW_MAX_ROWS = 8
private const val LAYOUT_MAX_KEYS_PER_ROW = 16
private const val PREVIEW_DEFAULT_KEYS_PER_ROW = 4
private val PREVIEW_KEY_HEIGHT = 52.dp
/** Matches [PreviewRowHandle] icon size; keeps layer panels aligned to the key strip. */
private val PREVIEW_ROW_HANDLE_SIZE = 24.dp
/** Matches [PreviewAddKeyButton] hit target. */
private val PREVIEW_ADD_KEY_SIZE = 28.dp
private val PREVIEW_ROW_INNER_GAP = 4.dp
/** Space taken by handle + gap before the first key (and symmetrically the add control). */
private val PREVIEW_KEYS_START_INSET = PREVIEW_ROW_HANDLE_SIZE + PREVIEW_ROW_INNER_GAP
private val PREVIEW_KEYS_END_INSET = PREVIEW_ADD_KEY_SIZE + PREVIEW_ROW_INNER_GAP

@Composable
private fun LayoutPreviewKey(
    mapping: KeyMapping,
    legendVisibility: LegendVisibility,
    legendModifierState: ModifierState,
    highlighted: Boolean,
    dragging: Boolean,
    onClick: () -> Unit,
    onBoundsInRoot: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val centerColor = MaterialTheme.colorScheme.onSurface
    val swipeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val centerSize = 16.dp
    val swipeSize = 9.dp
    val borderColor =
        when {
            highlighted -> MaterialTheme.colorScheme.primary
            dragging -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val borderWidth = if (highlighted || dragging) 2.dp else 1.dp
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val clickGate = rememberDragClickGate()
    val onDragStartLatest = rememberUpdatedState(onDragStart)
    val onDragLatest = rememberUpdatedState(onDrag)
    val onDragEndLatest = rememberUpdatedState(onDragEnd)
    val onDragCancelLatest = rememberUpdatedState(onDragCancel)
    val onClickLatest = rememberUpdatedState(onClick)
    DisposableEffect(Unit) {
        onDispose { onBoundsInRoot(Rect.Zero) }
    }
    Box(
        modifier =
            modifier
                .onGloballyPositioned {
                    coords = it
                    onBoundsInRoot(
                        Rect(
                            offset = it.positionInRoot(),
                            size =
                                Size(
                                    it.size.width.toFloat(),
                                    it.size.height.toFloat(),
                                ),
                        ),
                    )
                }
                .background(
                    when {
                        highlighted -> MaterialTheme.colorScheme.primaryContainer
                        dragging -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    RoundedCornerShape(6.dp),
                )
                .border(
                    borderWidth,
                    borderColor,
                    RoundedCornerShape(6.dp),
                )
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { local ->
                            clickGate.arm()
                            val root = coords?.localToRoot(local) ?: return@detectDragGesturesAfterLongPress
                            onDragStartLatest.value(root)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val root =
                                coords?.localToRoot(change.position)
                                    ?: return@detectDragGesturesAfterLongPress
                            onDragLatest.value(root)
                        },
                        onDragEnd = { onDragEndLatest.value() },
                        onDragCancel = {
                            clickGate.clear()
                            onDragCancelLatest.value()
                        },
                    )
                }
                .clickable(
                    onClick = {
                        if (!clickGate.consumeSuppressedClick()) {
                            onClickLatest.value()
                        }
                    },
                )
                .padding(2.dp),
    ) {
        for ((direction, alignment) in PREVIEW_DIRECTIONAL_ALIGNMENTS) {
            val zone = Zone.Directional(direction)
            val legend =
                keyLegend(
                    intent = mapping.intents[zone],
                    visibility = legendVisibility,
                    modifierState = legendModifierState,
                    shiftMappings = emptyMap(),
                    displayLabel = mapping.displayLabels[zone],
                )
            if (legend != null) {
                KeyLegendMark(
                    legend = legend,
                    fontSize = 9.sp,
                    iconSize = swipeSize,
                    color = swipeColor.copy(alpha = if (dragging) 0.35f else 1f),
                    modifier = Modifier.align(alignment),
                )
            }
        }
        val centerLegend =
            keyLegend(
                intent = mapping.intents[Zone.Center],
                visibility = legendVisibility,
                modifierState = legendModifierState,
                shiftMappings = emptyMap(),
                displayLabel = mapping.displayLabels[Zone.Center],
            )
        if (centerLegend != null) {
            KeyLegendMark(
                legend = centerLegend,
                fontSize = 14.sp,
                iconSize = centerSize,
                color = centerColor.copy(alpha = if (dragging) 0.35f else 1f),
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (mapping.intents[Zone.Center] is KeyIntent.Noop || mapping.intents[Zone.Center] == null) {
            Text(
                text = "·",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 12.sp,
                color = swipeColor.copy(alpha = if (dragging) 0.35f else 1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun LayoutLayer.toggleCommandId(): CommandId =
    when (this) {
        LayoutLayer.MAIN -> CommandId.TOGGLE_ABC_MODE
        LayoutLayer.NUMERIC -> CommandId.TOGGLE_NUMERIC_MODE
        LayoutLayer.EMOJI -> CommandId.TOGGLE_EMOJI_MODE
        LayoutLayer.CLIPBOARD -> CommandId.TOGGLE_CLIPBOARD_HISTORY
    }

private val PREVIEW_DIRECTIONAL_ALIGNMENTS =
    listOf(
        Direction.UP_LEFT to Alignment.TopStart,
        Direction.UP to Alignment.TopCenter,
        Direction.UP_RIGHT to Alignment.TopEnd,
        Direction.LEFT to Alignment.CenterStart,
        Direction.RIGHT to Alignment.CenterEnd,
        Direction.DOWN_LEFT to Alignment.BottomStart,
        Direction.DOWN to Alignment.BottomCenter,
        Direction.DOWN_RIGHT to Alignment.BottomEnd,
    )

@Composable
private fun KeyEditorDialog(
    position: KeyPosition,
    mapping: KeyMapping,
    shiftMappings: Map<String, String>,
    capsLockMappings: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (KeyMapping, Map<String, String>, Map<String, String>) -> Unit,
) {
    var draft by remember(position, mapping) { mutableStateOf(mapping) }
    var shifts by remember(position, shiftMappings) { mutableStateOf(shiftMappings) }
    var caps by remember(position, capsLockMappings) { mutableStateOf(capsLockMappings) }
    var zoneDraft by remember { mutableStateOf<Pair<Zone, KeyIntent>?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.layout_edit_key, position.row + 1, position.col + 1))
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.layout_zones),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ZONE_ORDER.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        row.forEach { (name, zone) ->
                            val intent = draft.intents[zone]
                            val label =
                                when (intent) {
                                    null -> "-"
                                    is KeyIntent.Text -> intent.text.ifBlank { "text" }
                                    is KeyIntent.Command -> intent.id.name.take(6)
                                    is KeyIntent.ModifierPress -> intent.modifier.name
                                    is KeyIntent.Noop -> "noop"
                                }
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .clickable {
                                            zoneDraft = zone to (intent ?: KeyIntent.Noop)
                                        }
                                        .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(name, fontSize = 9.sp, maxLines = 1)
                                Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.height(12.dp))
                IntStepperPreference(
                    value = draft.columnSpan,
                    onValueChange = { draft = draft.copy(columnSpan = it) },
                    valueRange = 1..5,
                    title = {
                        SettingTitle(
                            text = stringResource(R.string.layout_column_span),
                            infoText = stringResource(R.string.layout_column_span_info),
                        )
                    },
                    summary = {
                        Text(stringResource(R.string.layout_column_span_summary, draft.columnSpan))
                    },
                    onReset = { draft = draft.copy(columnSpan = 1) },
                    resetTo = 1,
                )

                SettingsSection(title = stringResource(R.string.layout_key_advanced)) {
                    val fillValue = draft.fillRole.name
                    SettingRow(
                        onReset = { draft = draft.copy(fillRole = KeyFillRole.AUTO) },
                    ) {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = fillValue,
                            onValueChange = { draft = draft.copy(fillRole = KeyFillRole.valueOf(it)) },
                            values = KeyFillRole.entries.map { it.name },
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.layout_fill_role),
                                    infoText = stringResource(R.string.layout_fill_role_info),
                                )
                            },
                            summary = {
                                Text(stringResource(R.string.layout_fill_role_summary, fillValue))
                            },
                            valueToText = { AnnotatedString(it) },
                        )
                    }

                    val behaviorChoice =
                        when {
                            draft.gestureConfig.slideAxis == null -> SLIDE_BEHAVIOR_NONE
                            draft.slideBehavior == SlideBehavior.SELECT_AND_DELETE ->
                                SlideBehavior.SELECT_AND_DELETE.name
                            else -> SlideBehavior.MOVE_CURSOR.name
                        }
                    val behaviorLabel =
                        when (behaviorChoice) {
                            SLIDE_BEHAVIOR_NONE -> stringResource(R.string.layout_slide_behavior_none)
                            SlideBehavior.SELECT_AND_DELETE.name ->
                                stringResource(R.string.layout_slide_behavior_select_and_delete)
                            else -> stringResource(R.string.layout_slide_behavior_move_cursor)
                        }
                    SettingRow(
                        onReset = {
                            draft =
                            draft.copy(
                            gestureConfig = draft.gestureConfig.copy(slideAxis = null),
                            slideBehavior = null,
                            )
                        },
                    ) {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = behaviorChoice,
                            onValueChange = { v ->
                                draft =
                                    when (v) {
                                        SLIDE_BEHAVIOR_NONE ->
                                            draft.copy(
                                                gestureConfig =
                                                    draft.gestureConfig.copy(slideAxis = null),
                                                slideBehavior = null,
                                            )
                                        SlideBehavior.SELECT_AND_DELETE.name ->
                                            draft.copy(
                                                gestureConfig =
                                                    draft.gestureConfig.copy(
                                                        slideAxis =
                                                            draft.gestureConfig.slideAxis
                                                                ?: SlideAxis.HORIZONTAL,
                                                    ),
                                                slideBehavior = SlideBehavior.SELECT_AND_DELETE,
                                            )
                                        else ->
                                            draft.copy(
                                                gestureConfig =
                                                    draft.gestureConfig.copy(
                                                        slideAxis =
                                                            draft.gestureConfig.slideAxis
                                                                ?: SlideAxis.BOTH,
                                                    ),
                                                slideBehavior = SlideBehavior.MOVE_CURSOR,
                                            )
                                    }
                            },
                            values =
                                listOf(
                                    SLIDE_BEHAVIOR_NONE,
                                    SlideBehavior.MOVE_CURSOR.name,
                                    SlideBehavior.SELECT_AND_DELETE.name,
                                ),
                            title = {
                                SettingTitle(
                                    text = stringResource(R.string.layout_slide_behavior),
                                    infoText = stringResource(R.string.layout_slide_behavior_info),
                                )
                            },
                            summary = {
                                Text(stringResource(R.string.layout_slide_behavior_summary, behaviorLabel))
                            },
                            valueToText = { id ->
                                AnnotatedString(
                                    when (id) {
                                        SLIDE_BEHAVIOR_NONE ->
                                            context.getString(R.string.layout_slide_behavior_none)
                                        SlideBehavior.SELECT_AND_DELETE.name ->
                                            context.getString(
                                                R.string.layout_slide_behavior_select_and_delete,
                                            )
                                        else ->
                                            context.getString(R.string.layout_slide_behavior_move_cursor)
                                    },
                                )
                            },
                        )
                    }

                    draft.gestureConfig.slideAxis?.let { slideAxis ->
                        val directionValue = slideAxis.name
                        SettingRow(
                            onReset = {
                                val defaultAxis =
                                when (draft.slideBehavior) {
                                SlideBehavior.SELECT_AND_DELETE -> SlideAxis.HORIZONTAL
                                else -> SlideAxis.BOTH
                            }
                                draft =
                                    draft.copy(
                                        gestureConfig =
                                            draft.gestureConfig.copy(slideAxis = defaultAxis),
                                    )
                            },
                        ) {
                            ListPreference(
                                type = ListPreferenceType.DROPDOWN_MENU,
                                value = directionValue,
                                onValueChange = { v ->
                                    draft =
                                        draft.copy(
                                            gestureConfig =
                                                draft.gestureConfig.copy(
                                                    slideAxis = SlideAxis.valueOf(v),
                                                ),
                                        )
                                },
                                values = SlideAxis.entries.map { it.name },
                                title = {
                                    SettingTitle(
                                        text = stringResource(R.string.layout_slide_direction),
                                        infoText = stringResource(R.string.layout_slide_direction_info),
                                    )
                                },
                                summary = {
                                    Text(
                                        stringResource(
                                            R.string.layout_slide_direction_summary,
                                            directionValue,
                                        ),
                                    )
                                },
                                valueToText = { AnnotatedString(it) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft, shifts, caps) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )

    zoneDraft?.let { (zone, intent) ->
        val initialText = (intent as? KeyIntent.Text)?.text
        ZoneEditorDialog(
            zone = zone,
            initialIntent = intent,
            displayLabel = draft.displayLabels[zone],
            repeatOverride = draft.repeatOverrides[zone],
            shiftMappedTo = initialText?.let { shifts[it] }.orEmpty(),
            capsMappedTo = initialText?.let { caps[it] }.orEmpty(),
            canClear = zone != Zone.Center,
            onDismiss = { zoneDraft = null },
            onApply = { newIntent, label, repeat, clear, previousText, shiftTo, capsTo ->
                val intents = draft.intents.toMutableMap()
                val labels = draft.displayLabels.toMutableMap()
                val repeats = draft.repeatOverrides.toMutableMap()
                if (clear && zone != Zone.Center) {
                    intents.remove(zone)
                    labels.remove(zone)
                    repeats.remove(zone)
                    shifts = shifts.withoutCaseText(previousText)
                    caps = caps.withoutCaseText(previousText)
                } else {
                    intents[zone] = newIntent ?: KeyIntent.Noop
                    if (label.isNullOrBlank()) labels.remove(zone) else labels[zone] = label
                    if (repeat == null) repeats.remove(zone) else repeats[zone] = repeat
                    val newText = (newIntent as? KeyIntent.Text)?.text
                    shifts = shifts.withCaseMapping(previousText, newText, shiftTo)
                    caps = caps.withCaseMapping(previousText, newText, capsTo)
                }
                draft =
                    draft
                        .copy(
                            displayLabels = labels,
                            repeatOverrides = repeats,
                        ).withUpdatedIntents(intents)
                zoneDraft = null
            },
        )
    }
}

private const val SLIDE_BEHAVIOR_NONE = "NONE"

/** Update or clear a commit-text -> case-form entry. [mappedTo] null means leave maps alone (non-text). */
private fun Map<String, String>.withCaseMapping(
    previousText: String?,
    currentText: String?,
    mappedTo: String?,
): Map<String, String> {
    if (mappedTo == null && currentText == null) {
        return withoutCaseText(previousText)
    }
    val out = toMutableMap()
    if (!previousText.isNullOrEmpty() && previousText != currentText) {
        out.remove(previousText)
    }
    if (currentText.isNullOrEmpty() || mappedTo == null) {
        return out
    }
    if (mappedTo.isBlank()) {
        out.remove(currentText)
    } else {
        out[currentText] = mappedTo
    }
    return out
}

private fun Map<String, String>.withoutCaseText(text: String?): Map<String, String> {
    if (text.isNullOrEmpty()) return this
    return this - text
}

@Composable
private fun ZoneEditorDialog(
    zone: Zone,
    initialIntent: KeyIntent,
    displayLabel: String?,
    repeatOverride: Boolean?,
    shiftMappedTo: String,
    capsMappedTo: String,
    canClear: Boolean,
    onDismiss: () -> Unit,
    onApply: (
        KeyIntent?,
        String?,
        Boolean?,
        clear: Boolean,
        previousText: String?,
        shiftTo: String?,
        capsTo: String?,
    ) -> Unit,
) {
    val typeInitial =
        when (initialIntent) {
            is KeyIntent.Text -> "text"
            is KeyIntent.Command -> "command"
            is KeyIntent.ModifierPress -> "modifier"
            is KeyIntent.Noop -> "noop"
        }
    val previousText = (initialIntent as? KeyIntent.Text)?.text
    var type by remember { mutableStateOf(typeInitial) }
    var textValue by remember {
        mutableStateOf(previousText.orEmpty())
    }
    var commandId by remember {
        mutableStateOf((initialIntent as? KeyIntent.Command)?.id?.name ?: CommandId.ENTER.name)
    }
    var selectedModifierId by remember {
        mutableStateOf((initialIntent as? KeyIntent.ModifierPress)?.modifier?.name ?: ModifierId.SHIFT.name)
    }
    var label by remember { mutableStateOf(displayLabel.orEmpty()) }
    var shiftTo by remember { mutableStateOf(shiftMappedTo) }
    var capsTo by remember { mutableStateOf(capsMappedTo) }
    var repeatMode by remember {
        mutableStateOf(
            when (repeatOverride) {
                true -> "true"
                false -> "false"
                null -> "default"
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.layout_edit_zone)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ListPreference(
                    type = ListPreferenceType.DROPDOWN_MENU,
                    value = type,
                    onValueChange = { type = it },
                    values = listOf("text", "command", "modifier", "noop"),
                    title = { Text(stringResource(R.string.layout_zone_type)) },
                    summary = { Text(type) },
                    valueToText = { AnnotatedString(it) },
                )
                when (type) {
                    "text" -> {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text(stringResource(R.string.layout_zone_text)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text(stringResource(R.string.layout_zone_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = shiftTo,
                            onValueChange = { shiftTo = it },
                            label = { Text(stringResource(R.string.layout_zone_shift)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = capsTo,
                            onValueChange = { capsTo = it },
                            label = { Text(stringResource(R.string.layout_zone_caps)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Text(
                            text = stringResource(R.string.layout_zone_case_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    "command" -> {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = commandId,
                            onValueChange = { commandId = it },
                            values = CommandId.entries.map { it.name },
                            title = { Text(stringResource(R.string.layout_zone_command)) },
                            summary = { Text(commandId) },
                            valueToText = { AnnotatedString(it) },
                        )
                    }
                    "modifier" -> {
                        ListPreference(
                            type = ListPreferenceType.DROPDOWN_MENU,
                            value = selectedModifierId,
                            onValueChange = { selectedModifierId = it },
                            values = ModifierId.entries.map { it.name },
                            title = { Text(stringResource(R.string.layout_zone_modifier)) },
                            summary = { Text(selectedModifierId) },
                            valueToText = { AnnotatedString(it) },
                        )
                    }
                }
                if (type == "text" || type == "command") {
                    ListPreference(
                        type = ListPreferenceType.DROPDOWN_MENU,
                        value = repeatMode,
                        onValueChange = { repeatMode = it },
                        values = listOf("default", "true", "false"),
                        title = { Text(stringResource(R.string.layout_zone_repeat)) },
                        summary = { Text(repeatMode) },
                        valueToText = { AnnotatedString(it) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newIntent: KeyIntent =
                        when (type) {
                            "text" -> KeyIntent.Text(textValue)
                            "command" -> KeyIntent.Command(CommandId.valueOf(commandId))
                            "modifier" -> KeyIntent.ModifierPress(ModifierId.valueOf(selectedModifierId))
                            else -> KeyIntent.Noop
                        }
                    val repeat =
                        when (repeatMode) {
                            "true" -> true
                            "false" -> false
                            else -> null
                        }
                    val isText = type == "text"
                    onApply(
                        newIntent,
                        label.takeIf { isText && it.isNotBlank() },
                        repeat.takeIf { isText || type == "command" },
                        false,
                        previousText,
                        if (isText) shiftTo else null,
                        if (isText) capsTo else null,
                    )
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (canClear) {
                    TextButton(
                        onClick = {
                            onApply(null, null, null, true, previousText, null, null)
                        },
                    ) {
                        Text(stringResource(R.string.layout_zone_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}
