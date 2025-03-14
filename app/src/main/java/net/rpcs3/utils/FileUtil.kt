package net.rpcs3.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import net.rpcs3.PrecompilerService
import net.rpcs3.PrecompilerServiceAction

object FileUtil {
    fun installPackages(context: Context, folderUri: Uri, depth: Int = 2) {
        if (depth <= 0) return
        val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        val subFolders = mutableListOf<Uri>()

        rootFolder.listFiles().forEach { file ->
            val fileUri = file.uri ?: return@forEach
            if (file.isDirectory) {
                subFolders.add(fileUri)
            } else {
                Log.d("FileUtil", "Installing package: $fileUri")
                PrecompilerService.start(context, PrecompilerServiceAction.Install, fileUri)
            }
        }

        if (depth > 0) {
            subFolders.forEach { subFolderUri ->
                Log.d("FileUtil", "Entering sub directory: $subFolderUri")
                installPackages(context, subFolderUri, depth - 1)
            }
        }
    }

    fun saveGameFolderUri(prefs: SharedPreferences, uri: Uri) {
        prefs.edit().putString("selected_game_folder", uri.toString()).apply()
    }
}
