package com.suave.keyboard.ui.components.settings.about

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.suave.keyboard.R
import com.suave.keyboard.utils.SimpleTopAppBar
import com.suave.keyboard.utils.TAG
import com.suave.keyboard.utils.openLink
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceTheme

const val GITHUB_URL = "https://github.com/sp1r1t/suave-keyboard"
const val ISSUE_TRACKER_URL = "https://github.com/sp1r1t/suave-keyboard/issues"
const val UPSTREAM_THUMBKEY_URL = "https://github.com/dessalines/thumb-key"
const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/juliankonrc"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Log.d(TAG, "Got to About activity")

    val ctx = LocalContext.current

    val version = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(text = stringResource(R.string.about), navController = navController)
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .background(color = MaterialTheme.colorScheme.surface),
            ) {
                ProvidePreferenceTheme {
                    Preference(
                        title = { Text(stringResource(R.string.version, version.orEmpty())) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.version, version.orEmpty()),
                            )
                        },
                    )
                    SettingsDivider()
                    PreferenceCategory(
                        title = { Text(stringResource(R.string.open_source)) },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.source_code)) },
                        summary = {
                            Text(stringResource(R.string.source_code_subtitle))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = stringResource(R.string.source_code),
                            )
                        },
                        onClick = {
                            openLink(GITHUB_URL, ctx)
                        },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.issue_tracker)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.BugReport,
                                contentDescription = stringResource(R.string.issue_tracker),
                            )
                        },
                        onClick = {
                            openLink(ISSUE_TRACKER_URL, ctx)
                        },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.inspired_by_thumb_key)) },
                        summary = {
                            Text(stringResource(R.string.inspired_by_thumb_key_subtitle))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = stringResource(R.string.inspired_by_thumb_key),
                            )
                        },
                        onClick = {
                            openLink(UPSTREAM_THUMBKEY_URL, ctx)
                        },
                    )
                    SettingsDivider()
                    PreferenceCategory(
                        title = { Text(stringResource(R.string.support)) },
                    )
                    Preference(
                        title = { Text(stringResource(R.string.buy_me_a_coffee)) },
                        summary = {
                            Text(stringResource(R.string.buy_me_a_coffee_subtitle))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Coffee,
                                contentDescription = stringResource(R.string.buy_me_a_coffee),
                            )
                        },
                        onClick = {
                            openLink(BUY_ME_A_COFFEE_URL, ctx)
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
}

@Preview
@Composable
fun AboutPreview() {
    AboutScreen(navController = rememberNavController())
}
