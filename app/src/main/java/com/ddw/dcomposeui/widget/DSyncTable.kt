package com.ddw.dcomposeui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ddw.dcomposeui.theme.AppColors

/**
 * 类描述：表格列定义——只描述一列的宽度、是否固定、对齐方式与表头内容。
 * cell 是「标准数据行」各列的渲染逻辑（接收整行对象，可组合任意多个字段）；
 * 异构行（小计/合计/分组头/合并行）不走 cell，由 DSL 的 columnRow/stickyFooterRow/fullSpanRow/mergedItem 单独提供。
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/06 10:00
 * @param T 行数据类型
 */
data class DTableColumn<T>(
    /** 列宽 */
    val width: Dp,
    /** 是否为固定列（不随横向滚动，通常放最左，如序号/商品列） */
    val frozen: Boolean = false,
    /** 单元格内容对齐方式（默认居中；多字段/文本列常用 CenterStart） */
    val alignment: Alignment = Alignment.Center,
    /** 表头内容（任意可组合，可含图标/点击/提示；无表头行场景可省略） */
    val header: @Composable () -> Unit = {},
    /** 标准数据行单元格内容（接收整行对象，可组合图片/文本/标签等多字段；仅 items 使用） */
    val cell: @Composable (T) -> Unit = {},
)

/** 表格默认样式 */
object DSyncTableDefaults {
    /** 表头背景 */
    val HeaderBackground = AppColors.LineOne
    /** 单元格背景 */
    val CellBackground = AppColors.BgPageFour
    /** 汇总/小计行背景 */
    val SummaryBackground = AppColors.BgPageThree
    /** 网格线颜色 */
    val GridLine = AppColors.LineOne
    /** 表头文字色 */
    val HeaderTextColor = AppColors.BasicOne
    /** 单元格文字色 */
    val CellTextColor = AppColors.BasicOne
    /** 占位文字色（如「请选择/请输入」） */
    val PlaceholderColor = AppColors.BasicThree
    /** 文字号 */
    val TextSize = 13.sp
    /** 默认表头行高 */
    val HeaderHeight = 28.dp
    /** 默认内容行高 */
    val RowHeight = 44.dp
}

/**
 * DSL 作用域：在 DSyncTable 内声明各类行（可任意顺序混排）。
 * - item/items：标准数据行，用各列 column.cell 渲染，参与横向联动
 * - columnRow：自定义按列行（如组内小计），各列内容自定，参与横向联动
 * - fullSpanRow：整行贯通行（如分组头/横幅），横跨可视宽度、不横向滚动
 * - stickyFooterRow：粘性底部行（如合计），固定在表格底部，数据超一屏时不随纵向滚动
 */
interface DSyncTableScope<T> {
    /**
     * 单条标准数据行（用各列 column.cell 渲染）
     * @param data 行数据
     * @param key 稳定 key
     * @param height 固定行高（默认取组件 rowHeight；与 minHeight 二选一）
     * @param minHeight 最小行高，设置后该行按内容自适应（行高≥此值，行内各列等高对齐）
     * @param background 背景色（默认取组件 cellBackground）
     */
    fun item(data: T, key: Any? = null, height: Dp? = null, minHeight: Dp? = null, background: Color? = null)

    /** 批量标准数据行 */
    fun items(items: List<T>, key: ((T) -> Any)? = null, height: Dp? = null, minHeight: Dp? = null, background: Color? = null)

    /**
     * 自定义按列行（各列内容自定，参与横向联动）；跟随数据一起纵向滚动，用于组内小计等异构行。
     * 需要「固定在表格底部的合计」请用 [stickyFooterRow]。
     * @param cell 按列索引返回该格内容（columnIndex 对应 columns 下标；无内容返回空即可）
     */
    fun columnRow(
        key: Any? = null,
        height: Dp? = null,
        minHeight: Dp? = null,
        background: Color = DSyncTableDefaults.SummaryBackground,
        cell: @Composable (columnIndex: Int) -> Unit,
    )

    /**
     * 粘性底部行（sticky footer，固定在表格底部，参与横向联动）；无论声明顺序都渲染在最底部，常用于合计。
     * 数据没占满一屏时紧跟在数据下方，数据超过一屏时固定在底部、不随纵向滚动。可多次调用（依次堆在底部）。
     * 要让「超过一屏固定底部」生效，需给表格一个确定的高度（如父容器 fillMaxHeight 或固定高度）。
     * @param cell 按列索引返回该格内容（columnIndex 对应 columns 下标；无内容返回空即可）
     */
    fun stickyFooterRow(
        key: Any? = null,
        height: Dp? = null,
        minHeight: Dp? = null,
        background: Color = DSyncTableDefaults.SummaryBackground,
        cell: @Composable (columnIndex: Int) -> Unit,
    )

    /**
     * 整行贯通行（横跨可视宽度、不横向滚动）；用于分组头、说明横幅等
     * @param content 整行内容
     */
    fun fullSpanRow(
        key: Any? = null,
        background: Color = DSyncTableDefaults.CellBackground,
        content: @Composable () -> Unit,
    )

    /**
     * 纵向合并行：一条主记录带 [subRowCount] 条子记录，[mergeColumns] 指定的列合并成一格跨满整组高度，
     * 其余列各自竖排 [subRowCount] 个子格。适合「工序单明细 + 多个做工人」这类一对多表格。
     *
     * 合并列用列定义里的 cell(data) 渲染，子格内容由 [subCell] 按 (列下标, 子行下标) 提供。
     * 整组是一个 LazyColumn item，组内子行会一次性全部测量，子行数别太大（几十条以内没问题）。
     *
     * @param data 主记录，供合并列渲染
     * @param subRowCount 子行数量，传 0 时按 1 处理（子格留空）
     * @param mergeColumns 需要纵向合并的列下标集合
     * @param subRowHeight 固定子行高度，合并格总高 = subRowCount × subRowHeight；内容超高会被裁
     * @param minSubRowHeight 最小子行高度。传了它就按内容自适应：每个子行取「该子行在所有拆分列中的最高内容」
     * 为行高（不低于此值），子格内文本可以换行；合并格总高随之变化，各列子行边界仍然对齐
     * @param key 稳定 key
     * @param background 背景色（默认取组件 cellBackground）
     * @param subCell 非合并列的子格内容
     */
    fun mergedItem(
        data: T,
        subRowCount: Int,
        mergeColumns: Set<Int>,
        subRowHeight: Dp = DSyncTableDefaults.RowHeight,
        minSubRowHeight: Dp? = null,
        key: Any? = null,
        background: Color? = null,
        subCell: @Composable (columnIndex: Int, subIndex: Int) -> Unit,
    )
}

