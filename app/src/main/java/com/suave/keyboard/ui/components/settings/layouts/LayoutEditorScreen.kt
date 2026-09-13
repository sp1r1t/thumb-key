package com.suave.keyboard.ui.components.settings.layouts

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Crop75
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardControlKey
import androidx.compose.material.icons.outlined.KeyboardOptionKey
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
import com.suave.keyboard.db.DEFAULT_KEY_HEIGHT
import com.suave.keyboard.db.DEFAULT_LANDSCAPE_KEY_HEIGHT
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
import com.suave.keyboard.layout.ActiveLayer
import com.suave.keyboard.layout.KeyInsertGap
import com.suave.keyboard.layout.LayerContent
import com.suave.keyboard.layout.LayerDefinition
import com.suave.keyboard.layout.LayerIcon
import com.suave.keyboard.layout.LayoutDraftHistory
import com.suave.keyboard.layout.LayoutPreviewSession
import com.suave.keyboard.layout.LayoutRegistry
import com.suave.keyboard.layout.LayoutResizeMemory
import com.suave.keyboard.layout.MAX_LAYERS
import com.suave.keyboard.layout.MAX_LAYER_HEIGHT_ROWS
import com.suave.keyboard.layout.NamedLayout
import com.suave.keyboard.layout.S12_CLIPBOARD_LAYER_HEIGHT_ROWS
import com.suave.keyboard.layout.S12_EMOJI_LAYER_HEIGHT_ROWS
import com.suave.keyboard.layout.UserLayoutStore
import com.suave.keyboard.layout.blankLayout
import com.suave.keyboard.layout.blankNamedLayout
import com.suave.keyboard.layout.normalizeTags
import com.suave.keyboard.layout.gridOrEmpty
import com.suave.keyboard.layout.idString
import com.suave.keyboard.layout.json.decodeNamedLayout
import com.suave.keyboard.layout.json.encodeNamedLayout
import com.suave.keyboard.layout.moveKeyToGap
import com.suave.keyboard.layout.moveRow
import com.suave.keyboard.layout.newLayerId
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
import com.suave.keyboard.ui.engine.KeyLegend
import com.suave.keyboard.ui.engine.KeyLegendMark
import com.suave.keyboard.ui.engine.LegendVisibility
import com.suave.keyboard.ui.engine.asImageVector
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.suave.keyboard.ui.engine.commandDisplayTitle
import com.suave.keyboard.ui.engine.commandEditorLegend
import com.suave.keyboard.ui.engine.createThemedEmojiPicker
import com.suave.keyboard.ui.engine.keyLegend
import com.suave.keyboard.ui.engine.legendColorVariant
import com.suave.keyboard.ui.engine.restingFillVariant
import com.suave.keyboard.ui.engine.switchLayerIconMap
import com.suave.keyboard.ui.engine.titleRes
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.colorVariantToColor
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LAYOUT_AUTO_SAVE_DEBOUNCE_MS = 250L
private const val LAYOUT_JSON_APPLY_DEBOUNCE_MS = 350L

