package com.suave.keyboard.ui.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.suave.keyboard.layout.LayerIcon
import com.suave.keyboard.layout.NamedLayout

fun LayerIcon.asImageVector(): ImageVector =
    when (this) {
        LayerIcon.Abc -> Icons.Outlined.Abc
        LayerIcon.Numbers -> Icons.Outlined.Numbers
        LayerIcon.EmojiEmotions -> Icons.Outlined.EmojiEmotions
        LayerIcon.History -> Icons.Outlined.History
        LayerIcon.Functions -> Icons.Outlined.Functions
        LayerIcon.Tag -> Icons.Outlined.Tag
        LayerIcon.Star -> Icons.Outlined.Star
        LayerIcon.Build -> Icons.Outlined.Build
        LayerIcon.Extension -> Icons.Outlined.Extension
        LayerIcon.Code -> Icons.Outlined.Code
        LayerIcon.Bolt -> Icons.Outlined.Bolt
        LayerIcon.GridView -> Icons.Outlined.GridView
        LayerIcon.Widgets -> Icons.Outlined.Widgets
        LayerIcon.Category -> Icons.Outlined.Category
    }

fun NamedLayout.switchLayerIconMap(): Map<String, ImageVector> =
    layers.associate { it.id to it.icon.asImageVector() }
