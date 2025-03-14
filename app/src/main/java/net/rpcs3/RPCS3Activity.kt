package net.rpcs3

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.isInvisible
import net.rpcs3.R
import net.rpcs3.databinding.ActivityRpcs3Binding

class RPCS3Activity : Activity() {
    private lateinit var binding: ActivityRpcs3Binding
    private lateinit var unregisterUsbEventListener: () -> Unit
  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRpcs3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        unregisterUsbEventListener = listenUsbEvents(this)
        enableFullScreenImmersive()

        binding.oscToggle.setOnClickListener { 
            binding.padOverlay.isInvisible = !binding.padOverlay.isInvisible 
            binding.oscToggle.setImageResource(if (binding.padOverlay.isInvisible) R.drawable.ic_osc_off else R.drawable.ic_show_osc)
        }

        binding.surfaceView.boot(intent.getStringExtra("path")!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterUsbEventListener()
    }

    private fun enableFullScreenImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        with(insetsController) {
            // System Bars include status, navigation and caption bars.
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        applyInsetsToPadOverlay()
    }

    private fun applyInsetsToPadOverlay() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.padOverlay) { view, windowInsets ->
            // I don't think we need `displayCutout` insets here as well
            // Since there is hardly any overlay overlapping with it
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableFullScreenImmersive()
    }
}
