package com.ddw.dcomposeui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * 滚轮选择的共用部件：日期区间选择（DDateRangePicker）与日期时间选择（DDateTimePicker）共用，
 * 保持两处滚轮手感与视觉完全一致。仅供本 module 内的选择器使用，故为 internal。
 */

/** 滚轮单行高度 */
internal val WheelItemHeight = 30.dp

/** 滚轮可见行数（奇数，中间行为选中项） */
internal const val WheelVisibleCount = 9

/** 年份列字号 */
internal val WheelYearTextSize = 18.sp

/** 月/日等列字号 */
internal val WheelDayTextSize = 19.sp

/** 单位标签字号 */
internal val WheelUnitTextSize = 14.sp

/** 滚轮区左右内边距 */
internal val WheelHorizontalPadding = 30.dp

/** 单位标签左右间距 */
internal val WheelUnitMargin = 8.dp

/** 日期时间选择（六列同行）的数字字号，比三列时小一号以容纳更多列 */
internal val WheelDateTimeTextSize = 15.sp

/** 日期时间选择（六列同行）的单位标签字号 */
internal val WheelDateTimeUnitTextSize = 11.sp

/** 日期时间选择（六列同行）的单位标签左右间距 */
internal val WheelDateTimeUnitMargin = 1.dp

/**
 * 组件描述：单列滚轮 + 右侧单位标签。数字滚轮占满单位左侧空间，单位垂直居中对齐选中行。
 *
 * @param items 数据项
 * @param selected 当前选中值
 * @param unit 单位标签（年/月/日/时/分/秒）
 * @param textSize 数字字号
 * @param wheelHeight 滚轮高度
 * @param columnWeight 该列在整行中的权重
 * @param unitTextSize 单位标签字号（列多时可传更小值）
 * @param unitMargin 单位标签左右间距（列多时可传更小值）
 * @param onSelected 选中回调
 */
@Composable
fun RowScope.DWheelColumn(
    items: List<Int>,
    selected: Int,
    unit: String,
    textSize: TextUnit,
    wheelHeight: Dp,
    columnWeight: Float,
    unitTextSize: TextUnit = WheelUnitTextSize,
    unitMargin: Dp = WheelUnitMargin,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .weight(columnWeight)
            .height(wheelHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DWheelPicker(
            items = items,
            selected = selected,
            textSize = textSize,
            onSelected = onSelected,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = unit,
            color = AppColors.BasicTwo,
            fontSize = unitTextSize,
            modifier = Modifier.padding(horizontal = unitMargin)
        )
    }
}

/**
 * 组件描述：通用整数滚轮。中间行为选中项（蓝色），越远离中心越透明越小（大气渐隐 + 卷曲缩放）；
 * 滚动吸附到整行。仅绘制数字，单位标签由外层负责。
 *
 * @param items 数据项
 * @param selected 当前选中值
 * @param textSize 数字字号
 * @param onSelected 选中回调（中心项每变一行即回调，不等滚动停止）
 * @param modifier 外部布局修饰
 */
