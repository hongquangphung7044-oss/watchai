package com.example.watchai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 手表专用深色主题 - 纯黑背景在 AMOLED 屏上像素关闭，极省电
private val WatchColorScheme = darkColorScheme(
    primary          = Color(0xFF1E88E5),  // 蓝色主色
    secondary        = Color(0xFF00BCD4),  // 青色辅色
    background       = Color.Black,         // 纯黑（AMOLED 像素关闭）
    surface          = Color(0xFF0D0D0D),
    surfaceVariant   = Color(0xFF1A1A1A),
    onBackground     = Color.White,
    onSurface        = Color.White,
    onPrimary        = Color.White,
    error            = Color(0xFFFF5252),
    onError          = Color.White,
)

@Composable
fun WatchAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WatchColorScheme,
        content = content
    )
}
