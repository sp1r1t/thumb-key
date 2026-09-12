package com.suave.keyboard.engine.modifier

import com.suave.keyboard.IMEService
import com.suave.keyboard.engine.action.SemanticAction
import com.suave.keyboard.engine.intent.ModifierId
import com.suave.keyboard.utils.autoCapitalizeCheck
import com.suave.keyboard.utils.isUriOrEmailOrPasswordField
import com.suave.keyboard.utils.textEndsSentenceForCaps

/**
 * After committing text (or space / spacebar multitap punctuation), put Shift into ONE_SHOT when
 * the editor wants a capital - matching upstream Thumb-Key auto-capitalize, without the English
 * "i" / "i'll" special capitalizers.
 *
 * Leaves [ActivationMode.LOCKED] and [ActivationMode.HELD] alone. Clears a leftover ONE_SHOT when
 * caps mode no longer wants a capital.
 *
 * [committed] is the action just executed: some InputConnections report 0 caps-mode after "? "
 * even though ". " / "! " work, so we also treat a just-committed sentence ending as a capital.
 */
fun applyAutoCapitalize(
    state: ModifierState,
    ime: IMEService,
    enabled: Boolean,
    committed: SemanticAction? = null,
): ModifierState {
    if (!enabled || isUriOrEmailOrPasswordField(ime)) {
        return clearOneShotShift(state)
    }
    val wantsCapital = autoCapitalizeCheck(ime) || committedImpliesSentenceCapital(committed, ime)
    val mode = state.active[ModifierId.SHIFT]?.mode
    return when {
        mode == ActivationMode.LOCKED || mode == ActivationMode.HELD -> state
        wantsCapital -> state.activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT)
        else -> clearOneShotShift(state)
    }
}

/** Initial Shift for a newly focused field when auto-capitalize is on. */
fun initialAutoCapitalizeState(
    ime: IMEService,
    enabled: Boolean,
): ModifierState {
    if (!enabled || isUriOrEmailOrPasswordField(ime)) return ModifierState()
    return if (autoCapitalizeCheck(ime)) {
        ModifierState().activate(ModifierId.SHIFT, ActivationMode.ONE_SHOT)
    } else {
        ModifierState()
    }
}

private fun committedImpliesSentenceCapital(
    committed: SemanticAction?,
    ime: IMEService,
): Boolean {
    // Do not require TYPE_TEXT_FLAG_CAP_SENTENCES: some OEM InputConnections capitalize
    // after ". " / "! " via getCursorCapsMode but return 0 for "? ", and their EditorInfo
    // may not expose the sentences flag either. The outer applyAutoCapitalize already skips
    // URI/email/password and respects the user toggle.
    val text =
        when (committed) {
            is SemanticAction.ReplaceLastText -> committed.text
            is SemanticAction.TypeText -> committed.text
            else -> return false
        }
    return textEndsSentenceForCaps(text)
}

private fun clearOneShotShift(state: ModifierState): ModifierState {
    val mode = state.active[ModifierId.SHIFT]?.mode
    return if (mode == ActivationMode.ONE_SHOT) state.deactivate(ModifierId.SHIFT) else state
}
