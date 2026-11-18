# Waveform Plotter 本地 CLion 263 安装包

安装和操作步骤见 [使用说明.md](使用说明.md)。

当前安装包：clion-waveform-plotter-1.3.2-local263.2.zip
目标 IDE：CL-263.7307.257（本机 /snap/clion/current）
上游：https://github.com/Szturin/Clion-Waveform-Plotter
源码：/home/zxk/Downloads/Clion-Waveform-Plotter-master.zip
源码 SHA256：d547a89f8972a400de7bce5eb5919451da1dae658f1c8c90212839fcdb402f0d

## 构建方式

使用本地源码 1.3.2 和缓存的 Kotlin 2.0.21 编译器，将全部 Kotlin 源码针对本机 CLion 263 的平台、CLion 和 nativeDebug API 重新编译。
编译参数包含 -no-stdlib -no-reflect -Xskip-metadata-version-check -jvm-target 17；运行环境为本机 IDE 的 JBR 25。
初版 1.3.2-local263 未修改上游 Kotlin 源码。后续版本兼容范围仍为 since-build=263.7307.257、until-build=263.*。

1.3.2-local263.1 修改三个文件：

- `DataBuffer.kt`：后加入的通道用 NaN 补齐已有时间帧；每个采样帧都推进所有通道，缺失读数留空。
- `DebugSessionListener.kt`：新增在新会话附加时触发的回调，先清除旧会话的采样和地址状态。
- `WaveformPanel.kt`：新 Debug 会话清空旧曲线并失效地址缓存；普通 Clear 只清曲线和显示值，保留已解析地址。

1.3.2-local263.2 在时间对齐修复上增加：

- `StreamingTimeWindow.kt` 和 `PlotCanvas.kt`：默认最近 10 秒的固定长度流式视图；支持改变时长、缩放、拖动查看历史和右键恢复跟踪。
- `WaveformPanel.kt`：点击名称自动尝试绘图；取消勾选隐藏曲线；增加持久化的 Window(s) 控件；防止异步地址解析过期回调影响新会话。
- `LiveWatchService.kt` 和 `DataBuffer.kt`：只采样选中的通道；隐藏曲线不影响 Y 轴范围；删除曲线后不再被采样逻辑重新创建。
- `PlotDebuggerVariableAction.kt`、`AddToPlotterAction.kt`、`WaveformToolWindowFactory.kt` 和插件 XML：增加调试变量右键绘图入口，修复首次打开工具窗口时添加变量可能失效的问题。
- `WaveformConfigService.kt`：保存窗口时长；修复恢复配置过程中被默认设置覆盖的问题。

完整源码：[clion-waveform-plotter-1.3.2-local263.2-source.zip](clion-waveform-plotter-1.3.2-local263.2-source.zip)。源码 ZIP 不能作为插件安装。

当前源码变更见 [local263.2.patch](local263.2.patch)；前一版变更见 [local263.1.patch](local263.1.patch)。

本机复现构建可在项目根目录运行：

```bash
python3 artifacts/waveform-plotter-263/build_local.py \
  --source artifacts/waveform-plotter-263/clion-waveform-plotter-1.3.2-local263.2-source.zip
```

构建脚本使用当前已安装 IDE 和本机已有 Gradle 编译器缓存，不访问网络。上游 Gradle 文件保留在源码备份中供参考；当前本地包通过此脚本构建。
使用 IDE 自带 Kotlin 运行库，不捆绑旧版 kotlin-stdlib。
安装包保留上游 README，含作者的使用许可说明。本包是本地构建，并非作者发布的正式兼容版本。

## 验证

- 所有源码针对已安装 IDE 的 542 个相关 JAR 编译通过。
- 使用已安装 CLion 的 PluginDescriptorLoader 和 PluginManagerCore 执行实际的插件兼容性检查。
- 旧包 1.2.0 被判为不兼容；本地包被判为兼容。
- 插件类可针对本机 IDE 库加载并解析其构造器、方法和字段，具体数量见各版本检查日志。
- ZIP/JAR 完整性和 plugin.xml 解析通过。
- 1.3.2-local263 已由用户安装并提供实际采样曲线截图。
- 新版复现并修复后加通道错位；检查覆盖后加通道、部分读取缺失、10000 点环形缓冲回绕，以及清空后重新采样。
- 当前版本检查了实际 PlotCanvas 的可见时间范围：即使已有 100 秒历史也只显示最近 10 秒，时长控件可改变实际窗口，隐藏曲线不影响 Y 轴范围。
- 使用内存输入输出流模拟 OpenOCD 响应，检查实际 Telnet 采样函数的变量选择、浮点解析和删除曲线行为。当前执行环境不允许创建本地监听 socket，因此该检查没有使用真实 TCP 连接。
- 使用实际 WaveformPanel 配合模拟的 Project/Debug 会话检查：打开面板即可识别已有会话，点击变量名称请求绘图，取消勾选隐藏曲线，窗口时长控件同步画布并保存配置。
- 新版的实际 Debug 菜单、会话切换和开发板采样仍需安装后验证。

初版兼容检查输出：

```
PASS: actual CLion 263 compatibility checker: incompatible=true

PASS: actual CLion 263 compatibility checker: incompatible=false
PASS: linked 36 plugin classes against installed IDE libraries
```

当前版本检查输出见 [validation-local263.2.txt](validation-local263.2.txt)，检查源码保存在 `checks-local263.2/`。前一版输出见 [validation-local263.1.txt](validation-local263.1.txt)。

当前安装包 SHA256：ab0fdbbb8c60157f2a3b774cca26af1f1a2dd8d220dba3ad0dec9f01a73c622f

1.3.2-local263.1 安装包 SHA256：a97566c22f50befaf7ab7d5e839d05b3c35a17e1dd49e0fa334ac27e8e3d57e5

初版安装包 SHA256：9d5e7ee19b42d7e2b56b2041ae41620d71e833a7b618b5da0542d0fff32aade6

## 安装和首次使用

CLion → 设置 → 插件 → 齿轮 → 从磁盘安装插件，选择本目录中的安装包并按提示重启。
启动 OpenOCD 调试，保持 Telnet 端口开启（默认 4444）。
打开 Waveform Plotter，添加 INS_angle[0] 等变量。
点击名称或勾选变量会自动尝试绘图，默认只显示最近 10 秒。数组元素和结构体成员首次需要通过 GDB 解析地址；若提示暂停，手动暂停一次，插件会继续解析并启动采样，然后按 F9 继续运行。
实时采样是读取内存；此构建没有增加运行中修改变量的功能。
