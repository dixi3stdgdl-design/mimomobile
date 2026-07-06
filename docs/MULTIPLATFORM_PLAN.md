# MiMo Mobile - Multiplatform Integration Plan

## Executive Summary

Análisis de tres estrategias para agregar soporte iOS a la app Android existente de MiMo Mobile.

---

## Current Architecture (Android)

```
com.mimo.mobile/
├── MainActivity.kt
├── network/
│   ├── WebSocketClient.kt    # Custom RFC 6455 over raw TCP
│   ├── ApiClient.kt          # OpenAI/Claude/MiMo API
│   ├── AdbHttpClient.kt
│   └── NetworkMonitor.kt
├── viewmodel/
│   └── MiMoViewModel.kt      # Centralized state + DataStore
└── ui/
    ├── screens/ (7)
    │   ├── ChatScreen.kt
    │   ├── TerminalScreen.kt
    │   ├── FileBrowserScreen.kt
    │   ├── RemoteScreen.kt
    │   ├── DeviceManagerScreen.kt
    │   ├── BuildVisualizerScreen.kt
    │   └── SettingsScreen.kt
    ├── components/
    │   ├── PerspectiveGridBackground.kt  # Canvas animation
    │   └── MatplotlibBackground.kt       # Canvas animation
    └── theme/
        └── Theme.kt
```

**Key Technical Details:**
- WebSocket: Custom implementation with manual frame encode/decode, exponential backoff reconnection
- Protocol: `Sec-WebSocket-Protocol: mimocode-v1` with JSON message payloads
- State: `AndroidViewModel` with `MutableStateFlow<AppState>`
- Persistence: Jetpack DataStore

---

## OPTION 1: Separate Swift App

### Architecture

```
MiMoMobile-iOS/
├── MiMoMobile/
│   ├── App/
│   │   ├── MiMoMobileApp.swift
│   │   └── ContentView.swift
│   ├── Models/
│   │   ├── WsMessage.swift
│   │   ├── AppState.swift
│   │   └── ... (mirrors Kotlin models)
│   ├── Network/
│   │   ├── WebSocketClient.swift  # URLSession/NWConnection
│   │   ├── ApiClient.swift
│   │   └── NetworkMonitor.swift   # NWPathMonitor
│   ├── ViewModels/
│   │   └── MiMoViewModel.swift    # @Observable
│   ├── Views/
│   │   ├── Chat/
│   │   ├── Terminal/
│   │   ├── Files/
│   │   ├── Remote/
│   │   ├── DeviceManager/
│   │   ├── Build/
│   │   ├── Settings/
│   │   └── Components/
│   │       ├── PerspectiveGridView.swift  # Metal
│   │       └── MatplotlibBackground.swift # CoreGraphics
│   └── Utilities/
│       ├── WebSocketFrame.swift   # RFC 6455 codec
│       └── Storage.swift          # UserDefaults
└── MiMoMobileTests/
```

### Code Distribution

| Layer | Shared | Android | iOS |
|-------|--------|---------|-----|
| Data Models | 0% | 100% Kotlin | 100% Swift |
| Business Logic | 0% | 100% Kotlin | 100% Swift |
| Networking | 0% | Java Socket | NWConnection |
| UI | 0% | Jetpack Compose | SwiftUI |
| Animations | 0% | Canvas | Metal |

**Total Code Duplication: 100%**

### Effort Estimate

| Component | Files | Weeks |
|-----------|-------|-------|
| Models | 6 | 0.5 |
| Network Layer | 4 | 2.5 |
| WebSocket RFC 6455 | 1 | 2.0 |
| Canvas Animations | 3 | 3.5 |
| ViewModels | 3 | 2.0 |
| Views (14 screens) | 14 | 5.0 |
| Navigation | 2 | 1.0 |
| Testing | 5 | 2.0 |
| **Total** | **~38** | **~18.5** |

### Pros
- Pure native experience (haptics, gestures, gestures)
- No cross-platform dependencies or tooling complexity
- Direct access to all Apple APIs
- Optimal performance for Metal animations

### Cons
- Complete code duplication - no shared logic
- Two separate codebases to maintain
- Bugs may exist on one platform but not the other
- Feature parity requires manual synchronization

### WebSocket Compatibility

Requires full reimplementation of:
- Frame encoding/decoding (masking, opcodes)
- Handshake with `Sec-WebSocket-Protocol: mimocode-v1`
- Exponential backoff reconnection logic

### Infrastructure Requirements
- macOS with Xcode 15+
- Apple Developer Account ($99/year)
- No impact on existing Android build

---

## OPTION 2: Kotlin Multiplatform (KMP)

### Architecture