/** 行定义（内部）：按列联动行、整行贯通行、纵向合并行三类 */
private sealed interface TableRowSpec {
    val key: Any?

    /** 按列布局行（固定列 + 横向滚动列，参与联动）；数据行、小计行、合计行都属此类。adaptive=true 时 height 作为最小行高 */
    class ColumnRow(
        override val key: Any?,
        val height: Dp,
        val adaptive: Boolean,
        val background: Color,
        val cell: @Composable (columnIndex: Int) -> Unit,
    ) : TableRowSpec

    /** 整行贯通行（不横向滚动） */
    class FullSpanRow(
        override val key: Any?,
        val background: Color,
        val content: @Composable () -> Unit,
    ) : TableRowSpec

    /**
     * 纵向合并行：合并列画一格跨满整组高，其余列竖排 subRowCount 个子格。
     * adaptive=true 时 subRowHeight 作为子行最小高度，实际行高按内容测量
     */
    class MergedRow(
        override val key: Any?,
        val subRowCount: Int,
        val subRowHeight: Dp,
        val adaptive: Boolean,
        val background: Color,
        val mergeColumns: Set<Int>,
        val mergedCell: @Composable (columnIndex: Int) -> Unit,
        val subCell: @Composable (columnIndex: Int, subIndex: Int) -> Unit,
    ) : TableRowSpec
}

/** DSyncTableScope 实现：普通行收进 specs（供 LazyColumn 渲染），合计行收进 footerSpecs（固定底部） */
private class DSyncTableScopeImpl<T>(
    private val columns: List<DTableColumn<T>>,
    private val defaultRowHeight: Dp,
    private val defaultMinRowHeight: Dp?,
    private val defaultCellBackground: Color,
) : DSyncTableScope<T> {

    val specs = mutableListOf<TableRowSpec>()

    /** 粘性底部行（固定在表格底部，常用于合计），与普通行分开收集，不进 LazyColumn */
    val footerSpecs = mutableListOf<TableRowSpec.ColumnRow>()

    /** 解析行高：行级 minHeight（自适应）> 行级 height（固定）> 组件级 minRowHeight（自适应）> 组件级 rowHeight（固定） */
    private fun resolveHeight(height: Dp?, minHeight: Dp?): Pair<Dp, Boolean> = when {
        minHeight != null -> minHeight to true
        height != null -> height to false
        defaultMinRowHeight != null -> defaultMinRowHeight to true
        else -> defaultRowHeight to false
    }

    override fun item(data: T, key: Any?, height: Dp?, minHeight: Dp?, background: Color?) {
        val (h, adaptive) = resolveHeight(height, minHeight)
        specs.add(
            TableRowSpec.ColumnRow(
                key = key,
                height = h,
                adaptive = adaptive,
                background = background ?: defaultCellBackground,
                cell = { columnIndex -> columns[columnIndex].cell(data) },
            )
        )
    }

    override fun items(items: List<T>, key: ((T) -> Any)?, height: Dp?, minHeight: Dp?, background: Color?) {
        items.forEach { data -> item(data, key?.invoke(data), height, minHeight, background) }
    }

    override fun columnRow(key: Any?, height: Dp?, minHeight: Dp?, background: Color, cell: @Composable (Int) -> Unit) {
        val (h, adaptive) = resolveHeight(height, minHeight)
        specs.add(TableRowSpec.ColumnRow(key, h, adaptive, background, cell))
    }

    override fun stickyFooterRow(key: Any?, height: Dp?, minHeight: Dp?, background: Color, cell: @Composable (Int) -> Unit) {
        val (h, adaptive) = resolveHeight(height, minHeight)
        footerSpecs.add(TableRowSpec.ColumnRow(key, h, adaptive, background, cell))
    }

    override fun fullSpanRow(key: Any?, background: Color, content: @Composable () -> Unit) {
        specs.add(TableRowSpec.FullSpanRow(key, background, content))
    }

    override fun mergedItem(
        data: T,
        subRowCount: Int,
        mergeColumns: Set<Int>,
        subRowHeight: Dp,
        minSubRowHeight: Dp?,
        key: Any?,
        background: Color?,
        subCell: @Composable (columnIndex: Int, subIndex: Int) -> Unit,
    ) {
        specs.add(
            TableRowSpec.MergedRow(
                key = key,
                // 没有子记录时也要占一行，否则整组高度为 0、合并列内容看不见
                subRowCount = subRowCount.coerceAtLeast(1),
                // 传了 minSubRowHeight 就走自适应，它同时作为子行高度下限
                subRowHeight = minSubRowHeight ?: subRowHeight,
                adaptive = minSubRowHeight != null,
                background = background ?: defaultCellBackground,
                mergeColumns = mergeColumns,
                mergedCell = { columnIndex -> columns[columnIndex].cell(data) },
                subCell = subCell,
            )
        )
    }
}

/**
 * 组件描述：横向联动滚动表格（左侧固定列 + 右侧多列横向滚动，整体纵向滚动）。
 * 表头与所有行共享同一个横向滚动状态实现联动，替代传统 View 里手动维护的 scrollViews + recordX。
 * 行内容用 DSL 声明：items（标准数据行）、columnRow（组内小计）、fullSpanRow（分组贯通行）、
 * mergedItem（纵向合并行）可任意混排；stickyFooterRow（粘性底部行，常用于合计）固定在表格底部，数据超过一屏时不随纵向滚动。
 * @param columns 列定义（宽度/是否固定/表头/标准行 cell）
 * @param showHeader 是否显示表头行（false 时无标题行，直接展示数据）
 * @param headerHeight 固定表头行高（未设置 minHeaderHeight 时生效）
 * @param minHeaderHeight 最小表头高；设置后按标题内容自适应（≥此值、各列等高），标题需换行时用它，否则超出会被裁
 * @param rowHeight 默认固定内容行高（未设置 minRowHeight 时生效）
 * @param minRowHeight 最小行高；设置后按内容自适应（行高≥此值、行内各列等高），不设则用固定 rowHeight
 * @param headerBackground 表头背景色
 * @param cellBackground 单元格背景色
 * @param gridLineColor 网格线颜色
 * @param scrollShadow 横向滚动时是否在固定列右侧显示阴影
 * @param emptyText 无任何行时展示的文案（默认「暂无数据」）
 * @param content 行声明 DSL
 *
 * 性能评估须用 release 包：debug 包关闭 R8、带组合追踪开销，且 isDebuggable=true 会让 ART 放弃部分优化，
 * Compose 会慢好几倍。本表格在低端机 debug 下滚动明显卡顿、同一版本 release 下顺滑，
 * 属于 debug 开销而非实现问题——别照着 debug 的表现去优化。
 */
