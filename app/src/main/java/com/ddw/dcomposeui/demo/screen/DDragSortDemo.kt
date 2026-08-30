package com.ddw.dcomposeui.demo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.DemoChip
import com.ddw.dcomposeui.demo.DemoScaffold
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import com.ddw.dcomposeui.widget.DDragSortItem
import com.ddw.dcomposeui.widget.rememberDDragSortState

/**
 * 组件描述：DDragSortList 演示。列表项按下右侧手柄即可拖动排序，也可切换成长按整行拖动；
 * 列表故意做长一些，拖到上下边缘时会持续自动滚动。
 *
 * @param onBack 返回首页
 */
@Composable
fun DDragSortDemo(onBack: () -> Unit) {
    // 用 SnapshotStateList 承载数据，onMove 里直接搬动元素，列表会即时重排
    val data = remember { mockProcessList().toMutableStateList() }
    // 手柄模式：true 长按整行拖动，false 按下右侧手柄拖动
    var longPressWholeRow by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val dragState = rememberDDragSortState(listState) { from, to ->
        data.add(to, data.removeAt(from))
    }

    DemoScaffold(title = "DDragSortList", onBack = onBack, contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DemoChip(text = "按手柄拖动", selected = !longPressWholeRow) { longPressWholeRow = false }
            Spacer(Modifier.width(8.dp))
            DemoChip(text = "长按整行拖动", selected = longPressWholeRow) { longPressWholeRow = true }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data, key = { it }) { item ->
                DDragSortItem(state = dragState, key = item) { isDragging ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 拖起来的那一项加阴影，视觉上「浮」起来
                            .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDragging) AppColors.ButtonThree else Color.White)
                            .then(
                                if (longPressWholeRow) Modifier.longPressDraggableHandle() else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${data.indexOf(item) + 1}",
                            color = AppColors.AccentPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = item,
                            color = AppColors.BasicOne,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        // 手柄模式下只有这个图标能发起拖动
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = "拖动排序",
                            tint = if (longPressWholeRow) AppColors.BasicFour else AppColors.BasicThree,
                            modifier = Modifier
                                .size(22.dp)
                                .then(
                                    if (longPressWholeRow) Modifier else Modifier.draggableHandle()
                                )
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "当前顺序：${data.joinToString(" → ")}",
                color = AppColors.BasicThree,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/** 造一批工序名当假数据 */
private fun mockProcessList(): List<String> = listOf(
    "裁剪", "印花", "绣花", "缝制", "水洗", "定型",
    "中检", "整烫", "尾查", "包装", "装箱", "出货"
)

@Preview(showBackground = true, widthDp = 375, heightDp = 700)
@Composable
private fun DDragSortDemoPreview() {
    AppComposeTheme {
        DDragSortDemo {}
    }
}
