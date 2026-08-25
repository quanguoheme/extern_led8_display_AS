# 外部数码管与硬件集成测试应用 (SC200 / GC111) - 项目文档与更新日志

## 1. 项目概述

本项目（`extern_led8_display_AS` / `com.example.extern_led8_display_for_sc200_gc111_c`）是专为基于 **移远 (Quectel) SC200 / GC111** 硬件方案的 Android 嵌入式工控/车载设备开发的硬件集成测试与控制应用。

应用集成了外部 3 位 8 段数码管驱动控制、多 SIM 卡槽动态切换与供电复位容错、车载 ACC 状态检测、AC 电源与充电状态监听、设备 SN/IMEI 二维码生成、继电器脉冲触发、全功能物理键盘按键映射以及底层硬件 JNI 驱动接口。

---

## 2. 硬件与系统规格

### 2.1 硬件与系统要求
- **目标芯片/主板**: Quectel SC200 / GC111 系列
- **操作系统**: Android 10 (API Level 29) 至 Android 14
- **屏幕分辨率**: 800 × 480（横屏 Landscape 优先适配）
- **显示密度**: 160 dpi (1dp = 1px)
- **内存/存储要求**: 内存 ≥ 3GB，存储 ≥ 16GB
- **必备权限与环境**:
  - `android.permission.READ_PHONE_STATE`（获取网络与 SIM 卡电话栈状态）
  - **Root 权限**（执行 `cmd phone get-imei 0` 获取设备 IMEI，以及系统级进程调试）
  - 本地 Native 动态库：`libhdxutil.so`（放置于 `app/libs` 或系统 `/system/lib/`）

---

## 3. 核心功能模块

### 3.1 外部 8 段 3 位数码管控制
- **点阵显示**: 支持向 3 位 8 段数码管输出指定字符或点阵（如全亮测试 `8.8.8`）。
- **循环计数测试**: 支持连续发送自增数值（`000` ~ `999`），用于验证数码管动态刷新与串行通信稳定性。
- **指示灯控制**: 支持红绿状态指示灯的独立开关控制。

### 3.2 多 SIM 卡槽切换与状态监控
- **3 卡槽下拉选择**: 支持在 `SIM 1`、`SIM 2`、`SIM 3` 之间无缝切换。
- **自动供电复位 (Power Reset)**: 切换 SIM 卡时通过 `SetDB9Power(0)` -> 延时 1000ms -> `SetDB9Power(1)` 完成卡槽冷复位。
- **电话栈状态监听与诊断**:
  - 结合 `TelephonyManager.getSimState()` 实时识别 `READY`、`ABSENT`、`NOT_READY`、`PIN/PUK_REQUIRED`、`CARD_RESTRICTED` 等状态。
  - 通过 `dumpsys telephony.registry` 获取当前活跃的 Data SubId 与服务状态。
- **CARD_IO_ERROR 自动容错恢复**: 当检测到 SIM 卡 IO 异常（`SIM_STATE_CARD_IO_ERROR`）时，自动触发卡槽电源复位并重新轮询，避免卡死。
- **UI 字符长度截断保护**: 限制状态显示文本最大长度，防止界面撑爆或崩溃。

### 3.3 设备 SN 与 IMEI 二维码生成
- **数据采集**: 结合系统属性 `ro.serialno` 和 Root 命令行 `cmd phone get-imei 0` 提取设备唯一序列号与 IMEI。
- **ZXing 二维码渲染**: 采用 ZXing 库将 `SN: <serial>, IMEI: <imei>` 动态编码为 512x512 的高对比度 Bitmap 并居中展示。
- **异步轮询重试机制**: 若 IMEI 尚未准备就绪，系统每隔 10 秒自动重新检测，并在获取成功后自动刷新二维码。

### 3.4 AC 电源与车载 ACC 状态检测
- **AC 电源广播监听**: 动态注册 `Intent.ACTION_BATTERY_CHANGED` 广播，实时区分：
  - AC 插入 充电中 (AC plugged, charging)
  - AC 插入 已充满 (AC plugged, full)
  - AC 插入 未充电 (AC plugged, not charging)
  - AC 未插入 未充电 (AC unplugged, not charging)
- **车载 ACC 状态轮询**: 每隔 1000ms 调用底层 JNI `HdxUtil.PowerOffScan()` 扫描 ACC 信号输入，实时反馈 ACC 插入 / 断开状态。

### 3.5 继电器测试 (Relay Pulse)
- 点击继电器测试按钮后，在后台子线程触发 GPIO 高电平：`HdxUtil.SetKeyboardPower(1)`，持续 444ms 后恢复低电平 `HdxUtil.SetKeyboardPower(0)`，模拟瞬时继电器吸合与释放。

### 3.6 物理按键与工控键盘事件拦截
- 重写 `dispatchKeyEvent` 处理自定义外设按键：
  - 功能键：`F1`、`F2`、`F3`、`F9`（字符）、`F10` (FN)、`F11` (扫描)、`F12` (左扫描)
  - 电话与导航键：`CALL`、`ENDCALL`、`DPAD_UP`、`DPAD_DOWN`、`ENTER`、`HOME`、`DEL`、`ESC`、`POWER`、`CAMERA`
  - 数字与符号键：`0` ~ `9`、`*`、`#`、`.`、音量加减等

---

## 4. JNI / Native 硬件接口说明 (`hdx.HdxUtil`)