@Composable
fun <T> DSyncTable(
    columns: List<DTableColumn<T>>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    headerHeight: Dp = DSyncTableDefaults.HeaderHeight,
    minHeaderHeight: Dp? = null,
    rowHeight: Dp = DSyncTableDefaults.RowHeight,
    minRowHeight: Dp? = null,
    headerBackground: Color = DSyncTableDefaults.HeaderBackground,
    cellBackground: Color = DSyncTableDefaults.CellBackground,
    gridLineColor: Color = DSyncTableDefaults.GridLine,
    scrollShadow: Boolean = true,
    emptyText: String = "暂无数据",
    content: DSyncTableScope<T>.() -> Unit,
) {
    // 表头与所有行共用同一个横向滚动状态 —— 这是「横向联动」的关键
    val hScroll = rememberScrollState()
    // 带原始下标的列（渲染时用下标回取 cell/header，保证固定列拆分后仍能对应）
    val indexedColumns = columns.withIndex().toList()
    val frozen = indexedColumns.filter { it.value.frozen }
    val scrollable = indexedColumns.filterNot { it.value.frozen }

    // 收集行定义（每次重组重新收集，lambda 捕获最新数据）
    val scope = DSyncTableScopeImpl(columns, rowHeight, minRowHeight, cellBackground).apply(content)
    val specs = scope.specs
    val footerSpecs = scope.footerSpecs

    Column(modifier) {
        // 表头行（showHeader=false 时无标题行，直接展示数据）
        if (showHeader) {
            ColumnRowLayout(
                frozen = frozen,
                scrollable = scrollable,
                hScroll = hScroll,
                rowHeight = minHeaderHeight ?: headerHeight,
                adaptive = minHeaderHeight != null,
                background = headerBackground,
                gridLineColor = gridLineColor,
                scrollShadow = scrollShadow,
            ) { columnIndex -> columns[columnIndex].header() }
        }

        if (specs.isEmpty()) {
            // 无任何数据行：只展示「暂无数据」，不显示合计行（没有数据，合计无意义）
            TableEmpty(text = emptyText)
        } else {
            // 有粘性底部行时数据区用 weight(fill=false)：数据没占满一屏就按内容高度收缩、让底部行紧跟其后；
            // 数据超过剩余空间则占满并内部滚动，把底部行顶到底部固定。无粘性底部行时保持原样（不加 weight）。
            val dataModifier = if (footerSpecs.isNotEmpty()) Modifier.weight(1f, fill = false) else Modifier
            LazyColumn(dataModifier) {
                itemsIndexed(specs, key = { index, spec -> spec.key ?: index }) { _, spec ->
                    when (spec) {
                        is TableRowSpec.ColumnRow -> ColumnRowLayout(
                            frozen = frozen,
                            scrollable = scrollable,
                            hScroll = hScroll,
                            rowHeight = spec.height,
                            adaptive = spec.adaptive,
                            background = spec.background,
                            gridLineColor = gridLineColor,
                            scrollShadow = scrollShadow,
                            cellContent = spec.cell,
                        )

                        is TableRowSpec.FullSpanRow -> FullSpanRowLayout(
                            background = spec.background,
                            gridLineColor = gridLineColor,
                            content = spec.content,
                        )

                        is TableRowSpec.MergedRow -> MergedRowLayout(
                            frozen = frozen,
                            scrollable = scrollable,
                            hScroll = hScroll,
                            subRowCount = spec.subRowCount,
                            subRowHeight = spec.subRowHeight,
                            adaptive = spec.adaptive,
                            background = spec.background,
                            gridLineColor = gridLineColor,
                            scrollShadow = scrollShadow,
                            mergeColumns = spec.mergeColumns,
                            mergedCell = spec.mergedCell,
                            subCell = spec.subCell,
                        )
                    }
                }
            }

            // 粘性底部行：固定在表格底部（不随 LazyColumn 纵向滚动），与数据行共享 hScroll 横向联动
            footerSpecs.forEach { spec ->
                ColumnRowLayout(
                    frozen = frozen,
                    scrollable = scrollable,
                    hScroll = hScroll,
                    rowHeight = spec.height,
                    adaptive = spec.adaptive,
                    background = spec.background,
                    gridLineColor = gridLineColor,
                    scrollShadow = scrollShadow,
                    cellContent = spec.cell,
                )
            }
        }
    }
}

/**
 * 组件描述：横向联动表格的表头行（拆开单用版）。
 * [DSyncTable] 自带表头，但它内部是 LazyColumn，无法嵌进 DRefreshList 这类分页列表
 * （内层会拿到无限高度约束而崩）。需要「表格 + 下拉刷新/上拉加载」时改用这一对：
 * 表头常驻列表之外，数据行用 [DSyncTableRow] 摊进列表的 items，两者传同一个 hScroll 即可横向联动。
 * @param hScroll 与各数据行共享的横向滚动状态，横向联动就靠它
 * @param minHeight 设置后按标题内容自适应表头高（各列等高、不低于该值）；不设则用固定 height，标题换行超出会被裁
 */
