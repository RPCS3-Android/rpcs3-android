package net.rpcs3.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
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
import androidx.compose.ui.unit.dp
import net.rpcs3.R
import net.rpcs3.RPCS3
import net.rpcs3.dialogs.AlertDialogQueue
import net.rpcs3.provider.AppDataDocumentProvider
import net.rpcs3.ui.common.ComposePreview
import net.rpcs3.ui.settings.components.core.PreferenceIcon
import net.rpcs3.ui.settings.components.core.PreferenceSubtitle
import net.rpcs3.ui.settings.components.core.PreferenceTitle
import net.rpcs3.ui.settings.components.preference.RegularPreference
import net.rpcs3.ui.settings.components.preference.SingleSelectionDialog
import net.rpcs3.ui.settings.components.preference.SwitchPreference
import net.rpcs3.ui.settings.components.preference.HomePreference
import org.json.JSONObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    navigateTo: (path: String) -> Unit,
    settings: JSONObject,
    path: String = ""
) {
    val settingValue = remember { mutableStateOf(settings) }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            val titlePath = path.replace("@@", " / ")
            LargeTopAppBar(
                title = { Text(text = "Advanced Settings$titlePath" , fontWeight = FontWeight.Medium) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            settings.keys().forEach { key ->
                val itemPath = "$path@@$key"
                item(key = key) {
                    val itemObject = settingValue.value[key] as? JSONObject

                    if (itemObject != null) {
                        when (val type = if (itemObject.has("type")) itemObject.getString("type") else null) {
                             null -> {
                                RegularPreference(
                                    title = key,
                                    leadingIcon = null
                                ) {
                                    Log.e("Main", "Navigate to settings$itemPath, object $itemObject")
                                    navigateTo("settings$itemPath")
                                }
                            }

                            "bool" -> {
                                var itemValue by remember {  mutableStateOf(itemObject.getBoolean("value"))  }
                                SwitchPreference (
                                    checked = itemValue,
                                    title = key,
                                    leadingIcon = null
                                ) { value ->
                                    if (!RPCS3.instance.settingsSet(itemPath, if (value) "true" else "false")) {
                                        AlertDialogQueue.showDialog("Setting error", "Failed to assign $itemPath value $value")
                                    } else {
                                        itemObject.put("value", value)
                                        itemValue = value
                                    }
                                }
                            }

                            "enum" -> {
                                var itemValue by remember {  mutableStateOf(itemObject.getString("value"))  }
                                val variantsJson = itemObject.getJSONArray("variants")
                                val variants = ArrayList<String>()
                                for (i in 0..<variantsJson.length()) {
                                    variants.add(variantsJson.getString(i))
                                }

                                SingleSelectionDialog(
                                    currentValue = if (itemValue in variants) itemValue else variants[0],
                                    values = variants,
                                    icon = null,
                                    title = key,
                                    onValueChange = {
                                            value ->
                                        if (!RPCS3.instance.settingsSet(itemPath, "\"" + value + "\"")) {
                                            AlertDialogQueue.showDialog("Setting error", "Failed to assign $itemPath value $value")
                                        } else {
                                            itemObject.put("value", value)
                                            itemValue = value
                                        }
                                    })

                            }

                            else -> {
                                Log.e("Main", "Unimplemented setting type $type")
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    navigateTo: (path: String) -> Unit,
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
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item(
                key = "internal_directory"
            ) {
                HomePreference(
                    title = "View Internal Directory",
                    icon = { PreferenceIcon(icon = painterResource(R.drawable.ic_folder)) },
                    description = "Open internal directory of RPCS3 in file manager"
                ) {
                    if (context.launchBrowseIntent(Intent.ACTION_VIEW) or context.launchBrowseIntent()) {
                        // No Activity found to handle action
                    }
                }
            }

            item(key = "advanced_settings") {
                HomePreference(title = "Advanced Settings", icon = { Icon(imageVector = Icons.Default.Settings, null) }, description = "Configure emulator advanced settings") {
                    navigateTo("settings@@$")
                }
            }
            
            item(
                key = "custom_gpu_driver"
            ) {
                HomePreference(
                    title = "Custom GPU Driver",
                    icon = { Icon(Icons.Outlined.Build, contentDescription = null) },
                    description = "Install alternative drivers for potentially better performance or accuracy"
                ) {
                    if (RPCS3.instance.supportsCustomDriverLoading()) {
                        navigateTo("drivers")
                    } else {
                        AlertDialogQueue.showDialog(
                            title = "Custom drivers not supported",
                            message = "Custom driver loading isn't currently supported for this device",
                            confirmText = "Close",
                            dismissText = ""
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ComposePreview {
//        SettingsScreen {}
    }
}

private fun Context.launchBrowseIntent(
    action: String = "android.provider.action.BROWSE"
): Boolean {
    return try {
        val intent = Intent(action).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = DocumentsContract.buildRootUri(
                AppDataDocumentProvider.AUTHORITY,
                AppDataDocumentProvider.ROOT_ID
            )
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        println("No activity found to handle $action intent")
        false
    }
}
