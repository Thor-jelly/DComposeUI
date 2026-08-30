package com.ddw.dcomposeui.theme

import androidx.compose.ui.graphics.Color

/**
 * 类描述：Compose UI 统一取色表，所有控件的颜色默认值都从这里取
 *
 * 创建人：吴冬冬
 *
 * 创建时间：2026/08/17 10:00
 */
object AppColors {

    //================ 主色及辅助色（用于需强调突出的图标或控件等） ================
    /** 主色 */
    val AccentPrimary = Color(0xFF15927E)

    /** 辅助蓝 */
    val AccentBlue = Color(0xFF507CF7)

    /** 辅助红 */
    val AccentRed = Color(0xFFF95757)

    /** 辅助绿 */
    val AccentGreen = Color(0xFF20CD69)

    /** 辅助橙 */
    val AccentOrange = Color(0xFFFF8F1F)

    /** 辅助紫 */
    val AccentPurple = Color(0xFF722ED1)

    //================ 基础文本色（主文本、次文本、辅助提示文本及部分 icon） ================
    /** 主文本 */
    val BasicOne = Color(0xFF262A2E)

    /** 次文本 */
    val BasicTwo = Color(0xFF4E5969)

    /** 辅助提示文本 */
    val BasicThree = Color(0xFF86909C)

    /** 更浅辅助文本，也用于控件关闭态 */
    val BasicFour = Color(0xFFC9CDD4)

    //================ 按钮色（主按钮与次按钮） ================
    /** 主按钮 */
    val ButtonOne = Color(0xFF15927E)

    /** 次按钮，同时作为水波纹颜色 */
    val ButtonTwo = Color(0xFFABD2CB)

    /** 浅按钮 */
    val ButtonThree = Color(0xFFEBFAF8)

    //================ 背景色（页面背景、局部辅助背景及白色） ================
    /** 页面背景一 */
    val BgPageOne = Color(0xFFE5E6EB)

    /** 页面背景二 */
    val BgPageTwo = Color(0xFFF2F3F5)

    /** 页面背景三 */
    val BgPageThree = Color(0xFFF7F8FA)

    /** 页面背景四（白） */
    val BgPageFour = Color(0xFFFFFFFF)

    //================ 线条及按钮描边色 ================
    /** 线条一 */
    val LineOne = Color(0xFFF2F3F5)

    /** 线条二 */
    val LineTwo = Color(0xFFE5E6EB)

    /** 线条三 */
    val LineThree = Color(0xFFC9CDD4)

    /** 分割线颜色 */
    val DividerColor = Color(0xFFEFF1F5)
}
