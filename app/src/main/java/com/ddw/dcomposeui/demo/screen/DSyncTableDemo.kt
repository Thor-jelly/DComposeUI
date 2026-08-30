package com.ddw.dcomposeui.demo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.demo.DemoChip
import com.ddw.dcomposeui.demo.DemoScaffold
import com.ddw.dcomposeui.theme.AppColors
import com.ddw.dcomposeui.theme.AppComposeTheme
import com.ddw.dcomposeui.widget.DSyncTable
import com.ddw.dcomposeui.widget.DTableColumn
import com.ddw.dcomposeui.widget.HintHeaderCell
import com.ddw.dcomposeui.widget.TableCellText
import com.ddw.dcomposeui.widget.TableHeaderText
import com.ddw.dcomposeui.widget.textColumn

/**
 * 类描述：Demo 用的订单明细行
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 11:30
 */
private data class OrderLine(
    val index: Int,
    val name: String,
    val spec: String,
    val qty: String,
    val price: String,
    val amount: String,
    val date: String,
    val status: String
)

/** Demo 里展示的四种表格用法 */
private enum class TableMode(val label: String) {
    Basic("基础表格"),
    Group("分组 + 小计"),
    Merged("纵向合并"),
    Empty("空数据")
}

/**
 * 组件描述：DSyncTable 演示。左侧「商品」列冻结不动，右侧多列横向滚动，表头与所有行联动；
 * 分组模式演示 fullSpanRow 与 columnRow，合并模式演示 mergedItem 的不连续纵向合并。
 *
 * @param onBack 返回首页
 */
@Composable
fun DSyncTableDemo(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(TableMode.Basic) }
    val lines = remember { mockLines() }
    val columns = rememberOrderColumns()

    DemoScaffold(title = "DSyncTable", onBack = onBack, contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TableMode.entries.forEach { item ->
                DemoChip(text = item.label, selected = mode == item) { mode = item }
            }
        }
        Text(
            text = when (mode) {
                TableMode.Basic -> "左右滑动表格：商品列固定，其余列跟着表头一起横向滚动，固定列右侧有滚动阴影"
                TableMode.Group -> "fullSpanRow 铺满可视宽度不参与横滚，columnRow 按列渲染小计并参与横滚"
                TableMode.Merged -> "合并列与拆分列按「合 不合 合 不合 合 不合 合」交替，mergeColumns 传下标集合、不要求连续。传了 minSubRowHeight 就按内容自适应：第 1 行规格换行把整组撑高，第 2 行长姓名只把所在子行撑高，其余走 40dp 最小高度"
                TableMode.Empty -> "一行都没声明时展示 emptyText，表头仍然保留"
            },
            color = AppColors.BasicThree,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        )

        Box(modifier = Modifier.padding(12.dp)) {
            when (mode) {
                TableMode.Basic -> DSyncTable(
                    columns = columns,
                    modifier = Modifier.fillMaxWidth(),
                    minRowHeight = 52.dp
                ) {
                    items(lines, key = { it.index })
                }

                TableMode.Group -> DSyncTable(
                    columns = columns,
                    modifier = Modifier.fillMaxWidth(),
                    minRowHeight = 52.dp
                ) {
                    // 两个分组，各自带分组头与小计行
                    listOf("SC2026081701" to lines.take(3), "SC2026081702" to lines.drop(3)).forEach { (orderNo, group) ->
                        fullSpanRow(key = "header_$orderNo") {
                            GroupHeader(orderNo = orderNo, count = group.size)
                        }
                        items(group, key = { "${orderNo}_${it.index}" })
                        columnRow(key = "subtotal_$orderNo", height = 36.dp) { columnIndex ->
                            // 只在需要的列写内容，其余列留空
                            when (columnIndex) {
                                0 -> TableCellText(text = "小计", fontWeight = FontWeight.Medium)
                                1 -> TableCellText(
                                    text = group.sumOf { it.qty.toInt() }.toString(),
                                    fontWeight = FontWeight.Medium
                                )
                                3 -> TableCellText(
                                    text = group.sumOf { it.amount.toInt() }.toString(),
                                    color = AppColors.AccentRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                TableMode.Merged -> MergedTable()

                TableMode.Empty -> DSyncTable(
                    columns = columns,
                    modifier = Modifier.fillMaxWidth(),
                    emptyText = "还没有明细，去添加一条吧"
                ) {
                    // 一行都不声明
                }
            }
        }
    }
}

/**
 * 列定义：第一列冻结且一格放两个字段（名称 + 规格），其余列横向滚动
 */
@Composable
private fun rememberOrderColumns(): List<DTableColumn<OrderLine>> = remember {
    listOf(
        DTableColumn(
            width = 130.dp,
            frozen = true,
            alignment = Alignment.CenterStart,
            header = { TableHeaderText("商品") },
            cell = { line -> ProductCell(name = line.name, spec = line.spec) }
        ),
        textColumn<OrderLine>("数量", 70.dp) { it.qty },
        textColumn<OrderLine>("单价", 70.dp) { it.price },
        DTableColumn(
            width = 90.dp,
            header = { HintHeaderCell(title = "金额", tip = "金额 = 数量 × 单价，改数量后自动重算") },
            cell = { line -> TableCellText(text = line.amount, color = AppColors.AccentRed) }
        ),
        textColumn<OrderLine>("交期", 100.dp) { it.date },
        DTableColumn(
            width = 80.dp,
            header = { TableHeaderText("状态") },
            cell = { line -> StatusCell(line.status) }
        )
    )
}

/**
 * 组件描述：一个单元格里放两行字段
 *
 * @param name 商品名
 * @param spec 规格
 */
@Composable
private fun ProductCell(name: String, spec: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(text = name, color = AppColors.BasicOne, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = spec, color = AppColors.BasicThree, fontSize = 11.sp)
    }
}

/**
 * 组件描述：状态单元格，用颜色区分进行中与已完成
 *
 * @param status 状态文案
 */
@Composable
private fun StatusCell(status: String) {
    val color = if (status == "已完成") AppColors.AccentGreen else AppColors.AccentOrange
    Text(text = status, color = color, fontSize = 12.sp, textAlign = TextAlign.Center)
}

/**
 * 组件描述：分组头，整行贯通不参与横向滚动
 *
 * @param orderNo 订单号
 * @param count 该组明细条数
 */
@Composable
private fun GroupHeader(orderNo: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BgPageThree)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .background(AppColors.AccentPrimary)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = "生产单 $orderNo", color = AppColors.BasicOne, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text(text = "共 $count 条", color = AppColors.BasicThree, fontSize = 11.sp)
    }
}

