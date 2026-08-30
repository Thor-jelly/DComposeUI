package com.ddw.dcomposeui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.ext.noRippleClick
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme

@Preview(showBackground = true, widthDp = 240)
@Composable
private fun DClearableFieldPreview() {
    AppComposeTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            DClearableField(state = rememberTextFieldState(), hint = "请输入")
            Spacer(Modifier.height(12.dp))
            DClearableField(state = rememberTextFieldState("已输入内容"), textAlign = TextAlign.End)
        }
    }
}

/**
 * 组件描述：可清空文本输入框（基于 TextFieldState）。右侧在有值时显示清空图标，点击清空。
 * 纯文本输入（无数字拦截）；数字输入请用 [DNumberField]。
 *
 * @param state 文本输入状态（由外部 rememberTextFieldState() 创建并持有）
 * @param modifier 外部布局修饰
 * @param hint 占位提示
 * @param clearable 是否显示清空按钮（有值时于右侧显示，默认开启）
 * @param singleLine 是否单行（默认单行）
 * @param textAlign 文本对齐
 * @param hintColor 占位文字颜色
 * @param textColor 文字颜色
 * @param fontSize 文字大小
 * @param enabled 是否可输入
 * @param focusKey 焦点标识：列表中每项建议传唯一 key，使各输入框焦点相互独立、避免复用串扰（默认 null 每实例独立）
 * @param autoFocus 是否进入即自动聚焦并拉起键盘（默认 false；列表场景勿开，避免多项抢焦点）
 */
@Composable
fun DClearableField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    hint: String = "",
    clearable: Boolean = true,
    singleLine: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    hintColor: Color = AppColors.BasicThree,
    textColor: Color = AppColors.BasicOne,
    fontSize: TextUnit = 14.sp,
    enabled: Boolean = true,
    focusKey: Any? = null,
    autoFocus: Boolean = false
) {
    // 焦点请求器：按 focusKey 记忆，列表中不同项各自独立，避免焦点/清空串扰
    val focusRequester = remember(focusKey) { FocusRequester() }
    // 进入即自动聚焦（effect 在组合完成后运行，此时输入框已关联 focusRequester）
    if (autoFocus) {
        LaunchedEffect(focusKey) { focusRequester.requestFocus() }
    }
    BasicTextField(
        state = state,
        enabled = enabled,
        textStyle = TextStyle(color = textColor, fontSize = fontSize, textAlign = textAlign),
        cursorBrush = SolidColor(AppColors.AccentBlue),
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
        modifier = modifier.focusRequester(focusRequester),
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

/**
 * 组件描述：输入框右侧的清空图标，[DClearableField] 与 [DNumberField] 共用
 * @param onClick 点击清空
 */
@Composable
internal fun ClearIcon(onClick: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.Cancel,
        contentDescription = "清空",
        tint = AppColors.BasicFour,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(16.dp)
            .noRippleClick(onClick)
    )
}
