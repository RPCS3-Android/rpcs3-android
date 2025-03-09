package net.rpcs3.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import net.rpcs3.FirmwareRepository
import net.rpcs3.GameRepository
import net.rpcs3.ProgressRepository
import net.rpcs3.RPCS3
import net.rpcs3.ui.games.GamesScreen
import net.rpcs3.dialogs.AlertDialogQueue
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AppNavHost() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    val context = LocalContext.current

    AlertDialogQueue.alertDialog()

    val installPkgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                val fd = descriptor?.parcelFileDescriptor?.fd

                if (fd != null) {
                    val installProgress =
                        ProgressRepository.create(context, "Package Installation")
                    GameRepository.createGameInstallEntry(installProgress)

                    thread(isDaemon = true) {
                        if (!RPCS3.instance.installPkgFile(fd, installProgress)) {
                            try {
                                ProgressRepository.onProgressEvent(installProgress, -1, 0)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                ProgressRepository.cancel(installProgress)
                            }
                        }

                        try {
                            descriptor.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    try {
                        descriptor?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val installFwLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                val fd = descriptor?.parcelFileDescriptor?.fd

                if (fd != null) {
                    val installProgress =
                        ProgressRepository.create(context, "Firmware Installation") { entry ->
                            if (entry.isFinished()) {
                                descriptor.close()
                                FirmwareRepository.progressChannel.value = null
                            }
                        }

                    FirmwareRepository.progressChannel.value = installProgress

                    thread(isDaemon = true) {
                        if (!RPCS3.instance.installFw(fd, installProgress)) {
                            try {
                                ProgressRepository.onProgressEvent(installProgress, -1, 0)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        try {
                            descriptor.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    try {
                        descriptor?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {
                        Spacer(Modifier.height(12.dp))

                        NavigationDrawerItem(
                            label = {
                                Text(
                                    "Firmware: " + (FirmwareRepository.version.value ?: "None")
                                )
                            },
                            selected = false,
                            icon = { Icon(Icons.Outlined.Build, contentDescription = null) },
                            badge = {
                                val progressChannel = FirmwareRepository.progressChannel
                                val progress = ProgressRepository.getItem(progressChannel.value)
                                val progressValue = progress?.value?.value
                                val maxValue = progress?.value?.max
                                Log.e("Main", "Update $progressChannel, $progress")
                                if (progressValue != null && maxValue != null) {
                                    if (maxValue.longValue != 0L) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .width(32.dp)
                                                .height(32.dp),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            progress = {
                                                progressValue.longValue.toFloat() / maxValue.longValue.toFloat()
                                            },
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .width(32.dp)
                                                .height(32.dp),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    }
                                }
                            }, // Placeholder
                            onClick = {
                                if (FirmwareRepository.progressChannel.value == null) {
                                    installFwLauncher.launch("*/*")
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text(
                                "RPCS3",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Open menu"
                                )
                            }
                        },
                        actions = {
//                            IconButton(onClick = { /* do something */ }) {
//                                Icon(
//                                    imageVector = Icons.Filled.Search,
//                                    contentDescription = null
//                                )
//                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { installPkgLauncher.launch("*/*") },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Add, "Add game")
                    }
                },
            ) { innerPadding -> Column(modifier = Modifier.padding(innerPadding)) { GamesScreen() } }
        }
    }
}
