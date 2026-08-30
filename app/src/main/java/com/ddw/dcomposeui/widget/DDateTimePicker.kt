package com.ddw.dcomposeui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import java.util.Calendar

@Preview(showBackground = true, widthDp = 375, heightDp = 500)
@Composable
private fun DDateTimePickerPreview() {
    AppComposeTheme {
        DateTimePickerContent(
            dateTime = "2026-07-22 12:00:00",
            minYear = 1970,
            maxYear = 2100,
            cancelText = "取消",
            confirmText = "确定",
            onCancel = {},
            onConfirm = {}
        )
    }
}

/**
 * 组件描述：日期时间选择底部弹窗，年月日一组、时分秒一组，共 6 列滚轮（滚轮手感与 [DDateRangePicker] 一致）。
 * 用 Dialog 从底部弹出，点空白与下滑都不关闭，仅「取消/确定」可关，避免误操作。
 * @param visible 是否显示
 * @param dateTime 初始日期时间（yyyy-MM-dd HH:mm:ss，非法时回退到当前时间）
 * @param title 顶部标题
 * @param onConfirm 确定回调，返回 yyyy-MM-dd HH:mm:ss
 * @param onDismiss 关闭回调
 * @param minYear 可选最小年份
 * @param maxYear 可选最大年份
 * @param cancelText 取消按钮文案
 * @param confirmText 确定按钮文案
 */
@Composable
fun DDateTimePicker(
    visible: Boolean,
    dateTime: String,
    title: String = "",
    onConfirm: (String) -> Unit,
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
                if (title.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = AppColors.BasicOne,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                DateTimePickerContent(
                    dateTime = dateTime,
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
 * 组件描述：日期时间选择的面板主体（不含弹窗外壳，便于预览）。
 * 年月日一行三列 + 时分秒一行三列，日数随年月联动并钳制。
 * @param dateTime 初始日期时间（yyyy-MM-dd HH:mm:ss）
 * @param minYear 可选最小年份
 * @param maxYear 可选最大年份
 * @param cancelText 取消按钮文案
 * @param confirmText 确定按钮文案
 * @param onCancel 取消回调
 * @param onConfirm 确定回调
 */
@Composable
private fun DateTimePickerContent(
    dateTime: String,
    minYear: Int,
    maxYear: Int,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val init = remember(dateTime) { parseDateTime(dateTime) }
    var year by remember { mutableIntStateOf(init[0]) }
    var month by remember { mutableIntStateOf(init[1]) }
    var day by remember { mutableIntStateOf(init[2]) }
    var hour by remember { mutableIntStateOf(init[3]) }
    var minute by remember { mutableIntStateOf(init[4]) }
    var second by remember { mutableIntStateOf(init[5]) }

    val years = remember(minYear, maxYear) { (minYear..maxYear).toList() }
    val months = remember { (1..12).toList() }
    val maxDay = daysInMonth(year, month)
    val days = remember(maxDay) { (1..maxDay).toList() }
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }
    // 时、分、秒共用 0~59，分与秒可直接复用同一份数据
    val safeDay = day.coerceIn(1, maxDay)
    val wheelHeight = WheelItemHeight * WheelVisibleCount

    Column(modifier = Modifier.fillMaxWidth()) {
        // 当前选中的完整日期时间，滚动时实时回显
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fmtDateTime(year, month, safeDay, hour, minute, second),
                color = AppColors.AccentBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        // 六列一行：年月日时分秒。列多且宽度有限，故不留左右外边距、单位标签用小号字
        Row(modifier = Modifier.fillMaxWidth()) {
            DWheelColumn(
                items = years,
                selected = year,
                unit = "年",
                textSize = WheelDateTimeTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1.3f,
                unitTextSize = WheelDateTimeUnitTextSize,
                unitMargin = WheelDateTimeUnitMargin,
                onSelected = { y ->
                    year = y
                    day = day.coerceAtMost(daysInMonth(y, month))
                }
            )
            DWheelColumn(
                items = months,
                selected = month,
                unit = "月",
                textSize = WheelDateTimeTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                unitTextSize = WheelDateTimeUnitTextSize,
                unitMargin = WheelDateTimeUnitMargin,
                onSelected = { m ->
                    month = m
                    day = day.coerceAtMost(daysInMonth(year, m))
                }
            )
            DWheelColumn(
                items = days,
                selected = safeDay,
                unit = "日",
                textSize = WheelDateTimeTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                unitTextSize = WheelDateTimeUnitTextSize,
                unitMargin = WheelDateTimeUnitMargin,
                onSelected = { day = it }
            )
            DWheelColumn(
                items = hours,
                selected = hour,
                unit = "时",
                textSize = WheelDateTimeTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                unitTextSize = WheelDateTimeUnitTextSize,
                unitMargin = WheelDateTimeUnitMargin,
                onSelected = { hour = it }
            )
            DWheelColumn(
                items = minutes,
                selected = minute,
                unit = "分",
                textSize = WheelDateTimeTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                unitTextSize = WheelDateTimeUnitTextSize,
                unitMargin = WheelDateTimeUnitMargin,
                onSelected = { minute = it }
            )
            DWheelColumn(
                items = minutes,
                selected = second,
                unit = "秒",
                textSize = WheelDateTimeTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                unitTextSize = WheelDateTimeUnitTextSize,
                unitMargin = WheelDateTimeUnitMargin,
                onSelected = { second = it }
            )
        }
        // 底部：取消 / 确定
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            DateActionButton(
                text = cancelText,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onCancel
            )
            Spacer(Modifier.width(12.dp))
            DateActionButton(
                text = confirmText,
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = { onConfirm(fmtDateTime(year, month, safeDay, hour, minute, second)) }
            )
        }
    }
}

/** 格式化为 yyyy-MM-dd HH:mm:ss */
private fun fmtDateTime(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(y, mo, d, h, mi, s)

/** 解析 yyyy-MM-dd HH:mm:ss 为 [年, 月, 日, 时, 分, 秒]，缺失或非法的部分回退到当前时间 */
private fun parseDateTime(s: String): IntArray {
    val now = Calendar.getInstance()
    val fallback = intArrayOf(
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH),
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        now.get(Calendar.SECOND)
    )
    val parts = s.trim().split(" ")
    val ymd = parts.getOrNull(0)?.split("-").orEmpty()
    val hms = parts.getOrNull(1)?.split(":").orEmpty()
    return intArrayOf(
        ymd.getOrNull(0)?.toIntOrNull() ?: fallback[0],
        ymd.getOrNull(1)?.toIntOrNull() ?: fallback[1],
        ymd.getOrNull(2)?.toIntOrNull() ?: fallback[2],
        hms.getOrNull(0)?.toIntOrNull() ?: fallback[3],
        hms.getOrNull(1)?.toIntOrNull() ?: fallback[4],
        hms.getOrNull(2)?.toIntOrNull() ?: fallback[5]
    )
}

/** 当前时间的 yyyy-MM-dd HH:mm:ss 文案（领料时间等字段的默认值） */
fun nowDateTimeText(): String {
    val c = Calendar.getInstance()
    return fmtDateTime(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND)
    )
}
