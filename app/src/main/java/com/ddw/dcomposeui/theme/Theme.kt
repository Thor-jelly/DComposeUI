package com.ddw.dcomposeui.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * 基于 AppColors 映射的 Material3 浅色配色方案（本库只提供浅色主题）
 */
private val AppLightColorScheme = lightColorScheme(
    // 主色：Button 背景、Switch/Checkbox 选中态、进度条、TextField 光标与聚焦边框
    primary = AppColors.AccentPrimary,
    // primary 背景之上的文字与图标
    onPrimary = Color.White,
    // 次要色：Chip、次级按钮、FilterChip 选中态
    secondary = AppColors.AccentBlue,
    // secondary 背景之上的文字与图标
    onSecondary = Color.White,
    // 错误色：表单校验错误、错误态 TextField 边框
    error = AppColors.AccentRed,
    // error 背景之上的文字与图标
    onError = Color.White,
    // Scaffold 页面背景
    background = Color.White,
    // Card / Sheet / Menu / Dialog 等容器表面
    surface = Color.White,
    // TextField 未聚焦边框、Divider、OutlinedButton 边框
    outline = AppColors.LineOne
)

/**
 * 淡水波纹配置：pressedAlpha 越小，点击时的涟漪越透明
 */
@OptIn(ExperimentalMaterial3Api::class)
private val AppRippleConfiguration = RippleConfiguration(
    color = AppColors.ButtonTwo,
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.08f,
        focusedAlpha = 0.08f,
        draggedAlpha = 0.08f,
        hoveredAlpha = 0.04f
    )
)

/**
 * 组件描述：库统一 Compose 主题，页面与 @Preview 的根部都用它包裹
 *
 * @param content 主题作用域内的 Compose 内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppComposeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppLightColorScheme) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides AppRippleConfiguration,
            content = content
        )
    }
}
