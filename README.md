# DComposeUI

一个专注于实用场景的 Jetpack Compose UI 组件与交互示例项目。

DComposeUI 将常见但实现细节较多的 UI 能力整理为可阅读、可运行的 Compose 源码，并通过独立 Demo 展示组件状态管理和交互方式。项目当前是单 `app` 模块示例工程，尚未发布 Maven 依赖。

## 组件

| 组件 | 能力 |
| --- | --- |
| `DRefreshList` | 下拉刷新、上拉加载、空态和无更多数据状态 |
| `DSyncTable` | 冻结列、表头与内容横向同步、分组/小计行、纵向合并 |
| `DBubbleMenu` | 锚点菜单、文本或自定义菜单项、自动贴屏 |
| `DBubbleTip` | 多方向锚点气泡、箭头定位、点击外部关闭 |
| `DWheelPicker` | 基于 LazyColumn 的吸附滚轮及中心项视觉效果 |
| `DDateTimePicker` | 年月日时分秒联动选择与日期校正 |
| `DDateRangePicker` | 开始/结束日期选择和区间校验 |
| `DDragSortItem` | LazyColumn 拖拽排序、拖动手柄、边缘自动滚动 |
| `DClearableField` | 可清空输入框、自动聚焦和列表焦点隔离 |
| `DNumberField` | 整数/小数限制、负数、最大值和失焦格式化 |

Demo 首页按以下六组集中展示组件：刷新列表、同步表格、气泡浮层、滚轮与日期选择、拖拽排序、输入框。

## Demo 效果

点击 Demo 名称可查看对应源码。

<table>
  <tr>
    <td align="center">
      <a href="app/src/main/java/com/ddw/dcomposeui/demo/screen/DRefreshListDemo.kt"><strong>刷新列表</strong></a><br>
      <img src="images/gifs/refresh-list.gif" width="240" alt="DRefreshList 下拉刷新与上拉加载 Demo">
    </td>
    <td align="center">
      <a href="app/src/main/java/com/ddw/dcomposeui/demo/screen/DSyncTableDemo.kt"><strong>同步表格</strong></a><br>
      <img src="images/gifs/sync-table.gif" width="240" alt="DSyncTable 冻结列与同步滚动 Demo">
    </td>
    <td align="center">
      <a href="app/src/main/java/com/ddw/dcomposeui/demo/screen/DBubbleDemo.kt"><strong>气泡浮层</strong></a><br>
      <img src="images/gifs/bubble.gif" width="240" alt="DBubbleMenu 与 DBubbleTip Demo">
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="app/src/main/java/com/ddw/dcomposeui/demo/screen/DWheelPickerDemo.kt"><strong>滚轮与日期选择</strong></a><br>
      <img src="images/gifs/wheel-date.gif" width="240" alt="DWheelPicker 与日期选择器 Demo">
    </td>
    <td align="center">
      <a href="app/src/main/java/com/ddw/dcomposeui/demo/screen/DDragSortDemo.kt"><strong>拖拽排序</strong></a><br>
      <img src="images/gifs/drag-sort.gif" width="240" alt="DDragSortItem 拖拽排序 Demo">
    </td>
    <td align="center">
      <a href="app/src/main/java/com/ddw/dcomposeui/demo/screen/DInputFieldDemo.kt"><strong>输入框</strong></a><br>
      <img src="images/gifs/input-fields.gif" width="240" alt="DClearableField 与 DNumberField Demo">
    </td>
  </tr>
</table>

## 公众号文章

DComposeUI 的 Compose 布局原理和组件源码实战会在微信公众号发布，README 与公众号文章互相引流。

- [微信公众号文章入口（暂时未发布）](https://mp.weixin.qq.com/)

> 当前链接仅作占位；文章发布后，请手动替换为正确的公众号文章地址。

## 运行项目

### 环境要求

- Android Studio（支持 AGP 9.3.1）
- Android SDK Platform 37
- Android 7.0 / API 24 或更高版本的设备或模拟器
- 首次构建需要联网下载 Gradle 和依赖

使用 Android Studio 打开仓库根目录，等待 Gradle Sync 完成后，选择 `app` 配置并运行即可。

也可以在 Windows PowerShell 中执行：

```powershell
.\gradlew.bat assembleDebug
```

连接设备或启动模拟器后安装：

```powershell
.\gradlew.bat installDebug
```

调试 APK 输出目录：`app/build/outputs/apk/debug/`。

## 如何复用组件

组件源码位于：

```text
app/src/main/java/com/ddw/dcomposeui/widget/
```

对应使用示例位于：

```text
app/src/main/java/com/ddw/dcomposeui/demo/screen/
```

当前仓库未配置 Android Library 模块和 Maven 发布，因此请勿添加不存在的 `implementation(...)` 坐标。你可以直接参考 Demo 使用组件，或将需要的组件源码及相关主题/扩展复制、抽取到自己的 Library 模块中。

拖拽排序采用组合式 API：通过 `rememberDDragSortState()` 创建状态，在调用方的 `LazyColumn` 中使用 `DDragSortItem` 包裹列表项，并在 `onMove` 中同步更新业务数据顺序。

## 项目结构

```text
app/src/main/java/com/ddw/dcomposeui/
├─ demo/          # Demo 入口、首页和示例页面
├─ ext/           # Compose 扩展
├─ theme/         # 应用颜色与主题
└─ widget/        # UI 组件实现
```

## 技术栈

- Kotlin 2.4.0
- Jetpack Compose / Material 3
- Compose BOM 2026.06.01
- Android Gradle Plugin 9.3.1
- Coil 3.5.0
- minSdk 24 / targetSdk 37 / compileSdk 37

## License

本项目基于 [MIT License](LICENSE) 开源。
