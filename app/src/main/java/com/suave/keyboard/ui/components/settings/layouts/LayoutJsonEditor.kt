package com.suave.keyboard.ui.components.settings.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suave.keyboard.R
import com.suave.keyboard.layout.json.LayoutJsonFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Editable layout JSON with 2-space pretty print, syntax colors, and collapsible
 * object/array nodes when the text parses as JSON.
 */
@Composable
fun LayoutJsonEditor(
    text: String,
    isError: Boolean,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(text) { runCatching { LayoutJsonFormat.parseToJsonElement(text) }.getOrNull() }
    val colors = jsonSyntaxColors()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (parsed != null) {
            val root = parsed
            var collapsed by remember { mutableStateOf(emptySet<String>()) }
            LaunchedEffect(Unit) {
                collapsed = defaultCollapsedPaths(root)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { collapsed = emptySet() }) {
                    Text(stringResource(R.string.layout_json_expand_all))
                }
                TextButton(onClick = { collapsed = allCollapsiblePaths(parsed) }) {
                    Text(stringResource(R.string.layout_json_collapse_all))
                }
            }
            val shape = RoundedCornerShape(12.dp)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 480.dp)
                        .border(
                            width = 1.dp,
                            color =
                                if (isError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            shape = shape,
                        ).background(MaterialTheme.colorScheme.surface, shape)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp),
            ) {
                JsonTreeNode(
                    element = parsed,
                    path = "$",
                    keyLabel = null,
                    depth = 0,
                    collapsed = collapsed,
                    colors = colors,
                    onToggle = { path ->
                        collapsed =
                            if (path in collapsed) {
                                collapsed - path
                            } else {
                                collapsed + path
                            }
                    },
                    onReplaceRoot = { next ->
                        onTextChange(LayoutJsonFormat.encodeToString(next))
                    },
                    replaceAt = { pathSegments, value ->
                        val next = parsed.setAt(pathSegments, value)
                        onTextChange(LayoutJsonFormat.encodeToString(next))
                    },
                    pathSegments = emptyList(),
                )
            }
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 420.dp),
                textStyle =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                isError = isError,
                visualTransformation = remember(colors) { JsonSyntaxVisualTransformation(colors) },
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Ascii,
                        autoCorrectEnabled = false,
                    ),
            )
        }
    }
}

@Composable
private fun jsonSyntaxColors(): JsonSyntaxColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        JsonSyntaxColors(
            key = scheme.primary,
            string = Color(0xFF6AAB73),
            number = Color(0xFF6B9BD1),
            boolean = Color(0xFFC792EA),
            nullValue = scheme.outline,
            punctuation = scheme.onSurfaceVariant,
            index = scheme.tertiary,
        )
    }
}

private data class JsonSyntaxColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val boolean: Color,
    val nullValue: Color,
    val punctuation: Color,
    val index: Color,
)

