package com.ddw.dcomposeui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ddw.dcomposeui.theme.AppComposeTheme

/**
 * 组件描述：带箭头的气泡提示浮层（Compose 版，锚定到目标控件上/下方，箭头指向锚点）。
 * 适合引导语、说明气泡等纯提示场景；若要弹选项菜单请用 [DBubbleMenu]。
 * 把锚点放进 anchor 槽、气泡内容放进 bubble 槽，visible 控制显隐；气泡自动定位到锚点上/下方，
 * 箭头对准锚点中心，超出窗口时自动收进安全区（横向收边距、竖向收回可视区，方位不翻转）。
 * 气泡宽度上限为窗口宽减左右安全边距，长文案会自动换行，调用方不必自己算死宽度。
 * 背景与箭头同色，内容只需写内部元素。
 * @param visible 是否显示气泡
 * @param onDismiss 关闭回调（触发时机取决于 dismissOnClickOutside 与 dismissOnBackPress）
 * @param yGravity 气泡在锚点上方还是下方
 * @param xGravity 气泡与锚点的水平对齐
 * @param bubbleColor 气泡背景色（含箭头）
 * @param cornerRadius 气泡圆角
 * @param arrowWidth 箭头底宽
 * @param arrowHeight 箭头高
 * @param offsetX 额外横向偏移
 * @param offsetY 锚点与气泡的间距
 * @param screenPadding 气泡距屏幕左右的最小安全边距，同时决定气泡最大宽度（窗口宽 - 2 倍该值）
 * @param contentPadding 气泡内容内边距
 * @param dismissOnClickOutside 点击气泡外部是否关闭
 * @param dismissOnBackPress 返回键是否关闭。开启需抢占焦点，会影响沉浸式全屏与输入法，故默认关闭
 * @param bubble 气泡内容
 * @param anchor 锚点内容
 */
@Composable
fun DBubbleTip(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    yGravity: BubbleYGravity = BubbleYGravity.BELOW,
    xGravity: BubbleXGravity = BubbleXGravity.CENTER,
    bubbleColor: Color = Color.Black.copy(alpha = 0.75f),
    cornerRadius: Dp = 4.dp,
    arrowWidth: Dp = 16.dp,
    arrowHeight: Dp = 8.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 4.dp,
    screenPadding: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = false,
    bubble: @Composable () -> Unit,
    anchor: @Composable BoxScope.() -> Unit
) {
    // 锚点边界（窗口坐标系），在布局完成回调里回填；箭头据此对准锚点中心
    var anchorBoundsInWindow by remember { mutableStateOf(IntRect.Zero) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            anchorBoundsInWindow = IntRect(coordinates.positionInWindow().round(), coordinates.size)
        }
    ) {
        anchor()
        // 等锚点坐标回填后再弹：否则首帧箭头会按 (0,0) 算落到最左，回调后才跳回锚点中心
        if (visible && anchorBoundsInWindow != IntRect.Zero) {
            val density = LocalDensity.current
            val offsetXPx = with(density) { offsetX.roundToPx() }
            val offsetYPx = with(density) { offsetY.roundToPx() }
            val screenPaddingPx = with(density) { screenPadding.roundToPx() }
            val arrowWidthPx = with(density) { arrowWidth.roundToPx() }
            val cornerPx = with(density) { cornerRadius.roundToPx() }
            val below = yGravity == BubbleYGravity.BELOW
            val maxBubbleWidth = bubbleMaxWidth(screenPadding)

            // 定位器为纯计算：只根据入参算出窗口位置，不读写任何 Compose 状态
            val positionProvider = remember(below, xGravity, offsetXPx, offsetYPx, screenPaddingPx) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val x = bubbleLeftPx(
                            anchorBounds = anchorBounds,
                            windowWidthPx = windowSize.width,
                            bubbleWidthPx = popupContentSize.width,
                            xGravity = xGravity,
                            offsetXPx = offsetXPx,
                            screenPaddingPx = screenPaddingPx
                        )
                        val y = bubbleTopPx(
                            anchorBounds = anchorBounds,
                            windowHeightPx = windowSize.height,
                            bubbleHeightPx = popupContentSize.height,
                            below = below,
                            offsetYPx = offsetYPx
                        )
                        return IntOffset(x, y)
                    }
                }
            }

            // 箭头中心 x：用与定位器相同的算式反推气泡左边缘，再取锚点中心的相对位置
            val arrowCenterXPx: (Int, Int) -> Int =
                remember(xGravity, offsetXPx, screenPaddingPx, arrowWidthPx, cornerPx) {
                    { bubbleWidthPx, windowWidthPx ->
                        val left = bubbleLeftPx(
                            anchorBounds = anchorBoundsInWindow,
                            windowWidthPx = windowWidthPx,
                            bubbleWidthPx = bubbleWidthPx,
                            xGravity = xGravity,
                            offsetXPx = offsetXPx,
                            screenPaddingPx = screenPaddingPx
                        )
                        // 夹在圆角与边缘之间，避免箭头露出圆角
                        val minArrow = cornerPx + arrowWidthPx / 2
                        val maxArrow = (bubbleWidthPx - cornerPx - arrowWidthPx / 2).coerceAtLeast(minArrow)
                        (anchorBoundsInWindow.center.x - left).coerceIn(minArrow, maxArrow)
                    }
                }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = onDismiss,
                properties = PopupProperties(
                    // 返回键要收到按键事件必须抢占焦点，点击外部与焦点无关，故两者分开控制
                    focusable = dismissOnBackPress,
                    dismissOnClickOutside = dismissOnClickOutside,
                    dismissOnBackPress = dismissOnBackPress
                )
            ) {
                BubbleWithArrow(
                    up = below,
                    color = bubbleColor,
                    arrowWidth = arrowWidth,
                    arrowHeight = arrowHeight,
                    arrowCenterXPx = arrowCenterXPx
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = maxBubbleWidth)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(bubbleColor)
                            .padding(contentPadding)
                    ) {
                        bubble()
                    }
                }
            }
        }
    }
}

