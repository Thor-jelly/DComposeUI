package com.ddw.dcomposeui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddw.dcomposeui.ext.noRippleClick
import com.ddw.dcomposeui.theme.AppColors

/**
 * 组件描述：Demo 页统一外壳，顶部返回标题栏 + 灰底内容区
 *
 * @param title 页面标题
 * @param onBack 返回首页
 * @param contentPadding 内容区四周留白，列表类演示传 0.dp 更合适
 * @param content 内容区。它所在的 Column 已占满剩余高度，内部可直接用 weight(1f) 撑开列表
 */
@Composable
fun DemoScaffold(
    title: String,
    onBack: () -> Unit,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BgPageThree)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .noRippleClick(onBack)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = AppColors.BasicOne,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                color = AppColors.BasicOne,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 14.dp)
            )
        }
        HorizontalDivider(color = AppColors.DividerColor)
        // 占满剩余高度，content 内部的 weight(1f) 才有可分配空间
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * 组件描述：演示小节，白色卡片 + 标题 + 可选说明
 *
 * @param title 小节标题
 * @param desc 说明文案，讲清这个用法的看点
 * @param content 小节内容
 */
@Composable
fun DemoSection(
    title: String,
    desc: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Text(text = title, color = AppColors.BasicOne, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (!desc.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(text = desc, color = AppColors.BasicThree, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/**
 * 组件描述：一行「标签 + 右侧内容」，Demo 里用于展示当前状态值
 *
 * @param label 左侧标签
 * @param value 右侧值
 */
@Composable
fun DemoValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = AppColors.BasicTwo, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(text = value, color = AppColors.AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 组件描述：Demo 里切换用法的小标签
 *
 * @param text 文案
 * @param selected 是否选中
 * @param onClick 点击回调
 */
@Composable
fun DemoChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AppColors.ButtonThree else AppColors.BgPageTwo)
            .noRippleClick(onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.AccentPrimary else AppColors.BasicTwo,
            fontSize = 13.sp
        )
    }
}
