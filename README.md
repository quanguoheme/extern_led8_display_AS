# External 8-Segment LED Display & Hardware Integration Test Application (SC200 / 111) - Project Documentation & Changelog

## 1. Project Overview

This project (`extern_led8_display_AS` / `com.example.extern_led8_display_for_sc200_111_c`) is a dedicated hardware integration testing and control application built for Android embedded industrial / vehicle-mounted devices based on the **Quectel SC200 / 111** hardware platform.

The application integrates control over an external 3-digit 8-segment LED display, dynamic 3-slot SIM card switching with power-cycle fault tolerance, vehicle ignition (ACC) status detection, AC power and battery charge state monitoring, dynamic device SN & IMEI QR code generation, relay pulse triggering, full physical keypad event mapping, and low-level hardware JNI driver interfaces.

---

## 2. Hardware & System Specifications

### 2.1 Hardware & OS Requirements
- **Target Chipset / Motherboard**: Quectel SC200 / 111 Series
- **Operating System**: Android 10 (API Level 29) through Android 14
- **Display Resolution**: 800 × 480 (Optimized for Landscape mode)
- **Display Density**: 160 dpi (1dp = 1px)
- **RAM / Storage**: RAM ≥ 3GB, Flash Storage ≥ 16GB
- **Required Permissions & System Environment**:
  - `android.permission.READ_PHONE_STATE` (Telephony stack & SIM state inspection)
  - **Root Access** (Required for `cmd phone get-imei 0` IMEI retrieval and system-level diagnostics)
  - Native Library: `libhdxutil.so` (Located in `app/libs` or system `/system/lib/`)

---

## 3. Core Functional Modules

### 3.1 External 8-Segment 3-Digit LED Display Control
- **Segment / Dot Display**: Supports outputting custom characters and dots to the 3-digit 8-segment LED display (e.g., all-on test `8.8.8`).
- **Dynamic Cyclic Counter Test**: Periodically sends incrementing digits (`000` ~ `999`) to verify refresh stability and serial communication integrity.
- **LED Indicator Control**: Independent control for red and green status LEDs.

### 3.2 Multi-SIM Slot Switching & Telephony State Monitoring
- **3-Slot Switching via Dropdown (Spinner)**: Enables seamless switching between `SIM 1`, `SIM 2`, and `SIM 3`.
- **Automatic Power Reset**: When switching slots, executes a cold reset via `SetDB9Power(0)` -> 1000ms delay -> `SetDB9Power(1)`.
- **Telephony Stack Status Inspection**:
  - Real-time detection via `TelephonyManager.getSimState()` for states such as `READY`, `ABSENT`, `NOT_READY`, `PIN/PUK_REQUIRED`, `CARD_RESTRICTED`, etc.
  - Telephony registry diagnosis via `dumpsys telephony.registry` extracting active data SubId and service status.
- **CARD_IO_ERROR Auto-Recovery**: Automatically triggers a slot power reset and restarts polling when `SIM_STATE_CARD_IO_ERROR` is detected.
- **UI String Overflow Protection**: Enforces text length limits to prevent UI overflows caused by verbose dumpsys outputs.

### 3.3 Device Serial Number & IMEI QR Code Generation
- **Hardware ID Extraction**: Retrieves device serial number from system property `ro.serialno` and IMEI via root command `cmd phone get-imei 0`.
- **ZXing QR Code Rendering**: Encodes `SN: <serial>, IMEI: <imei>` into a high-contrast 512x512 Bitmap rendered in real-time.
- **Asynchronous Polling & Retry Mechanism**: Automatically retries every 10 seconds if IMEI is not yet initialized until the QR code is successfully generated.

### 3.4 AC Power & Vehicle Ignition (ACC) Detection
- **AC Power Broadcast Monitoring**: Listens to `Intent.ACTION_BATTERY_CHANGED` broadcasts to classify power status:
  - AC plugged, charging
  - AC plugged, full
  - AC plugged, not charging
  - AC unplugged, not charging
- **Vehicle ACC Status Polling**: Polls the hardware ACC line every 1000ms using JNI `HdxUtil.PowerOffScan()`, updating the UI with ACC plugged / unplugged state.

### 3.5 Relay Pulse Test
- Tapping the relay test button spawns a background thread that pulls GPIO high (`HdxUtil.SetKeyboardPower(1)`), waits 444ms, and pulls it low (`HdxUtil.SetKeyboardPower(0)`), simulating a momentary relay pulse.

### 3.6 Physical Keypad & Event Interception
- Overrides `dispatchKeyEvent` to handle custom external keyboard inputs:
  - Function keys: `F1`, `F2`, `F3`, `F9` (Letter), `F10` (FN), `F11` (Scan), `F12` (Left Scan)
  - Telephony & Navigation keys: `CALL`, `ENDCALL`, `DPAD_UP`, `DPAD_DOWN`, `ENTER`, `HOME`, `DEL`, `ESC`, `POWER`, `CAMERA`
  - Numeric & Symbol keys: `0` ~ `9`, `*`, `#`, `.`, Volume Up/Down, etc.

---

## 4. JNI / Native Hardware API Specification (`hdx.HdxUtil`)

