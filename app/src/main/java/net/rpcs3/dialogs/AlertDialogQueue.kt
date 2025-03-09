
package net.rpcs3.dialogs

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

object AlertDialogQueue {
    val dialogs = mutableStateListOf<DialogData>()

    fun showDialog(title: String, message: String, onConfirm: () -> Unit = {}) {
        dialogs.add(DialogData(title, message, onConfirm))
    }

    fun dismissDialog() {
        if (dialogs.isNotEmpty()) {
            dialogs.removeAt(0)
        }
    }

    @Composable
    fun alertDialog() {
        if (dialogs.isNotEmpty()) {
            val dialog = dialogs.first()

            AlertDialog(
                onDismissRequest = { dismissDialog() },
                title = { Text(dialog.title) },
                text = { Text(dialog.message) },
                confirmButton = {
                    TextButton(onClick = {
                        dialog.onConfirm()
                        dismissDialog()
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

data class DialogData(val title: String, val message: String, val onConfirm: () -> Unit)