/**
 * 组件描述：气泡本体与指向锚点的箭头。
 * 箭头横向位置在放置阶段按气泡实际宽度算出，不依赖定位器回填状态。
 * @param up 箭头是否朝上（true 表示气泡在锚点下方）
 * @param arrowCenterXPx 箭头中心 x 的算法，入参依次为气泡宽度、窗口可用宽度（px）
 */
@Composable
private fun BubbleWithArrow(
    up: Boolean,
    color: Color,
    arrowWidth: Dp,
    arrowHeight: Dp,
    arrowCenterXPx: (bubbleWidthPx: Int, windowWidthPx: Int) -> Int,
    bubble: @Composable () -> Unit
) {
    Layout(
        content = {
            BubbleArrow(up = up, color = color, width = arrowWidth, height = arrowHeight)
            bubble()
        }
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val arrowPlaceable = measurables[0].measure(looseConstraints)
        val bubblePlaceable = measurables[1].measure(looseConstraints)
        val width = maxOf(arrowPlaceable.width, bubblePlaceable.width)
        // Popup 窗口按内容自适应，其测量上限即窗口可用宽度，与定位器拿到的 windowSize.width 一致
        val windowWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else width
        layout(width, arrowPlaceable.height + bubblePlaceable.height) {
            val arrowX = arrowCenterXPx(width, windowWidthPx) - arrowPlaceable.width / 2
            if (up) {
                arrowPlaceable.place(arrowX, 0)
                bubblePlaceable.place(0, arrowPlaceable.height)
            } else {
                bubblePlaceable.place(0, 0)
                arrowPlaceable.place(arrowX, bubblePlaceable.height)
            }
        }
    }
}

/** 气泡箭头三角：up=true 朝上（气泡在锚点下方），否则朝下 */
@Composable
private fun BubbleArrow(up: Boolean, color: Color, width: Dp, height: Dp) {
    Canvas(modifier = Modifier.size(width = width, height = height)) {
        val arrow = Path().apply {
            if (up) {
                moveTo(0f, size.height)
                lineTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
            }
            close()
        }
        drawPath(arrow, color)
    }
}

/**
 * 组件描述：气泡提示外观预览（Popup 无法在 Preview 渲染，故单独预览气泡容器样式）
 */
@Preview(showBackground = true, backgroundColor = 0xFFEFEFEF)
@Composable
private fun DBubbleTipPreview() {
    val bubbleColor = Color.Black.copy(alpha = 0.75f)
    AppComposeTheme {
        BubbleWithArrow(
            up = true,
            color = bubbleColor,
            arrowWidth = 16.dp,
            arrowHeight = 8.dp,
            arrowCenterXPx = { _, _ -> 60 }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "点击箭头或左右滑动卡片查看更多环节", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