@Composable
private fun JsonTreeNode(
    element: JsonElement,
    path: String,
    keyLabel: String?,
    depth: Int,
    collapsed: Set<String>,
    colors: JsonSyntaxColors,
    onToggle: (String) -> Unit,
    onReplaceRoot: (JsonElement) -> Unit,
    replaceAt: (List<Any>, JsonElement) -> Unit,
    pathSegments: List<Any>,
) {
    val mono =
        MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    val indent = Modifier.padding(start = (depth * 12).dp)
    when (element) {
        is JsonObject -> {
            val isCollapsed = path != "$" && path in collapsed
            Row(
                modifier = indent.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (path != "$") {
                    Icon(
                        imageVector =
                            if (isCollapsed) {
                                Icons.Outlined.ExpandMore
                            } else {
                                Icons.Outlined.ExpandLess
                            },
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .clickable { onToggle(path) },
                        tint = colors.punctuation,
                    )
                }
                if (keyLabel != null) {
                    Text(text = "\"$keyLabel\"", style = mono, color = colors.key)
                    Text(text = ": ", style = mono, color = colors.punctuation)
                }
                Text(
                    text = if (isCollapsed) "{ ... }" else "{",
                    style = mono,
                    color = colors.punctuation,
                )
            }
            if (!isCollapsed) {
                element.forEach { (childKey, child) ->
                    JsonTreeNode(
                        element = child,
                        path = "$path.$childKey",
                        keyLabel = childKey,
                        depth = depth + 1,
                        collapsed = collapsed,
                        colors = colors,
                        onToggle = onToggle,
                        onReplaceRoot = onReplaceRoot,
                        replaceAt = replaceAt,
                        pathSegments = pathSegments + childKey,
                    )
                }
                Text(
                    text = "}",
                    style = mono,
                    color = colors.punctuation,
                    modifier = indent,
                )
            }
        }
        is JsonArray -> {
            val isCollapsed = path in collapsed
            Row(
                modifier = indent.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector =
                        if (isCollapsed) {
                            Icons.Outlined.ExpandMore
                        } else {
                            Icons.Outlined.ExpandLess
                        },
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clickable { onToggle(path) },
                    tint = colors.punctuation,
                )
                if (keyLabel != null) {
                    Text(text = "\"$keyLabel\"", style = mono, color = colors.key)
                    Text(text = ": ", style = mono, color = colors.punctuation)
                }
                Text(
                    text =
                        if (isCollapsed) {
                            "[ ... ${element.size} ]"
                        } else {
                            "["
                        },
                    style = mono,
                    color = colors.punctuation,
                )
            }
            if (!isCollapsed) {
                element.forEachIndexed { index, child ->
                    JsonTreeNode(
                        element = child,
                        path = "$path[$index]",
                        keyLabel = null,
                        depth = depth + 1,
                        collapsed = collapsed,
                        colors = colors,
                        onToggle = onToggle,
                        onReplaceRoot = onReplaceRoot,
                        replaceAt = replaceAt,
                        pathSegments = pathSegments + index,
                    )
                }
                Text(
                    text = "]",
                    style = mono,
                    color = colors.punctuation,
                    modifier = indent,
                )
            }
        }
        is JsonNull -> {
            JsonPrimitiveRow(
                keyLabel = keyLabel,
                depth = depth,
                mono = mono,
                colors = colors,
                display = "null",
                color = colors.nullValue,
                editable = false,
                editValue = "",
                onCommit = {},
            )
        }
        is JsonPrimitive -> {
            val isString = element.isString
            val display =
                when {
                    isString -> "\"${element.content}\""
                    else -> element.content
                }
            val color =
                when {
                    isString -> colors.string
                    element.booleanOrNull != null -> colors.boolean
                    element.longOrNull != null || element.doubleOrNull != null -> colors.number
                    else -> colors.string
                }
            JsonPrimitiveRow(
                keyLabel = keyLabel,
                depth = depth,
                mono = mono,
                colors = colors,
                display = display,
                color = color,
                editable = true,
                editValue = if (isString) element.content else element.content,
                onCommit = { raw ->
                    val next =
                        when {
                            isString -> JsonPrimitive(raw)
                            raw == "true" || raw == "false" -> JsonPrimitive(raw.toBooleanStrict())
                            raw == "null" -> JsonNull
                            raw.toLongOrNull() != null -> JsonPrimitive(raw.toLong())
                            raw.toDoubleOrNull() != null -> JsonPrimitive(raw.toDouble())
                            else -> JsonPrimitive(raw)
                        }
                    if (pathSegments.isEmpty()) {
                        onReplaceRoot(next)
                    } else {
                        replaceAt(pathSegments, next)
                    }
                },
            )
        }
    }
}

@Composable
private fun JsonPrimitiveRow(
    keyLabel: String?,
    depth: Int,
    mono: TextStyle,
    colors: JsonSyntaxColors,
    display: String,
    color: Color,
    editable: Boolean,
    editValue: String,
    onCommit: (String) -> Unit,
) {
    var editing by remember(display) { mutableStateOf(false) }
    var draft by remember(editValue) { mutableStateOf(editValue) }
    LaunchedEffect(editValue) {
        if (!editing) draft = editValue
    }
    Row(
        modifier =
            Modifier
                .padding(start = (depth * 12).dp)
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (keyLabel != null) {
            Text(text = "\"$keyLabel\"", style = mono, color = colors.key)
            Text(text = ": ", style = mono, color = colors.punctuation)
        }
        if (editable && editing) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = mono.copy(color = color),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier =
                    Modifier
                        .widthIn(min = 48.dp, max = 360.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp),
                        ).padding(horizontal = 4.dp, vertical = 2.dp),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
            )
            TextButton(
                onClick = {
                    onCommit(draft)
                    editing = false
                },
            ) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { editing = false; draft = editValue }) {
                Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text(
                text = display,
                style = mono,
                color = color,
                modifier =
                    if (editable) {
                        Modifier
                            .clickable { editing = true }
                            .padding(vertical = 2.dp)
                    } else {
                        Modifier
                    },
            )
        }
    }
}

