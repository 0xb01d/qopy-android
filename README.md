# Qopy Android — Peer-to-Peer LAN Clipboard Sync

> **Native Android application built with Kotlin, Jetpack Compose, Material 3, and Foreground Service for seamless, silent LAN clipboard synchronization.**

## Features
- **Zero Configuration & Serverless**: Auto-discovery over local Wi-Fi via Android `NsdManager` (mDNS `_qopy._tcp.local.`).
- **Modern Material 3 Expressive UI**: Sleek status cards, live clipboard previews with sensitive data masking, and peer management.
- **Cryptographic 8-Character Pairing**: Crockford Base32 verification code (`XXXX-XXXX`) protecting peer trust.
- **End-to-End Encrypted**: TLS 1.3 + NaCl Box (X25519 + XSalsa20-Poly1305) via Bouncy Castle.
- **Zero Persistence**: Clipboard contents held only in RAM; zero clipboard history saved to SQLite or disk.
- **Background Sync**: Dedicated foreground service with ongoing status notifications and quick action controls.

## Tech Stack
- **Language**: Kotlin 1.9+ (JVM 17)
- **UI Framework**: Jetpack Compose + Material 3
- **Local Storage**: Room SQLite (for paired device keys and TLS cert hashes only)
- **Protocol**: Protocol Buffers Lite (`protobuf-javalite`)
- **Crypto**: Bouncy Castle (`bcprov-jdk18on`)
- **Discovery**: Android `NsdManager`

## Building & Running

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (Min SDK: 26 / Android 8.0+)

### Building from Command Line
```bash
gradle assembleDebug
```

### Running Tests
```bash
gradle test
```

## Architecture
- `com.qopy.ui`: Jetpack Compose presentation layer, screens, dialogs, and components.
- `com.qopy.ui.theme`: Material 3 themes, typography, and color schemes.
- `com.qopy.service`: Foreground service (`ClipboardSyncService`) and notification channel.
- `com.qopy.network`: Network Service Discovery (`NsdDiscovery`), TLS socket helpers, and pairing state machine.
- `com.qopy.crypto`: Node identity, key generation, and NaCl box cryptography.
- `com.qopy.data`: Room database and DAOs for trusted peer identity persistence.
- `com.qopy.sensitive`: Regex-based sensitive data detection and masking.

## License
MIT License.