/** 造几条订单明细当假数据 */
private fun mockLines(): List<OrderLine> = listOf(
    OrderLine(1, "纯棉圆领 T 恤", "白色 / S", "120", "38", "4560", "2026-09-01", "进行中"),
    OrderLine(2, "纯棉圆领 T 恤", "白色 / M", "200", "38", "7600", "2026-09-01", "进行中"),
    OrderLine(3, "宽松版牛仔裤", "深蓝 / 30", "80", "126", "10080", "2026-09-05", "已完成"),
    OrderLine(4, "针织开衫", "米白 / L", "60", "168", "10080", "2026-09-12", "进行中"),
    OrderLine(5, "工装夹克", "军绿 / XL", "45", "215", "9675", "2026-09-18", "已完成"),
    OrderLine(6, "亚麻直筒长裤", "浅咖 / 32", "95", "142", "13490", "2026-09-20", "进行中")
)

@Preview(showBackground = true, widthDp = 375, heightDp = 700)
@Composable
private fun DSyncTableDemoPreview() {
    AppComposeTheme {
        DSyncTableDemo {}
    }
}

// ==================== 纵向合并：合并列与拆分列交替分布 ====================

/**
 * 类描述：做工人子记录，一条工序单下可以挂多个
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 14:00
 */
private data class WorkerRecord(
    val name: String,
    val goodQty: String,
    val repairQty: String
)

/**
 * 类描述：交期状态标签，文案与配色一一对应
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 14:00
 */
private enum class DeadlineTag(val label: String, val color: Color) {
    Soon("即将超期", AppColors.AccentOrange),
    Overdue("已超期", AppColors.AccentRed),
    OnTime("按时交付", AppColors.BasicThree),
    Far("距交期>3天", AppColors.AccentGreen)
}

/**
 * 类描述：工序单明细行，一行带若干做工人子记录
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 14:00
 */
private data class ProcessLine(
    val index: Int,
    val orderNo: String,
    val spec: String,
    val deadline: String,
    val tag: DeadlineTag,
    val workers: List<WorkerRecord>
)

/**
 * 需要纵向合并的列下标。刻意做成「合 不合 合 不合 合 不合 合」交替：
 * 0 序号、2 工序单、4 规格、6 交期 合并成一格，1 做工人、3 良品数、5 返修登记数 按人拆成子行。
 * mergeColumns 是下标集合，不要求连续，合并列与拆分列可以任意穿插
 */
private val MergeColumns = setOf(0, 2, 4, 6)

/**
 * 组件描述：纵向合并表格，合并列与拆分列交替排布，用来验证不连续合并也能左右对齐
 */
