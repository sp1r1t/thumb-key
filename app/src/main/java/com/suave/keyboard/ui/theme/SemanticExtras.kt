package com.suave.keyboard.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.suave.keyboard.ui.theme.json.parseArgbHex
import com.suave.keyboard.ui.theme.json.toArgbHex

/**
 * Extra semantic colors themes define beyond Material [androidx.compose.material3.ColorScheme].
 * Success is used for affirmative actions; error lives on ColorScheme itself.
 */
data class SemanticExtras(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
) {
    fun toRoleMap(): Map<String, String> =
        linkedMapOf(
            "success" to success.toArgbHex(),
            "onSuccess" to onSuccess.toArgbHex(),
            "successContainer" to successContainer.toArgbHex(),
            "onSuccessContainer" to onSuccessContainer.toArgbHex(),
        )

    companion object {
        val SoftLight =
            SemanticExtras(
                success = Color(0xFF2E7D32),
                onSuccess = Color(0xFFFFFFFF),
                successContainer = Color(0xFFC8E6C9),
                onSuccessContainer = Color(0xFF1B5E20),
            )

        val SoftDark =
            SemanticExtras(
                success = Color(0xFF81C784),
                onSuccess = Color(0xFF0A1F0C),
                successContainer = Color(0xFF1B5E20),
                onSuccessContainer = Color(0xFFC8E6C9),
            )

        fun fromRoleMap(map: Map<String, String>): SemanticExtras =
            SemanticExtras(
                success = parseArgbHex(map.getValue("success")),
                onSuccess = parseArgbHex(map.getValue("onSuccess")),
                successContainer = parseArgbHex(map.getValue("successContainer")),
                onSuccessContainer = parseArgbHex(map.getValue("onSuccessContainer")),
            )
    }
}

val LocalSemanticExtras = staticCompositionLocalOf { SemanticExtras.SoftLight }
