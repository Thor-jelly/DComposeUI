package com.ddw.dcomposeui.demo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.DemoScaffold
import com.ddw.dcomposeui.demo.DemoSection
import com.ddw.dcomposeui.demo.DemoValueRow
import com.ddw.dcomposeui.ext.noRippleClick
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import com.ddw.dcomposeui.widget.DDateRangePicker
import com.ddw.dcomposeui.widget.DDateTimePicker
import com.ddw.dcomposeui.widget.DWheelPicker

/**
 * 组件描述：DWheelPicker 与两个日期弹窗的演示。上面是裸滚轮，能看清吸附与渐隐缩放；
 * 下面两个按钮分别拉起「日期时间六列联动」和「日期区间双页」弹窗。
 *
 * @param onBack 返回首页
 */
@Composable
fun DWheelPickerDemo(onBack: () -> Unit) {
    var hour by remember { mutableIntStateOf(9) }
    var showDateTime by remember { mutableStateOf(false) }
    var showDateRange by remember { mutableStateOf(false) }
    var dateTimeText by remember { mutableStateOf("2026-08-17 09:30:00") }
    var startDate by remember { mutableStateOf("2026-08-01") }
    var endDate by remember { mutableStateOf("2026-08-17") }

    DemoScaffold(title = "滚轮与日期选择", onBack = onBack) {
        // 内容比一屏高，套一层竖向滚动
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DemoSection(
                title = "DWheelPicker 裸滚轮",
                desc = "LazyColumn 上下各留 4 行空白，配 SnapFlingBehavior 吸附到整行；每项按到中心的距离算 alpha 与 scale，滚动时连续变化"
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DWheelPicker(
                        items = (0..23).toList(),
                        selected = hour,
                        textSize = 18.sp,
                        onSelected = { hour = it },
                        modifier = Modifier.width(90.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                DemoValueRow(label = "选中小时：", value = "$hour 时")
            }

            DemoSection(
                title = "DDateTimePicker",
                desc = "年月日时分秒六列同行，日列的天数随年月变化（切到 2 月能看到 29/28 天的切换）"
            ) {
                DemoValueRow(label = "已选：", value = dateTimeText)
                Spacer(Modifier.height(10.dp))
                DemoButton(text = "选择日期时间") { showDateTime = true }
            }

            DemoSection(
                title = "DDateRangePicker",
                desc = "开始 / 结束两个页签共用一套滚轮，确定时校验结束不早于开始（yyyy-MM-dd 定长，字典序即时间序）"
            ) {
                DemoValueRow(label = "已选：", value = "$startDate 至 $endDate")
                Spacer(Modifier.height(10.dp))
                DemoButton(text = "选择日期区间") { showDateRange = true }
            }
        }
    }

    DDateTimePicker(
        visible = showDateTime,
        dateTime = dateTimeText,
        title = "选择日期时间",
        onConfirm = {
            dateTimeText = it
            showDateTime = false
        },
        onDismiss = { showDateTime = false }
    )

    DDateRangePicker(
        visible = showDateRange,
        startDate = startDate,
        endDate = endDate,
        onConfirm = { start, end ->
            startDate = start
            endDate = end
            showDateRange = false
        },
        onDismiss = { showDateRange = false }
    )
}

/**
 * 组件描述：Demo 里用的主色按钮
 *
 * @param text 按钮文案
 * @param onClick 点击回调
 */
@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.AccentPrimary)
            .noRippleClick(onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 760)
@Composable
private fun DWheelPickerDemoPreview() {
    AppComposeTheme {
        DWheelPickerDemo {}
    }
}
