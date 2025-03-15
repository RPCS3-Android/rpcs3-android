
package net.rpcs3.ui.drivers

import android.content.Context
import android.util.Log
import android.net.Uri
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import net.rpcs3.utils.GpuDriverHelper
import net.rpcs3.utils.GpuDriverInstallResult
import net.rpcs3.utils.GpuDriverMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

@Composable
fun GpuDriversScreen(navigateBack: () -> Unit) {
    val context = LocalContext.current
    val drivers = remember { mutableStateOf(GpuDriverHelper.getInstalledDrivers(context)) }
    val selectedDriver = remember { mutableStateOf<String?>(null) }
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var isInstalling by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val driverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isInstalling = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val result = GpuDriverHelper.installDriver(context, stream)
                        if (result == GpuDriverInstallResult.Success) {
                            val updatedDrivers = GpuDriverHelper.getInstalledDrivers(context)
                            withContext(Dispatchers.Main) {
                                drivers.value = updatedDrivers
                            }
                        }
                        withContext(Dispatchers.Main) {
                            isInstalling = false
                            snackbarHostState.showSnackbar(
                                message = GpuDriverHelper.resolveInstallResultToString(result),
                                actionLabel = "Dismiss",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GpuDriver", "Error installing driver: ${e.message}")
                }
            }
        }
    }

    selectedDriver.value = prefs.getString("selected_gpu_driver", "System Driver-v")
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text(
                text = "Select a GPU Driver",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { driverPickerLauncher.launch("application/zip") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInstalling) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !isInstalling
            ) {
                if (isInstalling) {
                    Text("Installing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Install Driver"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(drivers.value.entries.toList()) { (file, metadata) ->
                    DriverItem(
                        file = file,
                        metadata = metadata,
                        isSelected = metadata.label == selectedDriver.value,
                        onSelect = { 
                            selectedDriver.value = metadata.label
                            prefs.edit().putString("selected_gpu_driver", selectedDriver.value ?: "").apply()
                        },
                        onDelete = { driverFile ->
                            coroutineScope.launch {
                                if (driverFile.deleteRecursively()) {
                                    drivers.value = GpuDriverHelper.getInstalledDrivers(context)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DriverItem(
    file: File,
    metadata: GpuDriverMetadata,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (File) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(file)
                true
            } else false
        }
    )
    
    SwipeToDismissBox(
        modifier = Modifier.animateContentSize(),
        state = dismissState,
        backgroundContent = {
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) { 
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(8.dp))
                        .padding(end = 16.dp)
                        .padding(vertical = 4.dp),
                    contentAlignment = androidx.compose.ui.Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect() },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = metadata.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = metadata.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false
    )
}