@Composable
private fun MergedTable() {
    val lines = remember { mockProcessLines() }
    val columns = rememberProcessColumns()
    DSyncTable(
        columns = columns,
        modifier = Modifier.fillMaxWidth(),
        headerHeight = 40.dp
    ) {
        lines.forEach { line ->
            mergedItem(
                data = line,
                // 有几个做工人就拆几个子行，没人时组件按 1 行留空处理
                subRowCount = line.workers.size,
                mergeColumns = MergeColumns,
                // 传 minSubRowHeight 走自适应：子格内容能换行，行高按内容长的那一列算
                minSubRowHeight = 40.dp,
                key = line.index
            ) { columnIndex, subIndex ->
                val worker = line.workers.getOrNull(subIndex)
                // 只处理拆分列，合并列的内容由列定义里的 cell 负责
                when (columnIndex) {
                    // 姓名可能带备注，放开行数，该子行会跟着变高
                    1 -> TableCellText(text = worker?.name.orEmpty(), maxLines = 3)
                    3 -> TableCellText(text = worker?.goodQty.orEmpty())
                    5 -> TableCellText(text = worker?.repairQty.orEmpty())
                }
            }
        }
    }
}

/**
 * 工序单表的列定义，合并列与拆分列交替排布。序号列冻结；
 * 拆分列的 cell 给空即可，它们在合并行里由 subCell 渲染，cell 不会被调用
 */
@Composable
private fun rememberProcessColumns(): List<DTableColumn<ProcessLine>> = remember {
    listOf(
        // 0 合并
        textColumn<ProcessLine>("序号", 48.dp, frozen = true) { it.index.toString() },
        // 1 拆分
        textColumn<ProcessLine>("做工人", 72.dp) { "" },
        // 2 合并
        textColumn<ProcessLine>("工序单", 68.dp) { it.orderNo },
        // 3 拆分
        textColumn<ProcessLine>("良品数", 66.dp) { "" },
        // 4 合并，规格可能很长，放开行数让它自己换行
        textColumn<ProcessLine>("规格", 76.dp, maxLines = 4) { it.spec },
        // 5 拆分
        textColumn<ProcessLine>("返修登记数", 88.dp) { "" },
        // 6 合并，要容纳「日期 + 最长的状态标签」，宽度留够
        DTableColumn(
            width = 192.dp,
            header = { TableHeaderText("交期") },
            cell = { line -> DeadlineCell(deadline = line.deadline, tag = line.tag) }
        )
    )
}

/**
 * 组件描述：交期单元格，日期后面跟一个圆角描边的状态标签
 *
 * @param deadline 交期日期
 * @param tag 交期状态
 */
@Composable
private fun DeadlineCell(deadline: String, tag: DeadlineTag) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = deadline, color = AppColors.BasicOne, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .border(0.5.dp, tag.color, RoundedCornerShape(9.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            // 标签不允许换行，宽度不足时宁可省略号，也不能把行高顶开
            Text(
                text = tag.label,
                color = tag.color,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 造几条工序单明细。故意混入长内容来看自适应：
 * 第 1 条的规格很长（合并列换行）、第 2 条有个做工人名字很长（拆分列换行），
 * 第 3 条全是短内容走最小行高，第 4 条没有做工人
 */
private fun mockProcessLines(): List<ProcessLine> = listOf(
    ProcessLine(
        index = 1, orderNo = "10001", spec = "红色；M；加绒加厚款；带帽",
        deadline = "2026-03-31", tag = DeadlineTag.Soon,
        workers = listOf(
            WorkerRecord("张三", "8", "2"),
            WorkerRecord("李四", "40", "0")
        )
    ),
    ProcessLine(
        index = 2, orderNo = "10001", spec = "红色；L",
        deadline = "2026-03-31", tag = DeadlineTag.Overdue,
        workers = listOf(
            WorkerRecord("王五", "10", "0"),
            WorkerRecord("欧阳建国（临时工）", "16", "1"),
            WorkerRecord("孙七", "4", "0")
        )
    ),
    ProcessLine(
        index = 3, orderNo = "10002", spec = "黑色；S",
        deadline = "2026-03-31", tag = DeadlineTag.OnTime,
        workers = listOf(WorkerRecord("张三", "20", "0"))
    ),
    ProcessLine(
        index = 4, orderNo = "10002", spec = "黑色；M",
        deadline = "2026-03-31", tag = DeadlineTag.Far,
        workers = emptyList()
    )
)

@Preview(name = "纵向合并-交替列", showBackground = true, widthDp = 375, heightDp = 400)
@Composable
private fun MergedTablePreview() {
    AppComposeTheme {
        MergedTable()
    }
}
