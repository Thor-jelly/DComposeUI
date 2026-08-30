package com.ddw.dcomposeui.demo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.DemoScaffold
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import com.ddw.dcomposeui.widget.DRefreshList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 每页条数 */
private const val PageSize = 12

/** 最多加载到第几页，之后进入「没有更多」 */
private const val MaxPage = 3

/**
 * 组件描述：DRefreshList 演示。下拉刷新回到第一页，上拉加载追加下一页，加载到 MaxPage 后进入没有更多；
 * 顶部开关可切换空数据态。
 *
 * @param onBack 返回首页
 */
@Composable
fun DRefreshListDemo(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(1) }
    var data by remember { mutableStateOf(mockPage(1)) }
    var refreshing by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var showEmpty by remember { mutableStateOf(false) }

    DemoScaffold(title = "DRefreshList", onBack = onBack, contentPadding = 0.dp) {
        // 开关区：切换空数据，用来看空布局在下拉刷新下依然可用
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "模拟空数据", color = AppColors.BasicTwo, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Switch(checked = showEmpty, onCheckedChange = { showEmpty = it })
            Spacer(Modifier.width(16.dp))
            Text(
                text = "第 $page / $MaxPage 页",
                color = AppColors.BasicThree,
                fontSize = 12.sp
            )
        }
        HorizontalDivider(color = AppColors.DividerColor)

        DRefreshList(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    delay(1200)
                    page = 1
                    data = mockPage(1)
                    refreshing = false
                }
            },
            onLoadMore = {
                scope.launch {
                    loadingMore = true
                    delay(1200)
                    page += 1
                    data = data + mockPage(page)
                    loadingMore = false
                }
            },
            isLoadingMore = loadingMore,
            // 到达最大页后不再响应上拉，底部常驻「没有更多数据了」
            noMoreData = page >= MaxPage,
            isEmpty = showEmpty || data.isEmpty(),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            items(if (showEmpty) 0 else data.size) { index ->
                Column {
                    Text(
                        text = data[index],
                        color = AppColors.BasicOne,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    )
                    HorizontalDivider(color = AppColors.DividerColor)
                }
            }
        }
    }
}

/** 造一页假数据 */
private fun mockPage(page: Int): List<String> =
    List(PageSize) { i -> "第 ${(page - 1) * PageSize + i + 1} 条数据，下拉试试刷新头部" }

@Preview(showBackground = true, widthDp = 375, heightDp = 700)
@Composable
private fun DRefreshListDemoPreview() {
    AppComposeTheme {
        DRefreshListDemo {}
    }
}
