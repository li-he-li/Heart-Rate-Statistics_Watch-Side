# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Smart Watch Heart Rate Monitoring System** - a three-tier application platform consisting of:
1. **Wear OS App** (Galaxy Watch 5) - Heart rate data collection
2. **Android Phone App** - Data relay and processing
3. **Desktop App** - Real-time visualization and analysis

**Current State:** This is a fresh Android project template with standard gradle structure. The implementation phase has not yet begun.

**Tech Stack:**
- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL (version catalog in `gradle/libs.versions.toml`)
- **Platform:** Android (minSdk 34, targetSdk 36, compileSdk 36)
- **Architecture Planning:** Kotlin Multiplatform (KMP) for code sharing across platforms

## Common Development Commands

```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build to connected device
./gradlew installDebug
```

## Architecture and Data Flow

The planned system follows this data flow:

```
Galaxy Watch 5 (Wear OS)
    ↓ [Data Layer API]
Android Phone App
    ↓ [WebSocket (primary) / BLE GATT (fallback)]
Desktop App
```

**Key Design Decisions:**
- **Data Layer API** used for Watch→Phone communication (Wear OS optimized, low power)
- **WebSocket** prioritized for Phone→Desktop transmission (cross-network, unlimited range)
- **BLE GATT Server** as backup for short-range Phone→Desktop (10-20 meters)
- **Shared business logic** via Kotlin Multiplatform (80-90% code reuse target)

## Target Hardware Specifications

**Watch Device:** Samsung Galaxy Watch 5 (44mm/40mm)
- Processor: Exynos W920 (5nm, dual-core 1.18GHz)
- RAM: 1.5GB, Storage: 16GB
- Battery: 398mAh (44mm) / 276mAh (40mm)
- Sensor: BioActive (3-in-1: Heart Rate + ECG + BIA)
- 8-PD PPG sensor, max 200Hz sampling
- Expected battery life: 3-5 hours continuous monitoring (dynamic sampling)

**Phone Recommendation:** Samsung Galaxy S23/S24 series (best Data Layer API stability)

## Key Technical Constraints

### Wear OS Limitations
- **Background services** terminated after 1 minute → Must use **Foreground Services**
- **Battery capacity** limits continuous high-frequency monitoring → Implement **dynamic sampling rate**
- **BLE Peripheral mode** requires special certificates → Use **Data Layer API** instead
- **Sampling rate** limited to ~5Hz normally (up to 10Hz with special permissions)

### Permissions Required (Wear OS)
```xml
BODY_SENSORS
BODY_SENSORS_BACKGROUND
ACCESS_BACKGROUND_LOCATION
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
```

### Permissions Required (Phone)
```xml
BLUETOOTH_SCAN (Android 12+)
BLUETOOTH_CONNECT
BLUETOOTH_ADVERTISE (BLE fallback)
FOREGROUND_SERVICE
INTERNET (WebSocket)
```

## Battery Optimization Strategies

1. **Dynamic Sampling Rate:**
   - Sleeping: 1Hz
   - Sitting: 1Hz
   - Walking: 3Hz
   - Running: 5Hz

2. **Sensor Optimization:**
   - Use sensor batching (aggregate 1 second)
   - Only sample when skin contact detected
   - Dark theme for AMOLED power saving

3. **Foreground Service Requirements:**
   - Must display persistent notification
   - Request battery optimization whitelist
   - Avoid `keepScreenOn`

## Project Structure (Planned)

```
heart-rate-monitor/
├── shared/              # KMP shared module (80-90% business logic)
│   ├── commonMain/      # Data models, domain logic, protocols
│   ├── androidMain/     # Android-specific implementations
│   └── desktopMain/     # Desktop-specific implementations
├── wear-app/            # Wear OS application
├── phone-app/           # Android phone application
└── desktop-app/         # Desktop application (Compose Multiplatform)
```

## Data Models

**Standard Heart Rate Data (KMP shared):**
```kotlin
@Serializable
data class HeartRateData(
    val timestamp: Long,           // Unix timestamp (ms)
    val heartRate: Int,            // BPM
    val deviceId: String,
    val batteryLevel: Int? = null,
    val signalQuality: Int? = null
)
```

**BLE GATT Standard Format (0x2A37):**
```kotlin
data class BleHeartRateMeasurement(
    val flags: Byte,
    val heartRate: Short,
    val energyExpended: Short? = null,
    val rrIntervals: List<Short>? = null
)
```

## Performance Targets

- **End-to-end latency:** < 1 second
- **Watch battery life:** > 4 hours continuous monitoring
- **BLE connection stability:** > 95%
- **Desktop memory usage:** < 500MB
- **Data accuracy:** > 99%

## File Organization

- `build.gradle.kts` - Root project build configuration
- `settings.gradle.kts` - Project structure settings
- `app/build.gradle.kts` - Main application module
- `gradle/libs.versions.toml` - Version catalog for dependency management
- `md/` - Technical documentation and feasibility studies
- `openspec/` - OpenSpec proposal system documentation

## OpenSpec Integration

This project uses the **OpenSpec** proposal system for managing changes. When working on features that involve:
- Planning or proposals
- New capabilities or breaking changes
- Architecture shifts
- Major performance/security work

Always reference `@/openspec/AGENTS.md` for the authoritative specification before coding.

<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->