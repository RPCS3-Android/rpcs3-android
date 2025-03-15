package net.rpcs3.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import net.rpcs3.PrecompilerService
import net.rpcs3.PrecompilerServiceAction

object FileUtil {
    fun installPackages(context: Context, rootFolderUri: Uri) {
        val workList = mutableListOf<Uri>()
        workList.add(rootFolderUri)

        while (workList.isNotEmpty()) {
            val currentFolderUri = workList.removeFirst()
            val currentFolder = DocumentFile.fromTreeUri(context, currentFolderUri) ?: continue

            currentFolder.listFiles().forEach { item ->
                if (item.isDirectory) {
                    workList.add(item.uri)
                } else if (item.isFile) {
                    Log.d("FileUtil", "Installing package: ${item.uri}")
                    PrecompilerService.start(context, PrecompilerServiceAction.Install, item.uri)
                }
            }
        }
    }

    fun saveGameFolderUri(prefs: SharedPreferences, uri: Uri) {
        prefs.edit().putString("selected_game_folder", uri.toString()).apply()
    }
}
