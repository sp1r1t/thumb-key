package com.suave.s12.textprocessors

import android.view.KeyEvent
import com.suave.s12.IMEService

interface TextProcessor {
    // intercepts text to be committed
    fun handleCommitText(
        ime: IMEService,
        input: CharSequence,
    )

    // intercepts non-text key events (e.g. ENTER, DEL, DPAD)
    fun handleKeyEvent(
        ime: IMEService,
        ev: KeyEvent,
    )

    // notifies before input ends (e.g. keyboard hidden) or other IME action (eg. copy, paste, emoji mode toggle)
    fun handleFinishInput(ime: IMEService)

    // notifies on selection update
    fun handleCursorUpdate(
        ime: IMEService,
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
    )

    // manually update position after switching layouts
    fun updateCursorPosition(ime: IMEService)
}
