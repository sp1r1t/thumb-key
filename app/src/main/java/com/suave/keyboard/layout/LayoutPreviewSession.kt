package com.suave.keyboard.layout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Layout editor <-> IME bridge. While the editor is open, [layout] holds the live draft and
 * [activeFallback] holds the open-time snapshot of that draft (a working keyboard if the
 * live edit is half-broken).
 *
 * [useEdited] chooses what the IME renders:
 * - true: the live draft
 * - false: the settings-selected layout, except when that selection is the layout under
 *   edit (registry already holds the draft) - then [activeFallback] is used instead
 *
 * Does not change [com.suave.keyboard.db.AppSettings.keyboardLayout].
 */
object LayoutPreviewSession {
    private val _layout = MutableStateFlow<NamedLayout?>(null)
    private val _activeFallback = MutableStateFlow<NamedLayout?>(null)
    private val _useEdited = MutableStateFlow(false)

    val layout: StateFlow<NamedLayout?> = _layout.asStateFlow()
    val activeFallback: StateFlow<NamedLayout?> = _activeFallback.asStateFlow()
    val useEdited: StateFlow<Boolean> = _useEdited.asStateFlow()

    val isActive: Boolean
        get() = _layout.value != null

    val isTryingOut: Boolean
        get() = _layout.value != null && _useEdited.value

    /**
     * Bind the editor draft. [activeFallback] must be the open-time snapshot of [edited]
     * (same id), kept apart so auto-save registering the draft cannot overwrite it.
     */
    fun bind(
        edited: NamedLayout,
        activeFallback: NamedLayout,
        useEdited: Boolean = false,
    ) {
        require(edited.id.isNotBlank()) { "Edited layout id must not be blank" }
        require(activeFallback.id.isNotBlank()) { "Active layout id must not be blank" }
        require(activeFallback.id == edited.id) {
            "activeFallback id (${activeFallback.id}) must match edited id (${edited.id})"
        }
        LayoutRegistry.register(edited)
        _activeFallback.value = activeFallback
        _useEdited.value = useEdited
        _layout.value = edited
    }

    fun start(layout: NamedLayout) {
        bind(
            edited = layout,
            activeFallback = _activeFallback.value?.takeIf { it.id == layout.id } ?: layout,
            useEdited = true,
        )
    }

    /** Push unsaved editor edits into an already-bound session. */
    fun updateIfActive(layout: NamedLayout) {
        val current = _layout.value ?: return
        if (current.id != layout.id) return
        LayoutRegistry.register(layout)
        _layout.value = layout
    }

    fun setUseEdited(enabled: Boolean) {
        if (_layout.value == null) return
        _useEdited.value = enabled
    }

    fun stop() {
        _layout.value = null
        _activeFallback.value = null
        _useEdited.value = false
    }

    /**
     * Layout the IME should render, given the settings-selected layout.
     *
     * When Edited is off, honors live [selected] so layout switch still works. The only
     * exception is when [selected] is the layout under edit: the registry holds the draft,
     * so we serve [activeFallback] instead and never trap typing on a broken mid-edit grid.
     */
    fun resolve(selected: NamedLayout): NamedLayout {
        val edited = _layout.value ?: return selected
        if (_useEdited.value) return edited
        if (selected.id == edited.id) {
            return _activeFallback.value ?: selected
        }
        return selected
    }
}
