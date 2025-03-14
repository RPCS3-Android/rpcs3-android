package net.rpcs3.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import net.rpcs3.PrecompilerService
import net.rpcs3.PrecompilerServiceAction

object FileUtil {
    fun installPackages(context: Context, folderUri: Uri) {
        val contentResolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri))

        contentResolver.query(childrenUri, 
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE), 
            null, null, null)?.use { cursor ->
        
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val mimeType = cursor.getString(1)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    installPackages(context, fileUri)
                } else {
                    Log.d("Install", "Installing package: $fileUri")
                    PrecompilerService.start(context, PrecompilerServiceAction.Install, fileUri)
                }
            }
        }
    }

    fun saveGameFolderUri(prefs: SharedPreferences, uri: Uri) {
        prefs.edit().putString("selected_game_folder", uri.toString()).apply()
    }
}
