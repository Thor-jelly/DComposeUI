package com.ddw.dcomposeui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ddw.dcomposeui.ext.noRippleClick
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme

/**
 * 组件描述：选项浮层的面板外观预览。Popup 的内容不参与 Preview 渲染，故这里直接预览面板；
 * 前两项为标准文本项，末项演示自定义样式。
 */
@Preview(widthDp = 200, heightDp = 240, showBackground = true, backgroundColor = 0xFFF2F3F5)
@Composable
private fun DBubbleMenuPreview() {
    AppComposeTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DBubbleMenuPanel(
                items = listOf<@Composable () -> Unit>(
                    { DBubbleMenuItem(text = "添加物料") {} },
                    { DBubbleMenuItem(text = "更新物料信息") {} },
                    { DBubbleMenuItem(text = "删除", textColor = AppColors.AccentRed) {} }
                ),
                width = 114.dp,
                cornerRadius = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                elevation = 8.dp,
                shadowColor = Color(0x99C7CAD1),
                background = Color.White,
                dividerColor = AppColors.DividerColor
            )
        }
    }
}

/**
 * 组件描述：选项浮层（Compose 版，锚定到触发控件弹出选项列表）。本重载接收文本列表，按统一样式渲染。
 * 需要个性化每一项（图标、红色警示文案、禁用态等）时改用接收 items 的重载；
 * 若只是展示引导提示气泡，用 [DBubbleTip]。
 * @param visible 是否显示
 * @param onDismiss 关闭回调（点击外部/返回键/选中项后触发）
 * @param options 选项文案
 * @param onSelect 选中回调（回传下标与文案）
 * @param autoDismissOnSelect 选中后是否自动收起。需要按条件决定关不关（如校验不通过要留着浮层）
 * 时传 false，改由 onSelect 内自行调用 onDismiss
 * @param width 面板宽度，null 表示按最长选项自适应
 * @param cornerRadius 面板圆角，默认仅上方带圆角以与触发控件衔接
 * @param dividerColor 选项间分隔线颜色，透明则不显示分隔线
 * @param screenPadding 浮层距屏幕左右的最小安全边距，同时决定面板最大宽度（窗口宽 - 2 倍该值）
 * @param anchor 触发控件（浮层以它为锚点定位）
 */
@Composable
fun DBubbleMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    options: List<String>,
    onSelect: (index: Int, option: String) -> Unit,
    modifier: Modifier = Modifier,
    autoDismissOnSelect: Boolean = true,
    yGravity: BubbleYGravity = BubbleYGravity.BELOW,
    xGravity: BubbleXGravity = BubbleXGravity.ALIGN_LEFT,
    width: Dp? = null,
    cornerRadius: RoundedCornerShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    elevation: Dp = 8.dp,
    shadowColor: Color = Color(0x99C7CAD1),
    background: Color = Color.White,
    dividerColor: Color = AppColors.DividerColor,
    textColor: Color = AppColors.BasicOne,
    fontSize: TextUnit = 14.sp,
    itemPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    offsetX: Dp = 0.dp,
    offsetY: Dp = 4.dp,
    screenPadding: Dp = 12.dp,
    anchor: @Composable BoxScope.() -> Unit
) {
    val textItems: List<@Composable () -> Unit> = options.mapIndexed { index, option ->
        {
            DBubbleMenuItem(
                text = option,
                textColor = textColor,
                fontSize = fontSize,
                contentPadding = itemPadding
            ) {
                onSelect(index, option)
                if (autoDismissOnSelect) onDismiss()
            }
        }
    }
    DBubbleMenu(
        visible = visible,
        onDismiss = onDismiss,
        items = textItems,
        modifier = modifier,
        yGravity = yGravity,
        xGravity = xGravity,
        width = width,
        cornerRadius = cornerRadius,
        elevation = elevation,
        shadowColor = shadowColor,
        background = background,
        dividerColor = dividerColor,
        offsetX = offsetX,
        offsetY = offsetY,
        screenPadding = screenPadding,
        anchor = anchor
    )
}