@Composable
fun DWheelPicker(
    items: List<Int>,
    selected: Int,
    textSize: TextUnit,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { WheelItemHeight.toPx() }
    val half = WheelVisibleCount / 2

    val initialIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 连续中心浮点索引：顶部留白 half 行，故 offset=0 时中心即 firstVisibleItemIndex
    val centerFloat by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset / itemHeightPx
        }
    }
    // 吸附后的中心整数索引
    val centerIndex by remember {
        derivedStateOf { centerFloat.roundToInt().coerceIn(0, items.lastIndex) }
    }

    // 中心项一变就回调，不等滚动停止：惯性滑动期间用户点确定时弹窗立刻关闭，
    // 若等停止再回调，那次回调永远不会发生，调用方拿到的还是滑动前的旧值
    LaunchedEffect(centerIndex, items) {
        if (items.isEmpty()) return@LaunchedEffect
        val value = items[centerIndex]
        if (value != selected) onSelected(value)
    }

    // 外部选中变化（如天数联动被钳制）：吸附滚动到对应项
    LaunchedEffect(selected, items.size) {
        val idx = items.indexOf(selected)
        if (idx >= 0 && idx != centerIndex && !listState.isScrollInProgress) {
            listState.scrollToItem(idx)
        }
    }

    Box(
        modifier = modifier.height(WheelItemHeight * WheelVisibleCount),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            // 上下留白 half 行，使首/尾项也能滚到中心
            contentPadding = PaddingValues(vertical = WheelItemHeight * half),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, value ->
                // 距中心的连续行距（跟随滚动平滑变化）
                val distance = index - centerFloat
                val ratio = (abs(distance) / half).coerceIn(0f, 1f)
                val isCenter = abs(distance) < 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight)
                        .graphicsLayer {
                            // 大气渐隐 + 卷曲缩放：越远越淡越小
                            alpha = 1f - ratio * 0.6f
                            val s = 1f - ratio * 0.35f
                            scaleX = s
                            scaleY = s
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        color = if (isCenter) AppColors.AccentBlue else AppColors.BasicThree,
                        fontSize = textSize,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
        // 中心选中区上下分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
        ) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.TopCenter),
                color = AppColors.LineTwo,
                thickness = 1.dp
            )
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = AppColors.LineTwo,
                thickness = 1.dp
            )
        }
    }
}

/**
 * 组件描述：日期弹窗底部操作按钮，复刻原生样式（主按钮=主题色实底白字，次按钮=白底浅灰描边深灰字）。
 *
 * @param text 按钮文案
 * @param primary 是否主按钮（true=主题色实底白字，false=白底浅灰边深灰字）
 * @param modifier 布局修饰（一般传 weight 控制占比）
 * @param onClick 点击回调
 */
@Composable
internal fun DateActionButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (primary) {
        Box(
            modifier = modifier
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.AccentPrimary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    } else {
        Box(
            modifier = modifier
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, AppColors.LineOne, RoundedCornerShape(2.dp))
                .background(Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = AppColors.BasicOne, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** 计算某年某月的最大天数（含闰年） */
internal fun daysInMonth(year: Int, month: Int): Int {
    val c = Calendar.getInstance()
    c.clear()
    c.set(Calendar.YEAR, year)
    c.set(Calendar.MONTH, month - 1)
    return c.getActualMaximum(Calendar.DAY_OF_MONTH)
}

@Preview(name = "单列滚轮", showBackground = true, widthDp = 120, heightDp = 300)
@Composable
private fun DWheelPickerPreview() {
    var value by remember { mutableIntStateOf(2026) }
    AppComposeTheme {
        DWheelPicker(
            items = (2020..2035).toList(),
            selected = value,
            textSize = WheelYearTextSize,
            onSelected = { value = it }
        )
    }
}

@Preview(name = "带单位的三列", showBackground = true, widthDp = 320, heightDp = 300)
@Composable
private fun DWheelColumnPreview() {
    var year by remember { mutableIntStateOf(2026) }
    var month by remember { mutableIntStateOf(8) }
    var day by remember { mutableIntStateOf(17) }
    val wheelHeight = WheelItemHeight * WheelVisibleCount
    AppComposeTheme {
        Row(modifier = Modifier.padding(horizontal = WheelHorizontalPadding)) {
            DWheelColumn(
                items = (2020..2035).toList(),
                selected = year,
                unit = "年",
                textSize = WheelYearTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1.2f,
                onSelected = { year = it }
            )
            DWheelColumn(
                items = (1..12).toList(),
                selected = month,
                unit = "月",
                textSize = WheelDayTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                onSelected = { month = it }
            )
            DWheelColumn(
                // 天数随年月联动，避免出现 2 月 31 日
                items = (1..daysInMonth(year, month)).toList(),
                selected = day,
                unit = "日",
                textSize = WheelDayTextSize,
                wheelHeight = wheelHeight,
                columnWeight = 1f,
                onSelected = { day = it }
            )
        }
    }
}
