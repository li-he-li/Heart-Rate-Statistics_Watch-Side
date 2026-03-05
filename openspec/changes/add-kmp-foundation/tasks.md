# Tasks: Add KMP Foundation for Heart Rate Monitoring System

## 1. Project Configuration and Gradle Setup
- [x] 1.1 Update root `build.gradle.kts` to include KMP plugin and classpath
- [x] 1.2 Update `gradle/libs.versions.toml` with KMP, Compose, and dependency versions
- [x] 1.3 Update `settings.gradle.kts` to include new module structure (shared, wear-app, phone-app, desktop-app)
- [x] 1.4 Verify project syncs successfully in Android Studio

## 2. Create Shared Module Structure
- [x] 2.1 Create `shared/` directory with `build.gradle.kts`
- [x] 2.2 Configure KMP targets (android, jvm for desktop)
- [x] 2.3 Create source set directories: `commonMain/`, `androidMain/`, `desktopMain/`
- [x] 2.4 Add kotlinx.serialization dependency to shared module
- [x] 2.5 Add test dependencies to shared module
- [x] 2.6 Verify shared module compiles with placeholder class

## 3. Implement Shared Data Models
- [x] 3.1 Create `data/model/HeartRateData.kt` in commonMain with serialization
- [x] 3.2 Create `data/model/DeviceInfo.kt` in commonMain with serialization
- [x] 3.3 Create `data/model/SensorReading.kt` in commonMain with serialization
- [x] 3.4 Add unit tests for data model serialization
- [x] 3.5 Verify JSON serialization works correctly

## 4. Create Shared Domain Layer
- [x] 4.1 Create `domain/repository/HeartRateRepository.kt` interface in commonMain
- [x] 4.2 Create `domain/usecase/ObserveHeartRate.kt` use case in commonMain
- [x] 4.3 Create `domain/usecase/GetBatteryLevel.kt` use case in commonMain
- [ ] 4.4 Add unit tests for use cases with mock repositories
- [x] 4.5 Verify domain layer compiles independently

## 5. Implement Platform-Specific Repository Scaffolds
- [x] 5.1 Create `expect class HeartRateRepositoryImpl` in commonMain
- [x] 5.2 Implement `actual class HeartRateRepositoryImpl` in androidMain (mock implementation)
- [x] 5.3 Implement `actual class HeartRateRepositoryImpl` in desktopMain (mock implementation)
- [x] 5.4 Verify expect/actual compiles across all platforms

## 6. Create Wear OS App Module
- [x] 6.1 Create `wear-app/` directory with `build.gradle.kts`
- [x] 6.2 Configure Wear OS SDK and dependencies
- [x] 6.3 Add dependency on shared module
- [x] 6.4 Create `AndroidManifest.xml` with Wear OS permissions
- [x] 6.5 Create MainActivity with basic UI (TextView-based)
- [x] 6.6 Create Wear OS app resources
- [x] 6.7 Verify Wear OS app builds successfully

## 7. Create Phone App Module
- [x] 7.1 Create `phone-app/` directory with `build.gradle.kts`
- [x] 7.2 Configure Android SDK and dependencies (Jetpack Compose)
- [x] 7.3 Add dependency on shared module
- [x] 7.4 Add dependencies for coroutines
- [x] 7.5 Create `AndroidManifest.xml` with phone permissions
- [x] 7.6 Create MainActivity with Jetpack Compose UI
- [x] 7.7 Create phone app resources
- [x] 7.8 Verify phone app builds successfully

## 8. Create Desktop App Module
- [x] 8.1 Create `desktop-app/` directory with `build.gradle.kts`
- [x] 8.2 Configure Compose Multiplatform for desktop
- [x] 8.3 Add dependency on shared module
- [x] 8.4 Create `main()` function with Compose Desktop window
- [x] 8.5 Create placeholder UI with heart rate display
- [x] 8.9 Verify desktop app builds successfully

## 9. Implement Communication Layer Scaffolds

## 7. Create Phone App Module
- [ ] 7.1 Create `phone-app/` directory with `build.gradle.kts`
- [ ] 7.2 Configure Android SDK and dependencies (Jetpack Compose)
- [ ] 7.3 Add dependency on shared module
- [ ] 7.4 Add dependencies for OkHttp (WebSocket) and Bluetooth
- [ ] 7.5 Create `AndroidManifest.xml` with phone permissions
- [ ] 7.6 Create placeholder MainActivity with Jetpack Compose UI
- [ ] 7.7 Create phone app icon and resources
- [ ] 7.8 Verify phone app builds successfully

## 8. Create Desktop App Module
- [ ] 8.1 Create `desktop-app/` directory with `build.gradle.kts`
- [ ] 8.2 Configure Compose Multiplatform for desktop
- [ ] 8.3 Add dependency on shared module
- [ ] 8.4 Add Ktor dependency for WebSocket server
- [ ] 8.5 Create `main()` function with Compose Desktop window
- [ ] 8.6 Create placeholder UI with heart rate display
- [ ] 8.7 Create desktop app icon and resources
- [ ] 8.8 Configure packaging for Windows/macOS/Linux
- [ ] 8.9 Verify desktop app builds and runs

