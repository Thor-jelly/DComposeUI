package com.ddw.dcomposeui.demo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.DemoScaffold
import com.ddw.dcomposeui.demo.DemoSection
import com.ddw.dcomposeui.demo.DemoValueRow
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import com.ddw.dcomposeui.widget.DClearableField
import com.ddw.dcomposeui.widget.DNumberField

/**
 * 组件描述：DClearableField / DNumberField 演示。分别展示可清空文本框、整数与小数拦截、允许负数、
 * 失焦补零，以及列表中用 focusKey 让每行输入框焦点互不串扰。
 *
 * @param onBack 返回首页
 */
@Composable
fun DInputFieldDemo(onBack: () -> Unit) {
    val textState = rememberTextFieldState("可清空的文本")
    val intState = rememberTextFieldState()
    val decimalState = rememberTextFieldState("12.5")
    val negativeState = rememberTextFieldState()
    // 列表场景：每行一个独立 state，配合 focusKey 保证焦点不互相抢
    val rowStates = remember { List(20) { TextFieldState() } }

    DemoScaffold(title = "输入框体系", onBack = onBack, contentPadding = 0.dp) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DemoSection(
                    title = "DClearableField",
                    desc = "基于 TextFieldState + TextFieldDecorator，有值时右侧出现清空图标，点清空后自动重新聚焦、键盘不收起"
                ) {
                    FieldBox { DClearableField(state = textState, hint = "请输入内容") }
                    Spacer(Modifier.height(10.dp))
                    DemoValueRow(label = "当前值：", value = textState.text.toString().ifEmpty { "（空）" })
                }
            }
            item {
                DemoSection(
                    title = "DNumberField 整数",
                    desc = "InputTransformation 在输入管线内拦截：字母与小数点打不进来，前导 0 自动清理，超过 maxValue 直接钳制"
                ) {
                    FieldBox {
                        DNumberField(
                            state = intState,
                            hint = "最多 9999",
                            maxValue = 9999.0,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    DemoValueRow(label = "当前值：", value = intState.text.toString().ifEmpty { "（空）" })
                }
            }
            item {
                DemoSection(
                    title = "DNumberField 两位小数 + 失焦补零",
                    desc = "decimalPlaces 限制小数位，padEndZero = true 时失焦把 1.5 补成 1.50，点别处收起键盘看效果"
                ) {
                    FieldBox {
                        DNumberField(
                            state = decimalState,
                            hint = "0.00",
                            decimalPlaces = 2,
                            padEndZero = true,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    DemoValueRow(label = "当前值：", value = decimalState.text.toString().ifEmpty { "（空）" })
                }
            }
            item {
                DemoSection(
                    title = "DNumberField 允许负数",
                    desc = "allowNegative = true 时放行单个前导减号，减号出现在中间或出现第二个都会被撤销"
                ) {
                    FieldBox {
                        DNumberField(
                            state = negativeState,
                            hint = "可输入负数",
                            decimalPlaces = 1,
                            allowNegative = true,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    DemoValueRow(label = "当前值：", value = negativeState.text.toString().ifEmpty { "（空）" })
                }
            }
            item {
                DemoSection(
                    title = "列表里的 focusKey",
                    desc = "LazyColumn 复用 item 时，FocusRequester 若被共享会出现「点了这行、焦点跳到另一行」。给每行传唯一 focusKey，各行焦点相互独立"
                ) {
                    Text(
                        text = "下面 20 行，随便点几行输入试试",
                        color = AppColors.BasicThree,
                        fontSize = 12.sp
                    )
                }
            }
            itemsIndexed(rowStates) { index, state ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第 ${index + 1} 行数量",
                        color = AppColors.BasicTwo,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.width(110.dp)) {
                        FieldBox {
                            DNumberField(
                                state = state,
                                hint = "0.00",
                                decimalPlaces = 2,
                                textAlign = TextAlign.End,
                                // 关键：每行唯一 key，焦点与清空互不影响
                                focusKey = index
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 组件描述：给输入框套一个统一的浅边框底座，纯粹为了 Demo 里看得清输入区范围
 *
 * @param content 输入框
 */
@Composable
private fun FieldBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, AppColors.LineTwo, RoundedCornerShape(4.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 760)
@Composable
private fun DInputFieldDemoPreview() {
    AppComposeTheme {
        DInputFieldDemo {}
    }
}