@Composable
fun <T> DSyncTableHeader(
    columns: List<DTableColumn<T>>,
    hScroll: ScrollState,
    height: Dp = DSyncTableDefaults.HeaderHeight,
    minHeight: Dp? = null,
    background: Color = DSyncTableDefaults.HeaderBackground,
    gridLineColor: Color = DSyncTableDefaults.GridLine,
    scrollShadow: Boolean = true,
) {
    val indexed = columns.withIndex().toList()
    ColumnRowLayout(
        frozen = indexed.filter { it.value.frozen },
        scrollable = indexed.filterNot { it.value.frozen },
        hScroll = hScroll,
        rowHeight = minHeight ?: height,
        adaptive = minHeight != null,
        background = background,
        gridLineColor = gridLineColor,
        scrollShadow = scrollShadow,
    ) { columnIndex -> columns[columnIndex].header() }
}

/**
 * 组件描述：横向联动表格的一行数据（拆开单用版，配合 [DSyncTableHeader]）。
 * 放进 LazyColumn / DRefreshList 的 items 中，与表头传同一个 hScroll 即可横向联动。
 * @param hScroll 与表头共享的横向滚动状态
 * @param minHeight 设置后按内容自适应行高（行内各列等高、不低于该值）；不设则用固定 height
 */
@Composable
fun <T> DSyncTableRow(
    columns: List<DTableColumn<T>>,
    data: T,
    hScroll: ScrollState,
    height: Dp = DSyncTableDefaults.RowHeight,
    minHeight: Dp? = null,
    background: Color = DSyncTableDefaults.CellBackground,
    gridLineColor: Color = DSyncTableDefaults.GridLine,
    scrollShadow: Boolean = true,
) {
    val indexed = columns.withIndex().toList()
    ColumnRowLayout(
        frozen = indexed.filter { it.value.frozen },
        scrollable = indexed.filterNot { it.value.frozen },
        hScroll = hScroll,
        rowHeight = minHeight ?: height,
        adaptive = minHeight != null,
        background = background,
        gridLineColor = gridLineColor,
        scrollShadow = scrollShadow,
    ) { columnIndex -> columns[columnIndex].cell(data) }
}

/**
 * 组件描述：按列布局的一行（固定列区 + 横向滚动列区），表头/数据行/小计行复用同一套结构
 * @param cellContent 按列原始下标返回该格内容（表头传 header，数据行传 cell，小计行自定义）
 */
@Composable
private fun <T> ColumnRowLayout(
    frozen: List<IndexedValue<DTableColumn<T>>>,
    scrollable: List<IndexedValue<DTableColumn<T>>>,
    hScroll: ScrollState,
    rowHeight: Dp,
    adaptive: Boolean,
    background: Color,
    gridLineColor: Color,
    scrollShadow: Boolean,
    cellContent: @Composable (columnIndex: Int) -> Unit,
) {
    // 自适应：外层按内容最小固有高度收缩，各列区/单元格 fillMaxHeight 实现行内等高（≥rowHeight 作为最小值）
    // 固定：单元格直接用固定 rowHeight
    val rowModifier = if (adaptive) Modifier.height(IntrinsicSize.Min) else Modifier
    val sectionModifier = if (adaptive) Modifier.fillMaxHeight() else Modifier
    val cellHeightModifier = if (adaptive) Modifier.fillMaxHeight().heightIn(min = rowHeight) else Modifier.height(rowHeight)

    Row(rowModifier) {
        // 固定列区：zIndex 抬高使其绘制在滚动区之上，并在右缘绘制横滑阴影
        Row(
            modifier = sectionModifier
                .zIndex(1f)
                .drawWithContent {
                    drawContent()
                    if (scrollShadow && hScroll.value > 0) {
                        val shadowWidth = 6.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0x1A000000), Color.Transparent),
                                startX = size.width,
                                endX = size.width + shadowWidth,
                            ),
                            topLeft = Offset(size.width, 0f),
                            size = Size(shadowWidth, size.height),
                        )
                    }
                },
        ) {
            frozen.forEach { iv ->
                TableCellBox(iv.value, cellHeightModifier, background, gridLineColor) { cellContent(iv.index) }
            }
        }
        // 横向滚动列区：与表头共享 hScroll
        Row(modifier = sectionModifier.horizontalScroll(hScroll)) {
            scrollable.forEach { iv ->
                TableCellBox(iv.value, cellHeightModifier, background, gridLineColor) { cellContent(iv.index) }
            }
        }
    }
}

/**
 * 组件描述：纵向合并行的布局，按固定或自适应两种方式确定各子行高度。
 *
 * 固定模式：每个子行都是 subRowHeight，整组高 = subRowCount × subRowHeight，内容超高会被裁。
 *
 * 自适应模式：同一子行的高度必须在所有拆分列之间保持一致，否则子行边界会错位；
 * 而这些格子分散在各列的 Column 里，Compose 没有跨父节点同步高度的机制。
 * 所以先用 SubcomposeLayout 把每个拆分列的每个子格按列宽试测一遍，
 * 取「该子行在各拆分列中的最大高度」作为行高，再拿这套高度正式渲染。
 * 代价是拆分列的内容会被组合两次（试测那趟只测量不绘制）。
 *
 * 性能注意：试测的所有格子（拆分列子格 + 合并列）合并在一个 subcompose 里一次性组合，
 * 靠发射顺序与 measurable 顺序一一对应来取值。不要退回「每格一个 subcompose」的写法——
 * 那样一行会新建十几个 composition，滚动时每有新行进入可视区都要付这笔开销。
 *
 * @param adaptive 是否按内容自适应子行高度，false 时 subRowHeight 为固定值、true 时为最小值
 * @param mergeColumns 需要纵向合并的列下标
 * @param mergedCell 合并列内容
 * @param subCell 子格内容，参数为 (列下标, 子行下标)
 */
