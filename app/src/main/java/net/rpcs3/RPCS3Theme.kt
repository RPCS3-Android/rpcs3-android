
package net.rpcs3

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun RPCS3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // TODO(Ishan09811): Implement dynamic colors option whenever settings gets implemented
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()

    val view = LocalView.current
    val activity = view.context as? Activity

    SideEffect {
        activity?.window?.apply {
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
            isNavigationBarContrastEnforced = false
            val insetsController = WindowInsetsControllerCompat(this, decorView)
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
