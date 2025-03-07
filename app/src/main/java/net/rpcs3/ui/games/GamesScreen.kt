package net.rpcs3.ui.games

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.rpcs3.FirmwareRepository
import net.rpcs3.Game
import net.rpcs3.GameInfo
import net.rpcs3.GameProgressType
import net.rpcs3.GameRepository
import net.rpcs3.ProgressRepository
import net.rpcs3.RPCS3Activity
import net.rpcs3.dialogs.AlertDialogQueue
import java.io.File

private fun withAlpha(color: Color, alpha: Float): Color {
    return Color(
        red = color.red,
        green = color.green,
        blue = color.blue,
        alpha = alpha
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameItem(game: Game) {
    val context = LocalContext.current
    val menuExpanded = remember { mutableStateOf(false) }
    val iconExists = remember { mutableStateOf(false) }

    Column {
        DropdownMenu(expanded = menuExpanded.value, onDismissRequest = { menuExpanded.value = false }) {
            if (game.progressList.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    onClick = {
                        menuExpanded.value = false
                        val path = File(game.info.path)
                        if (path.exists()) {
                            GameRepository.remove(game)
                            path.deleteRecursively()
                        }

                        // FIXME: delete cache
                    }
                )
            }
        }

        Card(shape = RectangleShape, modifier = Modifier.fillMaxSize().combinedClickable(
            onClick = {
                if (FirmwareRepository.version.value == null) {
                    AlertDialogQueue.showDialog(
                        title = "Firmware Missing",
                        message = "Please install the required firmware to continue."
                    )
                }
                else if (FirmwareRepository.progressChannel.value != null) {
                    AlertDialogQueue.showDialog(
                        title = "Firmware Missing",
                        message = "Please wait until firmware installs successfully to continue."
                    )
                } else if (game.info.path != "$" && game.findProgress(
                        arrayOf(
                            GameProgressType.Install,
                            GameProgressType.Remove
                        )
                    ) == null
                ) {
                    if (game.findProgress(GameProgressType.Compile) != null) {
                        AlertDialogQueue.showDialog(
                            title = "Game compiling isn't finished yet",
                            message = "Please wait until game compiles to continue."
                        )
                    } else {
                        GameRepository.onBoot(game)
                        val emulatorWindow = Intent(
                            context,
                            RPCS3Activity::class.java
                        )
                        emulatorWindow.putExtra("path", game.info.path)
                        context.startActivity(emulatorWindow)
                    }
                }
            },
            onLongClick = {
                if (game.info.name.value != "VSH") {
                    menuExpanded.value = true
                }
            }
        )
        ) {
            if (game.info.iconPath.value != null && !iconExists.value) {
                if (game.progressList.isNotEmpty()) {
                    val progressId = ProgressRepository.getItem(game.progressList.first().id)
                    if (progressId != null) {
                        val progressValue = progressId.value.value
                        val progressMax =  progressId.value.max

                        iconExists.value = (progressMax.longValue != 0L && progressValue.longValue == progressMax.longValue) || File(game.info.iconPath.value!!).exists()
                    }
                } else {
                    iconExists.value = File(game.info.iconPath.value!!).exists()
                }
            }

            Box(
                modifier = Modifier
                    .height(110.dp)
                    .align(alignment = Alignment.CenterHorizontally)
                    .fillMaxSize()
            ) {
                if (game.info.iconPath.value != null && iconExists.value) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = game.info.iconPath.value,
                            contentScale = if (game.info.name.value == "VSH") ContentScale.Fit else ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )
                    }
                }

                if (game.progressList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(withAlpha(Color.DarkGray, 0.6f))
                    ) {}

                    val progressChannel = game.progressList.first().id
                    val progress = ProgressRepository.getItem(progressChannel)
                    val progressValue = progress?.value?.value
                    val maxValue = progress?.value?.max

                    if (progressValue != null && maxValue != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (maxValue.longValue != 0L) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(64.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    progress = {
                                        progressValue.longValue.toFloat() / maxValue.longValue.toFloat()
                                    },
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(64.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }

//                val name = game.info.name.value
//                if (name != null) {
//                    Row(
//                        verticalAlignment = Alignment.Bottom,
//                        horizontalArrangement = Arrangement.Center,
//                        modifier = Modifier.fillMaxSize()
//                    ) {
//                        Text(name, textAlign = TextAlign.Center)
//                    }
//                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen() {
    val games = remember { GameRepository.list() }
    val isRefreshing = remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing.value,
        onRefresh = { isRefreshing.value = false },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp * 0.6f),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            items(count = games.size, key = { index -> games[index].info.path }) { index ->
                GameItem(games[index])
            }
        }
    }
}

@Preview
@Composable
fun GamesScreenPreview() {
    listOf(
        "Minecraft",
        "Skate 3",
        "Mirror's Edge",
        "Demon's Souls"
    ).forEach { x -> GameRepository.addPreview(arrayOf(GameInfo(x, x))) }

    GamesScreen()
}
