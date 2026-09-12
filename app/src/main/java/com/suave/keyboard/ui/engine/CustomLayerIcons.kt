package com.suave.keyboard.ui.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.suave.keyboard.layout.CustomLayerIcon
import com.suave.keyboard.layout.NamedLayout

fun CustomLayerIcon.asImageVector(): ImageVector =
    when (this) {
        CustomLayerIcon.Functions -> Icons.Outlined.Functions
        CustomLayerIcon.Tag -> Icons.Outlined.Tag
        CustomLayerIcon.Star -> Icons.Outlined.Star
        CustomLayerIcon.Build -> Icons.Outlined.Build
        CustomLayerIcon.Extension -> Icons.Outlined.Extension
        CustomLayerIcon.Code -> Icons.Outlined.Code
        CustomLayerIcon.Bolt -> Icons.Outlined.Bolt
        CustomLayerIcon.GridView -> Icons.Outlined.GridView
        CustomLayerIcon.Widgets -> Icons.Outlined.Widgets
        CustomLayerIcon.Category -> Icons.Outlined.Category
    }

fun NamedLayout.switchLayerIconMap(): Map<String, ImageVector> =
    customLayers.associate { it.id to it.icon.asImageVector() }
