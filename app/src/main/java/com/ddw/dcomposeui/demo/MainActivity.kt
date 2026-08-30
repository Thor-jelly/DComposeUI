package com.ddw.dcomposeui.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.screen.DBubbleDemo
import com.ddw.dcomposeui.demo.screen.DDragSortDemo
import com.ddw.dcomposeui.demo.screen.DInputFieldDemo
import com.ddw.dcomposeui.demo.screen.DRefreshListDemo
import com.ddw.dcomposeui.demo.screen.DSyncTableDemo
import com.ddw.dcomposeui.demo.screen.DWheelPickerDemo
import com.ddw.dcomposeui.ext.noRippleClick
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme

/**
 * 类描述：控件示例入口，首页为控件导航列表，点击进入对应演示页
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 11:00
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppComposeTheme {
                DemoApp()
            }
        }
    }
}

/**
 * 类描述：Demo 条目，标题为控件名，desc 写清这个控件的实现看点
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 11:00
 */
private enum class DemoEntry(val title: String, val desc: String) {
    RefreshList(
        "DRefreshList",
        "纯 Compose 下拉刷新 + 上拉加载 + 空态：自定义 NestedScrollConnection 接管越界拖动，Canvas 复刻 SmartRefresh 的箭头与 12 段转圈"
    ),
    SyncTable(
        "DSyncTable",
        "冻结列 + 横向同步滚动表格：表头与所有行共享同一个 ScrollState，行内容用 DSL 声明，支持分组行与小计行混排"
    ),
    Bubble(
        "DBubbleMenu / DBubbleTip",
        "锚定浮层：自定义 PopupPositionProvider 算定位并超屏收边，气泡箭头用自定义 Layout + Canvas 对准锚点中心"
    ),
    WheelPicker(
        "DWheelPicker 与日期选择",
        "LazyColumn + SnapFlingBehavior 做吸附滚轮，越远离中心越淡越小；日期弹窗做多列联动与天数校正"
    ),
    DragSort(
        "DDragSortList",
        "LazyColumn 拖拽排序：按 item key 跟踪被拖项、拖到边缘持续自动滚动、交换后等布局刷新避免抖动"
    ),
    InputField(
        "DClearableField / DNumberField",
        "TextFieldState 时代的输入框：TextFieldDecorator 装饰、InputTransformation 拦截非法输入、focusKey 治列表焦点串扰"
    )
}

/**
 * 组件描述：Demo 根节点，用一个 state 在首页与演示页间切换（示例工程不引入 navigation 依赖）
 */
@Composable
private fun DemoApp() {
    var current by remember { mutableStateOf<DemoEntry?>(null) }
    // 演示页里按系统返回键先回首页
    BackHandler(enabled = current != null) { current = null }
    val onBack: () -> Unit = { current = null }
    when (current) {
        null -> DemoHome { current = it }
        DemoEntry.RefreshList -> DRefreshListDemo(onBack)
        DemoEntry.SyncTable -> DSyncTableDemo(onBack)
        DemoEntry.Bubble -> DBubbleDemo(onBack)
        DemoEntry.WheelPicker -> DWheelPickerDemo(onBack)
        DemoEntry.DragSort -> DDragSortDemo(onBack)
        DemoEntry.InputField -> DInputFieldDemo(onBack)
    }
}

/**
 * 组件描述：首页控件导航列表
 *
 * @param onEntryClick 点击条目
 */
@Composable
private fun DemoHome(onEntryClick: (DemoEntry) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BgPageThree)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(text = "DComposeUI", color = AppColors.BasicOne, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(text = "自研 Compose 控件示例", color = AppColors.BasicThree, fontSize = 12.sp)
        }
        HorizontalDivider(color = AppColors.DividerColor)
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(DemoEntry.entries) { entry ->
                DemoEntryCard(title = entry.title, desc = entry.desc) { onEntryClick(entry) }
            }
        }
    }
}

/**
 * 组件描述：首页条目卡片
 *
 * @param title 控件名
 * @param desc 实现看点
 * @param onClick 点击回调
 */
@Composable
private fun DemoEntryCard(title: String, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .noRippleClick(onClick)
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, color = AppColors.BasicOne, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(text = desc, color = AppColors.BasicThree, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 720)
@Composable
private fun DemoHomePreview() {
    AppComposeTheme {
        DemoHome {}
    }
}