@Composable
private fun <T> MergedRowLayout(
    frozen: List<IndexedValue<DTableColumn<T>>>,
    scrollable: List<IndexedValue<DTableColumn<T>>>,
    hScroll: ScrollState,
    subRowCount: Int,
    subRowHeight: Dp,
    adaptive: Boolean,
    background: Color,
    gridLineColor: Color,
    scrollShadow: Boolean,
    mergeColumns: Set<Int>,
    mergedCell: @Composable (columnIndex: Int) -> Unit,
    subCell: @Composable (columnIndex: Int, subIndex: Int) -> Unit,
) {
    if (!adaptive) {
        // 固定高度：各子行等高，直接渲染，不需要预测量
        MergedRowContent(
            frozen = frozen,
            scrollable = scrollable,
            hScroll = hScroll,
            subHeights = List(subRowCount) { subRowHeight },
            background = background,
            gridLineColor = gridLineColor,
            scrollShadow = scrollShadow,
            mergeColumns = mergeColumns,
            mergedCell = mergedCell,
            subCell = subCell,
        )
        return
    }

    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val minSubHeightPx = subRowHeight.roundToPx()
        val allColumns = frozen + scrollable
        val splitColumns = allColumns.filterNot { it.index in mergeColumns }
        val mergedColumns = allColumns.filter { it.index in mergeColumns }

        // 第一趟：试测所有格子。全部放进同一个 subcompose 一次性组合（顺序：各拆分列的子格 → 各合并列），
        // 再按同样顺序取回 measurable 逐个测量。若逐格单独 subcompose，一行要新建十几个 composition，
        // 滚动时每有新行进入可视区都付这笔开销，低端机会明显掉帧
        val probeMeasurables = subcompose("probe") {
            splitColumns.forEach { iv ->
                repeat(subRowCount) { subIndex ->
                    // 内边距要与真实子格一致，否则可用宽度不同、测出的换行行数也不同
                    Box(modifier = Modifier.padding(horizontal = TableCellPadding)) {
                        subCell(iv.index, subIndex)
                    }
                }
            }
            mergedColumns.forEach { iv ->
                Box(modifier = Modifier.padding(horizontal = TableCellPadding)) {
                    mergedCell(iv.index)
                }
            }
        }
        var probeIndex = 0

        // 拆分列：同一子行取各列中的最大高度
        val subHeightsPx = IntArray(subRowCount) { minSubHeightPx }
        splitColumns.forEach { iv ->
            val cellWidthPx = iv.value.width.roundToPx()
            repeat(subRowCount) { subIndex ->
                val height = probeMeasurables[probeIndex++]
                    .measure(Constraints(minWidth = cellWidthPx, maxWidth = cellWidthPx))
                    .height
                subHeightsPx[subIndex] = maxOf(subHeightsPx[subIndex], height)
            }
        }

        // 合并列自己也可能比子行加起来还高（比如规格换了三行）
        var mergedNeedPx = 0
        mergedColumns.forEach { iv ->
            val cellWidthPx = iv.value.width.roundToPx()
            val height = probeMeasurables[probeIndex++]
                .measure(Constraints(minWidth = cellWidthPx, maxWidth = cellWidthPx))
                .height
            mergedNeedPx = maxOf(mergedNeedPx, height)
        }
        // 合并列更高时把差额摊到各子行，两边总高才能保持相等
        val sumPx = subHeightsPx.sum()
        if (mergedNeedPx > sumPx) {
            val extra = mergedNeedPx - sumPx
            val per = extra / subRowCount
            val remainder = extra % subRowCount
            for (i in 0 until subRowCount) {
                subHeightsPx[i] += per + if (i < remainder) 1 else 0
            }
        }

        val totalHeightPx = subHeightsPx.sum()
        val subHeights = subHeightsPx.map { it.toDp() }

        // 第二趟：拿测好的高度正式渲染
        val placeables = subcompose("content") {
            MergedRowContent(
                frozen = frozen,
                scrollable = scrollable,
                hScroll = hScroll,
                subHeights = subHeights,
                background = background,
                gridLineColor = gridLineColor,
                scrollShadow = scrollShadow,
                mergeColumns = mergeColumns,
                mergedCell = mergedCell,
                subCell = subCell,
            )
        }.map {
            it.measure(constraints.copy(minHeight = totalHeightPx, maxHeight = totalHeightPx))
        }

        layout(constraints.maxWidth, totalHeightPx) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

/**
 * 组件描述：合并行的实际渲染，各子行高度由 [subHeights] 给定。
 * 合并列画一格占满整组高，其余列按 subHeights 竖排子格。
 * 子格各自带 border，堆叠后组内横线自然形成，无需额外画分割线；
 * 又因 border 画在元素内侧不占尺寸，合并格与子格总高严格相等，左右能对齐。
 *
 * @param subHeights 每个子行的高度，size 即子行数
 */
@Composable
private fun <T> MergedRowContent(
    frozen: List<IndexedValue<DTableColumn<T>>>,
    scrollable: List<IndexedValue<DTableColumn<T>>>,
    hScroll: ScrollState,
    subHeights: List<Dp>,
    background: Color,
    gridLineColor: Color,
    scrollShadow: Boolean,
    mergeColumns: Set<Int>,
    mergedCell: @Composable (columnIndex: Int) -> Unit,
    subCell: @Composable (columnIndex: Int, subIndex: Int) -> Unit,
) {
    // 整组高度 = 各子行高度之和，合并格取这个高
    val totalHeight = subHeights.fold(0.dp) { acc, h -> acc + h }
    val cellHeightModifier = Modifier.height(totalHeight)

    /** 按列渲染：合并列一格到底，其余列竖排子格 */
    @Composable
    fun cellOf(iv: IndexedValue<DTableColumn<T>>) {
        if (iv.index in mergeColumns) {
            TableCellBox(iv.value, cellHeightModifier, background, gridLineColor) { mergedCell(iv.index) }
        } else {
            TableSubCellColumn(iv.value, subHeights, background, gridLineColor) { subIndex ->
                subCell(iv.index, subIndex)
            }
        }
    }

    Row(Modifier.height(totalHeight)) {
        // 固定列区：绘制在滚动区之上，并在右缘绘制横滑阴影（与普通行一致）
        Row(
            modifier = Modifier
                .zIndex(1f)
                .drawWithContent {
                    drawContent()
                    if (scrollShadow && hScroll.value > 0) {
                        val shadowWidth = 6.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0x1A000000), Color.Transparent),
                                startX = size.width,
                                endX = size.width + shadowWidth,
                            ),
                            topLeft = Offset(size.width, 0f),
                            size = Size(shadowWidth, size.height),
                        )
                    }
                },
        ) {
            frozen.forEach { iv -> cellOf(iv) }
        }
        // 横向滚动列区：与表头共享 hScroll
        Row(modifier = Modifier.horizontalScroll(hScroll)) {
            scrollable.forEach { iv -> cellOf(iv) }
        }
    }
}

