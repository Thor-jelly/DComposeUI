package com.ddw.dcomposeui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp

/** 气泡相对锚点的竖直方位 */
enum class BubbleYGravity { ABOVE, BELOW }

/** 气泡相对锚点的水平对齐 */
enum class BubbleXGravity { ALIGN_LEFT, CENTER, ALIGN_RIGHT }

/**
 * 按对齐方式与安全边距算出浮层左边缘 x（px）。
 * 提示气泡的定位器与箭头共用同一算式，保证两边对落点的结论一致，从而无需在布局阶段回写状态；
 * 选项浮层复用它，两个组件的越界行为因此始终一致。
 */
internal fun bubbleLeftPx(
    anchorBounds: IntRect,
    windowWidthPx: Int,
    bubbleWidthPx: Int,
    xGravity: BubbleXGravity,
    offsetXPx: Int,
    screenPaddingPx: Int
): Int {
    val rawX = when (xGravity) {
        BubbleXGravity.ALIGN_LEFT -> anchorBounds.left
        BubbleXGravity.ALIGN_RIGHT -> anchorBounds.right - bubbleWidthPx
        BubbleXGravity.CENTER -> anchorBounds.left + (anchorBounds.width - bubbleWidthPx) / 2
    } + offsetXPx
    // 超出屏幕时收进安全边距，避免浮层被裁切；浮层宽到放不下时退化为左侧贴边，防止夹取区间倒挂
    val maxX = (windowWidthPx - bubbleWidthPx - screenPaddingPx).coerceAtLeast(screenPaddingPx)
    return rawX.coerceIn(screenPaddingPx, maxX)
}

/**
 * 按方位算出浮层上边缘 y（px），并夹回窗口可视区。
 * 竖向只兜底不翻转方位：锚点贴近窗口上下边缘时把浮层收回可视区，箭头朝向仍由 below 决定。
 * @param below true 表示浮层展开在锚点下方
 */
internal fun bubbleTopPx(
    anchorBounds: IntRect,
    windowHeightPx: Int,
    bubbleHeightPx: Int,
    below: Boolean,
    offsetYPx: Int
): Int {
    val rawY = if (below) {
        anchorBounds.bottom + offsetYPx
    } else {
        anchorBounds.top - bubbleHeightPx - offsetYPx
    }
    // 浮层高到放不下时退化为顶部贴边，防止夹取区间倒挂
    val maxY = (windowHeightPx - bubbleHeightPx).coerceAtLeast(0)
    return rawY.coerceIn(0, maxY)
}

/** 浮层最大宽度：窗口宽减去左右安全边距，长内容据此换行，免得撑出屏幕后右侧被裁 */
@Composable
internal fun bubbleMaxWidth(screenPadding: Dp): Dp =
    (LocalConfiguration.current.screenWidthDp.dp - screenPadding * 2).coerceAtLeast(0.dp)