private fun defaultCollapsedPaths(root: JsonElement): Set<String> {
    val out = linkedSetOf<String>()
    fun walk(
        element: JsonElement,
        path: String,
        depth: Int,
    ) {
        when (element) {
            is JsonObject -> {
                if (depth >= 1 && path != "$") out.add(path)
                element.forEach { (key, child) ->
                    walk(child, if (path == "$") "$.$key" else "$path.$key", depth + 1)
                }
            }
            is JsonArray -> {
                if (path != "$") out.add(path)
                element.forEachIndexed { index, child ->
                    walk(child, "$path[$index]", depth + 1)
                }
            }
            else -> Unit
        }
    }
    walk(root, "$", 0)
    // Keep the root object open; collapse nested containers by default.
    out.remove("$")
    return out
}

private fun allCollapsiblePaths(root: JsonElement): Set<String> {
    val out = linkedSetOf<String>()
    fun walk(
        element: JsonElement,
        path: String,
    ) {
        when (element) {
            is JsonObject -> {
                if (path != "$") out.add(path)
                element.forEach { (key, child) ->
                    walk(child, if (path == "$") "$.$key" else "$path.$key")
                }
            }
            is JsonArray -> {
                out.add(path)
                element.forEachIndexed { index, child ->
                    walk(child, "$path[$index]")
                }
            }
            else -> Unit
        }
    }
    walk(root, "$")
    return out
}

private fun JsonElement.setAt(
    path: List<Any>,
    value: JsonElement,
): JsonElement {
    if (path.isEmpty()) return value
    val head = path.first()
    val tail = path.drop(1)
    return when (this) {
        is JsonObject -> {
            val key = head as String
            val child = this[key] ?: JsonNull
            JsonObject(toMutableMap().apply { put(key, child.setAt(tail, value)) })
        }
        is JsonArray -> {
            val index = head as Int
            require(index in indices) { "JSON array index out of range: $index" }
            val child = this[index]
            JsonArray(toMutableList().apply { set(index, child.setAt(tail, value)) })
        }
        else -> value
    }
}

private class JsonSyntaxVisualTransformation(
    private val colors: JsonSyntaxColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlightJson(text.text, colors), OffsetMapping.Identity)
}

private fun highlightJson(
    text: String,
    colors: JsonSyntaxColors,
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when (val c = text[i]) {
                '{', '}', '[', ']', ':', ',' -> {
                    withStyle(SpanStyle(color = colors.punctuation)) { append(c) }
                    i++
                }
                '"' -> {
                    val start = i
                    i++
                    while (i < text.length) {
                        when (text[i]) {
                            '\\' -> i = (i + 2).coerceAtMost(text.length)
                            '"' -> {
                                i++
                                break
                            }
                            else -> i++
                        }
                    }
                    val end = i
                    var j = end
                    while (j < text.length && text[j].isWhitespace()) j++
                    val isKey = j < text.length && text[j] == ':'
                    withStyle(SpanStyle(color = if (isKey) colors.key else colors.string)) {
                        append(text.substring(start, end))
                    }
                }
                else -> {
                    if (c.isWhitespace()) {
                        append(c)
                        i++
                    } else {
                        val start = i
                        while (i < text.length &&
                            !text[i].isWhitespace() &&
                            text[i] !in "{}[]:,"
                        ) {
                            i++
                        }
                        val token = text.substring(start, i)
                        val color =
                            when {
                                token == "true" || token == "false" -> colors.boolean
                                token == "null" -> colors.nullValue
                                token.toDoubleOrNull() != null -> colors.number
                                else -> colors.punctuation
                            }
                        withStyle(SpanStyle(color = color)) { append(token) }
                    }
                }
            }
        }
    }
}
