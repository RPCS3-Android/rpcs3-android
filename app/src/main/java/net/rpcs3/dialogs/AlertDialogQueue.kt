import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

object DialogQueue {
    val dialogs = mutableStateListOf<DialogData>()

    fun showDialog(title: String, message: String, onConfirm: () -> Unit = {}) {
        dialogs.add(DialogData(title, message, onConfirm))
    }

    fun dismissDialog() {
        if (dialogs.isNotEmpty()) {
            dialogs.removeAt(0)
        }
    }
}

data class DialogData(val title: String, val message: String, val onConfirm: () -> Unit)
