package com.ddw.dcomposeui.demo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.DemoScaffold
import com.ddw.dcomposeui.demo.DemoSection
import com.ddw.dcomposeui.demo.DemoValueRow
import com.ddw.dcomposeui.ext.noRippleClick
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import com.ddw.dcomposeui.widget.BubbleXGravity
import com.ddw.dcomposeui.widget.BubbleYGravity
import com.ddw.dcomposeui.widget.DBubbleMenu
import com.ddw.dcomposeui.widget.DBubbleMenuItem
import com.ddw.dcomposeui.widget.DBubbleTip

/**
 * 组件描述：DBubbleMenu / DBubbleTip 演示。菜单演示文本列表与自定义项两种重载；
 * 气泡提示演示三种水平对齐与上下方位，最右侧那个锚点故意贴边，用来看超屏自动收边。
 *
 * @param onBack 返回首页
 */
@Composable
fun DBubbleDemo(onBack: () -> Unit) {
    var menuVisible by remember { mutableStateOf(false) }
    var customMenuVisible by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf("（未选择）") }
    var tipLeft by remember { mutableStateOf(false) }
    var tipCenter by remember { mutableStateOf(false) }
    var tipEdge by remember { mutableStateOf(false) }
    var tipAbove by remember { mutableStateOf(false) }

    DemoScaffold(title = "气泡浮层", onBack = onBack) {
        // 内容比一屏高，套一层竖向滚动
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DemoSection(
                title = "DBubbleMenu 文本列表",
                desc = "传 options 字符串列表即可，浮层以按钮为锚点弹在下方并左对齐，选中后自动收起"
            ) {
                DBubbleMenu(
                    visible = menuVisible,
                    onDismiss = { menuVisible = false },
                    options = listOf("添加物料", "更新物料信息", "导出明细"),
                    onSelect = { index, option -> picked = "第 $index 项：$option" },
                    width = 130.dp
                ) {
                    OutlineButton(text = "更多操作") { menuVisible = true }
                }
                Spacer(Modifier.height(10.dp))
                DemoValueRow(label = "上次选择：", value = picked)
            }

            DemoSection(
                title = "DBubbleMenu 自定义项",
                desc = "换成接收 items 的重载，每项自己写，例如把删除做成红色；这里同时把浮层改成右对齐"
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    DBubbleMenu(
                        visible = customMenuVisible,
                        onDismiss = { customMenuVisible = false },
                        items = listOf(
                            {
                                DBubbleMenuItem(text = "编辑") {
                                    picked = "编辑"
                                    customMenuVisible = false
                                }
                            },
                            {
                                DBubbleMenuItem(text = "复制") {
                                    picked = "复制"
                                    customMenuVisible = false
                                }
                            },
                            {
                                DBubbleMenuItem(text = "删除", textColor = AppColors.AccentRed) {
                                    picked = "删除"
                                    customMenuVisible = false
                                }
                            }
                        ),
                        xGravity = BubbleXGravity.ALIGN_RIGHT,
                        width = 120.dp
                    ) {
                        OutlineButton(text = "右对齐菜单") { customMenuVisible = true }
                    }
                }
            }

            DemoSection(
                title = "DBubbleTip 水平对齐",
                desc = "箭头始终对准锚点中心。最右侧锚点贴着屏幕边，气泡会被收进 screenPadding 内，但箭头仍指着锚点"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DBubbleTip(
                        visible = tipLeft,
                        onDismiss = { tipLeft = false },
                        xGravity = BubbleXGravity.ALIGN_LEFT,
                        dismissOnClickOutside = true,
                        bubble = { TipText("左对齐的气泡提示") }
                    ) {
                        OutlineButton(text = "左对齐") { tipLeft = !tipLeft }
                    }
                    DBubbleTip(
                        visible = tipCenter,
                        onDismiss = { tipCenter = false },
                        xGravity = BubbleXGravity.CENTER,
                        dismissOnClickOutside = true,
                        bubble = { TipText("居中对齐，箭头在正中间") }
                    ) {
                        OutlineButton(text = "居中") { tipCenter = !tipCenter }
                    }
                    DBubbleTip(
                        visible = tipEdge,
                        onDismiss = { tipEdge = false },
                        xGravity = BubbleXGravity.CENTER,
                        dismissOnClickOutside = true,
                        bubble = { TipText("这条提示比较长，靠边时整体会往回收，但箭头仍指向锚点中心") }
                    ) {
                        OutlineButton(text = "贴边") { tipEdge = !tipEdge }
                    }
                }
            }

            DemoSection(
                title = "DBubbleTip 弹在上方",
                desc = "yGravity 传 ABOVE，箭头自动翻到气泡底部；锚点靠近屏幕底部时常用这个方向"
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DBubbleTip(
                        visible = tipAbove,
                        onDismiss = { tipAbove = false },
                        yGravity = BubbleYGravity.ABOVE,
                        dismissOnClickOutside = true,
                        bubble = { TipText("我在锚点上方") }
                    ) {
                        OutlineButton(text = "上方气泡") { tipAbove = !tipAbove }
                    }
                }
            }
        }
    }
}

/**
 * 组件描述：Demo 里用的描边小按钮，同时作为浮层锚点
 *
 * @param text 按钮文案
 * @param onClick 点击回调
 */
@Composable
private fun OutlineButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, AppColors.LineThree, RoundedCornerShape(4.dp))
            .background(Color.White)
            .noRippleClick(onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = AppColors.BasicOne, fontSize = 13.sp)
    }
}

/**
 * 组件描述：气泡内的白色文案
 *
 * @param text 文案
 */
@Composable
private fun TipText(text: String) {
    Text(text = text, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
}

@Preview(showBackground = true, widthDp = 375, heightDp = 760)
@Composable
private fun DBubbleDemoPreview() {
    AppComposeTheme {
        DBubbleDemo {}
    }
}