所有底层驱动调用均通过 Native 动态库 `libhdxutil.so` 封装于 [`HdxUtil`](file:///h:/debug_app/111_user_sdk/extern_led8_display_AS/app/src/main/java/hdx/HdxUtil.java) 类中：

| JNI 方法名 | 参数说明 | 功能描述 |
| :--- | :--- | :--- |
| `SetLed8Display(byte[] data)` | `byte[]` 字节数组（如 `{'8','.','8','.','8'}`） | 控制 3 位数码管显示内容 |
| `SetLed8DisplayString(String data)` | `String` 字符串 | 字符串形式控制数码管显示 |
| `SetGreedLed(int enable)` | `1`: 开, `0`: 关 | 控制绿色 LED 指示灯 |
| `SetRedLed(int enable)` | `1`: 开, `0`: 关 | 控制红色 LED 指示灯 |
| `SwitchSimCard(int id)` | `1`: SIM1, `2`: SIM2, `3`: SIM3 | 切换 SIM 卡硬件通道 |
| `SetDB9Power(int enable)` | `1`: 供电, `0`: 断电 | 控制 DB9 / SIM 卡槽外设供电 |
| `SetDB9Power2(int enable)` | `1`: 供电, `0`: 断电 | 控制次级 DB9 接口供电 |
| `PowerOffScan()` | 无 | 扫描车载 ACC 点火/断电信号（返回 1 为已接入） |
| `SetKeyboardPower(int enable)`| `1`: 通电, `0`: 断电 | 控制键盘供电 / 继电器脉冲触发 |
| `EnableBuzze(int enable)` | `1`: 响, `0`: 静音 | 蜂鸣器控制 |
| `SetCameraBacklightness(int br)` | `0 ~ 255` 亮度等级 | 摄像头补光灯亮度控制 |
| `TriggerScan()` / `TriggerScan2()`| 无 | 触发一维/二维扫描头扫码 |
| `SetIDCARDPower(int enable)` | `1`: 通电, `0`: 断电 | 身份证阅读模块供电 |
| `SetPrinterPower(int enable)` | `1`: 通电, `0`: 断电 | 热敏打印机模块供电 |
| `SetRfidPower(int enable)` | `1`: 通电, `0`: 断电 | RFID 读卡模块供电 |
| `SetFingerPower(int enable)` | `1`: 通电, `0`: 断电 | 指纹模块供电 |
| `SwitchFilter(int status)` | `0`: 红外滤光片, `1`: 普通滤光片 | 摄像头滤光片切换 |

---

## 5. 编译、构建与部署

### 5.1 环境要求
- **Android Gradle Plugin (AGP)**: 9.2.1
- **Gradle**: 8.x+
- **JDK 版本**: Java 17 (`JavaVersion.VERSION_17`)
- **Compile SDK**: 36 (minorApiLevel = 1) / **Target SDK**: 34 / **Min SDK**: 29

### 5.2 编译与安装命令

```bash
# 1. 编译 Debug 版本 APK
./gradlew assembleDebug

# 2. 编译 Release 版本 APK
./gradlew assembleRelease

# 3. 通过 ADB 安装至测试设备
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 授予必要权限并以 Root 调试
adb root
adb shell pm grant com.example.extern_led8_display_for_sc200_gc111_c android.permission.READ_PHONE_STATE
```

---

## 6. 版本更新历史 (Changelog)

### [v1.4.0] - 2026-06-08
- **界面国际化 (i18n)**:
  - 将应用默认界面语言切换为英文，保留中文多语言资源 (`values-zh/strings.xml`)。
  - 优化横屏与竖屏界面文本适配。
- **设备标识与二维码增强**:
  - 增加设备序列号 (`ro.serialno`) 与 IMEI 组合生成的二维码功能。
  - 增加 Root 命令行提取 IMEI (`cmd phone get-imei 0`) 及 10 秒定时重试机制。
- **SIM 卡槽映射修正**:
  - 修正 UI 选项与实际底层物理 SIM 卡槽的索引对应关系。
  - 增加 UI 文本长度截断保护，防止 dumpsys 字符串过长破坏布局。

### [v1.3.0] - 2026-05-13
- **车载 ACC 状态检测**:
  - 新增 ACC 状态定时检测机制（通过 `HdxUtil.PowerOffScan()` 轮询，周期 1000ms）。
  - 新增 ACC 插入/拔出状态的实时 UI 标签与多语言支持。
- **数码管显示优化**:
  - 将数码管演示字符由 4 位调整为 3 位 (`8.8.8`) 规范。
- **开发规范梳理**:
  - 更新项目开发规范 `RULES.md`，明确性能、测试与代码要求。

### [v1.2.0] - 2026-04-15
- **SIM 卡槽 CARD_IO_ERROR 容错与自愈**:
  - 针对 `TelephonyManager.SIM_STATE_CARD_IO_ERROR` 增加自动 `reset_sim_slot_power()` 供电复位处理。
  - 优化 SIM 切换状态机，增加超时判定（300 秒）与异常恢复。
- **提示与日志优化**:
  - 优化 SIM 切换过程中的 Toast 提示与详细 dumpsys 诊断日志输出。

### [v1.1.0] - 2026-03-04
- **多 SIM 卡动态切换支持**:
  - 新增 Spinner 控件支持 SIM 1 / 2 / 3 切换。
  - 增加切换时的 DB9 供电断电与复位逻辑 (`SetDB9Power`)。
- **继电器脉冲测试功能**:
  - 新增继电器测试按钮，通过 `SetKeyboardPower` 产生 444ms 动作脉冲。
- **电源状态监听**:
  - 注册 `BatteryManager` 广播监听 AC 充电、充满及未插入状态。

### [v1.0.0] - 2026-03-03
- **项目初始化**:
  - 搭建 Android 基础工程与 Gradle 配置。
  - 集成 `libhdxutil.so` 及其 Native 接口封装 `HdxUtil`。
  - 实现 8 段 3 位数码管基础显示测试与全键盘按键事件拦截分发。