All low-level hardware interactions are bridged via `libhdxutil.so` through the [`HdxUtil`](file:///h:/debug_app/111_user_sdk/extern_led8_display_AS/app/src/main/java/hdx/HdxUtil.java) class:

| JNI Method | Parameters | Description |
| :--- | :--- | :--- |
| `SetLed8Display(byte[] data)` | `byte[]` byte array (e.g. `{'8','.','8','.','8'}`) | Displays data on 3-digit 8-segment LED |
| `SetLed8DisplayString(String data)` | `String` text | Displays string on 3-digit LED |
| `SetGreedLed(int enable)` | `1`: On, `0`: Off | Controls green indicator LED |
| `SetRedLed(int enable)` | `1`: On, `0`: Off | Controls red indicator LED |
| `SwitchSimCard(int id)` | `1`: SIM1, `2`: SIM2, `3`: SIM3 | Switches physical SIM card channel |
| `SetDB9Power(int enable)` | `1`: Power On, `0`: Power Off | Controls DB9 / SIM slot power supply |
| `SetDB9Power2(int enable)` | `1`: Power On, `0`: Power Off | Controls secondary DB9 power supply |
| `PowerOffScan()` | None | Scans vehicle ACC ignition signal (`1` = plugged) |
| `SetKeyboardPower(int enable)`| `1`: Power On, `0`: Power Off | Controls keypad power / triggers relay pulse |
| `EnableBuzze(int enable)` | `1`: Beep, `0`: Mute | Controls hardware buzzer |
| `SetCameraBacklightness(int br)` | `0 ~ 255` Brightness | Adjusts camera fill-light brightness |
| `TriggerScan()` / `TriggerScan2()`| None | Triggers 1D / 2D barcode scanner engine |
| `SetIDCARDPower(int enable)` | `1`: Power On, `0`: Power Off | Powers ID card reader module |
| `SetPrinterPower(int enable)` | `1`: Power On, `0`: Power Off | Powers thermal printer module |
| `SetRfidPower(int enable)` | `1`: Power On, `0`: Power Off | Powers RFID reader module |
| `SetFingerPower(int enable)` | `1`: Power On, `0`: Power Off | Powers fingerprint module |
| `SwitchFilter(int status)` | `0`: IR Filter, `1`: Normal Filter | Switches camera optical filter |

---

## 5. Build, Compilation & Deployment

### 5.1 Build Environment
- **Android Gradle Plugin (AGP)**: 9.2.1
- **Gradle Version**: 8.x+
- **JDK Version**: Java 17 (`JavaVersion.VERSION_17`)
- **Compile SDK**: 36 (minorApiLevel = 1) / **Target SDK**: 34 / **Min SDK**: 29

### 5.2 Build & Installation Commands

```bash
# 1. Build Debug APK
./gradlew assembleDebug

# 2. Build Release APK
./gradlew assembleRelease

# 3. Install to device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Grant required runtime permissions with root
adb root
adb shell pm grant com.example.extern_led8_display_for_sc200_111_c android.permission.READ_PHONE_STATE
```

---

## 6. Version Changelog

### [v1.4.0] - 2026-06-08
- **UI Internationalization (i18n)**:
  - Switched default application UI language to English, preserving Chinese localization in `values-zh/strings.xml`.
  - Optimized landscape and portrait layout string presentation.
- **Device Identity & QR Code Enhancement**:
  - Added QR code generation combining device Serial Number (`ro.serialno`) and IMEI.
  - Added root command extraction for IMEI (`cmd phone get-imei 0`) with a 10-second recurring retry timer.
- **SIM Card Slot Mapping Fix**:
  - Corrected mapping between UI dropdown indices and physical SIM card slots.
  - Added UI text length bounding to prevent layout clipping from verbose telephony dumpsys logs.

### [v1.3.0] - 2026-05-13
- **Vehicle ACC Status Detection**:
  - Added periodic ACC ignition status polling via `HdxUtil.PowerOffScan()` every 1000ms.
  - Added real-time ACC plugged / unplugged UI indicators with multilingual strings.
- **LED Display Optimization**:
  - Adjusted LED display test output from 4 digits to 3 digits (`8.8.8`).
- **Development Rules Clean-up**:
  - Updated `RULES.md` specifying performance, testing, and code guidelines.

### [v1.2.0] - 2026-04-15
- **SIM Slot CARD_IO_ERROR Fault Recovery**:
  - Added automatic power reset (`reset_sim_slot_power()`) upon encountering `TelephonyManager.SIM_STATE_CARD_IO_ERROR`.
  - Optimized SIM switching state machine with timeout handling (300 seconds) and retry loops.
- **Diagnostics & Notification Improvements**:
  - Refined Toast notification messages during SIM switching and enriched dumpsys diagnostic log output.

### [v1.1.0] - 2026-03-04
- **Multi-SIM Slot Dynamic Switching**:
  - Added Spinner dropdown UI for SIM 1 / 2 / 3 selection.
  - Implemented DB9 power cycling logic (`SetDB9Power`) during slot switching.
- **Relay Pulse Test**:
  - Added relay test button producing a 444ms pulse via `SetKeyboardPower`.
- **Power State Broadcast Monitoring**:
  - Registered `BatteryManager` broadcast receiver to display AC charging, full, and disconnected states.

### [v1.0.0] - 2026-03-03
- **Initial Project Release**:
  - Established Android Gradle project architecture.
  - Integrated native library `libhdxutil.so` and JNI wrapper `HdxUtil`.
  - Implemented 3-digit 8-segment LED display test and full keypad event dispatcher.
