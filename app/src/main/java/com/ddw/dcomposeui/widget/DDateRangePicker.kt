package com.ddw.dcomposeui.widget

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DDateRangePickerPreview() {
    AppComposeTheme {
        DateRangePickerContent(
            startDate = "2026-07-01",
            endDate = "2026-07-15",
            minYear = 1970,
            maxYear = 2100,
            cancelText = "取消",
            confirmText = "确定",
            onCancel = {},
            onConfirm = { _, _ -> }
        )
    }
}

/** 页签文字字号 */
private val TabTextSize = 16.sp

/**
 * 组件描述：纯 Compose 的日期范围选择底部弹窗，复刻老项目 DFDateSelect 的「开始/结束 + 年月日滚轮」交互与样式。
 * 用 Dialog 从底部弹出（无拖拽手势，下滑与点击空白都不会关闭，仅「取消/确定」按钮可关，避免误操作）；
 * 开始/结束两页切换，各自年月日三列滚轮（单位标签固定在列右侧），日数随年月联动；
 * 点确定校验结束日期不早于开始日期，日期格式 yyyy-MM-dd。
 *
 * @param visible 是否显示
 * @param startDate 初始开始日期（yyyy-MM-dd）
 * @param endDate 初始结束日期（yyyy-MM-dd）
 * @param onConfirm 确定回调，返回校验后的开始、结束日期
 * @param onDismiss 关闭回调
 * @param minYear 可选最小年份
 * @param maxYear 可选最大年份
 * @param cancelText 取消按钮文案
 * @param confirmText 确定按钮文案
 */
@Composable
fun DDateRangePicker(
    visible: Boolean,
    startDate: String,
    endDate: String,
    onConfirm: (start: String, end: String) -> Unit,
    onDismiss: () -> Unit,
    minYear: Int = 1970,
    maxYear: Int = 2100,
    cancelText: String = "取消",
    confirmText: String = "确定"
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 允许面板铺满宽度
            dismissOnClickOutside = false    // 点击空白不关闭，防止误操作
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0.35f) }
        // 全屏容器 + 底部对齐白色面板；Dialog 无拖拽手势，下滑不会关闭
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
            ) {
                DateRangePickerContent(
                    startDate = startDate,
                    endDate = endDate,
                    minYear = minYear,
                    maxYear = maxYear,
                    cancelText = cancelText,
                    confirmText = confirmText,
                    onCancel = onDismiss,
                    onConfirm = onConfirm
                )
            }
        }
    }
}

/**
 * 组件描述：日期范围选择的面板主体（不含弹窗外壳，便于 @Preview 预览）。
 * 顶部开始/结束两页切换，各年月日三列滚轮，日数随年月联动；确定时校验结束不早于开始。
 *
 * @param startDate 初始开始日期（yyyy-MM-dd）
 * @param endDate 初始结束日期（yyyy-MM-dd）
 * @param minYear 可选最小年份
 * @param maxYear 可选最大年份
 * @param cancelText 取消按钮文案
 * @param confirmText 确定按钮文案
 * @param onCancel 取消回调
 * @param onConfirm 确定回调（返回校验后的开始、结束日期）
 */
@Composable
private fun DateRangePickerContent(
    startDate: String,
    endDate: String,
    minYear: Int,
    maxYear: Int,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: (start: String, end: String) -> Unit
) {
    val context = LocalContext.current
    // 解析初始起止日期
    val initStart = remember(startDate) { parseYmd(startDate) }
    val initEnd = remember(endDate) { parseYmd(endDate) }

    // 当前页：0=开始，1=结束
    var tab by remember { mutableIntStateOf(0) }

    // 开始日期
    var startY by remember { mutableIntStateOf(initStart[0]) }
    var startM by remember { mutableIntStateOf(initStart[1]) }
    var startD by remember { mutableIntStateOf(initStart[2]) }
    // 结束日期
    var endY by remember { mutableIntStateOf(initEnd[0]) }
    var endM by remember { mutableIntStateOf(initEnd[1]) }
    var endD by remember { mutableIntStateOf(initEnd[2]) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部：开始 / 结束 两页切换，标题为「日期 + 开始/结束」同行显示
        Row(modifier = Modifier.fillMaxWidth()) {
            DateTab(
                dateText = fmtYmd(startY, startM, startD),
                label = "开始",
                selected = tab == 0,
                onClick = { tab = 0 }
            )
            DateTab(
                dateText = fmtYmd(endY, endM, endD),
                label = "结束",
                selected = tab == 1,
                onClick = { tab = 1 }
            )
        }

        // 年月日滚轮（按当前页显示开始/结束）
        if (tab == 0) {
            WheelDatePicker(
                year = startY, month = startM, day = startD,
                minYear = minYear, maxYear = maxYear,
                onChange = { y, m, d -> startY = y; startM = m; startD = d }
            )
        } else {
            WheelDatePicker(
                year = endY, month = endM, day = endD,
                minYear = minYear, maxYear = maxYear,
                onChange = { y, m, d -> endY = y; endM = m; endD = d }
            )
        }

        // 底部：取消 / 确定
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateActionButton(
                text = cancelText,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onCancel
            )
            Spacer(Modifier.width(8.dp))
            DateActionButton(
                text = confirmText,
                primary = true,
                modifier = Modifier.weight(2f),
                onClick = {
                    val start = fmtYmd(startY, startM, startD)
                    val end = fmtYmd(endY, endM, endD)
                    // yyyy-MM-dd 定长，字典序即时间序
                    if (end < start) {
                        Toast.makeText(context, "结束时间不能小于开始时间", Toast.LENGTH_SHORT).show()
                        return@DateActionButton
                    }
                    onConfirm(start, end)
                }
            )
        }
    }
}

