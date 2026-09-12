package com.suave.s12.ui.components.clipboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.suave.s12.R
import com.suave.s12.db.ClipboardItem
import com.suave.s12.engine.feedback.HapticType
import com.suave.s12.engine.output.LiveClipboardImage
import com.suave.s12.ui.engine.playHaptic
import com.suave.s12.utils.ClipboardImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

val spacing = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(
    clipboardItems: List<ClipboardItem>,
    isEnabled: Boolean,
    onItemClick: (ClipboardItem) -> Unit,
    onItemPaste: (ClipboardItem) -> Unit,
    onItemDelete: (ClipboardItem) -> Unit,
    onItemTogglePin: (ClipboardItem) -> Unit,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onGoToClipboardSettings: () -> Unit,
    keyHeight: Float,
    keyPadding: Int,
    cornerRadius: Float,
    vibrateOnTap: Boolean,
    tapHapticType: HapticType = HapticType.KEYBOARD_TAP,
    liveImage: LiveClipboardImage? = null,
    onLiveImageClick: () -> Unit = {},
    onLiveImagePaste: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val headerHeight = (keyHeight * 0.6f).dp
    val backdropColor = MaterialTheme.colorScheme.surfaceContainerLow
    val currentClip = liveImage
    val showLive =
        currentClip != null && clipboardItems.none { it.sourceKey == currentClip.sourceKey }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(backdropColor),
    ) {
        ClipboardHeader(
            onBack = onBack,
            onClearAll = onClearAll,
            showClearAll = isEnabled,
            height = headerHeight,
            keyPadding = keyPadding,
            cornerRadius = cornerRadius,
        )

        val historyEmpty = clipboardItems.isEmpty()
        if (!isEnabled && !showLive) {
            ClipboardDisabledView(
                onGoToClipboardSettings = onGoToClipboardSettings,
                cornerRadius = cornerRadius,
            )
        } else if (isEnabled && historyEmpty && !showLive) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(spacing),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.clipboard_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val pinnedItems = clipboardItems.filter { it.isPinned }
            val unpinnedItems = clipboardItems.filter { !it.isPinned }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = keyPadding.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (currentClip != null && showLive) {
                    item(key = "live-${currentClip.sourceKey}") {
                        LiveClipboardImageRow(
                            image = currentClip,
                            onClick = onLiveImageClick,
                            onPaste = onLiveImagePaste,
                            cornerRadius = cornerRadius,
                            vibrateOnTap = vibrateOnTap,
                            tapHapticType = tapHapticType,
                        )
                    }
                }
                if (isEnabled) {
                    if (pinnedItems.isNotEmpty()) {
                        items(pinnedItems, key = { "p-${it.id}" }) { item ->
                            ClipboardItemRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onPaste = { onItemPaste(item) },
                                onDelete = { onItemDelete(item) },
                                onTogglePin = { onItemTogglePin(item) },
                                cornerRadius = cornerRadius,
                                vibrateOnTap = vibrateOnTap,
                                tapHapticType = tapHapticType,
                            )
                        }
                    }
                    if (unpinnedItems.isNotEmpty()) {
                        items(unpinnedItems, key = { "u-${it.id}" }) { item ->
                            ClipboardItemRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onPaste = { onItemPaste(item) },
                                onDelete = { onItemDelete(item) },
                                onTogglePin = { onItemTogglePin(item) },
                                cornerRadius = cornerRadius,
                                vibrateOnTap = vibrateOnTap,
                                tapHapticType = tapHapticType,
                            )
                        }
                    }
                } else {
                    item(key = "history-disabled") {
                        ClipboardDisabledView(
                            onGoToClipboardSettings = onGoToClipboardSettings,
                            cornerRadius = cornerRadius,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipboardDisabledView(
    onGoToClipboardSettings: () -> Unit,
    cornerRadius: Float,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(spacing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Text(
                text = stringResource(R.string.clipboard_disabled),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onGoToClipboardSettings,
                shape = RoundedCornerShape(cornerRadius.dp),
            ) {
                Text(stringResource(R.string.clipboard_go_to_settings))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipboardHeader(
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    showClearAll: Boolean,
    height: Dp,
    keyPadding: Int,
    cornerRadius: Float,
) {
    val backTooltipState = rememberTooltipState()
    val clearTooltipState = rememberTooltipState()
    val backRightTooltipState = rememberTooltipState()

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(keyPadding.dp)
                .clip(RoundedCornerShape(cornerRadius.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.clipboard_back))
                    }
                },
                state = backTooltipState,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.clipboard_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = stringResource(R.string.clipboard_history),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row {
                if (showClearAll) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(R.string.clipboard_clear_all))
                            }
                        },
                        state = clearTooltipState,
                    ) {
                        IconButton(onClick = onClearAll) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.clipboard_clear_all),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.clipboard_back))
                        }
                    },
                    state = backRightTooltipState,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.clipboard_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    android.text.format.DateUtils
        .getRelativeTimeSpanString(
            timestamp,
            System.currentTimeMillis(),
            android.text.format.DateUtils.SECOND_IN_MILLIS,
            android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()

@Composable
private fun LiveClipboardImageRow(
    image: LiveClipboardImage,
    onClick: () -> Unit,
    onPaste: () -> Unit,
    cornerRadius: Float,
    vibrateOnTap: Boolean,
    tapHapticType: HapticType,
) {
    ClipboardEntryRow(
        onClick = onClick,
        onPaste = onPaste,
        onDelete = null,
        onTogglePin = null,
        isPinned = false,
        cornerRadius = cornerRadius,
        vibrateOnTap = vibrateOnTap,
        tapHapticType = tapHapticType,
        thumbUri = image.uri,
        thumbFile = null,
        label = stringResource(R.string.clipboard_image),
        meta = stringResource(R.string.clipboard_current),
        pinLabel = "",
    )
}

@Composable
private fun ClipboardItemRow(
    item: ClipboardItem,
    onClick: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    cornerRadius: Float,
    vibrateOnTap: Boolean,
    tapHapticType: HapticType,
) {
    val context = LocalContext.current
    val imageFile =
        item.localPath
            ?.takeIf { item.isImage() }
            ?.let { ClipboardImageStore.fileFor(context, it) }
    val label =
        if (item.isImage()) {
            stringResource(R.string.clipboard_image)
        } else {
            item.text.replace("\n", " ").replace("\r", "")
        }
    val meta =
        if (item.isImage()) {
            formatTimestamp(item.timestamp)
        } else {
            stringResource(R.string.clipboard_characters, item.text.length)
        }
    ClipboardEntryRow(
        onClick = onClick,
        onPaste = onPaste,
        onDelete = onDelete,
        onTogglePin = onTogglePin,
        isPinned = item.isPinned,
        cornerRadius = cornerRadius,
        vibrateOnTap = vibrateOnTap,
        tapHapticType = tapHapticType,
        thumbUri = null,
        thumbFile = imageFile,
        label = label,
        meta = if (item.isImage()) meta else formatTimestamp(item.timestamp),
        extraMeta = if (item.isImage()) null else meta,
        pinLabel =
            if (item.isPinned) {
                stringResource(R.string.clipboard_unpin)
            } else {
                stringResource(R.string.clipboard_pin)
            },
        showImageThumb = item.isImage(),
    )
}

@Composable
private fun ClipboardEntryRow(
    onClick: () -> Unit,
    onPaste: () -> Unit,
    onDelete: (() -> Unit)?,
    onTogglePin: (() -> Unit)?,
    isPinned: Boolean,
    cornerRadius: Float,
    vibrateOnTap: Boolean,
    tapHapticType: HapticType,
    thumbUri: Uri?,
    thumbFile: File?,
    label: String,
    meta: String,
    extraMeta: String? = null,
    pinLabel: String,
    showImageThumb: Boolean = true,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val view = LocalView.current
    val showThumb = showImageThumb && (thumbUri != null || thumbFile != null)

    Box {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { offset ->
                                pressOffset = offset
                                showContextMenu = true
                                if (vibrateOnTap) {
                                    view.playHaptic(tapHapticType)
                                }
                            },
                        )
                    },
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(spacing),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (showThumb) {
                    ClipboardThumb(
                        uri = thumbUri,
                        file = thumbFile,
                        modifier = Modifier.size(48.dp),
                    )
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.weight(1f),
                )

                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (extraMeta != null) {
                        Text(
                            text = extraMeta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val menuOffset =
            with(density) {
                DpOffset(pressOffset.x.toDp(), pressOffset.y.toDp())
            }
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = menuOffset,
            properties = PopupProperties(focusable = false),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clipboard_paste)) },
                onClick = {
                    showContextMenu = false
                    onPaste()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                    )
                },
            )
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clipboard_delete)) },
                    onClick = {
                        showContextMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                        )
                    },
                )
            }
            if (onTogglePin != null) {
                DropdownMenuItem(
                    text = { Text(pinLabel) },
                    onClick = {
                        showContextMenu = false
                        onTogglePin()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ClipboardThumb(
    uri: Uri?,
    file: File?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri, file) {
        value =
            withContext(Dispatchers.IO) {
                decodeClipboardThumbnail(context, uri, file)?.asImageBitmap()
            }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.clipboard_image),
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(6.dp)),
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = stringResource(R.string.clipboard_image),
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun decodeClipboardThumbnail(
    context: Context,
    uri: Uri?,
    file: File?,
    maxPx: Int = 128,
): Bitmap? =
    try {
        when {
            file != null && file.isFile -> decodeFileThumbnail(file, maxPx)
            uri != null -> decodeUriThumbnail(context, uri, maxPx)
            else -> null
        }
    } catch (_: Exception) {
        null
    }

private fun decodeFileThumbnail(
    file: File,
    maxPx: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(file)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height, maxPx))
        }
    }
    return decodeBitmapFactory(file.absolutePath, maxPx)
}

private fun decodeUriThumbnail(
    context: Context,
    uri: Uri,
    maxPx: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height, maxPx))
        }
    }
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = input.readBytes()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPx)
            }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
    return null
}

private fun decodeBitmapFactory(
    path: String,
    maxPx: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val opts =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPx)
        }
    return BitmapFactory.decodeFile(path, opts)
}

private fun sampleSize(
    width: Int,
    height: Int,
    maxPx: Int,
): Int {
    val longest = maxOf(width, height).coerceAtLeast(1)
    var sample = 1
    while (longest / sample > maxPx * 2) {
        sample *= 2
    }
    return sample
}