```
MiMoMobile/
├── shared/                           # NEW: KMP module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/mimo/shared/
│       │   ├── models/              # All data classes
│       │   ├── viewmodel/           # Business logic
│       │   ├── network/
│       │   │   ├── WebSocketClient.kt   # expect
│       │   │   └── ApiClient.kt
│       │   ├── repository/
│       │   └── domain/
│       ├── androidMain/kotlin/.../
│       │   └── network/
│       │       └── WebSocketClient.android.kt  # actual (Java Socket)
│       └── iosMain/kotlin/.../
│           └── network/
│               └── WebSocketClient.ios.kt      # actual (NWConnection)
├── android/                          # EXISTING: Refactored to use shared
│   ├── build.gradle.kts
│   └── src/main/java/com/mimo/mobile/
│       ├── MainActivity.kt
│       └── ui/                       # Native Compose UI
└── ios/                              # NEW: SwiftUI views
    ├── MiMoMobile.xcodeproj
    └── MiMoMobile/
        ├── App/
        ├── Views/                    # SwiftUI
        │   ├── Chat/
        │   ├── Terminal/
        │   ├── Files/
        │   ├── Remote/
        │   ├── DeviceManager/
        │   ├── Build/
        │   ├── Settings/
        │   └── Components/
        │       ├── PerspectiveGridView.swift  # UIViewRepresentable
        │       └── MatplotlibBackground.swift
        └── ViewModels/
            └── MiMoViewModel.swift   # Wraps shared KMP ViewModel
```

### Code Distribution

| Layer | Shared KMP | Android Native | iOS Native |
|-------|------------|----------------|------------|
| Data Models | 100% | - | - |
| ViewModel | 90% | - | - |
| Business Logic | 85% | - | - |
| WebSocket Core | 70% | expect/actual | expect/actual |
| API Client | 95% | - | - |
| Animations | 0% | Canvas | Metal |
| UI | 0% | Compose | SwiftUI |

**Shared Code: ~60% of total logic**

### Effort Estimate

| Component | KMP Files | iOS Files | Weeks |
|-----------|-----------|-----------|-------|
| Extract shared module | 15 | - | 3.0 |
| Shared Models | 6 | - | 0.5 |
| Shared ViewModel | 3 | - | 1.5 |
| WebSocket expect/actual | 2 | - | 2.0 |
| iOS Views (SwiftUI) | - | 14 | 4.5 |
| iOS Animations (Metal) | - | 3 | 2.5 |
| iOS ViewModel wrapper | - | 2 | 1.0 |
| iOS Navigation | - | 2 | 1.0 |
| Integration Testing | 8 | 3 | 2.0 |
| **Total** | **~34** | **~24** | **~18.0** |

### Pros
- Shared business logic (~60% code reuse)
- Bug fixes apply to both platforms simultaneously
- Models and ViewModels are identical cross-platform
- Growing ecosystem with JetBrains backing
- Single source of truth for data layer

### Cons
- Requires refactoring existing ViewModel to extract to `commonMain`
- Learning curve for KMP expect/actual pattern
- Animations cannot be shared (platform-specific graphics APIs)
- WebSocket abstraction requires careful design

### WebSocket Compatibility

```
commonMain/
  expect class WebSocketClient {
      fun connect(host: String, port: Int)
      fun send(message: WsMessage)
      fun disconnect()
      val messages: Flow<WsMessage>
  }

androidMain/
  actual class WebSocketClient {
      // Uses java.net.Socket (existing implementation)
  }

iosMain/
  actual class WebSocketClient {
      // Uses NWConnection with custom RFC 6455 frames
  }
```

### Infrastructure Requirements
- macOS with Xcode 15+ (for iOS compilation)
- Android Studio with Kotlin Multiplatform plugin
- Gradle 8.x with KMP support
- CocoaPods or SPM for iOS dependencies

---

## OPTION 3: Flutter Migration

### Architecture

```
MiMoMobile-Flutter/
├── lib/
│   ├── main.dart
│   ├── app.dart
│   ├── models/
│   │   ├── ws_message.dart
│   │   ├── app_state.dart
│   │   └── ... (6 files)
│   ├── network/
│   │   ├── websocket_client.dart
│   │   ├── api_client.dart
│   │   ├── adb_http_client.dart
│   │   └── network_monitor.dart
│   ├── providers/                    # Riverpod
│   │   ├── connection_provider.dart
│   │   ├── chat_provider.dart
│   │   └── ... (5 files)
│   ├── screens/                      # 7 screens
│   │   ├── chat_screen.dart
│   │   ├── terminal_screen.dart
│   │   └── ... 
│   ├── widgets/
│   │   ├── perspective_grid.dart     # CustomPainter
│   │   ├── matplotlib_background.dart
│   │   └── ... (5 files)
│   └── utils/
│       ├── websocket_frame.dart
│       └── extensions.dart
├── android/
├── ios/
└── pubspec.yaml
```

