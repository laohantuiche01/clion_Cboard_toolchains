# Waveform Plotter 本地 CLion 263 安装包

安装和操作步骤见 [使用说明.md](使用说明.md)。

当前安装包：clion-waveform-plotter-1.3.2-local263.1.zip
目标 IDE：CL-263.7307.257（本机 /snap/clion/current）
上游：https://github.com/Szturin/Clion-Waveform-Plotter
源码：/home/zxk/Downloads/Clion-Waveform-Plotter-master.zip
源码 SHA256：d547a89f8972a400de7bce5eb5919451da1dae658f1c8c90212839fcdb402f0d

## 构建方式

使用本地源码 1.3.2 和缓存的 Kotlin 2.0.21 编译器，将全部 Kotlin 源码针对本机 CLion 263 的平台、CLion 和 nativeDebug API 重新编译。
编译参数包含 -no-stdlib -no-reflect -Xskip-metadata-version-check -jvm-target 17；运行环境为本机 IDE 的 JBR 25。
初版 1.3.2-local263 未修改上游 Kotlin 源码。当前 1.3.2-local263.1 在此基础上修改三个文件，兼容范围仍为 since-build=263.7307.257、until-build=263.*：

- `DataBuffer.kt`：后加入的通道用 NaN 补齐已有时间帧；每个采样帧都推进所有通道，缺失读数留空。
- `DebugSessionListener.kt`：新增在新会话附加时触发的回调，先清除旧会话的采样和地址状态。
- `WaveformPanel.kt`：新 Debug 会话清空旧曲线并失效地址缓存；普通 Clear 只清曲线和显示值，保留已解析地址。

源码变更见 [local263.1.patch](local263.1.patch)，时间对齐检查见 [AlignmentCheck.java](AlignmentCheck.java)。
使用 IDE 自带 Kotlin 运行库，不捆绑旧版 kotlin-stdlib。
安装包保留上游 README，含作者的使用许可说明。本包是本地构建，并非作者发布的正式兼容版本。

## 验证

- 所有源码针对已安装 IDE 的 542 个相关 JAR 编译通过。
- 使用已安装 CLion 的 PluginDescriptorLoader 和 PluginManagerCore 执行实际的插件兼容性检查。
- 旧包 1.2.0 被判为不兼容；本地包被判为兼容。
- 36 个插件类可针对本机 IDE 库加载并解析其构造器、方法和字段。
- ZIP/JAR 完整性和 plugin.xml 解析通过。
- 1.3.2-local263 已由用户安装并提供实际采样曲线截图。
- 新版复现并修复后加通道错位；检查覆盖后加通道、部分读取缺失、10000 点环形缓冲回绕，以及清空后重新采样。
- 新版的实际面板交互、新 Debug 会话自动重置和开发板采样尚未验证。

初版兼容检查输出：

```
PASS: actual CLion 263 compatibility checker: incompatible=true

PASS: actual CLion 263 compatibility checker: incompatible=false
PASS: linked 36 plugin classes against installed IDE libraries
```

新版检查输出见 [validation-local263.1.txt](validation-local263.1.txt)。

新版安装包 SHA256：a97566c22f50befaf7ab7d5e839d05b3c35a17e1dd49e0fa334ac27e8e3d57e5

初版安装包 SHA256：9d5e7ee19b42d7e2b56b2041ae41620d71e833a7b618b5da0542d0fff32aade6

## 安装和首次使用

CLion → 设置 → 插件 → 齿轮 → 从磁盘安装插件，选择本目录中的安装包并按提示重启。
启动 OpenOCD 调试，保持 Telnet 端口开启（默认 4444）。
打开 Waveform Plotter，添加 INS_angle[0] 等变量。
数组元素和结构体成员需要通过 GDB 解析地址，因此首次启动时先暂停一次，添加并勾选变量、点击 Live，然后继续运行。
实时采样是读取内存；此构建没有增加运行中修改变量的功能。
