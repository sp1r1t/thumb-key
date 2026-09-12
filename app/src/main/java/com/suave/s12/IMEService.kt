package com.suave.s12

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.suave.s12.db.AppDB
import com.suave.s12.db.AppSettings
import com.suave.s12.db.DEFAULT_CLIPBOARD_HISTORY_ENABLED
import com.suave.s12.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.suave.s12.db.DEFAULT_INLINE_SUGGESTIONS
import com.suave.s12.db.DEFAULT_INLINE_SUGGESTION_HEIGHT
import com.suave.s12.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_COPY
import com.suave.s12.db.DEFAULT_SHOW_TOAST_ON_CUT
import com.suave.s12.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.suave.s12.db.isCredentialStorageUnlocked
import com.suave.s12.engine.output.LiveClipboardImage
import com.suave.s12.ime.InlineAutofillHost
import com.suave.s12.ime.createInlineSuggestionsRequest
import com.suave.s12.utils.KeyboardDefinition
import com.suave.s12.utils.KeyboardLayout
import com.suave.s12.utils.TAG
import com.suave.s12.utils.ThumbKeyClipboardManager
import com.suave.s12.utils.toBool
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private fun setupView(): ComposeKeyboardView {
        val app = application as ThumbkeyApplication
        val settingsRepo = app.appSettingsRepository

        val layoutIndex = settingsRepo.appSettings.value?.keyboardLayout
        if (layoutIndex != null) {
            currentKeyboardDefinition = KeyboardLayout.entries[layoutIndex].keyboardDefinition
        }

        val view = ComposeKeyboardView(this, settingsRepo)
        suppressImeAutofill(view)
        window?.window?.decorView?.let { decorView ->
            suppressImeAutofill(decorView)
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        view.let {
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }

    var currentKeyboardDefinition: KeyboardDefinition? = null
    private var clipboardManager: ThumbKeyClipboardManager? = null
    private val noLiveClipboardImage = MutableStateFlow<LiveClipboardImage?>(null)
    private var unlockReceiver: BroadcastReceiver? = null
    val inlineAutofill = InlineAutofillHost()
    val inputEpoch = MutableStateFlow(0)
    val notice = MutableStateFlow<ImeNotice?>(null)
    private val noticeSeq = AtomicInteger(0)

    fun showNotice(text: String) {
        notice.value = ImeNotice(text = text, seq = noticeSeq.incrementAndGet())
    }

    fun clearNotice(seq: Int) {
        if (notice.value?.seq == seq) {
            notice.value = null
        }
    }

    /**
     * Keep one input view for the IME session. Replacing it on every [onStartInput] tears down
     * inflated Autofill chips (Firefox and Chrome restart input when a login field focuses).
     * Numeric/editor changes still apply because Compose keys off [inputEpoch].
     */
    override fun onCreateInputView(): View = setupView()

    override fun onStartInput(
        attribute: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInput(attribute, restarting)
        refreshCurrentKeyboardDefinition()
        bumpInputEpoch()
    }

    override fun onFinishInput() {
        inlineAutofill.clear()
        super.onFinishInput()
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        clipboardIngestPrimary()
        bumpInputEpoch()
    }

    // Lifecycle Methods
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    private fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)

    override val lifecycle = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        if (isCredentialStorageUnlocked(this)) {
            startClipboard()
        } else {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        onCredentialStorageUnlocked()
                    }
                }
            ContextCompat.registerReceiver(
                this,
                receiver,
                IntentFilter(Intent.ACTION_USER_UNLOCKED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            unlockReceiver = receiver
        }
    }

    override fun onDestroy() {
        unregisterUnlockReceiver()
        clipboardManager?.stopListening()
        clipboardManager = null
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun startClipboard() {
        if (clipboardManager != null) return
        val app = application as ThumbkeyApplication
        clipboardManager = ThumbKeyClipboardManager(this, app.clipboardRepository)
        clipboardManager?.startListening()
        clipboardManager?.clearExpired()
    }

    private fun onCredentialStorageUnlocked() {
        // Move settings out of credential storage if this is the first unlock after
        // the Direct Boot change, then rebuild the keyboard so it picks up the user's layout.
        AppDB.getDatabase(this)
        startClipboard()
        setInputView(setupView())
        bumpInputEpoch()
        unregisterUnlockReceiver()
    }

    private fun unregisterUnlockReceiver() {
        unlockReceiver?.let {
            unregisterReceiver(it)
            unlockReceiver = null
        }
    }

    // Cursor update Methods
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)

        cursorMoved =
            if (ignoreCursorMove) {
                ignoreCursorMove = false
                false
            } else {
                Log.d(TAG, "cursor moved")
                cursorAnchorInfo.selectionStart != selectionStart ||
                    cursorAnchorInfo.selectionEnd != selectionEnd
            }

        currentKeyboardDefinition?.settings?.textProcessor?.handleCursorUpdate(
            this,
            selectionStart,
            selectionEnd,
            cursorAnchorInfo.selectionStart,
            cursorAnchorInfo.selectionEnd,
        )

        selectionStart = cursorAnchorInfo.selectionStart
        selectionEnd = cursorAnchorInfo.selectionEnd
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (finishingInput) {
            inlineAutofill.clear()
        }
        super.onFinishInputView(finishingInput)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        val settings = appSettingsOrSync()
        if (!(settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()) {
            return null
        }
        val heightDp = settings?.inlineSuggestionHeight ?: DEFAULT_INLINE_SUGGESTION_HEIGHT
        val heightPx =
            TypedValue
                .applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    heightDp.toFloat(),
                    resources.displayMetrics,
                ).toInt()
                .coerceAtLeast(1)
        inlineAutofill.markWaiting(heightPx)
        return createInlineSuggestionsRequest(this, heightPx, uiExtras)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        val settings = appSettingsOrSync()
        if (!(settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()) {
            inlineAutofill.clear()
            return false
        }
        val items = response.inlineSuggestions
        val pinned = items.count { it.info.isPinned }
        Log.d(TAG, "inline suggestions response count=${items.size} pinned=$pinned")
        inlineAutofill.show(this, items)
        return items.isNotEmpty()
    }

    fun acceptTopInlineSuggestion(): Boolean = inlineAutofill.acceptTop()

    override fun onEvaluateInputViewShown(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        val showOnScreenKeyboard =
            (settings?.showOnScreenKeyboard ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD).toBool()

        // Always call super implementation because Android docs says:
        // "If you override this method you must call through to the superclass implementation."
        // https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onEvaluateInputViewShown()
        return super.onEvaluateInputViewShown() ||
            showOnScreenKeyboard
    }

    // Disable the fullscreen text editor if set by the user
    override fun onUpdateExtractingVisibility(ei: EditorInfo) {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        if ((settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()) {
            ei.imeOptions =
                ei.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }
        super.onUpdateExtractingVisibility(ei)
    }

    fun didCursorMove(): Boolean = cursorMoved

    fun ignoreNextCursorMove() {
        // This gets reset on the next call to `onUpdateCursorAnchorInfo`
        ignoreCursorMove = true
    }

    override fun onWindowHidden() {
        inlineAutofill.clear()
        currentKeyboardDefinition?.settings?.textProcessor?.handleFinishInput(this)
        super.onWindowHidden()
    }

    private var ignoreCursorMove: Boolean = false
    private var cursorMoved: Boolean = false
    private var selectionStart: Int = 0
    private var selectionEnd: Int = 0

    // ViewModelStore Methods
    override val viewModelStore = ViewModelStore()

    // SaveStateRegistry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    fun clipboardUsePrivate(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        val clipboardHistoryEnabled = (settings?.clipboardHistoryEnabled ?: DEFAULT_CLIPBOARD_HISTORY_ENABLED).toBool()
        val usePrivateClipboard = (settings?.usePrivateClipboard ?: DEFAULT_USE_PRIVATE_CLIPBOARD).toBool()
        return clipboardHistoryEnabled && usePrivateClipboard
    }

    fun showToastOnCopy(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        return (settings?.showToastOnCopy ?: DEFAULT_SHOW_TOAST_ON_COPY).toBool()
    }

    fun showToastOnCut(): Boolean {
        val settingsRepo = (application as ThumbkeyApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        return (settings?.showToastOnCut ?: DEFAULT_SHOW_TOAST_ON_CUT).toBool()
    }

    fun clipboardAddPrivateClip(text: String): Unit? = clipboardManager?.addPrivateClip(text)

    fun clipboardWasLastCopyDoneViaSystem(): Boolean = clipboardManager?.wasLastCopyOperationDoneViaSystem() ?: true

    fun clipboardGetLastClip(): String? = clipboardManager?.getLastClip()

    fun clipboardIngestPrimary() {
        clipboardManager?.ingestPrimaryClip()
    }

    fun clipboardLiveImage(): StateFlow<LiveClipboardImage?> =
        clipboardManager?.liveImage ?: noLiveClipboardImage

    private fun bumpInputEpoch() {
        inputEpoch.value = inputEpoch.value + 1
    }

    private fun refreshCurrentKeyboardDefinition() {
        val layoutIndex = appSettingsOrSync()?.keyboardLayout
        if (layoutIndex != null && layoutIndex in KeyboardLayout.entries.indices) {
            currentKeyboardDefinition = KeyboardLayout.entries[layoutIndex].keyboardDefinition
        }
    }

    private fun appSettingsOrSync(): AppSettings? {
        val repo = (application as ThumbkeyApplication).appSettingsRepository
        return repo.getSettingsSync()
    }

    private fun suppressImeAutofill(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
    }
}

data class ImeNotice(
    val text: String,
    val seq: Int,
)
