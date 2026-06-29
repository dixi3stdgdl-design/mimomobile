# MiMo Mobile

Android app for remotely controlling MiMo Code CLI from your phone.

## Features

- **Chat** — Send prompts to MiMo Code and receive responses in real-time
- **Build Visualizer** — Monitor project builds with animated pipeline visualization
- **Remote Desktop** — View and control your PC screen from your phone
- **Terminal** — Execute shell commands remotely
- **File Browser** — Navigate and edit project files
- **Device Manager** — Control connected Android devices via ADB
- **Settings** — Configure server connection and preferences

## Architecture

```
Android App (Kotlin/Compose)
    ↕ WebSocket (port 8765)
Python Server (asyncio.Protocol)
    ↕ Subprocess
MiMo Code CLI
```

## Setup

### Server (PC)
```bash
cd mimo-mobile-server
python3 server.py
```

### App (Android)
1. Install APK from `app/build/outputs/apk/debug/app-debug.apk`
2. Enter your PC's IP address in Settings
3. Connect with PIN: `MIMO2026`

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, MVVM, DataStore
- **Server**: Python 3.13, asyncio.Protocol, pure stdlib
- **Communication**: WebSocket (custom implementation, no OkHttp)

## License

MIT
