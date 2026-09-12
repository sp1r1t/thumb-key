package com.suave.s12.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.suave.s12.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestOutTextField() {
    var text by remember { mutableStateOf("") }
    var showField by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    var focusNonce by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val hideButton = showField && fieldFocused && imeVisible

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (!hideButton) {
            Button(
                onClick = {
                    showField = true
                    focusNonce += 1
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.test_out_thumbkey))
            }
        }
        if (showField) {
            OutlinedTextField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (hideButton) 0.dp else 12.dp)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(focusRequester)
                        .onFocusChanged { fieldFocused = it.isFocused },
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.test_out_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 3,
            )
            LaunchedEffect(focusNonce) {
                withFrameNanos { }
                focusRequester.requestFocus()
                keyboardController?.show()
                bringIntoViewRequester.bringIntoView()
            }
            LaunchedEffect(imeVisible, fieldFocused, hideButton) {
                if (fieldFocused && imeVisible) {
                    withFrameNanos { }
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
    }
}
