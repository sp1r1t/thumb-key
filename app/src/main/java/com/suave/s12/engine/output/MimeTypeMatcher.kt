package com.suave.s12.engine.output

/**
 * MIME matching for IME commitContent: editor contentMimeTypes may be wildcards
 * (image star) while the clipboard offers a concrete type (image/png).
 */
object MimeTypeMatcher {
    fun editorAccepts(
        accepted: Array<out String>?,
        offered: String,
    ): Boolean {
        if (accepted.isNullOrEmpty()) return false
        val offeredParts = parse(offered) ?: return false
        return accepted.any { acceptedType ->
            val acceptedParts = parse(acceptedType) ?: return@any false
            typeMatches(acceptedParts, offeredParts)
        }
    }

    fun firstMatching(
        accepted: Array<out String>?,
        offered: Iterable<String>,
    ): String? = offered.firstOrNull { editorAccepts(accepted, it) }

    fun isImage(mime: String): Boolean = editorAccepts(arrayOf("image/*"), mime)

    fun isWildcard(mime: String): Boolean {
        val parts = parse(mime) ?: return false
        return parts.type == "*" || parts.subtype == "*"
    }

    /**
     * Pick the MIME string to put on InputContentInfo. Concrete clipboard types must match
     * the editor; a clipboard image wildcard may use the editor's first accepted image type.
     * A concrete mismatch (image/png vs image/jpeg) is not remapped.
     */
    fun chooseOfferedMime(
        accepted: Array<out String>?,
        offered: Iterable<String>,
    ): String? {
        val offeredList = offered.toList()
        val matched = firstMatching(accepted, offeredList)
        if (matched != null) return concreteType(matched)
        if (offeredList.none { isWildcard(it) }) return null
        val acceptedImage = accepted?.firstOrNull { isImage(it) } ?: return null
        return concreteType(acceptedImage)
    }

    /**
     * InputContentInfo wants a concrete type. An image wildcard becomes image/png so the
     * editor receives a real MIME string rather than a pattern.
     */
    fun concreteType(mime: String): String {
        val parts = parse(mime) ?: return mime
        val type = parts.type.lowercase()
        val subtype = parts.subtype.lowercase()
        if (subtype == "*") {
            return if (type == "image") "image/png" else mime
        }
        return "$type/$subtype"
    }

    private fun typeMatches(
        accepted: MimeParts,
        offered: MimeParts,
    ): Boolean {
        val typeOk = accepted.type == "*" || accepted.type.equals(offered.type, ignoreCase = true)
        val subtypeOk =
            accepted.subtype == "*" || accepted.subtype.equals(offered.subtype, ignoreCase = true)
        return typeOk && subtypeOk
    }

    private fun parse(raw: String): MimeParts? {
        val cleaned = raw.substringBefore(';').trim()
        val slash = cleaned.indexOf('/')
        if (slash <= 0 || slash == cleaned.lastIndex) return null
        val type = cleaned.substring(0, slash).trim()
        val subtype = cleaned.substring(slash + 1).trim()
        if (type.isEmpty() || subtype.isEmpty()) return null
        return MimeParts(type, subtype)
    }

    private data class MimeParts(
        val type: String,
        val subtype: String,
    )
}