private val ZONE_PAD_ROWS: List<List<Zone>> =
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
    val appSettings by appSettingsViewModel.appSettings.observeAsState()
    val appearanceKeyHeight = appSettings?.keyHeight ?: DEFAULT_KEY_HEIGHT
    val appearanceLandscapeKeyHeight = appSettings?.landscapeKeyHeight ?: DEFAULT_LANDSCAPE_KEY_HEIGHT

    var draft by remember { mutableStateOf<NamedLayout?>(null) }
    var sessionBaseline by remember { mutableStateOf<NamedLayout?>(null) }
    var existedOnOpen by remember { mutableStateOf(false) }
    var tagsField by remember { mutableStateOf("") }
    var persistJob by remember { mutableStateOf<Job?>(null) }
    var blankSetup by remember { mutableStateOf(createFrom == "blank" && editId == null) }
    var blankRowCount by remember { mutableStateOf(4) }
    var blankRowSizes by remember { mutableStateOf(listOf(5, 5, 5, 4)) }
    var selectedLayer by remember { mutableStateOf<ActiveLayer>(ActiveLayer.Main) }
    var editingKey by remember { mutableStateOf<KeyPosition?>(null) }
    var keyFlashPositions by remember { mutableStateOf<Set<KeyPosition>>(emptySet()) }
    var keyFlashGeneration by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }
    var customLayerDialog by remember { mutableStateOf<CustomLayerDialogMode?>(null) }
    var layoutJsonText by remember { mutableStateOf("") }
    var layoutJsonError by remember { mutableStateOf<String?>(null) }
    var layoutJsonFromEditor by remember { mutableStateOf(false) }
    var layoutJsonApplyJob by remember { mutableStateOf<Job?>(null) }
    val history = remember { LayoutDraftHistory() }
    val resizeMemoryByLayer = remember { mutableMapOf<String, LayoutResizeMemory>() }

    fun resizeMemory(layer: ActiveLayer): LayoutResizeMemory =
        resizeMemoryByLayer.getOrPut(layer.idString()) { LayoutResizeMemory() }

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
                    // Keep a first-open copy out of the enabled list until Back keeps it.
                    if (existedOnOpen) {
                        ensureLayoutEnabled(layout.id)
                    }
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
        // Fallback is always the open-time snapshot of this draft (same id). When Edited is
        // off and settings still select this layout, the IME uses that snapshot so a
        // half-broken mid-edit grid cannot trap typing. Other selected layouts resolve live.
        LayoutPreviewSession.bind(
            edited = layout,
            activeFallback = sessionBaseline?.takeIf { it.id == layout.id } ?: layout,
            useEdited = tryingOut,
        )
    }

    fun beginSession(
        layout: NamedLayout,
        existed: Boolean,
    ) {
        sessionBaseline = layout
        existedOnOpen = existed
        tagsField = layout.tags.joinToString(", ")
        history.clear()
        dirty = !existed
        bindPreviewSession(layout, tryingOut = false)
    }

    fun popEditor() {
        LayoutPreviewSession.stop()
        if (navController.previousBackStackEntry == null) {
            activity?.finish()
        } else {
            navController.popBackStack()
        }
    }

    fun leaveEditor() {
        persistJob?.cancel()
        val current = draft
        if (current == null) {
            popEditor()
            return
        }
        scope.launch {
            try {
                store.save(layoutForDisk(current))
                ensureLayoutEnabled(current.id)
            } catch (e: Exception) {
                Toast
                    .makeText(
                        ctx,
                        ctx.getString(R.string.layout_action_failed, e.message ?: ""),
                        Toast.LENGTH_LONG,
                    ).show()
            } finally {
                popEditor()
            }
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
                        LayoutRegistry.unregister(current.id)
                    } else {
                        sessionBaseline?.let {
                            store.save(layoutForDisk(it))
                            LayoutRegistry.register(it)
                        }
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
                popEditor()
            }
        }
    }

    fun commitDraft(
        next: NamedLayout,
        coalesceTitle: Boolean = false,
    ) {
        val cur = draft
        if (next.layer(selectedLayer) == null) {
            selectedLayer = next.homeActive()
        }
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
        tagsField = previous.tags.joinToString(", ")
        markDirtyAgainstBaseline(previous)
        schedulePersist(previous)
    }

    fun redoDraft() {
        val cur = draft ?: return
        val next = history.redo(cur) ?: return
        draft = next
        tagsField = next.tags.joinToString(", ")
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
    val sessionBoundHere = previewLayout?.id == layout.id
    val isPreviewing = sessionBoundHere && useEditedLayout

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

    LaunchedEffect(layout) {
        if (layoutJsonFromEditor) {
            layoutJsonFromEditor = false
            return@LaunchedEffect
        }
        layoutJsonApplyJob?.cancel()
        layoutJsonText = encodeNamedLayout(layout)
        layoutJsonError = null
    }

    fun onLayoutJsonChange(text: String) {
        layoutJsonText = text
        val parsed =
            runCatching { decodeNamedLayout(text).copy(id = layout.id) }
        parsed
            .onFailure { err ->
                layoutJsonError = err.message ?: err.toString()
                layoutJsonApplyJob?.cancel()
            }.onSuccess { next ->
                layoutJsonError = null
                if (next == draft) {
                    layoutJsonApplyJob?.cancel()
                    return
                }
                layoutJsonApplyJob?.cancel()
                layoutJsonApplyJob =
                    scope.launch {
                        delay(LAYOUT_JSON_APPLY_DEBOUNCE_MS)
                        layoutJsonFromEditor = true
                        commitDraft(next)
                    }
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
                OutlinedTextField(
                    value = tagsField,
                    onValueChange = { text ->
                        tagsField = text
                        commitDraft(
                            layout.copy(tags = normalizeTags(text.split(',', ';'))),
                            coalesceTitle = true,
                        )
                    },
                    label = { Text(stringResource(R.string.layout_tags)) },
                    supportingText = { Text(stringResource(R.string.layout_tags_hint)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        keyFlashPositions = keyFlashPositions,
                        keyFlashGeneration = keyFlashGeneration,
                        onAssignLayerSwitch = { pos, layer, zone ->
                            commitGridEdit { layerGrid ->
                                val mapping = layerGrid.getValue(pos)
                                val intents = mapping.intents.toMutableMap()
                                intents[zone] = layer.toSwitchIntent()
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
                                keyFlashPositions = setOf(from, to)
                                keyFlashGeneration += 1
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
                        onLayerHeightRowsChange = { layer, totalRows ->
                            val def = layout.layer(layer) ?: return@LayerSwitchAssigner
                            val gridRows = def.gridRowCount()
                            val contentRows = (totalRows - gridRows).coerceAtLeast(0)
                            if (contentRows > 0 && def.content == LayerContent.None) {
                                return@LayerSwitchAssigner
                            }
                            commitDraft(
                                layout.withLayer(def.copy(contentRows = contentRows)),
                            )
                        },
                        onLayerContentChange = { layer, content ->
                            val def = layout.layer(layer) ?: return@LayerSwitchAssigner
                            val gridRows = def.gridRowCount()
                            val next =
                                when (content) {
                                    LayerContent.None ->
                                        def.copy(content = LayerContent.None, contentRows = 0)
                                    LayerContent.EmojiPicker,
                                    LayerContent.ClipboardHistory,
                                    -> {
                                        val rows =
                                            if (def.content == content && def.contentRows > 0) {
                                                def.contentRows
                                            } else {
                                                defaultContentRows(layer.id, gridRows)
                                            }
                                        def.copy(content = content, contentRows = rows)
                                    }
                                }
                            commitDraft(layout.withLayer(next))
                        },
                        onAddCustomLayer = {
                            if (layout.layers.size < MAX_LAYERS) {
                                customLayerDialog = CustomLayerDialogMode.Add
                            }
                        },
                        onEditCustomLayer = { id ->
                            layout.layer(id)?.let {
                                customLayerDialog = CustomLayerDialogMode.Edit(it)
                            }
                        },
                        onSeedLayer =
                            if (selectedLayer != layout.homeActive() && grid.isEmpty()) {
                                {
                                    val seed =
                                        when (selectedLayer.id) {
                                            ActiveLayer.EMOJI,
                                            ActiveLayer.CLIPBOARD,
                                            -> listOf(4)
                                            else -> listOf(5, 5, 5, 4)
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
                            if (
                                isUserLayerId(selectedLayer.id) &&
                                    selectedLayer.id != layout.homeLayerId &&
                                    layout.layer(selectedLayer) != null
                            ) {
                                {
                                    val cur = draft
                                    if (cur != null) {
                                        commitDraft(cur.withoutLayer(selectedLayer.id))
                                        selectedLayer = cur.homeActive()
                                    }
                                }
                            } else {
                                null
                            },
                    )
                    customLayerDialog?.let { mode ->
                        CustomLayerConfigDialog(
                            mode = mode,
                            onDismiss = { customLayerDialog = null },
                            onSave = { title, icon ->
                                val cur = draft ?: return@CustomLayerConfigDialog
                                when (mode) {
                                    CustomLayerDialogMode.Add -> {
                                        if (cur.layers.size >= MAX_LAYERS) {
                                            customLayerDialog = null
                                            return@CustomLayerConfigDialog
                                        }
                                        val id = newLayerId()
                                        val layer =
                                            LayerDefinition(
                                                id = id,
                                                title = title,
                                                icon = icon,
                                                overlay = false,
                                                keyGrid = blankLayout(listOf(5, 5, 5, 4)),
                                            )
                                        commitDraft(cur.withLayer(layer))
                                        selectedLayer = ActiveLayer(id)
                                    }
                                    is CustomLayerDialogMode.Edit -> {
                                        val updated =
                                            mode.layer.copy(title = title, icon = icon)
                                        commitDraft(cur.withLayer(updated))
                                    }
                                }
                                customLayerDialog = null
                            },
                        )
                    }
                }

                SettingsSection(
                    title = stringResource(R.string.layout_settings),
                    initiallyExpanded = false,
                ) {
                    val layoutKeyHeight = layout.keyHeight ?: appearanceKeyHeight
                    val layoutLandscapeKeyHeight =
                        layout.landscapeKeyHeight ?: appearanceLandscapeKeyHeight
                    IntStepperPreference(
                        value = layoutKeyHeight,
                        onValueChange = { v ->
                            commitDraft(layout.copy(keyHeight = v))
                        },
                        valueRange = 10..200,
                        title = {
                            SettingTitle(
                                text = stringResource(R.string.layout_key_height),
                                infoText = stringResource(R.string.layout_key_height_info),
                            )
                        },
                        summary = {
                            Text(
                                if (layout.keyHeight == null) {
                                    stringResource(
                                        R.string.layout_key_height_summary_appearance,
                                        appearanceKeyHeight.toString(),
                                    )
                                } else {
                                    stringResource(
                                        R.string.layout_key_height_summary,
                                        layoutKeyHeight.toString(),
                                    )
                                },
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Crop75,
                                contentDescription = null,
                            )
                        },
                        onReset = {
                            commitDraft(layout.copy(keyHeight = null))
                        },
                        resetTo = appearanceKeyHeight,
                    )
                    IntStepperPreference(
                        value = layoutLandscapeKeyHeight,
                        onValueChange = { v ->
                            commitDraft(layout.copy(landscapeKeyHeight = v))
                        },
                        valueRange = 10..200,
                        title = {
                            SettingTitle(
                                text = stringResource(R.string.layout_landscape_key_height),
                                infoText = stringResource(R.string.layout_landscape_key_height_info),
                            )
                        },
                        summary = {
                            Text(
                                if (layout.landscapeKeyHeight == null) {
                                    stringResource(
                                        R.string.layout_landscape_key_height_summary_appearance,
                                        appearanceLandscapeKeyHeight.toString(),
                                    )
                                } else {
                                    stringResource(
                                        R.string.layout_landscape_key_height_summary,
                                        layoutLandscapeKeyHeight.toString(),
                                    )
                                },
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Crop75,
                                contentDescription = null,
                            )
                        },
                        onReset = {
                            commitDraft(layout.copy(landscapeKeyHeight = null))
                        },
                        resetTo = appearanceLandscapeKeyHeight,
                    )
                    SwitchPreference(
                        value = layout.landscapeFloating,
                        onValueChange = { on ->
                            commitDraft(layout.copy(landscapeFloating = on))
                        },
                        title = {
                            SettingTitle(
                                text = stringResource(R.string.layout_landscape_floating),
                                infoText = stringResource(R.string.layout_landscape_floating_info),
                            )
                        },
                        summary = {
                            Text(
                                stringResource(
                                    if (layout.landscapeFloating) {
                                        R.string.layout_landscape_floating_on
                                    } else {
                                        R.string.layout_landscape_floating_off
                                    },
                                ),
                            )
                        },
                    )
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

                SettingsSection(
                    title = stringResource(R.string.layout_json),
                    initiallyExpanded = false,
                    infoText = stringResource(R.string.layout_json_info),
                ) {
                    val jsonInvalid = layoutJsonError != null
                    Text(
                        text =
                            if (jsonInvalid) {
                                stringResource(
                                    R.string.layout_json_invalid,
                                    layoutJsonError.orEmpty(),
                                )
                            } else {
                                stringResource(R.string.layout_json_valid)
                            },
                        color =
                            if (jsonInvalid) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LayoutJsonEditor(
                        text = layoutJsonText,
                        isError = jsonInvalid,
                        onTextChange = ::onLayoutJsonChange,
                    )
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
                    } else if (sessionBoundHere) {
                        Text(
                            text = stringResource(R.string.layout_stable_preview_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text =
                            stringResource(
                                if (existedOnOpen) {
                                    R.string.layout_unsaved_changes
                                } else {
                                    R.string.layout_unsaved_new_copy
                                },
                            ),
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
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text(stringResource(R.string.layout_cancel_edits))
                    }
                }
                }
                // Keep the test field mounted for the whole editor session so toggling
                // Edited/Active only swaps the IME layout and does not hide the keyboard.
                if (sessionBoundHere) {
                    LayoutPreviewTestField(requestShow = isPreviewing)
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
                namedLayout = layout,
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
                    keyFlashPositions = setOf(pos)
                    keyFlashGeneration += 1
                    editingKey = null
                },
            )
        }
    }
}

private sealed class PreviewDrag {
    data class Layer(val layer: ActiveLayer) : PreviewDrag()

    data class Key(val from: KeyPosition) : PreviewDrag()

    data class Row(val from: Int) : PreviewDrag()
}

/**
 * Pinned under the editor while a preview session is bound. Stays composed across Edited
 * toggles so the IME is not torn down. [requestShow] focuses and opens the keyboard when
 * Edited/Try-on turns on; turning Edited off leaves focus alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayoutPreviewTestField(requestShow: Boolean) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget

    LaunchedEffect(requestShow) {
        if (!requestShow) return@LaunchedEffect
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
    selected: ActiveLayer,
    onSelect: (ActiveLayer) -> Unit,
    namedLayout: NamedLayout,
    grid: Map<KeyPosition, KeyMapping>,
    onKeyClick: (KeyPosition) -> Unit,
    keyFlashPositions: Set<KeyPosition> = emptySet(),
    keyFlashGeneration: Int = 0,
    onAssignLayerSwitch: (KeyPosition, ActiveLayer, Zone) -> Unit,
    onSwapKeys: (KeyPosition, KeyPosition) -> Unit,
    onMoveKeyToGap: (KeyPosition, KeyInsertGap) -> Unit,
    onRemoveKey: (KeyPosition) -> Unit,
    onMoveRow: (Int, Int) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onResizeRows: (List<Int>) -> Unit,
    onLayerHeightRowsChange: (ActiveLayer, Int) -> Unit,
    onLayerContentChange: (ActiveLayer, LayerContent) -> Unit,
    onAddCustomLayer: () -> Unit,
    onEditCustomLayer: (String) -> Unit,
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
                namedLayout = namedLayout,
                onSelect = onSelect,
                onAddCustomLayer = onAddCustomLayer,
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
            val selectedDef = namedLayout.layer(selected)
            val layerContent = selectedDef?.content ?: LayerContent.None
            LayerWidgetPicker(
                content = layerContent,
                onContentChange = { onLayerContentChange(selected, it) },
            )
            if (layerContent != LayerContent.None) {
                val contentRows = namedLayout.contentRows(selected)
                val panelRows = contentRows.coerceAtLeast(1)
                LayoutPreviewLayerPanel(
                    content = if (contentRows > 0) layerContent else LayerContent.None,
                    height = PREVIEW_KEY_HEIGHT * panelRows,
                    keyHeight = PREVIEW_KEY_HEIGHT,
                    heightControls =
                        PanelHeightControls(
                            totalRows = namedLayout.heightRows(selected),
                            minRows = namedLayout.gridRowCount(selected),
                            maxRows = MAX_LAYER_HEIGHT_ROWS,
                            defaultRows =
                                defaultPanelHeightTotal(selected.id)
                                    .coerceAtLeast(namedLayout.gridRowCount(selected)),
                            onChange = { onLayerHeightRowsChange(selected, it) },
                        ),
                )
            }
            LayoutPreviewGrid(
                layout = grid,
                switchLayerIcons = namedLayout.switchLayerIconMap(),
                onKeyClick = onKeyClick,
                onResizeRows = onResizeRows,
                highlightedKey = hoverKey,
                draggingKey = (drag as? PreviewDrag.Key)?.from,
                keyFlashPositions = keyFlashPositions,
                keyFlashGeneration = keyFlashGeneration,
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
            val selectedUserLayer =
                selected.takeIf {
                    isUserLayerId(it.id) && it.id != namedLayout.homeLayerId
                }
            if (selectedUserLayer != null) {
                TextButton(
                    onClick = { onEditCustomLayer(selectedUserLayer.id) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.layout_edit_custom_layer))
                }
            }
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
                            imageVector = activeLayerChipIcon(current.layer, namedLayout),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = activeLayerChipLabel(current.layer, namedLayout),
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
                                    switchLayerIcons = namedLayout.switchLayerIconMap(),
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

private val WELL_KNOWN_LAYER_IDS =
    setOf(
        ActiveLayer.MAIN,
        ActiveLayer.NUMERIC,
        ActiveLayer.EMOJI,
        ActiveLayer.CLIPBOARD,
    )

private fun isUserLayerId(id: String): Boolean = id !in WELL_KNOWN_LAYER_IDS

@Composable
private fun activeLayerChipLabel(
    layer: ActiveLayer,
    namedLayout: NamedLayout,
): String {
    namedLayout.layer(layer)?.title?.takeIf { it.isNotBlank() }?.let { return it }
    return when (layer.id) {
        ActiveLayer.MAIN -> stringResource(R.string.layout_layer_main)
        ActiveLayer.NUMERIC -> stringResource(R.string.layout_layer_numeric)
        ActiveLayer.EMOJI -> stringResource(R.string.layout_layer_emoji)
        ActiveLayer.CLIPBOARD -> stringResource(R.string.layout_layer_clipboard)
        else -> stringResource(R.string.layout_custom_layer_fallback)
    }
}

/** S12-style panel defaults: total rows including the key grid. */
private fun defaultPanelHeightTotal(layerId: String): Int =
    when (layerId) {
        ActiveLayer.EMOJI -> S12_EMOJI_LAYER_HEIGHT_ROWS
        ActiveLayer.CLIPBOARD -> S12_CLIPBOARD_LAYER_HEIGHT_ROWS
        else -> 0
    }

/** Default content strip height when enabling a panel on this layer. */
private fun defaultContentRows(
    layerId: String,
    gridRows: Int,
): Int {
    val fromBuiltin = defaultPanelHeightTotal(layerId) - gridRows
    return if (fromBuiltin > 0) fromBuiltin else 5
}

private enum class LayerPanelChoice {
    NONE,
    EMOJI,
    CLIPBOARD,
}

/**
 * Layer widgets (emoji picker / clipboard history) live above the key grid - they are not
 * key-zone actions. Chips make that obvious next to the preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerWidgetPicker(
    content: LayerContent,
    onContentChange: (LayerContent) -> Unit,
) {
    val choice =
        when (content) {
            LayerContent.None -> LayerPanelChoice.NONE
            LayerContent.EmojiPicker -> LayerPanelChoice.EMOJI
            LayerContent.ClipboardHistory -> LayerPanelChoice.CLIPBOARD
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Widgets,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingTitle(
                text = stringResource(R.string.layout_layer_widget),
                infoText = stringResource(R.string.layout_layer_widget_info),
            )
        }
        Text(
            text =
                stringResource(
                    when (choice) {
                        LayerPanelChoice.NONE -> R.string.layout_layer_widget_summary_none
                        LayerPanelChoice.EMOJI -> R.string.layout_layer_widget_summary_emoji
                        LayerPanelChoice.CLIPBOARD -> R.string.layout_layer_widget_summary_clipboard
                    },
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = choice == LayerPanelChoice.NONE,
                onClick = { onContentChange(LayerContent.None) },
                label = { Text(stringResource(R.string.layout_layer_widget_none)) },
            )
            FilterChip(
                selected = choice == LayerPanelChoice.EMOJI,
                onClick = { onContentChange(LayerContent.EmojiPicker) },
                label = { Text(stringResource(R.string.layout_layer_widget_emoji)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEmotions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            FilterChip(
                selected = choice == LayerPanelChoice.CLIPBOARD,
                onClick = { onContentChange(LayerContent.ClipboardHistory) },
                label = { Text(stringResource(R.string.layout_layer_widget_clipboard)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

private fun activeLayerChipIcon(
    layer: ActiveLayer,
    namedLayout: NamedLayout,
): ImageVector =
    namedLayout.layer(layer)?.icon?.asImageVector() ?: Icons.Outlined.Functions

private fun ActiveLayer.toSwitchIntent(): KeyIntent = KeyIntent.SwitchLayer(idString())

private sealed class CustomLayerDialogMode {
    data object Add : CustomLayerDialogMode()

    data class Edit(
        val layer: LayerDefinition,
    ) : CustomLayerDialogMode()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerChips(
    selected: ActiveLayer,
    namedLayout: NamedLayout,
    onSelect: (ActiveLayer) -> Unit,
    onAddCustomLayer: () -> Unit,
    onDragStart: (ActiveLayer, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val layers = remember(namedLayout.layers) { namedLayout.availableLayers() }
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
        layers.forEach { layer ->
            key(layer.idString()) {
                var coords by remember(layer.idString()) { mutableStateOf<LayoutCoordinates?>(null) }
                val clickGate = rememberDragClickGate(layer.idString())
                val onDragStartLatest = rememberUpdatedState(onDragStart)
                val onDragLatest = rememberUpdatedState(onDrag)
                val onDragEndLatest = rememberUpdatedState(onDragEnd)
                val onDragCancelLatest = rememberUpdatedState(onDragCancel)
                val label = activeLayerChipLabel(layer, namedLayout)
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
                            imageVector = activeLayerChipIcon(layer, namedLayout),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier =
                        Modifier
                            .onGloballyPositioned { coords = it }
                            .pointerInput(layer.idString()) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { local ->
                                        clickGate.arm()
                                        val root =
                                            coords?.localToRoot(local)
                                                ?: return@detectDragGesturesAfterLongPress
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
        if (namedLayout.layers.size < MAX_LAYERS) {
            FilterChip(
                selected = false,
                onClick = onAddCustomLayer,
                label = { Text(stringResource(R.string.layout_add_custom_layer)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomLayerConfigDialog(
    mode: CustomLayerDialogMode,
    onDismiss: () -> Unit,
    onSave: (title: String, icon: LayerIcon) -> Unit,
) {
    val initial =
        when (mode) {
            CustomLayerDialogMode.Add -> null
            is CustomLayerDialogMode.Edit -> mode.layer
        }
    var title by remember(mode) {
        mutableStateOf(initial?.title ?: "")
    }
    var icon by remember(mode) {
        mutableStateOf(initial?.icon ?: LayerIcon.Functions)
    }
    val canSave = title.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (mode is CustomLayerDialogMode.Add) {
                        R.string.layout_add_custom_layer_title
                    } else {
                        R.string.layout_edit_custom_layer_title
                    },
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.layout_custom_layer_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    text = stringResource(R.string.layout_custom_layer_icon),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LayerIcon.entries.forEach { option ->
                        FilterChip(
                            selected = icon == option,
                            onClick = { icon = option },
                            label = { Text(option.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.asImageVector(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), icon) },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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

private data class PanelHeightControls(
    val totalRows: Int,
    val minRows: Int,
    val maxRows: Int,
    val defaultRows: Int,
    val onChange: (Int) -> Unit,
)

@Composable
private fun LayoutPreviewLayerPanel(
    content: LayerContent,
    height: Dp,
    keyHeight: Dp,
    heightControls: PanelHeightControls? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(
                    start = PREVIEW_KEYS_START_INSET,
                    bottom = 4.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = if (heightControls != null) PREVIEW_ROW_INNER_GAP else PREVIEW_KEYS_END_INSET)
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

        if (heightControls != null) {
            PanelHeightChevrons(
                controls = heightControls,
                modifier =
                    Modifier
                        .width(PREVIEW_ADD_KEY_SIZE)
                        .height(height.coerceAtLeast(PREVIEW_KEY_HEIGHT * 2)),
            )
        }
    }
}

@Composable
private fun PanelHeightChevrons(
    controls: PanelHeightControls,
    modifier: Modifier = Modifier,
) {
    val canTaller = controls.totalRows < controls.maxRows
    val canShorter = controls.totalRows > controls.minRows
    val atDefault = controls.totalRows == controls.defaultRows
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = { controls.onChange(controls.totalRows + 1) },
            enabled = canTaller,
            modifier = Modifier.size(PREVIEW_ADD_KEY_SIZE),
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = stringResource(R.string.layout_panel_taller),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = controls.totalRows.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        IconButton(
            onClick = { controls.onChange(controls.totalRows - 1) },
            enabled = canShorter,
            modifier = Modifier.size(PREVIEW_ADD_KEY_SIZE),
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.layout_panel_shorter),
                modifier = Modifier.size(20.dp),
            )
        }
        if (!atDefault) {
            IconButton(
                onClick = { controls.onChange(controls.defaultRows) },
                modifier = Modifier.size(PREVIEW_ADD_KEY_SIZE),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = stringResource(R.string.layout_panel_height_reset),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun LayoutPreviewGrid(
    layout: Map<KeyPosition, KeyMapping>,
    switchLayerIcons: Map<String, ImageVector> = emptyMap(),
    onKeyClick: (KeyPosition) -> Unit,
    onResizeRows: (List<Int>) -> Unit,
    highlightedKey: KeyPosition? = null,
    draggingKey: KeyPosition? = null,
    keyFlashPositions: Set<KeyPosition> = emptySet(),
    keyFlashGeneration: Int = 0,
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
                        if (mapping.fillRole == KeyFillRole.SPACER) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .weight(mapping.columnSpan)
                                        .fillMaxHeight(),
                            )
                        } else {
                            LayoutPreviewKey(
                                mapping = mapping,
                                legendVisibility = legendVisibility,
                                legendModifierState = legendModifierState,
                                switchLayerIcons = switchLayerIcons,
                                highlighted = highlightedKey == pos,
                                dragging = draggingKey == pos || rowDragging,
                                flashGeneration =
                                    if (pos in keyFlashPositions) keyFlashGeneration else 0,
                                onClick = { onKeyClick(pos) },
                                onBoundsInRoot = { rect -> onKeyBoundsInRoot(pos, rect) },
                                onDragStart = { root -> onKeyDragStart(pos, root) },
                                onDrag = onKeyDrag,
                                onDragEnd = onKeyDragEnd,
                                onDragCancel = onKeyDragCancel,
                                modifier =
                                    Modifier
                                        .weight(mapping.columnSpan)
                                        .fillMaxHeight(),
                            )
                        }
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
    TextButton(
        onClick = onClick,
        enabled = enabled,
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
    switchLayerIcons: Map<String, ImageVector> = emptyMap(),
    highlighted: Boolean,
    dragging: Boolean,
    flashGeneration: Int = 0,
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
    val restingBg = MaterialTheme.colorScheme.surfaceVariant
    val flashBg = MaterialTheme.colorScheme.primaryContainer
    val restingBorder = MaterialTheme.colorScheme.outlineVariant
    val flashBorder = MaterialTheme.colorScheme.primary
    val flash = remember { Animatable(0f) }
    LaunchedEffect(flashGeneration) {
        if (flashGeneration <= 0) {
            flash.snapTo(0f)
            return@LaunchedEffect
        }
        flash.snapTo(1f)
        flash.animateTo(0f, animationSpec = tween(durationMillis = 750))
    }
    val flashT = flash.value
    val borderColor =
        when {
            highlighted -> MaterialTheme.colorScheme.primary
            dragging -> MaterialTheme.colorScheme.tertiary
            else -> lerp(restingBorder, flashBorder, flashT)
        }
    val borderWidth = if (highlighted || dragging || flashT > 0.05f) 2.dp else 1.dp
    val backgroundColor =
        when {
            highlighted -> MaterialTheme.colorScheme.primaryContainer
            dragging -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else -> lerp(restingBg, flashBg, flashT)
        }
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
                .graphicsLayer {
                    val scale = 1f + (0.06f * flashT)
                    scaleX = scale
                    scaleY = scale
                }
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
                    backgroundColor,
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
                    switchLayerIcons = switchLayerIcons,
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
                switchLayerIcons = switchLayerIcons,
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
private fun KeyEditorZonePad(
    mapping: KeyMapping,
    onZoneClick: (Zone) -> Unit,
) {
    val span = mapping.columnSpan.coerceIn(1f, 5f)
    val shape = RoundedCornerShape(16.dp)
    val fill = colorVariantToColor(mapping.restingFillVariant(distinctLetterControlColors = true))
    val swipeColor = colorVariantToColor(legendColorVariant(isCenter = false))
    val centerColor = colorVariantToColor(legendColorVariant(isCenter = true))
    val centerIntent = mapping.intents[Zone.Center]
    val centerCommand =
        if (centerIntent is KeyIntent.Command) {
            commandDisplayTitle(centerIntent.id)
        } else {
            null
        }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Keep a comfortable key height (span-1 size). Widen with span for a visual cue,
        // but never exceed the dialog - do not flatten the key to preserve a 1:N ratio.
        val keyHeight = maxOf(PREVIEW_KEY_HEIGHT * 2.5f, maxWidth * 0.62f)
        val widthFactor = 1f + (span - 1) * 0.55f
        val keyWidth = minOf(maxWidth, keyHeight * widthFactor)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .width(keyWidth)
                        .height(keyHeight)
                        .clip(shape)
                        .background(fill)
                        .border(1.dp, MaterialTheme.colorScheme.outline, shape),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                ) {
                    for ((direction, alignment) in PREVIEW_DIRECTIONAL_ALIGNMENTS) {
                        ZonePadLegend(
                            mapping = mapping,
                            zone = Zone.Directional(direction),
                            large = false,
                            color = swipeColor,
                            modifier = Modifier.align(alignment),
                        )
                    }
                    ZonePadLegend(
                        mapping = mapping,
                        zone = Zone.Center,
                        large = true,
                        color = centerColor,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    ZONE_PAD_ROWS.forEach { row ->
                        Row(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                        ) {
                            row.forEach { zone ->
                                Box(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { onZoneClick(zone) },
                                )
                            }
                        }
                    }
                }
            }
            if (centerCommand != null) {
                Text(
                    text = "($centerCommand)",
                    fontSize = 12.sp,
                    color = swipeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ZonePadLegend(
    mapping: KeyMapping,
    zone: Zone,
    large: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val intent = mapping.intents[zone]
    val fontSize = if (large) 36.sp else 18.sp
    val iconSize = if (large) 40.dp else 22.dp
    when (intent) {
        null, is KeyIntent.Noop -> {
            // Empty zones still show a soft dot so every swipe slot is an obvious tap target
            // (same visual language as the center sensor and the app logo).
            Box(
                modifier =
                    modifier
                        .size(if (large) 10.dp else 7.dp)
                        .background(color.copy(alpha = 0.45f), CircleShape),
            )
        }
        is KeyIntent.Command -> {
            KeyLegendMark(
                legend = commandEditorLegend(intent.id),
                fontSize = fontSize,
                iconSize = iconSize,
                color = color,
                modifier = modifier,
            )
        }
        else -> {
            val legend =
                keyLegend(
                    intent = intent,
                    visibility = LegendVisibility(),
                    modifierState = ModifierState.NONE,
                    shiftMappings = emptyMap(),
                    displayLabel = mapping.displayLabels[zone],
                )
            if (legend != null) {
                KeyLegendMark(
                    legend = legend,
                    fontSize = fontSize,
                    iconSize = iconSize,
                    color = color,
                    modifier = modifier,
                )
            } else {
                Box(
                    modifier =
                        modifier
                            .size(if (large) 10.dp else 7.dp)
                            .background(color.copy(alpha = 0.45f), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun KeyEditorDialog(
    position: KeyPosition,
    mapping: KeyMapping,
    namedLayout: NamedLayout,
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
                KeyEditorZonePad(
                    mapping = draft,
                    onZoneClick = { zone ->
                        zoneDraft = zone to (draft.intents[zone] ?: KeyIntent.Noop)
                    },
                )

                Box(modifier = Modifier.height(12.dp))
                IntStepperPreference(
                    value = draft.columnSpan.toInt().coerceIn(1, 5),
                    onValueChange = { draft = draft.copy(columnSpan = it.toFloat()) },
                    valueRange = 1..5,
                    title = {
                        SettingTitle(
                            text = stringResource(R.string.layout_column_span),
                            infoText = stringResource(R.string.layout_column_span_info),
                        )
                    },
                    summary = {
                        Text(
                            stringResource(
                                R.string.layout_column_span_summary,
                                draft.columnSpan.toInt().coerceIn(1, 5),
                            ),
                        )
                    },
                    onReset = { draft = draft.copy(columnSpan = 1f) },
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
                            values = KeyFillRole.entries.filter { it != KeyFillRole.SPACER }.map { it.name },
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
            namedLayout = namedLayout,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneEditorDialog(
    zone: Zone,
    initialIntent: KeyIntent,
    displayLabel: String?,
    repeatOverride: Boolean?,
    shiftMappedTo: String,
    capsMappedTo: String,
    canClear: Boolean,
    namedLayout: NamedLayout,
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
    val context = LocalContext.current
    val legacyCommand = initialIntent as? KeyIntent.Command
    val legacySwitchLayerId = legacyCommand?.id?.switchLayerIdOrNull()
    val legacyModifierId = legacyCommand?.id?.modifierIdOrNull()
    val typeInitial =
        when (initialIntent) {
            is KeyIntent.Text -> "text"
            is KeyIntent.Command ->
                when {
                    legacySwitchLayerId != null -> "switchLayer"
                    legacyModifierId != null -> "modifier"
                    else -> "command"
                }
            is KeyIntent.ModifierPress -> "modifier"
            is KeyIntent.SwitchLayer -> "switchLayer"
            is KeyIntent.Noop -> "noop"
        }
    val previousText = (initialIntent as? KeyIntent.Text)?.text
    var type by remember { mutableStateOf(typeInitial) }
    var textValue by remember { mutableStateOf(previousText.orEmpty()) }
    var selectedCommand by remember {
        mutableStateOf(
            legacyCommand
                ?.id
                ?.takeUnless { it.isLayerSwitchCommand() || it.isModifierCommand() }
                ?: CommandId.ENTER,
        )
    }
    var switchLayerId by remember {
        mutableStateOf(
            (initialIntent as? KeyIntent.SwitchLayer)?.layerId
                ?: legacySwitchLayerId
                ?: namedLayout.availableLayers().firstOrNull()?.idString().orEmpty(),
        )
    }
    var selectedModifierId by remember {
        mutableStateOf(
            (initialIntent as? KeyIntent.ModifierPress)?.modifier
                ?: legacyModifierId
                ?: ModifierId.SHIFT,
        )
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
    val typeValues = listOf("text", "command", "modifier", "switchLayer", "noop")
    fun typeLabel(key: String): String =
        context.getString(
            when (key) {
                "text" -> R.string.layout_zone_type_text
                "command" -> R.string.layout_zone_type_command
                "modifier" -> R.string.layout_zone_type_modifier
                "switchLayer" -> R.string.layout_zone_type_switch_layer
                else -> R.string.layout_zone_type_noop
            },
        )
    fun repeatLabel(key: String): String =
        context.getString(
            when (key) {
                "true" -> R.string.layout_zone_repeat_on
                "false" -> R.string.layout_zone_repeat_off
                else -> R.string.layout_zone_repeat_default
            },
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.layout_edit_zone)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SettingTitle(text = stringResource(R.string.layout_zone_type))
                Text(
                    text = stringResource(R.string.layout_zone_type_summary, typeLabel(type)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    typeValues.forEach { key ->
                        FilterChip(
                            selected = type == key,
                            onClick = { type = key },
                            label = { Text(typeLabel(key)) },
                        )
                    }
                }
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
                        SettingTitle(text = stringResource(R.string.layout_zone_command))
                        Text(
                            text =
                                stringResource(
                                    R.string.layout_zone_command_summary,
                                    commandDisplayTitle(selectedCommand),
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        ZoneCommandPicker(
                            selected = selectedCommand,
                            onSelect = { selectedCommand = it },
                        )
                    }
                    "modifier" -> {
                        SettingTitle(text = stringResource(R.string.layout_zone_modifier))
                        Text(
                            text =
                                stringResource(
                                    R.string.layout_zone_modifier_summary,
                                    modifierDisplayTitle(selectedModifierId),
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        ZoneModifierPicker(
                            selected = selectedModifierId,
                            onSelect = { selectedModifierId = it },
                        )
                    }
                    "switchLayer" -> {
                        SettingTitle(
                            text = stringResource(R.string.layout_zone_switch_layer),
                            infoText = stringResource(R.string.layout_zone_switch_layer_info),
                        )
                        val selectedLayer =
                            namedLayout.availableLayers().find { it.idString() == switchLayerId }
                        Text(
                            text =
                                stringResource(
                                    R.string.layout_zone_switch_layer_summary,
                                    selectedLayer?.let { activeLayerChipLabel(it, namedLayout) }
                                        ?: switchLayerId.ifBlank { "-" },
                                    selectedLayer?.let { switchLayerPressEffect(it) }
                                        ?: stringResource(R.string.layout_zone_switch_effect_custom),
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        ZoneLayerPicker(
                            namedLayout = namedLayout,
                            selectedId = switchLayerId,
                            onSelect = { switchLayerId = it },
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
                        summary = {
                            Text(
                                stringResource(
                                    R.string.layout_zone_repeat_summary,
                                    repeatLabel(repeatMode),
                                ),
                            )
                        },
                        valueToText = { AnnotatedString(repeatLabel(it)) },
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
                            "command" -> KeyIntent.Command(selectedCommand)
                            "modifier" -> KeyIntent.ModifierPress(selectedModifierId)
                            "switchLayer" ->
                                KeyIntent.SwitchLayer(
                                    switchLayerId.trim().ifBlank {
                                        namedLayout.availableLayers().first().idString()
                                    },
                                )
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

@Composable
private fun modifierDisplayTitle(id: ModifierId): String =
    stringResource(
        when (id) {
            ModifierId.CTRL -> R.string.layout_zone_modifier_ctrl
            ModifierId.ALT -> R.string.layout_zone_modifier_alt
            ModifierId.SHIFT -> R.string.layout_zone_modifier_shift
            ModifierId.ESC -> R.string.layout_zone_modifier_esc
            ModifierId.META -> R.string.layout_zone_modifier_meta
        },
    )

@Composable
private fun switchLayerPressEffect(layer: ActiveLayer): String =
    stringResource(
        when (layer.id) {
            ActiveLayer.MAIN -> R.string.layout_zone_switch_effect_main
            ActiveLayer.NUMERIC -> R.string.layout_zone_switch_effect_numeric
            ActiveLayer.EMOJI -> R.string.layout_zone_switch_effect_emoji
            ActiveLayer.CLIPBOARD -> R.string.layout_zone_switch_effect_clipboard
            else -> R.string.layout_zone_switch_effect_custom
        },
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneCommandPicker(
    selected: CommandId,
    onSelect: (CommandId) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CommandId.entries
            .filterNot { it.isLayerSwitchCommand() || it.isModifierCommand() }
            .forEach { id ->
            ZonePickCell(
                selected = id == selected,
                onClick = { onSelect(id) },
                legend = commandEditorLegend(id),
                title = commandDisplayTitle(id),
                infoText =
                    if (id == CommandId.TOGGLE_LANDSCAPE_FLOATING) {
                        stringResource(R.string.command_toggle_landscape_floating_info)
                    } else {
                        null
                    },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneModifierPicker(
    selected: ModifierId,
    onSelect: (ModifierId) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModifierId.entries.forEach { id ->
            ZonePickCell(
                selected = id == selected,
                onClick = { onSelect(id) },
                legend =
                    when (id) {
                        ModifierId.SHIFT -> KeyLegend.Icon(Icons.Outlined.KeyboardArrowUp)
                        ModifierId.CTRL -> KeyLegend.Icon(Icons.Outlined.KeyboardControlKey)
                        ModifierId.ALT -> KeyLegend.Icon(Icons.Outlined.KeyboardOptionKey)
                        ModifierId.ESC -> KeyLegend.Text("esc")
                        ModifierId.META -> KeyLegend.Text("Meta")
                    },
                title = modifierDisplayTitle(id),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneLayerPicker(
    namedLayout: NamedLayout,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        namedLayout.availableLayers().forEach { layer ->
            val id = layer.idString()
            ZonePickCell(
                selected = id == selectedId,
                onClick = { onSelect(id) },
                legend = KeyLegend.Icon(activeLayerChipIcon(layer, namedLayout)),
                title = activeLayerChipLabel(layer, namedLayout),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZonePickCell(
    selected: Boolean,
    onClick: () -> Unit,
    legend: KeyLegend,
    title: String,
    infoText: String? = null,
) {
    var showInfo by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val fill =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Box(modifier = Modifier.width(76.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(fill)
                    .border(if (selected) 2.dp else 1.dp, borderColor, shape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KeyLegendMark(
                legend = legend,
                fontSize = 16.sp,
                iconSize = 26.dp,
                color = contentColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (infoText != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.more_info),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .requiredSize(16.dp)
                        .clickable(role = Role.Button) { showInfo = true },
            )
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
