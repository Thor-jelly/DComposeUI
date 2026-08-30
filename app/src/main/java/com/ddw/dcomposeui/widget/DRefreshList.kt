package com.ddw.dcomposeui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 整体原理（先看这里，后面代码就好懂了）
// ----------------------------------------------------------------------------
// 1) 一个用 px 表示的拖动偏移量 offset：
//      offset > 0 表示顶部下拉（露出「头部」刷新指示器）
//      offset < 0 表示底部上拉（露出「尾部」加载指示器）
//      offset = 0 表示正常状态，列表正常滚动
// 2) 布局：用一个 Box 叠三层——头部(顶)、尾部(底)、列表(中间)。
//      三层都根据 offset 做位移(Modifier.offset)，营造「整体被拖动」的效果。
// 3) 手势：给 Box 挂一个 NestedScrollConnection。列表(LazyColumn)是「内层滚动」，
//      它滚不动(到顶/到底)后剩余的手指位移会「冒泡」给这个 connection，
//      我们就用这部分剩余位移去改变 offset，从而把头/尾拖出来。
//      三个回调各司其职：
//        onPreScroll  ：滑动「之前」——头/尾已经拉出来时，反向滑动先把它收回去
//        onPostScroll ：滑动「之后」——列表到边界后剩余的位移，拿来拉出头/尾(带阻尼)
//        onPreFling   ：松手(惯性滑动开始)——判断是否越过阈值，越过就触发刷新/加载
// 4) 触发后指示器要「保持」：不是我们自己维持，而是业务把 isRefreshing / isLoadingMore
//      置 true，我们用 LaunchedEffect 监听：true 就把 offset 定在指示器高度；
//      业务加载完置回 false，我们再用动画把 offset 收回 0。
// 5) 上拉加载完成收回时：若只是把 offset 弹回 0，列表会整体「下移」回原位，把刚加载的
//      新数据顶出屏幕。所以收回的每一帧会同步滚动列表抵消这段位移，让新数据可见。
// ============================================================================

/** 头/尾指示器的展示高度（刷新中/加载中时 offset 会停在这个高度）；参考 ClassicsHeader 取较舒展的 60dp 以容纳两行文案 */
private val IndicatorHeight = 60.dp

/** 触发刷新/加载的下拉/上拉阈值（拖动超过它松手才会触发） */
private val TriggerHeight = 50.dp

/** 最大可拖动距离（再怎么拉也不超过它，避免拉飞） */
private val MaxDrag = 100.dp

/** 拖拽阻尼系数：手指位移只有一半转成 offset，手感更「跟手带阻力」 */
private const val DragResistance = 0.5f

/**
 * 组件描述：纯 Compose 的「下拉刷新 + 上拉加载」列表，仿 SmartRefresh ClassicsHeader/Footer 的拖动 + 文案效果。
 * 通过自定义 NestedScrollConnection 处理列表越界后的剩余拖动：顶部下拉露出头部、底部上拉露出尾部，
 * 松手超过阈值触发刷新/加载；期间保持指示器，业务把 isRefreshing/isLoadingMore 置回 false 后自动收起。
 * 无任何 Android View 依赖。
 *
 * @param isRefreshing 是否正在刷新（业务侧控制；true 时保持头部指示器）
 * @param onRefresh 下拉刷新回调（内部需把 isRefreshing 置 true，加载完置 false）
 * @param onLoadMore 上拉加载回调（内部需把 isLoadingMore 置 true，加载完置 false）
 * @param modifier 外部布局修饰（一般 fillMaxSize/weight）
 * @param isLoadingMore 是否正在加载更多（true 时保持尾部指示器）
 * @param noMoreData 是否没有更多数据（列表底部常驻展示「没有更多数据了」，且不再响应上拉加载）
 * @param enableRefresh 是否允许下拉刷新
 * @param enableLoadMore 是否允许上拉加载
 * @param listState 列表滚动状态
 * @param contentPadding 列表内边距
 * @param verticalArrangement 列表纵向排列
 * @param isEmpty 是否为空数据（true 时展示空布局；仍保留下拉刷新能力）
 * @param emptyContent 自定义空布局（默认展示「暂无数据」，可传入自定义）
 * @param content 列表内容（LazyListScope，调用方用 items 添加）
 */
