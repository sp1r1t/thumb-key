package com.suave.keyboard.layout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Layout editor <-> IME bridge. While the editor is open, [layout] holds the live draft and
 * [activeFallback] holds the keyboard to use for normal typing (selected layout, or the
 * editor baseline when that selected layout is the one being edited).
 *
 * [useEdited] chooses which of those the IME renders. Try-on turns it on; the settings bar
 * toggle can flip it so you can still type on a working layout mid-edit.
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
     * Bind the editor draft. [activeFallback] is what the IME shows when [useEdited] is false
     * (kept as a snapshot so auto-save registering the draft cannot overwrite it).
     */
    fun bind(
        edited: NamedLayout,
        activeFallback: NamedLayout,
        useEdited: Boolean = false,
    ) {
        require(edited.id.isNotBlank()) { "Edited layout id must not be blank" }
        require(activeFallback.id.isNotBlank()) { "Active layout id must not be blank" }
        LayoutRegistry.register(edited)
        _activeFallback.value = activeFallback
        _useEdited.value = useEdited
        _layout.value = edited
    }

    fun start(layout: NamedLayout) {
        bind(
            edited = layout,
            activeFallback = _activeFallback.value ?: layout,
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

    /** Layout the IME should render, given the settings-selected layout as last resort. */
    fun resolve(selected: NamedLayout): NamedLayout {
        val edited = _layout.value ?: return selected
        return if (_useEdited.value) {
            edited
        } else {
            _activeFallback.value ?: selected
        }
    }
}
