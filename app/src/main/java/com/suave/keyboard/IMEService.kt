package com.suave.keyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.ColorDrawable
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
import com.suave.keyboard.db.AppDB
import com.suave.keyboard.db.AppSettings
import com.suave.keyboard.db.DEFAULT_DISABLE_FULLSCREEN_EDITOR
import com.suave.keyboard.db.DEFAULT_INLINE_SUGGESTIONS
import com.suave.keyboard.db.DEFAULT_INLINE_SUGGESTION_HEIGHT
import com.suave.keyboard.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.suave.keyboard.db.DEFAULT_SHOW_TOAST_ON_COPY
import com.suave.keyboard.db.DEFAULT_SHOW_TOAST_ON_CUT
import com.suave.keyboard.db.DEFAULT_USE_PRIVATE_CLIPBOARD
import com.suave.keyboard.db.isCredentialStorageUnlocked
import com.suave.keyboard.engine.output.LiveClipboardImage
import com.suave.keyboard.ime.InlineAutofillHost
import com.suave.keyboard.ime.createInlineSuggestionsRequest
import com.suave.keyboard.ime.inlineChipSlotHeightDp
import com.suave.keyboard.utils.TAG
import com.suave.keyboard.utils.SuaveClipboardManager
import com.suave.keyboard.utils.toBool
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private fun setupView(): ComposeKeyboardView {
        val app = application as SuaveApplication
        val settingsRepo = app.appSettingsRepository

        val view = ComposeKeyboardView(this, settingsRepo)
        suppressImeAutofill(view)
        window?.window?.let { win ->
            // Transparent so floating landscape can show the host through gaps; Compose paints
            // the board / backdrop where keys live.
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.decorView.let { decorView ->
                suppressImeAutofill(decorView)
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }
        }
        view.let {
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }

    /**
     * Layout option [com.suave.keyboard.layout.NamedLayout.landscapeFloating] in landscape:
     * report insets as if the IME covers nothing (apps keep drawing underneath), and limit
     * touches to the key blocks so the Split/Dual gap can pass through.
     */
    @Volatile
    private var landscapeFloating: Boolean = false
    private val floatingTouchableRegion = Region()

    fun setLandscapeFloating(enabled: Boolean) {
        if (landscapeFloating == enabled) return
        landscapeFloating = enabled
        if (!enabled) {
            floatingTouchableRegion.setEmpty()
        }
        requestInsetsUpdate()
    }

    /** [rects] are in the input [View] coordinate space (Compose root). */
    fun setFloatingTouchableRects(
        inputView: View,
        rects: List<Rect>,
    ) {
        floatingTouchableRegion.setEmpty()
        val origin = IntArray(2)
        inputView.getLocationInWindow(origin)
        for (rect in rects) {
            if (rect.isEmpty) continue
            floatingTouchableRegion.op(
                Rect(
                    rect.left + origin[0],
                    rect.top + origin[1],
                    rect.right + origin[0],
                    rect.bottom + origin[1],
                ),
                Region.Op.UNION,
            )
        }
        requestInsetsUpdate()
    }

    private fun requestInsetsUpdate() {
        // onComputeInsets is driven by the decor ViewTreeObserver; force a pass.
        window?.window?.decorView?.let { decor ->
            decor.requestLayout()
            decor.invalidate()
        }
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        if (!landscapeFloating) return
        val decor = window?.window?.decorView ?: return
        // Lie that the IME covers nothing so hosts keep painting full-screen underneath
        // (AOSP ThemedNavBarKeyboard floating mode).
        val height = decor.height
        if (height <= 0) return
        outInsets.contentTopInsets = height
        outInsets.visibleTopInsets = height
        if (floatingTouchableRegion.isEmpty) {
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME
        } else {
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(floatingTouchableRegion)
        }
    }

    private var clipboardManager: SuaveClipboardManager? = null
    private val noLiveClipboardImage = MutableStateFlow<LiveClipboardImage?>(null)
    private var unlockReceiver: BroadcastReceiver? = null
    val inlineAutofill = InlineAutofillHost()
    val inputEpoch = MutableStateFlow(0)
    val notice = MutableStateFlow<ImeNotice?>(null)
    private val noticeSeq = AtomicInteger(0)

    fun showNotice(
        text: String,
        detail: String? = null,
    ) {
        notice.value = ImeNotice(text = text, seq = noticeSeq.incrementAndGet(), detail = detail)
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
        activeInstance = WeakReference(this)
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
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        unregisterUnlockReceiver()
        clipboardManager?.stopListening()
        clipboardManager = null
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun startClipboard() {
        if (clipboardManager != null) return
        val app = application as SuaveApplication
        clipboardManager = SuaveClipboardManager(this, app.clipboardRepository)
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
            if (ignoreCursorMoveCount > 0) {
                ignoreCursorMoveCount--
                false
            } else {
                Log.d(TAG, "cursor moved")
                cursorAnchorInfo.selectionStart != selectionStart ||
                    cursorAnchorInfo.selectionEnd != selectionEnd
            }

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
        val stripHeightDp = settings?.inlineSuggestionHeight ?: DEFAULT_INLINE_SUGGESTION_HEIGHT
        val heightPx =
            TypedValue
                .applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    inlineChipSlotHeightDp(stripHeightDp).toFloat(),
                    resources.displayMetrics,
                ).toInt()
                .coerceAtLeast(1)
        inlineAutofill.markWaiting(heightPx)
        return createInlineSuggestionsRequest(this, heightPx)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        val settings = appSettingsOrSync()
        if (!(settings?.inlineSuggestions ?: DEFAULT_INLINE_SUGGESTIONS).toBool()) {
            inlineAutofill.clear()
            return false
        }
        inlineAutofill.show(this, response.inlineSuggestions)
        return true
    }

    fun acceptTopInlineSuggestion(): Boolean = inlineAutofill.acceptTop()

    override fun onEvaluateInputViewShown(): Boolean {
        val settingsRepo = (application as SuaveApplication).appSettingsRepository
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
        val settingsRepo = (application as SuaveApplication).appSettingsRepository
        val settings = settingsRepo.appSettings.getValue()
        if ((settings?.disableFullscreenEditor ?: DEFAULT_DISABLE_FULLSCREEN_EDITOR).toBool()) {
            ei.imeOptions =
                ei.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }
        super.onUpdateExtractingVisibility(ei)
    }

    fun didCursorMove(): Boolean = cursorMoved

    /**
     * Ignore the next [count] [onUpdateCursorAnchorInfo] reports so self-inflicted edits
     * (commit, delete+commit for space multitap) do not look like the user moved the cursor.
     */
    fun ignoreNextCursorMove(count: Int = 1) {
        ignoreCursorMoveCount += count.coerceAtLeast(1)
    }

    override fun onWindowHidden() {
        inlineAutofill.clear()
        super.onWindowHidden()
    }

    private var ignoreCursorMoveCount: Int = 0
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
        val settings = appSettingsOrSync()
        return (settings?.usePrivateClipboard ?: DEFAULT_USE_PRIVATE_CLIPBOARD).toBool()
    }

    fun showToastOnCopy(): Boolean {
        val settings = appSettingsOrSync()
        return (settings?.showToastOnCopy ?: DEFAULT_SHOW_TOAST_ON_COPY).toBool()
    }

    fun showToastOnCut(): Boolean {
        val settings = appSettingsOrSync()
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

    private fun appSettingsOrSync(): AppSettings? {
        val repo = (application as SuaveApplication).appSettingsRepository
        return repo.getSettingsSync()
    }

    private fun suppressImeAutofill(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
    }

    companion object {
        @Volatile
        private var activeInstance: WeakReference<IMEService>? = null

        /**
         * Shows a notice on the live keyboard, if Suave's IME is currently running.
         * @return true if a notice was shown
         */
        fun showNoticeOnActiveIme(
            text: String,
            detail: String? = null,
        ): Boolean {
            val ime = activeInstance?.get() ?: return false
            ime.showNotice(text, detail)
            return true
        }
    }
}

data class ImeNotice(
    val text: String,
    val seq: Int,
    /** Optional small secondary label drawn beside [text] (e.g. "private" on a private copy). */
    val detail: String? = null,
)