/**
 * 组件描述：选项浮层（Compose 版，锚定到触发控件弹出选项列表）。本重载接收自定义内容数组，
 * 每项内容与点击行为都由调用方决定，浮层只负责定位、白底圆角投影与分隔线。
 * 注意：本重载不会因点击某项而自动收起（浮层无从判断自定义内容里哪次点击算「选中」），
 * 各项的点击回调需自行调用 onDismiss；若只是文本选项，用接收 options 的重载可自动收起。
 * 超出窗口时自动收进安全区（横向收边距、竖向收回可视区），展开方位不翻转。
 * @param visible 是否显示
 * @param onDismiss 关闭回调（点击外部/返回键触发）
 * @param items 自定义选项内容
 * @param width 面板宽度，null 表示按内容自适应
 * @param cornerRadius 面板圆角，默认仅上方带圆角以与触发控件衔接
 * @param dividerColor 选项间分隔线颜色，透明则不显示分隔线
 * @param screenPadding 浮层距屏幕左右的最小安全边距，同时决定面板最大宽度（窗口宽 - 2 倍该值）
 * @param anchor 触发控件（浮层以它为锚点定位）
 */
@Composable
fun DBubbleMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    yGravity: BubbleYGravity = BubbleYGravity.BELOW,
    xGravity: BubbleXGravity = BubbleXGravity.ALIGN_LEFT,
    width: Dp? = null,
    cornerRadius: RoundedCornerShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    elevation: Dp = 8.dp,
    shadowColor: Color = Color(0x99C7CAD1),
    background: Color = Color.White,
    dividerColor: Color = AppColors.DividerColor,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 4.dp,
    screenPadding: Dp = 12.dp,
    anchor: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        anchor()
        if (visible && items.isNotEmpty()) {
            val density = LocalDensity.current
            val offsetXPx = with(density) { offsetX.roundToPx() }
            val offsetYPx = with(density) { offsetY.roundToPx() }
            val screenPaddingPx = with(density) { screenPadding.roundToPx() }
            val below = yGravity == BubbleYGravity.BELOW
            val maxPanelWidth = bubbleMaxWidth(screenPadding)

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

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = onDismiss,
                // focusable：抢占焦点，点击外部与返回键都能关闭
                properties = PopupProperties(focusable = true)
            ) {
                DBubbleMenuPanel(
                    items = items,
                    width = width,
                    maxWidth = maxPanelWidth,
                    cornerRadius = cornerRadius,
                    elevation = elevation,
                    shadowColor = shadowColor,
                    background = background,
                    dividerColor = dividerColor
                )
            }
        }
    }
}

/**
 * 组件描述：选项浮层的面板容器（白底 + 圆角 + 投影，各项之间加分隔线）。
 * @param width null 表示按内容自适应宽度
 * @param maxWidth 宽度上限，Unspecified 表示不限制；自适应与固定宽度都会被它收住
 * @param dividerColor 透明时不画分隔线
 */
@Composable
private fun DBubbleMenuPanel(
    items: List<@Composable () -> Unit>,
    width: Dp?,
    cornerRadius: RoundedCornerShape,
    elevation: Dp,
    shadowColor: Color,
    background: Color,
    dividerColor: Color,
    maxWidth: Dp = Dp.Unspecified
) {
    Column(
        modifier = Modifier
            // 上限放在宽度之前，长选项文案才会换行而不是把面板撑出屏幕
            .widthIn(max = maxWidth)
            // 自适应时取最宽选项的固有宽度，好让各项都能 fillMaxWidth 撑满、文案对齐一致
            .then(if (width != null) Modifier.width(width) else Modifier.width(IntrinsicSize.Max))
            .shadow(
                elevation = elevation,
                shape = cornerRadius,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(background, cornerRadius)
    ) {
        items.forEachIndexed { index, item ->
            item()
            if (dividerColor != Color.Transparent && index < items.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }
        }
    }
}

/** 组件描述：选项浮层的文本项（撑满面板宽度、整行可点、文案居中）。传 items 自定义内容时也可直接复用它 */
@Composable
fun DBubbleMenuItem(
    text: String,
    textColor: Color = AppColors.BasicOne,
    fontSize: TextUnit = 14.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
