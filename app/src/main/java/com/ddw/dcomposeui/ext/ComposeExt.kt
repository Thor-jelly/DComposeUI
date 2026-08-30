package com.ddw.dcomposeui.ext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * 无水波纹点击，常用于遮罩层、整块可点区域等不需要 ripple 效果的场景
 *
 * @param onClick 点击回调
 */
fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.composed {
    clickable(
        // 关掉水波纹
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}
