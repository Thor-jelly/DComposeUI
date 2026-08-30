package com.ddw.dcomposeui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

@Preview(showBackground = true, widthDp = 240)
@Composable
private fun DNumberFieldPreview() {
    AppComposeTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            DNumberField(state = rememberTextFieldState(), hint = "仅整数", textAlign = TextAlign.End)
            Spacer(Modifier.height(12.dp))
            DNumberField(state = rememberTextFieldState("99999"), decimalPlaces = 2, textAlign = TextAlign.End)
        }
    }
}

/**
 * 组件描述：数字输入框（基于 TextFieldState）。用 InputTransformation 在输入管线内实时过滤：
 * 合法性校验 + 小数位限制 + 前导 0 清理 + 最大值钳制，失焦时按小数位格式化。
 *
 * @param state 文本输入状态（由外部 rememberTextFieldState() 创建并持有）
 * @param modifier 外部布局修饰
 * @param hint 占位提示
 * @param maxValue 最大数值（超出会静默钳制为该值；默认 999999.9999）
 * @param decimalPlaces 允许的小数位数（0 表示仅整数）
 * @param allowNegative 是否允许负数（默认 false）
 * @param formatOnFocusLost 失焦时是否按小数位格式化文本（默认 true）
 * @param padEndZero 格式化时是否补足末尾 0（如 2 位小数下 "1.5"→"1.50"；默认 false）
 * @param textAlign 文本对齐（最小/最大值输入常用居中或右对齐）
 * @param hintColor 占位文字颜色
 * @param textColor 文字颜色
 * @param fontSize 文字大小
 * @param enabled 是否可输入
 * @param clearable 是否显示清空按钮（输入框有值时于右侧显示，默认开启；点清空后自动重新聚焦）
 * @param focusKey 焦点标识：列表中每项建议传唯一 key，使各输入框焦点相互独立、避免复用串扰（默认 null 每实例独立）
 * @param autoFocus 是否进入即自动聚焦并拉起键盘（默认 false；列表场景勿开，避免多项抢焦点）
 */
@Composable
fun DNumberField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    hint: String = "",
    maxValue: Double = 999999.9999,
    decimalPlaces: Int = 0,
    allowNegative: Boolean = false,
    formatOnFocusLost: Boolean = true,
    padEndZero: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    hintColor: Color = AppColors.BasicThree,
    textColor: Color = AppColors.BasicOne,
    fontSize: TextUnit = 14.sp,
    enabled: Boolean = true,
    clearable: Boolean = true,
    focusKey: Any? = null,
    autoFocus: Boolean = false
) {
    // 焦点请求器：按 focusKey 记忆，列表中不同项各自独立，避免焦点/清空串扰
    val focusRequester = remember(focusKey) { FocusRequester() }
    // 进入即自动聚焦（effect 在组合完成后运行，此时输入框已关联 focusRequester）
    if (autoFocus) {
        LaunchedEffect(focusKey) { focusRequester.requestFocus() }
    }
    val isDecimal = decimalPlaces > 0
    val keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
    val textStyle = TextStyle(color = textColor, fontSize = fontSize, textAlign = textAlign)

    // 输入拦截：非法输入撤销本次修改（revertAllChanges），前导 0/超限则原地改写 buffer
    val filter = InputTransformation {
        val text = toString()
        if (text.isEmpty()) return@InputTransformation
        // 负号处理：允许负数时放行单个前导「-」，否则出现「-」即非法
        if (allowNegative) {
            if (text == "-") return@InputTransformation
            if (text.count { it == '-' } > 1 || (text.contains('-') && !text.startsWith("-"))) {
                revertAllChanges()
                return@InputTransformation
            }
        } else if (text.contains('-')) {
            revertAllChanges()
            return@InputTransformation
        }
        // 格式校验：整数或指定小数位（按是否允许负数拼接符号位）
        val signPart = if (allowNegative) "-?" else ""
        val pattern = if (isDecimal) "^$signPart\\d*(\\.\\d{0,$decimalPlaces})?$" else "^$signPart\\d*$"
        if (!Regex(pattern).matches(text)) {
            revertAllChanges()
            return@InputTransformation
        }
        // 前导 0 清理（如 007→7、00.5→0.5；单独的 0、0.x 不动）
        if (Regex("^-?0+\\d+.*").matches(text)) {
            val negative = text.startsWith("-")
            val digits = text.substring(if (negative) 1 else 0).dropWhile { it == '0' }
            val normalized = if (digits.isEmpty() || digits.startsWith(".")) "0$digits" else digits
            replace(0, length, if (negative) "-$normalized" else normalized)
            return@InputTransformation
        }
        // 超过最大值：钳制为最大值（「.」「-」等中间态无法解析，放行）
        val num = text.toBigDecimalOrNull() ?: return@InputTransformation
        if (num > BigDecimal.valueOf(maxValue)) {
            replace(0, length, formatNumber(maxValue, decimalPlaces, padEndZero))
        }
    }

    BasicTextField(
        state = state,
        enabled = enabled,
        inputTransformation = filter,
        textStyle = textStyle,
        cursorBrush = SolidColor(AppColors.AccentBlue),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focus ->
            // 失焦且非空时按小数位格式化，仅在结果变化时写回，避免无谓重组
            if (formatOnFocusLost && !focus.isFocused && state.text.isNotEmpty()) {
                val formatted = formatOnBlur(state.text.toString(), decimalPlaces, padEndZero)
                if (formatted != state.text.toString()) {
                    state.edit { replace(0, length, formatted) }
                }
            }
        },
        decorator = TextFieldDecorator { inner ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // propagateMinConstraints：把宽度约束下传，强制文本区(innerTextField)撑满，使 textAlign 生效
                Box(modifier = Modifier.weight(1f), propagateMinConstraints = true) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = hint,
                            color = hintColor,
                            fontSize = fontSize,
                            style = TextStyle(textAlign = textAlign),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    inner()
                }
                // 清空按钮：可清空且有值时于右侧显示，点击清空输入
                if (clearable && enabled && state.text.isNotEmpty()) {
                    ClearIcon {
                        // 清空后重新聚焦，保持键盘并可继续输入
                        state.edit { delete(0, length) }
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    )
}

/** 失焦格式化：处理「.」「-」等中间态，其余按小数位格式化 */
private fun formatOnBlur(text: String, decimalPlaces: Int, padEndZero: Boolean): String {
    if (text == "." || text == "-." || text == "-") {
        return if (padEndZero && decimalPlaces > 0) "0." + "0".repeat(decimalPlaces) else "0"
    }
    val number = text.toDoubleOrNull() ?: return ""
    return formatNumber(number, decimalPlaces, padEndZero)
}

/** 按小数位格式化数字（FLOOR 截断，不进位）；padEndZero=true 补足末尾 0，否则省略多余 0 */
private fun formatNumber(number: Double, decimalPlaces: Int, padEndZero: Boolean): String {
    val pattern = StringBuilder("0")
    for (i in 0 until decimalPlaces) {
        if (i == 0) pattern.append(".")
        pattern.append(if (padEndZero) '0' else '#')
    }
    return DecimalFormat(pattern.toString()).apply { roundingMode = RoundingMode.FLOOR }.format(number)
}