## 9. Implement Communication Layer Scaffolds
- [ ] 9.1 Create `expect class DataLayerClient` in commonMain (Watch→Phone)
- [ ] 9.2 Implement `actual class DataLayerClient` in androidMain (mock)
- [ ] 9.3 Create `expect class WebSocketClient` in commonMain (Phone→Desktop)
- [ ] 9.4 Implement `actual class WebSocketClient` in androidMain (mock)
- [ ] 9.5 Implement `actual class WebSocketClient` in desktopMain (mock)
- [ ] 9.6 Create `expect class BleClient` in commonMain (Phone→Desktop fallback)
- [ ] 9.7 Implement `actual class BleClient` in androidMain (mock)
- [ ] 9.8 Implement `actual class BleClient` in desktopMain (mock)

## 10. Add Dependency Injection Setup
- [ ] 10.1 Add Koin dependency to all modules
- [ ] 10.2 Create `di/AppModule.kt` in shared module
- [ ] 10.3 Set up DI in Wear OS app
- [ ] 10.4 Set up DI in phone app
- [ ] 10.5 Set up DI in desktop app
- [ ] 10.6 Verify DI resolves dependencies correctly

## 11. Create Common UI Components (Shared)
- [ ] 11.1 Create `presentation/model/HeartRateUiState.kt` in commonMain
- [ ] 11.2 Create `presentation/viewmodel/HeartRateViewModel.kt` in commonMain
- [ ] 11.3 Create base UI components in commonMain (if applicable to KMP)
- [ ] 11.4 Add ViewModel tests

## 12. Implement Platform UI Scaffolds
- [ ] 12.1 Create heart rate display screen in Wear OS app (Compose for Wear OS)
- [ ] 12.2 Create status screen in phone app (Jetpack Compose)
- [ ] 12.3 Create heart rate display screen in desktop app (Compose Multiplatform)
- [ ] 12.4 Connect ViewModels to UIs
- [ ] 12.5 Verify all UIs render without crashing

## 13. Add Navigation Structure
- [ ] 13.1 Set up navigation in Wear OS app (Compose Wear OS navigation)
- [ ] 13.2 Set up navigation in phone app (Compose Navigation)
- [ ] 13.3 Set up navigation in desktop app (Compose Multiplatform navigation or custom)
- [ ] 13.4 Verify navigation works between placeholder screens

## 14. Configure CI/CD Pipeline
- [ ] 14.1 Create `.github/workflows/build.yml` for GitHub Actions
- [ ] 14.2 Configure build steps for all three platforms
- [ ] 14.3 Add unit test execution to CI
- [ ] 14.4 Add build artifact generation (APK for Android, native binaries for desktop)
- [ ] 14.5 Verify CI pipeline runs successfully

## 15. Documentation and Developer Setup
- [ ] 15.1 Update `CLAUDE.md` with KMP project structure
- [ ] 15.2 Create `README.md` with build instructions for all platforms
- [ ] 15.3 Add troubleshooting guide for common KMP build issues
- [ ] 15.4 Document how to run each platform app locally
- [ ] 15.5 Add architecture diagrams to documentation

## 16. Validation and Testing
- [ ] 16.1 Build all three platform apps from clean state
- [ ] 16.2 Run unit tests on shared module (target: 80% coverage of domain layer)
- [ ] 16.3 Deploy Wear OS app to emulator or device
- [ ] 16.4 Deploy phone app to emulator or device
- [ ] 16.5 Run desktop app on development machine
- [ ] 16.6 Verify all apps launch without crashes
- [ ] 16.7 Create smoke test that passes on all platforms

## Dependencies and Parallelization

**Critical Path** (must be sequential):
1 → 2 → 3 → 4 → 5 → (6, 7, 8) → 9 → 10 → 11 → 12 → 13 → 14 → 16

**Parallelizable Work**:
- Tasks 6, 7, 8 (platform app modules) can be done in parallel after task 5
- Tasks 12.1, 12.2, 12.3 (platform UIs) can be done in parallel after task 11
- Tasks 13.1, 13.2, 13.3 (navigation) can be done in parallel after task 12
- Task 15 (documentation) can be done in parallel with implementation tasks

**Validation Gates**:
- After task 2: Verify shared module compiles
- After task 5: Verify expect/actual compiles
- After tasks 6-8: Verify all platform apps build
- After task 16: All validation tests pass

## Estimated Completion Order

1. **Foundation** (Tasks 1-5): Project structure, shared module, data models, domain layer
2. **Platform Apps** (Tasks 6-8): Wear OS, Phone, Desktop app scaffolds
3. **Communication Layer** (Task 9): Platform-specific API scaffolds
4. **DI and UI** (Tasks 10-13): Dependency injection, ViewModels, UI components, navigation
5. **CI/CD and Docs** (Tasks 14-15): Automation and documentation
6. **Validation** (Task 16): End-to-end testing and verification