### Code Distribution

| Layer | Shared Dart | Platform-Specific |
|-------|-------------|-------------------|
| All Layers | 95% | 5% (platform channels) |

**Shared Code: ~95% of total**

### Effort Estimate

| Component | Files | Weeks |
|-----------|-------|-------|
| Project Setup | 5 | 1.0 |
| Models | 6 | 0.5 |
| Network Layer | 5 | 2.5 |
| Providers (Riverpod) | 5 | 1.5 |
| Screens (7) | 7 | 3.5 |
| Widgets/Components | 5 | 2.0 |
| Animations (CustomPainter) | 2 | 2.0 |
| Navigation | 2 | 1.0 |
| Testing | 5 | 2.0 |
| **Total** | **~42** | **~16.0** |

### Pros
- Highest code reuse (~95%)
- Fastest development cycle
- Hot reload accelerates UI iteration
- Single codebase for both platforms
- Rich widget ecosystem

### Cons
- **REPLACES existing Android app** (user constraint violation)
- Abandons Jetpack Compose investment
- Non-native UI feel
- Animation performance may not match Metal/Canvas
- Larger app binary size

### WebSocket Compatibility

```dart
// Uses package:web_socket_channel
// Custom frame codec for RFC 6455 masking
class WebSocketFrame {
  static Uint8List encode(Uint8List data) { ... }
  static (Uint8List?, int) decode(Uint8List data, int offset) { ... }
}
```

### Infrastructure Requirements
- Flutter SDK 3.x
- Xcode 15+ (for iOS)
- Android SDK
- Dart/Flutter IDE plugins

---

## Recommendation

### Winner: OPTION 2 - Kotlin Multiplatform

**Rationale:**

1. **Respects User Constraint**: Keeps existing Android native app intact - just extracts shared logic

2. **Practical Code Sharing**: ~60% shared code without requiring app rewrite

3. **Incremental Adoption**: Can migrate file-by-file, keeping Android working throughout

4. **WebSocket Compatibility**: expect/actual pattern handles platform differences cleanly

5. **Risk Profile**: Lower than Flutter (no full rewrite) vs higher than Swift (actual sharing benefit)

6. **Long-term Maintenance**: Single source of truth for business logic reduces drift

### Migration Strategy

**Phase 1 (Weeks 1-3):** Create KMP shared module, extract Models
**Phase 2 (Weeks 4-6):** Extract ViewModel logic to commonMain
**Phase 3 (Weeks 7-9):** WebSocket expect/actual abstraction
**Phase 4 (Weeks 10-14):** iOS SwiftUI views implementation
**Phase 5 (Weeks 15-18):** Animations, testing, polish

### Files to Create/Modify

**New KMP Module:**
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/.../models/` (6 files)
- `shared/src/commonMain/kotlin/.../viewmodel/MiMoViewModel.kt`
- `shared/src/commonMain/kotlin/.../network/WebSocketClient.kt` (expect)
- `shared/src/androidMain/kotlin/.../network/WebSocketClient.android.kt` (actual)
- `shared/src/iosMain/kotlin/.../network/WebSocketClient.ios.kt` (actual)

**iOS App:**
- `ios/MiMoMobile/` (24 files total)
- 14 SwiftUI views
- 3 Metal animation views
- 2 ViewModel wrappers
- 5 utility files

**Android Modifications:**
- Update `build.gradle.kts` to depend on shared module
- Remove duplicated model classes (use shared)

---

## Infrastructure Requirements Summary

| Requirement | Option 1 (Swift) | Option 2 (KMP) | Option 3 (Flutter) |
|-------------|------------------|----------------|-------------------|
| macOS | Required | Required | Required |
| Xcode | Required | Required | Required |
| Android Studio | Not needed | Required | Required |
| Apple Dev Account | $99/year | $99/year | $99/year |
| Gradle/KMP Setup | No | Yes | No |
| Flutter SDK | No | No | Required |

---

## Risk Assessment

| Risk | Option 1 | Option 2 | Option 3 |
|------|----------|----------|----------|
| Code Drift | High | Low | None |
| Feature Parity | Manual | Auto (shared) | Auto (shared) |
| Performance | Native | Near-native | Good |
| Learning Curve | Low | Medium | Low |
| Refactor Effort | None | Medium | Complete |

---

*Plan generated for MiMo Mobile multiplatform integration*