/**
 * 组件描述：整行贯通行（横跨可视宽度、不横向滚动）；用于分组头、说明横幅等
 * @param content 整行内容
 */
@Composable
private fun FullSpanRowLayout(
    background: Color,
    gridLineColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .border(0.5.dp, gridLineColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        content()
    }
}

/**
 * 组件描述：单个单元格容器（统一宽高、背景、网格线、内边距与对齐）
 * @param content 单元格内容
 */
@Composable
private fun <T> TableCellBox(
    column: DTableColumn<T>,
    heightModifier: Modifier,
    background: Color,
    gridLineColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(column.width)
            .then(heightModifier)
            .background(background)
            .border(0.5.dp, gridLineColor)
            .padding(horizontal = TableCellPadding),
        contentAlignment = column.alignment,
    ) {
        content()
    }
}

/** 单元格左右内边距。合并行预测量时也要用它，否则测出的可用宽度与实际渲染不一致 */
private val TableCellPadding = 8.dp

/**
 * 组件描述：非合并列在合并行里的形态——竖排 subRowCount 个等高子格
 * @param subCell 子格内容，参数为子行下标
 */
@Composable
private fun <T> TableSubCellColumn(
    column: DTableColumn<T>,
    subHeights: List<Dp>,
    background: Color,
    gridLineColor: Color,
    subCell: @Composable (subIndex: Int) -> Unit,
) {
    Column(modifier = Modifier.width(column.width)) {
        subHeights.forEachIndexed { subIndex, height ->
            Box(
                modifier = Modifier
                    .width(column.width)
                    .height(height)
                    .background(background)
                    .border(0.5.dp, gridLineColor)
                    .padding(horizontal = TableCellPadding),
                contentAlignment = column.alignment,
            ) {
                subCell(subIndex)
            }
        }
    }
}

/**
 * 组件描述：表格空态——表头下方居中展示提示文字（默认「暂无数据」）
 * @param text 空态文字
 */
@Composable
private fun TableEmpty(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.BasicThree, fontSize = 14.sp)
    }
}

/**
 * 列总宽撑不满可用宽度时，把富余宽度按各列原宽度的比例分摊，避免表格右侧留白（列被隐藏后常见）。
 * 总宽已超出可用宽度时原样返回，由横向滚动处理。
 * @param availableWidth 可用宽度，一般取表格容器的宽度（可用 BoxWithConstraints 的 maxWidth）
 */
fun <T> List<DTableColumn<T>>.stretchToFill(availableWidth: Dp): List<DTableColumn<T>> {
    if (isEmpty() || availableWidth <= 0.dp) return this
    val total = fold(0.dp) { acc, column -> acc + column.width }
    if (total <= 0.dp || total >= availableWidth) return this
    val scale = availableWidth / total
    // 逐列累加已分配宽度，最后一列吃掉误差，保证总和正好等于可用宽度（不留 1dp 缝）
    var allocated = 0.dp
    return mapIndexed { index, column ->
        val width = if (index == lastIndex) availableWidth - allocated else column.width * scale
        allocated += width
        column.copy(width = width)
    }
}

/**
 * 便捷构造：纯文字表头 + 纯文字单元格的列（只读表格最常用）
 * @param title 表头文字
 * @param width 列宽
 * @param frozen 是否固定列
 * @param maxLines 表头与单元格共用的最大行数；放开换行时表头行高、行最小高度要一并加大，否则文字会被裁
 * @param text 从行数据取单元格文字
 */
fun <T> textColumn(
    title: String,
    width: Dp,
    frozen: Boolean = false,
    maxLines: Int = 1,
    text: (T) -> String,
): DTableColumn<T> = DTableColumn(
    width = width,
    frozen = frozen,
    header = { TableHeaderText(title, maxLines = maxLines) },
    cell = { TableCellText(text(it), maxLines = maxLines) },
)

/**
 * 组件描述：表头默认文字样式（居中、13sp、字体色 Primary）
 * @param text 表头文字
 * @param maxLines 最大行数（默认 1，超出省略）
 */