@Composable
fun DRefreshList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    noMoreData: Boolean = false,
    enableRefresh: Boolean = true,
    enableLoadMore: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    isEmpty: Boolean = false,
    emptyContent: @Composable () -> Unit = { DefaultListEmpty() },
    content: LazyListScope.() -> Unit
) {
    // dp 转 px：手势/布局计算都用 px（手指位移的单位就是 px）
    val density = LocalDensity.current
    val indicatorPx = with(density) { IndicatorHeight.toPx() }
    val triggerPx = with(density) { TriggerHeight.toPx() }
    val maxDragPx = with(density) { MaxDrag.toPx() }

    // 核心状态：拖动偏移量(px)。>0 头部，<0 尾部，=0 正常。用 FloatState 减少装箱。
    val offset = remember { mutableFloatStateOf(0f) }

    // 上次刷新完成时间(毫秒)：初始为组件创建时刻；每次刷新结束时更新，供头部「上次更新」展示
    val lastRefreshTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    // 记录上一帧的刷新状态，用于识别刷新「结束」的时刻(true→false)
    val wasRefreshing = remember { mutableStateOf(false) }
    // 头部「上次更新 MM-dd HH:mm」文案(仅在时间变化时重新格式化)
    val lastUpdateText = remember(lastRefreshTime.longValue) {
        "上次更新 " + SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(lastRefreshTime.longValue))
    }

    // connection 是 remember 出来的「长期对象」，只创建一次；
    // 但它内部要用到 isRefreshing 等会变化的值，直接闭包捕获会「过期」（拿到旧值）。
    // 用 rememberUpdatedState 包一层：对象不变，但每次重组把最新值写进去，闭包里读的就是最新值。
    val curOnRefresh by rememberUpdatedState(onRefresh)
    val curOnLoadMore by rememberUpdatedState(onLoadMore)
    val curRefreshing by rememberUpdatedState(isRefreshing)
    val curLoadingMore by rememberUpdatedState(isLoadingMore)
    val curNoMore by rememberUpdatedState(noMoreData)
    val curEnableRefresh by rememberUpdatedState(enableRefresh)
    val curEnableLoadMore by rememberUpdatedState(enableLoadMore)

    // 监听「刷新状态」：开始刷新→把头部定在指示器高度保持住；刷新结束→用动画收回 0。
    // animate(初值, 目标值){ v,_ -> ... } 是挂起的补间动画，每帧回调把新值写回 offset。
    LaunchedEffect(isRefreshing) {
        // 刷新结束(true→false)时记录本次更新时间，供头部「上次更新」展示
        if (wasRefreshing.value && !isRefreshing) {
            lastRefreshTime.longValue = System.currentTimeMillis()
        }
        wasRefreshing.value = isRefreshing
        if (isRefreshing && offset.floatValue <= 0f) {
            animate(offset.floatValue, indicatorPx) { v, _ -> offset.floatValue = v }
        } else if (!isRefreshing && offset.floatValue > 0f) {
            animate(offset.floatValue, 0f) { v, _ -> offset.floatValue = v }
        }
    }
    // 监听「加载状态」：逻辑同上，只是方向相反（尾部是负方向）。
    LaunchedEffect(isLoadingMore) {
        if (isLoadingMore && offset.floatValue >= 0f) {
            animate(offset.floatValue, -indicatorPx) { v, _ -> offset.floatValue = v }
        } else if (!isLoadingMore && offset.floatValue < 0f) {
            // 加载完成：若只是把 offset 弹回 0，列表会整体「下移」回原位，
            // 刚加载出来的新数据仍停在屏幕外下方，用户会误以为没有更多数据。
            // 处理：回弹的每一帧让列表同步「向后滚动」相同距离，抵消这段下移，
            // 从而把新内容顶上来显示，明确告诉用户「后面还有数据」。
            var prev = offset.floatValue
            listState.scroll {
                animate(prev, 0f) { v, _ ->
                    val delta = v - prev
                    prev = v
                    offset.floatValue = v
                    if (delta != 0f) scrollBy(delta)
                }
            }
        }
    }

    // 自定义嵌套滚动连接：拦截列表越界后的滚动，用来驱动 offset。
    val connection = remember {
        object : NestedScrollConnection {

            /**
             * 滑动「发生前」被调用。available = 本次手指想滚动的量（y 正=向下，负=向上）。
             * 作用：如果头/尾已经拉出来了(offset≠0)，反向滑动时「优先把它收回去」，
             * 而不是让列表先滚。返回值 = 我们消费掉的量（列表拿不到这部分）。
             */
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只处理手指拖动，惯性/程序滚动不处理
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val dy = available.y
                val cur = offset.floatValue
                // 头部已露出(cur>0) 且手指向上(dy<0)：先把头部往回收（收到 0 为止），刷新中不收
                if (cur > 0f && dy < 0f && !curRefreshing) {
                    val newVal = (cur + dy).coerceAtLeast(0f)
                    offset.floatValue = newVal
                    return Offset(0f, newVal - cur) // 消费掉的这段
                }
                // 尾部已露出(cur<0) 且手指向下(dy>0)：先把尾部往回收，加载中不收
                if (cur < 0f && dy > 0f && !curLoadingMore) {
                    val newVal = (cur + dy).coerceAtMost(0f)
                    offset.floatValue = newVal
                    return Offset(0f, newVal - cur)
                }
                return Offset.Zero // 其余情况不消费，交给列表正常滚动
            }

            /**
             * 滑动「发生后」被调用。consumed=列表已消费的量，available=列表没消费完的「剩余量」。
             * 列表滚到顶还继续下拉，或滚到底还继续上拉，剩余量就会出现在这里——正好拿来拉头/尾。
             * dy * 阻尼系数：让拖动比手指慢一半，手感更真实；coerce 限制在最大拖动范围内。
             */
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                // 正在刷新/加载时不允许再拖
                if (curRefreshing || curLoadingMore) return Offset.Zero
                val dy = available.y
                // 到顶后继续下拉(dy>0)：拉出头部
                if (dy > 0f && curEnableRefresh) {
                    offset.floatValue = (offset.floatValue + dy * DragResistance).coerceAtMost(maxDragPx)
                    return Offset(0f, dy) // 声明这段剩余量我们用了
                }
                // 到底后继续上拉(dy<0)：拉出尾部；没有更多数据时不再响应上拉(底部已常驻提示)
                if (dy < 0f && curEnableLoadMore && !curNoMore) {
                    offset.floatValue = (offset.floatValue + dy * DragResistance).coerceAtLeast(-maxDragPx)
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }

            /**
             * 松手时（惯性滑动开始前）被调用，是挂起函数。这里做「释放判定」：
             *   头部拉过阈值 → 触发 onRefresh（之后由 LaunchedEffect(isRefreshing) 保持头部高度）
             *   尾部拉过阈值 → 触发 onLoadMore
             *   没过阈值   → 用动画把 offset 弹回 0
             * 只要当前有拖动偏移，就返回 available（把这段惯性速度「吃掉」），避免列表接着甩动。
             */
            override suspend fun onPreFling(available: Velocity): Velocity {
                val cur = offset.floatValue
                if (cur > 0f) {
                    if (cur >= triggerPx && curEnableRefresh && !curRefreshing) {
                        curOnRefresh() // 触发刷新；保持动作交给 LaunchedEffect
                    } else {
                        animate(cur, 0f) { v, _ -> offset.floatValue = v } // 没过阈值，弹回
                    }
                    return available
                }
                if (cur < 0f) {
                    if (-cur >= triggerPx && curEnableLoadMore && !curNoMore && !curLoadingMore) {
                        curOnLoadMore()
                    } else {
                        animate(cur, 0f) { v, _ -> offset.floatValue = v }
                    }
                    return available
                }
                return Velocity.Zero // 没有拖动偏移，正常惯性交给列表
            }
        }
    }

    // 外层容器：clipToBounds 裁掉超出边界的头/尾；nestedScroll 挂上我们的连接。
    Box(
        modifier = modifier
            .clipToBounds()
            .nestedScroll(connection)
    ) {
        val off = offset.floatValue

        // —— 头部：只在下拉(off>0)时才绘制 ——
        // 位置计算：头部靠顶部对齐(TopCenter)，默认它的 y 从 0 开始（即紧贴顶部可见）。
        // 我们希望「没拉时藏在屏幕上方」，所以整体上移一个指示器高度：y = off - indicatorPx。
        //   off=0        → y=-indicatorPx（正好完全藏在上方看不见）
        //   off=indicator→ y=0（完全露出）
        if (off > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(IndicatorHeight)
                    .offset { IntOffset(0, (off - indicatorPx).roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                RefreshHeaderContent(
                    refreshing = isRefreshing,
                    releaseReady = off >= triggerPx,
                    lastUpdateText = lastUpdateText
                )
            }
        }

        // —— 尾部：只在上拉(off<0)时才绘制 ——
        // 尾部靠底部对齐(BottomCenter)，默认紧贴底部可见。我们希望「没拉时藏在屏幕下方」，
        // 所以整体下移一个指示器高度：y = indicatorPx + off（off 为负，会把它往上顶回来）。
        //   off=0            → y=indicatorPx（藏在下方看不见）
        //   off=-indicator   → y=0（完全露出）
        if (off < 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(IndicatorHeight)
                    .offset { IntOffset(0, (indicatorPx + off).roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                LoadFooterContent(
                    loading = isLoadingMore,
                    releaseReady = -off >= triggerPx,
                    noMore = noMoreData
                )
            }
        }

        // —— 列表内容：整体跟随 offset 位移，营造「被一起拖动」的感觉 ——
        // off>0 向下移露出头部；off<0 向上移露出尾部；off=0 正常。
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, off.roundToInt()) },
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement
        ) {
            if (isEmpty) {
                // 空数据：用一个占满视口的 item 承载空布局，
                // 这样 LazyColumn 仍是可滚动的父容器，空态下依然能下拉刷新
                item(key = "__empty__") {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        emptyContent()
                    }
                }
            } else {
                content()
                // 没有更多数据：列表底部常驻提示，正常滚动到底即可看到，无需再上拉
                if (noMoreData) {
                    item(key = "__no_more_footer__") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "没有更多数据了", color = AppColors.BasicThree, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 组件描述：默认空布局——居中「暂无数据」
 */
@Composable
private fun DefaultListEmpty() {
    Text(text = "暂无数据", color = AppColors.BasicThree, fontSize = 14.sp)
}

/**
 * 组件描述：下拉刷新头部内容。三种态：
 * 下拉中(箭头朝下) / 释放态(箭头朝上) / 刷新中(转圈)。
 * 布局仿 ClassicsHeader：左侧图标 + 右侧两行文案（状态文案在上、上次更新时间在下）。
 *
 * @param refreshing 是否刷新中
 * @param releaseReady 是否已拖过阈值（松手即触发）
 * @param lastUpdateText 上次更新时间文案（如「上次更新 07-14 10:30」）
 */
@Composable
private fun RefreshHeaderContent(refreshing: Boolean, releaseReady: Boolean, lastUpdateText: String) {
    // 仿 SmartRefresh ClassicsHeader：左侧图标 + 右侧一列两行（标题在上、上次更新时间在下），整体水平居中
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (refreshing) {
            // 刷新中：仿 ProgressDrawable 的 12 段渐变旋转指示器
            SmartProgressIndicator(color = AppColors.BasicTwo, modifier = Modifier.size(20.dp))
        } else {
            // 箭头：下拉时朝下(0°)，拖过阈值时旋转到朝上(180°)
            SmartArrow(
                color = AppColors.BasicTwo,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (releaseReady) 180f else 0f)
            )
        }
        Spacer(Modifier.width(12.dp))
        // 右侧两行：上为当前状态文案(标题)，下为上次更新时间
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    refreshing -> "正在刷新..."
                    releaseReady -> "释放立即刷新"
                    else -> "下拉可以刷新"
                },
                color = AppColors.BasicTwo,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = lastUpdateText,
                color = AppColors.BasicThree,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 组件描述：上拉加载尾部内容。
 * 加载中(转圈) / 上拉中(箭头朝上) / 释放态(箭头朝下)；
 * noMore 为兜底态（正常由列表底部常驻项展示「没有更多数据了」，此处一般不会触发）。
 *
 * @param loading 是否加载中
 * @param releaseReady 是否已拖过阈值（松手即触发）
 * @param noMore 是否没有更多数据（兜底展示）
 */
@Composable
private fun LoadFooterContent(loading: Boolean, releaseReady: Boolean, noMore: Boolean) {
    // 仿 SmartRefresh ClassicsFooter：图标 + 单行文案、水平居中；配色/字号/图标尺寸与头部保持一致
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        when {
            // 终态弱提示：用中灰(BasicThree)，不喧宾夺主
            noMore -> Text(text = "没有更多数据了", color = AppColors.BasicThree, fontSize = 15.sp)
            loading -> {
                SmartProgressIndicator(color = AppColors.BasicTwo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(text = "正在加载...", color = AppColors.BasicTwo, fontSize = 15.sp)
            }
            else -> {
                // 箭头：上拉时朝上(180°)，拖过阈值时旋转到朝下(0°)
                SmartArrow(
                    color = AppColors.BasicTwo,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (releaseReady) 0f else 180f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (releaseReady) "释放立即加载" else "上拉加载更多",
                    color = AppColors.BasicTwo,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * 组件描述：仿 SmartRefresh ArrowDrawable 的箭头，用 Canvas 复刻，保证与项目其它下拉刷新箭头样式一致。
 * 默认绘制「朝下」的粗箭头（竖杆 + V 形头），朝向由外部 Modifier.rotate 控制（0°朝下 / 180°朝上）。
 * @param color 箭头填充色
 * @param modifier 尺寸/旋转等修饰
 */
@Composable
private fun SmartArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val lineWidth = w * 30f / 225f            // 杆宽：宽度的 30/225（与原 Drawable 比例一致）
        val vector1 = lineWidth * 0.70710678f     // lineWidth * sin(45°)
        val vector2 = lineWidth / 0.70710678f     // lineWidth / sin(45°)
        val path = Path().apply {
            moveTo(w / 2f, h)                                              // 箭头尖（底部中点）
            lineTo(0f, h / 2f)                                            // 左翼外缘
            lineTo(vector1, h / 2f - vector1)                            // 左翼内缘
            lineTo(w / 2f - lineWidth / 2f, h - vector2 - lineWidth / 2f) // 左侧杆脚
            lineTo(w / 2f - lineWidth / 2f, 0f)                          // 竖杆左上
            lineTo(w / 2f + lineWidth / 2f, 0f)                          // 竖杆右上
            lineTo(w / 2f + lineWidth / 2f, h - vector2 - lineWidth / 2f) // 右侧杆脚
            lineTo(w - vector1, h / 2f - vector1)                        // 右翼内缘
            lineTo(w, h / 2f)                                            // 右翼外缘
            close()
        }
        drawPath(path = path, color = color)
    }
}

/**
 * 组件描述：仿 SmartRefresh ProgressDrawable 的加载指示器，用 Canvas 复刻。
 * 12 段「胶囊条」环形排布、透明度递增，整体按 30° 步进旋转，形成 iOS 风格的转圈效果。
 * @param color 指示器基础色（各段在此基础上叠加不同透明度）
 * @param modifier 尺寸等修饰
 */
@Composable
private fun SmartProgressIndicator(color: Color, modifier: Modifier = Modifier) {
    // 无限旋转：0→12 对应 12 个 30° 步进位置，线性循环（每格约 83ms，一圈 1s）
    val transition = rememberInfiniteTransition(label = "smart_progress")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "smart_progress_step"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val r = max(1f, w / 22f)          // 胶囊粗细的一半 / 端头半径（与原 Drawable 比例一致）
        val cx = w / 2f
        val cy = size.height / 2f
        val rInner = cx - 5 * r           // 胶囊内端到圆心的距离
        val rOuter = cx - r               // 胶囊外端到圆心的距离
        val baseDeg = (step.toInt() % 12) * 30    // 当前整体旋转角（30° 步进）
        for (i in 0 until 12) {
            // 第 i 段透明度：(i+5)*17，最大截断到 255（与原 Drawable 一致），形成渐隐拖尾
            val segColor = color.copy(alpha = ((i + 5) * 17).coerceAtMost(255) / 255f)
            val rad = Math.toRadians((baseDeg + 30 * (i + 1)).toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            // 用圆头粗线画胶囊：两端自动生成半圆，等效「两端圆 + 中间矩形」
            drawLine(
                color = segColor,
                start = Offset(cx + rInner * c, cy + rInner * s),
                end = Offset(cx + rOuter * c, cy + rOuter * s),
                strokeWidth = 2 * r,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(name = "有数据", showBackground = true, widthDp = 360, heightDp = 260)
@Composable
private fun DRefreshListPreview() {
    AppComposeTheme {
        DRefreshList(isRefreshing = false, onRefresh = {}, onLoadMore = {}) {
            items(10) { index ->
                Column {
                    Text(
                        text = "第 ${index + 1} 条数据",
                        color = AppColors.BasicOne,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    HorizontalDivider(color = AppColors.DividerColor)
                }
            }
        }
    }
}

@Preview(name = "刷新中", showBackground = true, widthDp = 360, heightDp = 260)
@Composable
private fun DRefreshListRefreshingPreview() {
    AppComposeTheme {
        DRefreshList(isRefreshing = true, onRefresh = {}, onLoadMore = {}) {
            items(6) { index ->
                Text(
                    text = "第 ${index + 1} 条数据",
                    color = AppColors.BasicOne,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Preview(name = "空数据", showBackground = true, widthDp = 360, heightDp = 200)
@Composable
private fun DRefreshListEmptyPreview() {
    AppComposeTheme {
        DRefreshList(isRefreshing = false, onRefresh = {}, onLoadMore = {}, isEmpty = true) {}
    }
}
