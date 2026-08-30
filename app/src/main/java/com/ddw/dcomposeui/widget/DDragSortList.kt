package com.ddw.dcomposeui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * 记住 LazyColumn 拖拽排序状态。实现参考 Calvin-LL/Reorderable：以 key 跟踪被拖项、拖到边缘持续自动滚动、
 * 交换涉及首个可见项时用 requestScrollToItem 补偿避免跳动、交换后等布局更新避免“飘”。
 *
 * @param lazyListState 目标 LazyColumn 的滚动状态
 * @param onMove 交换回调（from -> to），业务侧据此调整数据顺序（建议用 SnapshotStateList 同步更新）
 */
@Composable
fun rememberDDragSortState(
    lazyListState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit
): DDragSortState {
    val scope = rememberCoroutineScope()
    val scrollThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    return remember(lazyListState) {
        DDragSortState(lazyListState, scope, onMove, scrollThresholdPx)
    }
}

/**
 * 类描述：LazyColumn 拖拽排序状态，以 item 的 key 跟踪被拖项，依据布局信息交换并在边缘持续自动滚动
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 10:30
 */
class DDragSortState internal constructor(
    private val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
    private val scrollThreshold: Float
) {
    /** 当前被拖项的 key（null 表示未拖拽） */
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    /** 刚放手的项 key（用于落位回弹动画，动画结束后置空） */
    var previousDraggingItemKey by mutableStateOf<Any?>(null)
        private set

    /** 刚放手项的回弹平移（从松手时的位移动画回落到 0） */
    internal val previousDraggingItemOffset = Animatable(0f)

    /** 手指相对拖拽起点的累计位移 */
    private var draggedDelta by mutableFloatStateOf(0f)

    /** 拖拽起始时被拖项的 offset */
    private var initialOffset by mutableIntStateOf(0)

    /** 拖拽期间的每帧处理任务（统一做交换与边缘滚动） */
    private var dragJob: Job? = null

    // layoutInfo 在 onMove 后不会立刻更新，这段时间用预测 offset，避免被拖项瞬间“飘”
    private var oldDraggingItemIndex: Int? = null
    private var predictedDraggingItemOffset: Int? = null

    /** 被拖项当前布局信息（按 key 匹配） */
    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }

    /** 被拖项视觉平移量（跟手；交换后布局未刷新期间用预测 offset） */
    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            val offset = if (item.index != oldDraggingItemIndex || oldDraggingItemIndex == null) {
                oldDraggingItemIndex = null
                predictedDraggingItemOffset = null
                item.offset
            } else {
                predictedDraggingItemOffset ?: item.offset
            }
            draggedDelta + (initialOffset - offset)
        } ?: 0f

    /** 是否正在拖拽指定 key */
    fun isDragging(key: Any?): Boolean = key != null && key == draggingItemKey

    /** 开始拖拽：按 key 记录被拖项及其起始 offset，并启动每帧处理循环 */
    internal fun onDragStart(key: Any) {
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.also {
            draggingItemKey = key
            initialOffset = it.offset
        }
        dragJob?.cancel()
        dragJob = scope.launch {
            while (isActive) {
                // 先处理边缘滚动，再处理交换（交换内部会等待布局刷新）
                val amount = checkForOverScroll()
                if (amount != 0f) lazyListState.scrollBy(amount.coerceIn(-MAX_SCROLL_STEP, MAX_SCROLL_STEP))
                moveIfNeeded()
                withFrameNanos { }
            }
        }
    }

    /** 拖拽移动：仅累计位移，交换与滚动由每帧循环统一处理 */
    internal fun onDrag(offsetY: Float) {
        draggedDelta += offsetY
    }

    /** 结束 / 取消拖拽：重置并停止循环 */
    internal fun onDragStopped() {
        // 放手后让被拖项从当前位移平滑回落到目标格子，避免瞬间跳位
        val key = draggingItemKey
        if (key != null) {
            val startOffset = draggingItemOffset
            previousDraggingItemKey = key
            scope.launch {
                previousDraggingItemOffset.snapTo(startOffset)
                previousDraggingItemOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                previousDraggingItemKey = null
            }
        }
        draggingItemKey = null
        draggedDelta = 0f
        initialOffset = 0
        oldDraggingItemIndex = null
        predictedDraggingItemOffset = null
        dragJob?.cancel()
        dragJob = null
    }

    /** 命中目标项（被拖项矩形包含目标项中心）则交换，并等待布局刷新以避免连续误交换 */
    private suspend fun moveIfNeeded() {
        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key != draggingItemKey &&
                (item.offset + item.size / 2f) in startOffset..endOffset
        } ?: return
        if (draggingItem.index == targetItem.index) return

        // 交换涉及首个可见项时，补偿滚动位置，避免 LazyColumn 自动重锚导致整列跳动
        if (draggingItem.index == lazyListState.firstVisibleItemIndex ||
            targetItem.index == lazyListState.firstVisibleItemIndex
        ) {
            lazyListState.requestScrollToItem(
                lazyListState.firstVisibleItemIndex,
                lazyListState.firstVisibleItemScrollOffset
            )
        }

        oldDraggingItemIndex = draggingItem.index
        onMove(draggingItem.index, targetItem.index)
        // 预测交换后被拖项应处的 offset（布局刷新前先用它，避免“飘”）
        predictedDraggingItemOffset = if (targetItem.index > draggingItem.index) {
            (targetItem.offset + targetItem.size) - draggingItem.size
        } else {
            targetItem.offset
        }
        // 等布局刷新（当前值 + 更新后值）后再允许下一次交换
        runCatching {
            withTimeout(LAYOUT_UPDATE_TIMEOUT) {
                snapshotFlow { lazyListState.layoutInfo }.take(2).collect()
            }
        }
        oldDraggingItemIndex = null
        predictedDraggingItemOffset = null
    }

    /** 计算距上下边缘阈值内需要滚动的量（0 表示无需滚动） */
    private fun checkForOverScroll(): Float {
        val draggingItem = draggingItemLayoutInfo ?: return 0f
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val viewStart = lazyListState.layoutInfo.viewportStartOffset
        val viewEnd = lazyListState.layoutInfo.viewportEndOffset
        return when {
            draggedDelta > 0 -> (endOffset - viewEnd + scrollThreshold).coerceAtLeast(0f)
            draggedDelta < 0 -> (startOffset - viewStart - scrollThreshold).coerceAtMost(0f)
            else -> 0f
        }
    }

    private companion object {
        /** 每帧自动滚动的最大步长（px），避免过快 */
        const val MAX_SCROLL_STEP = 18f

        /** 等待布局刷新的超时（ms），防止极端情况卡住 */
        val LAYOUT_UPDATE_TIMEOUT = 1000.milliseconds
    }
}

