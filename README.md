# Heart Rate Monitoring System

A three-tier cross-platform heart rate monitoring system built with Kotlin Multiplatform (KMP), targeting Wear OS, Android, and Desktop platforms.

## Overview

This system enables real-time heart rate monitoring from a Samsung Galaxy Watch 5, relayed through an Android phone, and visualized on a desktop application.

### Architecture

```
┌─────────────────────┐
│  Galaxy Watch 5     │  Heart Rate Data Collection
│  (Wear OS App)      │  via BioActive Sensor
└──────────┬──────────┘
           │ Data Layer API
           ▼
┌─────────────────────┐
│  Android Phone      │  Data Relay & Processing
│  (Phone App)        │  WebSocket/BLE Server
└──────────┬──────────┘
           │ WebSocket (primary)
           │ BLE GATT (fallback)
           ▼
┌─────────────────────┐
│  Desktop App        │  Real-time Visualization
│  (Windows/Mac/Linux)│  Data Analysis & Export
└─────────────────────┘
```

### Tech Stack

- **Language**: Kotlin Multiplatform (KMP)
- **UI Frameworks**:
  - Wear OS: Traditional Views (future: Compose for Wear OS)
  - Phone: Jetpack Compose
  - Desktop: Compose Multiplatform
- **Architecture**: Clean Architecture with MVVM
- **Dependency Injection**: Koin
- **Serialization**: kotlinx.serialization
- **Async**: Kotlin Coroutines & Flow

## Features

### Current (Phase 1)

- ✅ KMP project structure with 80-90% code sharing
- ✅ Shared data models with validation
- ✅ Domain layer with repositories and use cases
- ✅ Presentation layer with ViewModels
- ✅ Mock implementations for all communication layers
- ✅ Platform-specific UI scaffolds
- ✅ Comprehensive unit tests
- ✅ Shared UI utilities (theming, formatting)

### Planned (Phase 2+)

- ⏳ Real Wear OS sensor integration
- ⏳ Data Layer API implementation
- ⏳ WebSocket communication
- ⏳ Bluetooth Low Energy fallback
- ⏳ Foreground services for background monitoring
- ⏳ Dynamic sampling rate optimization
- ⏳ Desktop visualization with charts
- ⏳ Data persistence and export

## Project Structure

```
heart-rate-monitor/
├── shared/              # KMP shared module (business logic)
│   ├── commonMain/      # Shared code across all platforms
│   ├── androidMain/     # Android-specific implementations
│   └── desktopMain/     # Desktop-specific implementations
├── wear-app/            # Wear OS application
├── phone-app/           # Android phone application
├── desktop-app/         # Desktop application
└── gradle/              # Build configuration
```

## Getting Started

### Prerequisites

- **JDK**: 17 or higher
- **Android Studio**: Hedgehog (2023.1.1) or later
- **Gradle**: 9.2 (managed by Gradle Wrapper)
- **Wear OS Emulator/Device**: Galaxy Watch 5 (44mm/40mm)
- **Phone**: Android 14+ (API 34+)
- **Desktop**: Windows 10+, macOS 12+, or Linux

### Building the Project

#### Clone and Setup

```bash
# Clone the repository
git clone <repository-url>
cd MyApplication

# The project uses Gradle Wrapper - no additional setup needed
```

#### Build All Platforms

```bash
# Build all modules
./gradlew build

# Clean and rebuild
./gradlew clean build
```

#### Build Specific Platform

```bash
# Wear OS app (debug APK)
./gradlew :wear-app:assembleDebug

# Phone app (debug APK)
./gradlew :phone-app:assembleDebug

# Desktop app (native distribution)
./gradlew :desktop-app:installDist
```

### Running Unit Tests

```bash
# Run all tests
./gradlew test

# Run shared module tests only
./gradlew :shared:testDebugUnitTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Installing on Device

#### Wear OS App

```bash
# Connect Wear OS emulator or device via ADB
./gradlew :wear-app:installDebug

# Or manually install the APK
adb -e install wear-app/build/outputs/apk/debug/wear-app-debug.apk
```

#### Phone App

```bash
# Connect Android device via ADB
./gradlew :phone-app:installDebug

# Or manually install the APK
adb install phone-app/build/outputs/apk/debug/phone-app-debug.apk
```

#### Desktop App

```bash
# Run the desktop application
./desktop-app/build/install/desktop-app/bin/desktop-app

# Or create native distributions
./gradlew :desktop-app:packageDistributionForCurrentOS
```

## Development

### Module Dependencies

```
desktop-app ──┐
              ├──> shared (KMP common code)
