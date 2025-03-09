package net.rpcs3.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import net.rpcs3.R
import net.rpcs3.provider.AppDataDocumentProvider
import net.rpcs3.ui.common.ComposePreview
import net.rpcs3.ui.settings.components.core.PreferenceIcon
import net.rpcs3.ui.settings.components.core.PreferenceSubtitle
import net.rpcs3.ui.settings.components.core.PreferenceTitle
import net.rpcs3.ui.settings.components.preference.RegularPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit
) {
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Settings", fontWeight = FontWeight.Medium) },
                scrollBehavior = topBarScrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft, null)
                    }
                }
            )
        }
    ) { contentPadding ->
        val context = LocalContext.current

        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

        val firmwareFilePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { resultUri ->
            resultUri?.let { selectedFileUri = it }
            Toast.makeText(context, resultUri.toString(), Toast.LENGTH_SHORT).show()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // We can LazyList DSL for each preference later
            // We can also put the HorizontalDivider into the
            // DSL overload instead of adding manually
            item(
                key = "internal_directory"
            ) {
                RegularPreference(
                    title = { PreferenceTitle(title = "View Internal Directory") },
                    leadingIcon = { PreferenceIcon(icon = painterResource(R.drawable.ic_folder)) },
                    subtitle = { PreferenceSubtitle(text = "Open internal directory of RPCS3 in file manager") },
                ) {
                    context.launchBrowseIntent()
                }
            }

            item { HorizontalDivider() }
            item(
                key = "firmware_installation",
            ) {
                RegularPreference(
                    title = "Install Firmware",
                    leadingIcon = Icons.Default.Build,
                    subtitle = { PreferenceSubtitle(text = "Install PS3 Firmware") },
                ) {
                    firmwareFilePicker.launch("*/*")
                }
            }

            item { HorizontalDivider() }
            item(
                key = "custom_driver_installation"
            ) {
                RegularPreference(
                    title = "Install Custom Driver",
                    leadingIcon = Icons.Default.Build,
                ) {
                    /* no-op */
                }
            }
            item { HorizontalDivider() }
        }

        // Create a data class with title and onClick lambda?
//        val items: List<String> =
//            remember { mutableListOf("Install Firmware", "Install custom driver") }

    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ComposePreview {
        SettingsScreen {}
    }
}

private fun Context.launchBrowseIntent(): Boolean {
    return try {
        val intent = Intent("android.provider.action.BROWSE").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = DocumentsContract.buildRootUri(AppDataDocumentProvider.ROOT_ID, AppDataDocumentProvider.AUTHORITY)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}