package net.rpcs3.ui.settings.components.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import net.rpcs3.ui.common.ComposePreview
import net.rpcs3.ui.settings.components.core.PreferenceIcon
import net.rpcs3.ui.settings.components.core.PreferenceSubtitle
import net.rpcs3.ui.settings.components.core.PreferenceTitle


/**
 * Created using Android Studio
 * User: Muhammad Ashhal
 * Date: Sat, Mar 08, 2025
 * Time: 10:35 pm
 */

@Composable
fun SliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: @Composable () -> Unit,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    sliderColors: SliderColors = SliderDefaults.colors(),
    onClick: (() -> Unit)? = null
) {
    RegularPreference(
        modifier = modifier,
        title = title,
        leadingIcon = leadingIcon,
        subtitle = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                subtitle?.invoke()
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    colors = sliderColors
                )
            }
        },
        trailingContent = trailingContent,
        enabled = enabled,
        onClick = { onClick?.invoke() }
    )
}

@Composable
fun SliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    sliderColors: SliderColors = SliderDefaults.colors()
) {
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        title = { PreferenceTitle(title = title) },
        leadingIcon = { PreferenceIcon(icon = leadingIcon) },
        modifier = modifier,
        subtitle = subtitle,
        trailingContent = trailingContent,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        sliderColors = sliderColors
    )
}

@PreviewLightDark
@Composable
private fun SliderPreferencePreview() {
    ComposePreview {
        var value by remember { mutableFloatStateOf(0.5f) }
        SliderPreference(
            value = value,
            onValueChange = { value = it },
            title = "Refresh Duration",
            leadingIcon = Icons.Default.Refresh,
            subtitle = { PreferenceSubtitle(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor sit amet, consectetur adipiscing elit.") }
        )
    }
}

@PreviewLightDark
@Composable
private fun SliderPreferenceDisabledPreview() {
    ComposePreview {
        var value by remember { mutableFloatStateOf(0.5f) }
        SliderPreference(
            value = value,
            onValueChange = { value = it },
            title = "Refresh Duration",
            leadingIcon = Icons.Default.Refresh,
            subtitle = { PreferenceSubtitle(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor sit amet, consectetur adipiscing elit.") },
            enabled = false
        )
    }
}