phone-app  ──┘

wear-app ─────> shared (KMP common code)
```

### Common Development Commands

```bash
# Clean build artifacts
./gradlew clean

# Build without tests
./gradlew assembleDebug

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Generate dependency updates
./gradlew dependencyUpdates

# Analyze dependencies
./gradlew :shared:dependencies --configuration runtimeClasspath
```

### Code Organization

#### Shared Module

- **data/**: Data models, DTOs, API interfaces
  - `model/`: HeartRateData, DeviceInfo, SensorReading
  - `communication/`: DataLayerClient, WebSocketClient, BleClient
- **domain/**: Business logic, use cases, repository interfaces
  - `repository/`: HeartRateRepository interface
  - `usecase/`: ObserveHeartRate, GetBatteryLevel
- **presentation/**: ViewModels, UI state, utilities
  - `model/`: HeartRateUiState, ConnectionStatus
  - `viewmodel/`: HeartRateViewModel
  - `ui/`: Theme, Formatters, Constants

#### Platform Modules

Each platform module contains:
- Platform-specific main activity/entry point
- UI components (Compose or Views)
- Platform-specific implementations of shared interfaces
- Resources (strings, drawables, etc.)

## Testing

### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew :shared:test --tests "com.heartrate.shared.domain.usecase.UseCaseTest"

# Run with verbose output
./gradlew test --info
```

### Instrumented Tests

```bash
# Requires connected device/emulator
./gradlew :wear-app:connectedAndroidTest
./gradlew :phone-app:connectedAndroidTest
```

### Test Coverage

Current test coverage:
- Data models: 100% (serialization, validation)
- Domain layer: 100% (use cases, repositories)
- Presentation layer: 90% (ViewModels)
- Platform-specific: 0% (Phase 2)

## Troubleshooting

### Windows Build Issues

**Problem**: `Unable to delete directory` errors during build
**Solution**: This is a known Windows file locking issue. Try:
```bash
./gradlew --stop
# Wait a few seconds
./gradlew build --no-daemon
```

### Gradle Daemon Issues

**Problem**: Build fails with daemon errors
**Solution**: Stop all daemons and retry:
```bash
./gradlew --stop
./gradlew build
```

### Wear OS Emulator Not Detected

**Problem**: ADB doesn't detect Wear OS emulator
**Solution**: Check emulator connection:
```bash
adb devices -l
# Should show both phone and wear emulators
```

### Out of Memory Errors

**Problem**: Build fails with OutOfMemoryError
**Solution**: Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

## Contributing

### Code Style

This project follows Kotlin coding conventions:
- Use `ktlint` for formatting: `./gradlew ktlintFormat`
- Run static analysis: `./gradlew detekt`
- Follow Clean Architecture principles

### Commit Messages

Follow conventional commits format:
```
feat: add Wear OS sensor integration
fix: resolve WebSocket connection timeout
docs: update README with build instructions
test: add unit tests for HeartRateViewModel
```

### Pull Request Process

1. Create a feature branch from `master`
2. Make changes and ensure tests pass
3. Update documentation as needed
4. Submit a pull request with description
5. Address review feedback
6. Merge after approval

## Architecture Decisions

### Why Kotlin Multiplatform?

- Only technology supporting Wear OS + code sharing
- Native performance on all platforms
- Strong Kotlin ecosystem and IDE support
- 80-90% code sharing achievable

### Why Clean Architecture?

- Domain logic independent of platforms (testable, reusable)
- Platform-specific implementations only where necessary
- ViewModels shared across all platforms
- Aligns with Android best practices

### Why Compose?

- Modern declarative UI framework
- Cross-platform support (Android, Desktop, future: Wear OS)
- Less boilerplate than traditional Views
- Better state management

## Performance Targets

- **End-to-end latency**: < 1 second
- **Watch battery life**: > 4 hours continuous monitoring
- **BLE connection stability**: > 95%
- **Desktop memory usage**: < 500MB
- **Data accuracy**: > 99%

## License

[Add your license information here]

## Authors

[Add author information here]

## Acknowledgments

- Kotlin Multiplatform team
- Jetbrains Compose Multiplatform
- Samsung Wear OS developer resources
- Android Jetpack libraries

## Resources

- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Wear OS Developer Guide](https://developer.android.com/training/wearables)
- [OpenSpec Proposal System](./openspec/AGENTS.md)

## Support

For issues, questions, or contributions, please:
- Open an issue on GitHub
- Check existing documentation in `md/` directory
- Review OpenSpec proposals in `openspec/` directory
- Refer to `CLAUDE.md` for AI assistant development guidance
