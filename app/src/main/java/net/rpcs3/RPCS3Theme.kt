// 文件：theme/SciFiTheme.kt
package net.rpcs3.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

// ==================== 赛博朋克核心配色方案 ====================
private val SciFiDarkColorPalette = darkColorScheme(
    primary = Color(0xFF4ECDC4).copy(alpha = 0.85f),    // 半透明青蓝（参考网页1的青色主调）
    secondary = Color(0xFFD13F76).copy(alpha = 0.8f),  // 半透明品红（参考网页2的洋红色）
    tertiary = Color(0xFF6B8DD6).copy(alpha = 0.75f),  // 霓虹蓝紫
    surface = Color(0x1A2B2F3D),                       // 深空灰透明层
    background = Color(0x0D000000),                    // 超透明黑背景
    onPrimary = Color.White.copy(alpha = 0.95f),
    error = Color(0xFFFF453A).copy(alpha = 0.9f)
)

private val SciFiLightColorPalette = lightColorScheme(
    primary = Color(0xFF0066CC).copy(alpha = 0.8f),    // 透明钴蓝
    secondary = Color(0xFFFF0080).copy(alpha = 0.75f), // 霓虹粉（参考网页2的洋红色加强）
    surface = Color(0xE5F0F8FF),                        // 玻璃白层
    background = Color(0xDDFFFFFF),                     // 雾面白背景
    onPrimary = Color.Black.copy(alpha = 0.9f)
)

// ==================== 主题核心实现 ====================
@Composable
fun SciFiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SciFiDarkColorPalette else SciFiLightColorPalette
    val view = LocalView.current
    val activity = view.context as? Activity

    // 系统栏透明处理（参考网页1的暗调背景要求）
    SideEffect {
        activity?.window?.apply {
            statusBarColor = Color.Transparent.hashCode()
            navigationBarColor = Color.Transparent.hashCode()
            WindowCompat.setDecorFitsSystemWindows(this, false)
            
            WindowCompat.getInsetsController(this, decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme.copy(
            surface = colorScheme.surface.copy(alpha = 0.7f),   // 层叠透明度增强
            background = colorScheme.background.copy(alpha = 0.6f)
        ),
        typography = SciFiTypography
    ) {
        CompositionLocalProvider(
            LocalRippleTheme = SciFiRippleTheme,
            content = content
        )
    }
}

// ==================== 辅助组件 ====================
private object SciFiRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = if (isSystemInDarkTheme()) 
        Color(0xFF4ECDC4).copy(alpha = 0.2f) else 
        Color(0xFFFF0080).copy(alpha = 0.15f)

    @Composable
    override fun rippleAlpha() = RippleAlpha(
        draggedAlpha = 0.3f,
        focusedAlpha = 0.2f,
        hoveredAlpha = 0.15f,
        pressedAlpha = 0.2f
    )
}

// ==================== 文字样式扩展 ====================
private val SciFiTypography = Typography(
    titleLarge = MaterialTheme.typography.titleLarge.copy(
        color = Color(0xFF4ECDC4).copy(alpha = 0.9f)  // 青蓝色标题
    ),
    bodyMedium = MaterialTheme.typography.bodyMedium.copy(
        color = Color.White.copy(alpha = 0.85f)       // 高可读性文字
    )
)
