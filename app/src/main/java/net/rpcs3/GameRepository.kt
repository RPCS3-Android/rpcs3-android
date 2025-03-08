package net.rpcs3

import android.content.res.Resources.NotFoundException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.security.InvalidParameterException

@Serializable
data class GameInfo(val path: String, var name: String? = null, var iconPath: String? = null)

data class GameInfoStore(
    val path: String,
    val name: MutableState<String?> = mutableStateOf(null),
    val iconPath: MutableState<String?> = mutableStateOf(null)
)

enum class GameProgressType {
    Install,
    Compile,
    Remove,
}

data class GameProgress(val id: Long, val type: GameProgressType)

data class Game(
    val info: GameInfoStore,
    val progressList: SnapshotStateList<GameProgress> = mutableStateListOf()
) {
    fun addProgress(progress: GameProgress) {
        if (findProgress(progress.type) != null) {
            throw InvalidParameterException()
        }

        progressList += progress
    }

    fun findProgress(type: GameProgressType) =
        progressList.filter { elem -> elem.type == type }.ifEmpty { null }

    fun findProgress(types: Array<GameProgressType>) =
        progressList.filter { elem -> types.contains(elem.type) }.ifEmpty { null }

    fun removeProgress(type: GameProgressType) =
        progressList.removeIf { progress -> progress.type == type }
}

private fun toStore(info: GameInfo) =
    GameInfoStore(info.path, mutableStateOf(info.name), mutableStateOf(info.iconPath))

private fun toInfo(store: GameInfoStore) =
    GameInfo(store.path, store.name.value, store.iconPath.value)

class GameRepository {
    private val games = mutableStateListOf<Game>()

    companion object {
        private val instance = GameRepository()

        fun save() {
            try {
                File(RPCS3.rootDirectory + "games.json").writeText(Json.encodeToString(instance.games.map { game ->
                    toInfo(
                        game.info
                    )
                }.filter { info -> info.path != "$" }))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun load() {
            withContext(Dispatchers.IO) {
                try {
                    instance.games.clear()
                    instance.games += Json.decodeFromString<Array<GameInfo>>(
                        File(RPCS3.rootDirectory + "games.json").readText()
                    ).map { info -> Game(toStore(info)) }
                } catch (_: NotFoundException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun add(gameInfos: Array<GameInfo>, progressId: Long) {
            synchronized(instance) {
                val progressEntry =
                    instance.games.filter { game -> game.info.path == "$" }.find { game ->
                        val progress = game.findProgress(GameProgressType.Install)
                            ?.find { progress -> progress.id == progressId }
                        progress != null
                    }

                if (progressEntry != null) {
                    instance.games.remove(progressEntry)
                }

                gameInfos.forEach { info ->
                    val existsGame = instance.games.find { x -> x.info.path == info.path }
                    if (existsGame == null) {
                        val newGame = Game(toStore(info))
                        newGame.addProgress(GameProgress(progressId, GameProgressType.Install))
                        instance.games.add(0, newGame)
                    } else {
                        existsGame.info.name.value = info.name ?: existsGame.info.name.value
                        existsGame.info.iconPath.value =
                            info.iconPath ?: existsGame.info.iconPath.value
                        existsGame.addProgress(GameProgress(progressId, GameProgressType.Install))
                    }
                }
                save()
            }
        }

        fun addPreview(gameInfos: Array<GameInfo>) {
            instance.games += gameInfos.map { info -> Game(toStore(info)) }
        }

        fun onBoot(game: Game) {
            synchronized(instance) {
                if (instance.games.first() != game) {
                    instance.games.remove(game)
                    instance.games.add(0, game)
                    save()
                }
            }
        }

        fun createGameInstallEntry(progressId: Long) {
            synchronized(instance) {
                val game = Game(GameInfoStore("$"))
                game.addProgress(GameProgress(progressId, GameProgressType.Install))
                instance.games.add(0, game)
            }
        }

        fun clearProgress(progressId: Long) {
            synchronized(instance) {
                instance.games.forEach { game -> game.progressList.removeIf { progress -> progress.id == progressId } }
                instance.games.removeIf { game -> game.info.path == "$" && game.progressList.isEmpty() }
            }
        }

        fun remove(game: Game) {
            synchronized(instance) {
                instance.games -= game
                save()
            }
        }

        fun find(path: String): Game? {
            synchronized(instance) {
                return instance.games.find { game -> game.info.path == path }
            }
        }

        fun list() = instance.games
    }
}