/**
 * 组件描述：开始/结束页签，标题为「日期 开始/结束」同行显示，选中态主题色加粗 + 底部整宽指示器。
 *
 * @param dateText 当前所选日期文本
 * @param label 页签标签（开始/结束）
 * @param selected 是否选中
 * @param onClick 点击回调
 */
@Composable
private fun RowScope.DateTab(
    dateText: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$dateText $label",
            color = if (selected) AppColors.AccentPrimary else AppColors.BasicTwo,
            fontSize = TabTextSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) AppColors.AccentPrimary else Color.Transparent)
        )
    }
}

/**
 * 组件描述：年月日三列滚轮，单位标签「年/月/日」固定在每列右侧并垂直居中对齐选中行。
 * 年列更宽（weight 1.2），月/日列 weight 1；日数随年/月联动（自动取该月最大天数并钳制）。
 *
 * @param year 年
 * @param month 月
 * @param day 日
 * @param minYear 最小年份
 * @param maxYear 最大年份
 * @param onChange 变化回调（返回联动钳制后的年月日）
 */
@Composable
private fun WheelDatePicker(
    year: Int,
    month: Int,
    day: Int,
    minYear: Int,
    maxYear: Int,
    onChange: (Int, Int, Int) -> Unit
) {
    val years = remember(minYear, maxYear) { (minYear..maxYear).toList() }
    val months = remember { (1..12).toList() }
    val maxDay = daysInMonth(year, month)
    val days = remember(maxDay) { (1..maxDay).toList() }
    val safeDay = day.coerceIn(1, maxDay)
    val wheelHeight = WheelItemHeight * WheelVisibleCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WheelHorizontalPadding)
    ) {
        // 年
        DWheelColumn(
            items = years,
            selected = year,
            unit = "年",
            textSize = WheelYearTextSize,
            wheelHeight = wheelHeight,
            columnWeight = 1.2f,
            onSelected = { y -> onChange(y, month, day.coerceAtMost(daysInMonth(y, month))) }
        )
        // 月
        DWheelColumn(
            items = months,
            selected = month,
            unit = "月",
            textSize = WheelDayTextSize,
            wheelHeight = wheelHeight,
            columnWeight = 1f,
            onSelected = { m -> onChange(year, m, day.coerceAtMost(daysInMonth(year, m))) }
        )
        // 日
        DWheelColumn(
            items = days,
            selected = safeDay,
            unit = "日",
            textSize = WheelDayTextSize,
            wheelHeight = wheelHeight,
            columnWeight = 1f,
            onSelected = { d -> onChange(year, month, d) }
        )
    }
}

/** 年月日格式化为 yyyy-MM-dd */
private fun fmtYmd(y: Int, m: Int, d: Int): String = "%04d-%02d-%02d".format(y, m, d)

/** 解析 yyyy-MM-dd 为 [年, 月, 日]，非法时回退到默认 */
private fun parseYmd(s: String): IntArray {
    val p = s.split("-")
    val y = p.getOrNull(0)?.toIntOrNull() ?: 2024
    val m = p.getOrNull(1)?.toIntOrNull() ?: 1
    val d = p.getOrNull(2)?.toIntOrNull() ?: 1
    return intArrayOf(y, m, d)
}
