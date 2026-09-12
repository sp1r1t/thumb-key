package com.suave.s12.engine.capability

import android.os.Build
import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * Orthogonal [EditorInfo] dump for the IME debug bar. Does not invent a richer capability
 * level: Firefox web fields, Termux, and password boxes all stay whatever
 * [EditorCapabilityResolver] already classified them as.
 *
 * [describe] is the short chip (class, variation, real multiline, content mime). [verbose]
 * adds default-y IME flags and hex values so they can be copied without covering the board.
 */
object EditorInfoDebug {
    private const val SEP = " . "

    fun describe(editorInfo: EditorInfo?): String = label(editorInfo).compact

    fun verbose(editorInfo: EditorInfo?): String = label(editorInfo).verbose

    fun label(editorInfo: EditorInfo?): EditorInfoDebugLabel {
        if (editorInfo == null) return EditorInfoDebugLabel("NO_EDITOR", "NO_EDITOR")
        val mime =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                editorInfo.contentMimeTypes
            } else {
                null
            }
        return label(editorInfo.inputType, editorInfo.imeOptions, mime)
    }

    fun describe(
        inputType: Int,
        imeOptions: Int = 0,
        contentMimeTypes: Array<out String>? = null,
    ): String = label(inputType, imeOptions, contentMimeTypes).compact

    fun verbose(
        inputType: Int,
        imeOptions: Int = 0,
        contentMimeTypes: Array<out String>? = null,
    ): String = label(inputType, imeOptions, contentMimeTypes).verbose

    fun label(
        inputType: Int,
        imeOptions: Int = 0,
        contentMimeTypes: Array<out String>? = null,
    ): EditorInfoDebugLabel {
        val compact = tokens(inputType, imeOptions, contentMimeTypes, verbose = false).joinToString(SEP)
        val verboseTokens = tokens(inputType, imeOptions, contentMimeTypes, verbose = true)
        val verbose =
            verboseTokens.joinToString(SEP) +
                " | inputType=0x${inputType.toString(16)} imeOptions=0x${imeOptions.toString(16)}"
        return EditorInfoDebugLabel(compact = compact, verbose = verbose)
    }

    private fun tokens(
        inputType: Int,
        imeOptions: Int,
        contentMimeTypes: Array<out String>?,
        verbose: Boolean,
    ): List<String> {
        val tokens = mutableListOf<String>()
        val klass = inputType and InputType.TYPE_MASK_CLASS
        if (klass == InputType.TYPE_NULL) {
            tokens += "RAW"
            tokens += "TYPE_NULL"
            appendContent(tokens, contentMimeTypes)
            return tokens
        }

        tokens += "EDITABLE"
        when (klass) {
            InputType.TYPE_CLASS_TEXT -> {
                tokens += "TEXT"
                appendTextVariation(tokens, inputType)
                appendTextFlags(tokens, inputType, verbose)
            }

            InputType.TYPE_CLASS_NUMBER -> {
                tokens += "NUMBER"
                appendNumberVariation(tokens, inputType)
                appendNumberFlags(tokens, inputType)
            }

            InputType.TYPE_CLASS_PHONE -> {
                tokens += "PHONE"
            }

            InputType.TYPE_CLASS_DATETIME -> {
                tokens += "DATETIME"
                appendDatetimeVariation(tokens, inputType)
            }

            else -> {
                tokens += "CLASS:0x${klass.toString(16)}"
            }
        }
        if (verbose) appendImeOptions(tokens, imeOptions)
        appendContent(tokens, contentMimeTypes)
        return tokens
    }

    private fun appendTextVariation(
        tokens: MutableList<String>,
        inputType: Int,
    ) {
        when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_URI -> tokens += "URI"
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> tokens += "EMAIL"
            InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> tokens += "EMAIL_SUBJECT"
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> tokens += "SHORT_MSG"
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> tokens += "LONG_MSG"
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> tokens += "NAME"
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> tokens += "POSTAL"
            InputType.TYPE_TEXT_VARIATION_PASSWORD -> tokens += "PASSWORD"
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> tokens += "VISIBLE_PASSWORD"
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> tokens += "WEB_EDIT"
            InputType.TYPE_TEXT_VARIATION_FILTER -> tokens += "FILTER"
            InputType.TYPE_TEXT_VARIATION_PHONETIC -> tokens += "PHONETIC"
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> tokens += "WEB_EMAIL"
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> tokens += "WEB_PASSWORD"
        }
    }

    private fun appendTextFlags(
        tokens: MutableList<String>,
        inputType: Int,
        verbose: Boolean,
    ) {
        if (verbose) {
            if (inputType has InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) tokens += "CAP_CHARS"
            if (inputType has InputType.TYPE_TEXT_FLAG_CAP_WORDS) tokens += "CAP_WORDS"
            if (inputType has InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) tokens += "CAP_SENTENCES"
            if (inputType has InputType.TYPE_TEXT_FLAG_AUTO_CORRECT) tokens += "AUTO_CORRECT"
            if (inputType has InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) tokens += "AUTO_COMPLETE"
        }
        // IME_MULTI_LINE only means the IME may use a multiline editor. Firefox sets it on
        // single-line web inputs, so the chip only shows MULTILINE for the real text flag.
        if (inputType has InputType.TYPE_TEXT_FLAG_MULTI_LINE) tokens += "MULTILINE"
        if (verbose && inputType has InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) tokens += "IME_MULTILINE"
        if (verbose && inputType has InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) tokens += "NO_SUGGESTIONS"
    }

    private fun appendNumberVariation(
        tokens: MutableList<String>,
        inputType: Int,
    ) {
        if (inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
            tokens += "PASSWORD"
        }
    }

    private fun appendNumberFlags(
        tokens: MutableList<String>,
        inputType: Int,
    ) {
        if (inputType has InputType.TYPE_NUMBER_FLAG_SIGNED) tokens += "SIGNED"
        if (inputType has InputType.TYPE_NUMBER_FLAG_DECIMAL) tokens += "DECIMAL"
    }

    private fun appendDatetimeVariation(
        tokens: MutableList<String>,
        inputType: Int,
    ) {
        when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_DATETIME_VARIATION_DATE -> tokens += "DATE"
            InputType.TYPE_DATETIME_VARIATION_TIME -> tokens += "TIME"
        }
    }

    private fun appendImeOptions(
        tokens: MutableList<String>,
        imeOptions: Int,
    ) {
        if (imeOptions has EditorInfo.IME_FLAG_NO_EXTRACT_UI) tokens += "NO_EXTRACT"
        if (imeOptions has EditorInfo.IME_FLAG_NO_FULLSCREEN) tokens += "NO_FULLSCREEN"
        when (imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> tokens += "ACTION:GO"
            EditorInfo.IME_ACTION_SEARCH -> tokens += "ACTION:SEARCH"
            EditorInfo.IME_ACTION_SEND -> tokens += "ACTION:SEND"
            EditorInfo.IME_ACTION_NEXT -> tokens += "ACTION:NEXT"
            EditorInfo.IME_ACTION_DONE -> tokens += "ACTION:DONE"
            EditorInfo.IME_ACTION_PREVIOUS -> tokens += "ACTION:PREV"
        }
    }

    private fun appendContent(
        tokens: MutableList<String>,
        contentMimeTypes: Array<out String>?,
    ) {
        if (contentMimeTypes.isNullOrEmpty()) return
        tokens += "CONTENT:${contentMimeTypes.joinToString(",")}"
    }

    private infix fun Int.has(flag: Int): Boolean = this and flag == flag
}

data class EditorInfoDebugLabel(
    val compact: String,
    val verbose: String,
)