@Composable
fun TableHeaderText(text: String, maxLines: Int = 1) {
    Text(
        text = text,
        color = DSyncTableDefaults.HeaderTextColor,
        fontSize = DSyncTableDefaults.TextSize,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 组件描述：单元格默认文字样式（默认居中、13sp）
 * @param text 单元格文字
 * @param color 文字色（默认 Basic/1）
 * @param textAlign 对齐（默认居中）
 * @param fontWeight 字重（默认常规）
 * @param maxLines 最大行数（默认 1，超出省略）
 */
@Composable
fun TableCellText(
    text: String,
    color: Color = DSyncTableDefaults.CellTextColor,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        color = color,
        fontSize = DSyncTableDefaults.TextSize,
        textAlign = textAlign,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 组件描述：带问号提示的表头单元格（可选标题 + 问号图标，点图标弹窗展示提示）
 * @param tip 提示文案（弹窗内容）
 * @param title 表头文字（可空；为空时只显示问号图标）
 */
@Composable
fun HintHeaderCell(
    tip: String,
    title: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 有标题才显示文字与间距；无标题时只留问号图标
        if (!title.isNullOrEmpty()) {
            TableHeaderText(title)
            Spacer(Modifier.width(2.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = "提示",
            tint = AppColors.BasicThree,
            modifier = Modifier
                .size(16.dp)
                .clickable { showDialog = true },
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = { Text(text = tip, color = AppColors.BasicOne, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("我知道了") }
            }
        )
    }
}

// ==================== 以下为预览 ====================

/** 预览数据：生产单预算总量行 */
private data class BudgetRowPreview(
    val index: Int,
    val orderNo: String,
    val type: String,
    val warehouse: String,
    val picker: String,
    val date: String,
)

@Preview(name = "默认样式-生产单预算总量", widthDp = 375, heightDp = 280, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableDefaultPreview() {
    val rows = List(6) { i ->
        BudgetRowPreview(i + 1, "36623", "领用出仓", "鼎盛", "万里", "2025-10-22 08:57:42")
    }
    val columns = listOf(
        textColumn<BudgetRowPreview>("序", 40.dp, frozen = true) { it.index.toString() },
        textColumn<BudgetRowPreview>("出入仓单号", 80.dp) { it.orderNo },
        textColumn<BudgetRowPreview>("类型", 80.dp) { it.type },
        textColumn<BudgetRowPreview>("仓库", 60.dp) { it.warehouse },
        textColumn<BudgetRowPreview>("领料人", 60.dp) { it.picker },
        textColumn<BudgetRowPreview>("创建日期", 140.dp) { it.date },
    )
    DSyncTable(columns = columns, modifier = Modifier.fillMaxWidth(), rowHeight = 36.dp) {
        items(rows, key = { it.index })
    }
}

/** 预览数据：商品用量配置行 */
private data class UsageRowPreview(
    val spec: String,
    val part: String,
    val usage: String,
    val lossRate: String,
    val lossQty: String,
    val unit: String,
)

@Preview(name = "自定义样式-商品用量配置", widthDp = 375, heightDp = 280, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableCustomPreview() {
    val rows = listOf(
        UsageRowPreview("白色;S", "前片", "40.0000", "2", "12", "米"),
        UsageRowPreview("白色;M", "后片", "40", "0", "7", "米"),
        UsageRowPreview("白色;L", "领口", "20", "3", "5", "米"),
        UsageRowPreview("", "请选择", "请输入", "请输入", "-", "请选择"),
    )
    val columns = listOf(
        DTableColumn<UsageRowPreview>(
            width = 140.dp, frozen = true,
            header = { TableHeaderText("规格") },
            cell = { PreviewSpecCell(it.spec) },
        ),
        DTableColumn<UsageRowPreview>(
            width = 100.dp,
            header = { TableHeaderText("部位") },
            cell = { PreviewDropdownCell(it.part) },
        ),
        DTableColumn<UsageRowPreview>(
            width = 90.dp,
            header = { TableHeaderText("用量") },
            cell = { PreviewInputCell(it.usage) },
        ),
        DTableColumn<UsageRowPreview>(
            width = 90.dp,
            header = { HintHeaderCell(title = "损耗率", tip = "单位 %，损耗率 = 损耗量 / 用量 × 100%") },
            cell = { PreviewInputCell(it.lossRate) },
        ),
        textColumn<UsageRowPreview>("损耗量", 90.dp) { it.lossQty },
        DTableColumn<UsageRowPreview>(
            width = 100.dp,
            header = { TableHeaderText("用量单位") },
            cell = { PreviewDropdownCell(it.unit) },
        ),
        DTableColumn<UsageRowPreview>(
            width = 70.dp,
            header = { TableHeaderText("操作") },
            cell = { PreviewActionCell() },
        ),
    )
    DSyncTable(columns = columns, modifier = Modifier.fillMaxWidth()) {
        items(rows)
    }
}

/** 预览数据：生产单商品明细行（体现「一个单元格多字段」+ 分组 + 小计） */
private data class SkuLinePreview(
    val name: String,
    val spec: String,
    val code: String,
    val qty: String,
    val price: String,
    val amount: String,
)

@Preview(name = "分组+小计+多字段", widthDp = 375, heightDp = 360, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableGroupSubtotalPreview() {
    val group1 = listOf(
        SkuLinePreview("勿动材料", "红色;m", "下单交叉-02", "10", "5", "50"),
        SkuLinePreview("勿动材222222222料", "红色;s", "下单交叉-03", "8", "5", "40"),
    )
    val columns = listOf(
        // 商品列（固定）：一个单元格组合 图片 + 名称 + 规格 + 编码，左对齐
        DTableColumn<SkuLinePreview>(
            width = 150.dp, frozen = true, alignment = Alignment.CenterStart,
            header = { TableHeaderText("图片/规格") },
            cell = { PreviewProductCell(name = it.name, spec = it.spec, code = it.code) },
        ),
        textColumn<SkuLinePreview>("量", 50.dp) { it.qty },
        textColumn<SkuLinePreview>("单价", 70.dp) { it.price },
        textColumn<SkuLinePreview>("小计金额", 80.dp) { it.amount },
        textColumn<SkuLinePreview>("商品编码", 110.dp) { it.code },
    )
    DSyncTable(columns = columns, modifier = Modifier.fillMaxWidth(), minRowHeight = 56.dp) {
        // 分组头：整行贯通、不横向滚动
        fullSpanRow(key = "g1-head") {
            PreviewGroupHeader(orderNo = "生产单：17076", deliveryDate = "交期：2026-08-07")
        }
        // 该组数据行
        items(group1, key = { it.spec })
        // 该组小计行：按列联动，第 0 列显示「小计」，小计金额列显示合计
        columnRow(key = "g1-sub", height = 20.dp) { columnIndex ->
            when (columnIndex) {
                0 -> TableCellText("小计")
                3 -> TableCellText("90")
                else -> {}
            }
        }
    }
}

@Preview(name = "空数据-暂无数据", widthDp = 375, heightDp = 200, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableEmptyPreview() {
    val columns = listOf(
        textColumn<BudgetRowPreview>("序", 40.dp, frozen = true) { it.index.toString() },
        textColumn<BudgetRowPreview>("出入仓单号", 80.dp) { it.orderNo },
        textColumn<BudgetRowPreview>("类型", 80.dp) { it.type },
        textColumn<BudgetRowPreview>("仓库", 60.dp) { it.warehouse },
    )
    DSyncTable(columns = columns, modifier = Modifier.fillMaxWidth()) {
        // 不声明任何行 → 展示「暂无数据」
    }
}

@Preview(name = "无表头行", widthDp = 375, heightDp = 180, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableNoHeaderPreview() {
    val rows = List(4) { i ->
        BudgetRowPreview(i + 1, "36623", "领用出仓", "鼎盛", "万里", "2025-10-22 08:57:42")
    }
    val columns = listOf(
        textColumn<BudgetRowPreview>("序", 40.dp, frozen = true) { it.index.toString() },
        textColumn<BudgetRowPreview>("出入仓单号", 80.dp) { it.orderNo },
        textColumn<BudgetRowPreview>("类型", 80.dp) { it.type },
        textColumn<BudgetRowPreview>("仓库", 60.dp) { it.warehouse },
    )
    // showHeader = false：不渲染表头行，直接展示数据
    DSyncTable(columns = columns, showHeader = false, modifier = Modifier.fillMaxWidth(), rowHeight = 36.dp) {
        items(rows, key = { it.index })
    }
}

/** 粘性底部行预览用列 */
private fun footerPreviewColumns() = listOf(
    textColumn<BudgetRowPreview>("序", 40.dp, frozen = true) { it.index.toString() },
    textColumn<BudgetRowPreview>("出入仓单号", 80.dp) { it.orderNo },
    textColumn<BudgetRowPreview>("类型", 80.dp) { it.type },
    textColumn<BudgetRowPreview>("仓库", 60.dp) { it.warehouse },
    textColumn<BudgetRowPreview>("领料人", 60.dp) { it.picker },
    textColumn<BudgetRowPreview>("创建日期", 140.dp) { it.date },
)

@Preview(name = "粘性底部行-超一屏固定底部", widthDp = 375, heightDp = 300, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableStickyFooterPinnedPreview() {
    val rows = List(12) { i ->
        BudgetRowPreview(i + 1, "36623", "领用出仓", "鼎盛", "万里", "2025-10-22 08:57:42")
    }
    // 数据超过一屏：数据区内部滚动，合计行被顶到底部固定（需给表格确定高度，这里用 fillMaxHeight）
    DSyncTable(columns = footerPreviewColumns(), modifier = Modifier.fillMaxHeight(), rowHeight = 36.dp) {
        items(rows, key = { it.index })
        stickyFooterRow { columnIndex ->
            when (columnIndex) {
                0 -> TableCellText("合计", fontWeight = FontWeight.Medium)
                1 -> TableCellText("12 单", fontWeight = FontWeight.Medium)
                else -> {}
            }
        }
    }
}

@Preview(name = "粘性底部行-未满一屏跟随数据", widthDp = 375, heightDp = 300, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DSyncTableStickyFooterFollowPreview() {
    val rows = List(3) { i ->
        BudgetRowPreview(i + 1, "36623", "领用出仓", "鼎盛", "万里", "2025-10-22 08:57:42")
    }
    // 数据没占满一屏：合计行紧跟在数据下方，下方留白
    DSyncTable(columns = footerPreviewColumns(), modifier = Modifier.fillMaxHeight(), rowHeight = 36.dp) {
        items(rows, key = { it.index })
        stickyFooterRow { columnIndex ->
            when (columnIndex) {
                0 -> TableCellText("合计", fontWeight = FontWeight.Medium)
                1 -> TableCellText("3 单", fontWeight = FontWeight.Medium)
                else -> {}
            }
        }
    }
}

/** 预览用：商品多字段单元格（图片占位 + 名称 + 规格 + 编码） */
@Composable
private fun PreviewProductCell(name: String, spec: String, code: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 图片占位
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.BgPageTwo, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("图", color = AppColors.BasicThree, fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            TableCellText(name, textAlign = TextAlign.Start, fontWeight = FontWeight.Medium)
            TableCellText(spec, color = AppColors.BasicThree, textAlign = TextAlign.Start)
            TableCellText(code, color = AppColors.BasicThree, textAlign = TextAlign.Start)
        }
    }
}

/** 预览用：分组头（两行：生产单 + 交期） */
@Composable
private fun PreviewGroupHeader(orderNo: String, deliveryDate: String) {
    Column {
        Text(orderNo, color = AppColors.AccentBlue, fontSize = 13.sp)
        Spacer(Modifier.height(2.dp))
        Text(deliveryDate, color = AppColors.BasicThree, fontSize = 12.sp)
    }
}

/** 预览用：规格文本 + 编辑图标 */
@Composable
private fun PreviewSpecCell(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text.isNotEmpty()) {
            TableCellText(text)
            Spacer(Modifier.width(4.dp))
        }
        PreviewEditIcon()
    }
}

/** 预览用：输入框样式单元格 */
@Composable
private fun PreviewInputCell(text: String) {
    val placeholder = text == "请输入"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .border(0.5.dp, AppColors.LineThree, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        TableCellText(text, color = if (placeholder) DSyncTableDefaults.PlaceholderColor else DSyncTableDefaults.CellTextColor)
    }
}

/** 预览用：下拉框样式单元格（文字 + 右侧下拉箭头） */
@Composable
private fun PreviewDropdownCell(text: String) {
    val placeholder = text == "请选择"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .border(0.5.dp, AppColors.LineThree, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TableCellText(text, color = if (placeholder) DSyncTableDefaults.PlaceholderColor else DSyncTableDefaults.CellTextColor)
        PreviewArrowDown()
    }
}

/** 预览用：操作列（增/删圆形图标按钮） */
@Composable
private fun PreviewActionCell() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewCircleIcon(plus = true, color = AppColors.AccentPrimary)
        PreviewCircleIcon(plus = false, color = AppColors.BasicThree)
    }
}

/** 预览用：向下箭头（自绘） */
@Composable
private fun PreviewArrowDown() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.42f)
            lineTo(w * 0.5f, h * 0.6f)
            lineTo(w * 0.7f, h * 0.42f)
        }
        drawPath(path, color = AppColors.BasicThree, style = Stroke(width = 1.5.dp.toPx()))
    }
}

/** 预览用：编辑图标（自绘一支斜笔） */
@Composable
private fun PreviewEditIcon() {
    Canvas(modifier = Modifier.size(14.dp)) {
        val s = size.minDimension
        drawLine(
            color = AppColors.BasicThree,
            start = Offset(s * 0.28f, s * 0.72f),
            end = Offset(s * 0.72f, s * 0.28f),
            strokeWidth = 1.5.dp.toPx(),
        )
        drawLine(
            color = AppColors.BasicThree,
            start = Offset(s * 0.22f, s * 0.8f),
            end = Offset(s * 0.34f, s * 0.76f),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

/** 预览用：圆形增/减图标（自绘）；plus=true 画加号，false 画减号 */
@Composable
private fun PreviewCircleIcon(plus: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = color, radius = r)
        val inset = r * 0.5f
        // 横线
        drawLine(
            color = Color.White,
            start = Offset(center.x - inset, center.y),
            end = Offset(center.x + inset, center.y),
            strokeWidth = 1.5.dp.toPx(),
        )
        // 加号补一条竖线
        if (plus) {
            drawLine(
                color = Color.White,
                start = Offset(center.x, center.y - inset),
                end = Offset(center.x, center.y + inset),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}