/**
 * 类描述：可拖拽排序项的作用域，向 item 内容暴露拖拽把手 Modifier
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 10:30
 */
interface DDragSortItemScope {
    /** 按下即可拖拽（适合专用拖动手柄图标） */
    fun Modifier.draggableHandle(enabled: Boolean = true): Modifier

    /** 长按后可拖拽（适合整行拖拽） */
    fun Modifier.longPressDraggableHandle(enabled: Boolean = true): Modifier
}

/** DDragSortItemScope 实现：把手势转交给共享的 DDragSortState */
private class DragSortItemScopeImpl(
    private val state: DDragSortState,
    private val key: Any
) : DDragSortItemScope {
    override fun Modifier.draggableHandle(enabled: Boolean): Modifier =
        this.pointerInput(state, key, enabled) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { state.onDragStart(key) },
                onDrag = { change, dragAmount -> change.consume(); state.onDrag(dragAmount.y) },
                onDragEnd = { state.onDragStopped() },
                onDragCancel = { state.onDragStopped() }
            )
        }

    override fun Modifier.longPressDraggableHandle(enabled: Boolean): Modifier =
        this.pointerInput(state, key, enabled) {
            if (!enabled) return@pointerInput
            detectDragGesturesAfterLongPress(
                onDragStart = { state.onDragStart(key) },
                onDrag = { change, dragAmount -> change.consume(); state.onDrag(dragAmount.y) },
                onDragEnd = { state.onDragStopped() },
                onDragCancel = { state.onDragStopped() }
            )
        }
}

/**
 * 组件描述：可拖拽排序的列表项，包裹在 LazyColumn 的 item 中。
 * 拖拽项自动置顶并跟手平移；content 提供 isDragging，并在其内用 longPressDraggableHandle/draggableHandle 指定拖拽区域。
 *
 * @param state rememberDDragSortState 返回的状态
 * @param key 该项唯一 key（需与 LazyColumn items 的 key 一致）
 * @param modifier 外部布局修饰
 * @param content 内容，参数为是否正在拖拽
 */
@Composable
fun LazyItemScope.DDragSortItem(
    state: DDragSortState,
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable DDragSortItemScope.(isDragging: Boolean) -> Unit
) {
    val dragging = state.isDragging(key)
    val itemModifier = when {
        // 正在拖拽：置顶并跟手平移
        dragging -> modifier
            .zIndex(1f)
            .graphicsLayer { translationY = state.draggingItemOffset }
        // 刚放手：置顶并做回落动画
        key == state.previousDraggingItemKey -> modifier
            .zIndex(1f)
            .graphicsLayer { translationY = state.previousDraggingItemOffset.value }
        // 其余项：交换时做位置动画，丝滑让位
        else -> modifier.animateItem()
    }
    Box(itemModifier) {
        val scope = remember(state, key) { DragSortItemScopeImpl(state, key) }
        scope.content(dragging)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 220)
@Composable
private fun DDragSortListPreview() {
    val data = remember { mutableStateListOf("裁剪", "缝制", "水洗", "质检", "包装") }
    val listState = rememberLazyListState()
    val dragState = rememberDDragSortState(listState) { from, to ->
        data.add(to, data.removeAt(from))
    }
    AppComposeTheme {
        LazyColumn(state = listState) {
            itemsIndexed(data, key = { _, item -> item }) { index, item ->
                DDragSortItem(state = dragState, key = item) { isDragging ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDragging) AppColors.ButtonThree else Color.White)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. $item",
                            color = AppColors.BasicOne,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        // 把手上按下即可拖动；想整行长按拖动就把 longPressDraggableHandle 加到 Row 上
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = "拖动排序",
                            tint = AppColors.BasicThree,
                            modifier = Modifier.draggableHandle()
                        )
                    }
                }
            }
        }
    }
}
