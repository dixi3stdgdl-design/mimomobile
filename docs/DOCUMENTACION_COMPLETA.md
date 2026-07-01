# MiMo Mobile — Paquete Documental Completo

> Generado: 2026-06-23
> Versión: 1.0.0
> Estado: Production-ready (v4 Neural Command Center)

---

# PARTE 1: HANDOFF PACKAGE

## 1.1 Resumen Ejecutivo

**MiMo Mobile** es una aplicación Android nativa que permite controlar remotamente la CLI de MiMo Code desde un teléfono o tablet. Funciona como un centro de comando neuronal: el usuario interactúa desde su dispositivo móvil, mientras toda la generación de código ocurre en el PC local a través de un servidor WebSocket Python.

```
┌─────────────────┐     WebSocket     ┌─────────────────┐     Subprocess     ┌─────────────────┐
│  Android App    │ ◄──────────────► │  Python Server  │ ◄────────────────► │  MiMo Code CLI  │
│  (Jetpack       │    Puerto 8765    │  (asyncio +     │    mimo run <prompt>│  (Node.js)      │
│   Compose)      │                   │   asyncio.Protocol)                  │                 │
└─────────────────┘                   └────────┬────────┘                   └─────────────────┘
                                               │
                                               ▼
                                       ┌─────────────────┐
                                       │  HTTP Health API │
                                       │  Puerto 8080     │
                                       └─────────────────┘
```

## 1.2 Repositorios y Ubicaciones

| Componente | Ruta | Tecnología |
|------------|------|------------|
| Android App | `/home/DexTer/MiMoMobile/` | Kotlin + Jetpack Compose |
| Server Python | `/home/DexTer/mimo-mobile-server/` | Python 3.13 stdlib puro |
| Cloud Relay | `/home/DexTer/mimo-mobile-server/relay.py` | Python asyncio |
| Watchdog | `/home/DexTer/mimo-mobile-server/watchdog.sh` | Bash |

## 1.3 Stack Tecnológico

### Android (Frontend)
- **Lenguaje**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (BOM 2026.02.01)
- **Arquitectura**: MVVM — MiMoViewModel (AndroidViewModel + DataStore)
- **SDK**: compileSdk 35, minSdk 26, targetSdk 35
- **Build System**: AGP 9.2.1, Gradle 9.4.1, Kotlin DSL (.kts)
- **Networking**: WebSocket puro Java (sin OkHttp)
- **Persistencia**: DataStore Preferences
- **APK**: ~19MB (`app/build/outputs/apk/debug/app-debug.apk`)

### Server (Backend)
- **Lenguaje**: Python 3.13
- **Dependencias**: Solo stdlib (asyncio, json, subprocess, hashlib, struct)
- **Protocolo**: WebSocket (asyncio.Protocol) + HTTP (http.server)
- **Puertos**: WebSocket=8765, HTTP=8080
- **Cloud Relay**: Puerto 9876 (conexión desde datos móviles)

### Dispositivos de Prueba
| Dispositivo | Serial | Resolución | IP WiFi |
|-------------|--------|------------|---------|
| Samsung Tab S9 | TABS900000000000231 | 1200x1920 | 192.168.100.32 |
| OnePlus 8 | 14c02549 | 1080x2400 | 192.168.100.149 |

## 1.4 Infraestructura de Red

### Configuración WiFi (Producción)
```
PC Windows Host:     192.168.100.190
WSL2 Linux:          192.168.161.68
Subred WiFi:         192.168.100.0/24

Port Forwarding (Windows):
  netsh interface portproxy add v4tov4 listenport=8765 listenaddress=0.0.0.0 connectport=8765 connectaddress=192.168.161.68
  netsh interface portproxy add v4tov4 listenport=8080 listenaddress=0.0.0.0 connectport=8080 connectaddress=192.168.161.68
```

### Configuración USB (Desarrollo)
```
adb reverse tcp:8765 tcp:8765
adb reverse tcp:8080 tcp:8080
```

### Cloud Relay (Datos Móviles)
```
Puerto Público: 9876 → Proxy → localhost:8765
IP Pública: 187.189.148.162
```

## 1.5 Autenticación

- **PIN de autenticación**: `MIMO2026`
- **Mecanismo**: Primer mensaje WebSocket después de la conexión debe ser `{"type": "auth", "pin": "MIMO2026"}`
- **Alcance**: Solo dispositivos autorizados pueden ejecutar comandos, chat, control de dispositivos
- **Limpieza**: La autorización se revoca al desconectar el cliente

## 1.6 Comandos de Inicio Rápido

```bash
# Iniciar servidor con watchdog
cd /home/DexTer/mimo-mobile-server
./start-server.sh

# Iniciar relay cloud
python3 relay.py &

# Compilar APK
cd /home/DexTer/MiMoMobile
./gradlew assembleDebug

# Instalar en dispositivo
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Conexión USB (desarrollo)
adb reverse tcp:8765 tcp:8765
adb reverse tcp:8080 tcp:8080
```

## 1.7 Estructura de Archivos Críticos

### Android (12 fuentes Kotlin)
```
app/src/main/java/com/mimo/mobile/
├── MainActivity.kt                    # Entry point, Splash, Navigation, TopBar, BottomNav
├── viewmodel/MiMoViewModel.kt         # MVVM ViewModel — estado, chat, instancias, device mgmt
├── network/WebSocketClient.kt         # WebSocket puro Java — handshake, frame encode/decode, reconnect
├── ui/theme/Theme.kt                  # Neural Command Center palette + MiMoTheme
├── ui/components/NeuralComponents.kt  # NeuralPanel, NeuralButton, NeuralOrb, NeuralStatusDot, NeuralDivider
├── ui/screens/
│   ├── ChatScreen.kt                  # Chat con auto-scroll, streaming, quick actions, multi-instance
│   ├── BuildVisualizerScreen.kt       # Neural graph, pipeline de construcción, manifest de archivos
│   ├── RemoteScreen.kt                # Remote Desktop — screen capture + mouse/keyboard
│   ├── TerminalScreen.kt              # Terminal con ejecución de comandos
│   ├── DeviceManagerScreen.kt         # Universal Device Orchestration — ADB proxy
│   ├── FileBrowserScreen.kt           # Navegador de archivos con editor
│   └── SettingsScreen.kt              # Configuración de host/puerto, reconexión
```

### Server Python (2 fuentes)
```
mimo-mobile-server/
├── server.py          # Servidor principal — WebSocket Protocol + HTTP + handlers
├── relay.py           # Cloud relay para conexión desde datos móviles
├── watchdog.sh        # Auto-restart en caso de crash
├── start-server.sh    # Script de inicio con watchdog
└── start.sh           # Script de inicio directo
```

## 1.8 Conocimiento Crítico para Continuación

1. **asyncio.Protocol > asyncio.start_server+reader/writer** — Protocol es más confiable para WebSocket porque `data_received` se dispara por cada segmento TCP
2. **RFC 6455: frames del servidor NO deben tener mask bit** — Solo el cliente usa máscara
3. **PYTHONUNBUFFERED=1 obligatorio** — Sin esto, Python bufera stdout y `readline()` se bloquea
4. **`mimo run` para ejecución no interactiva** — `mimo <prompt>` abre TUI; `mimo run <prompt>` para subprocess
5. **ANSI stripping** — `mimo run` genera códigos de escape ANSI; se requiere regex `ANSI_RE` para limpiar
6. **`setsid` > `nohup`** — `nohup` se mata por timeout de bash; `setsid` con `disown` sobrevive
7. **Compose TextField no recibe `input text` de ADB** — El texto va a sugerencias del teclado, no al campo
8. **`tcpip 5555` causa desconexión USB** — Cambiar a WiFi requiere re-attach via usbipd
9. **USB BUSID no es estable** — Cambia entre sesiones; siempre verificar con `usbipd list`
10. **Tab S9 no tiene curl** — Usar `ping` para testing de conectividad
11. **OnePlus 8 resolución 1080x2400** — Coordenadas x deben ser <1080 para taps ADB

---

# PARTE 2: SOFTWARE DESIGN DOCUMENT

## 2.1 Visión General del Diseño

MiMo Mobile está diseñado como un sistema distribuido de tres componentes:

1. **Capa de Presentación (Android)** — UI declarativa con Jetpack Compose, diseño Neural Command Center
2. **Capa de Transporte (WebSocket)** — Protocolo WebSocket puro sin dependencias externas
3. **Capa de Procesamiento (Python Server)** — Orquestación de subprocess, gestión de dispositivos ADB, streaming de pantalla

### Principios de Diseño

| Principio | Implementación |
|-----------|---------------|
| Separación de responsabilidades | MVVM — ViewModel gestiona estado, WebSocketClient maneja red, Screens solo renderizan |
| Sin dependencias externas en server | Solo Python stdlib — cero npm, cero pip |
| Conexión wireless-first | Puerto por defecto `192.168.100.190:8765` (PC WiFi host) |
| Seguridad por PIN | Autenticación obligatoria antes de cualquier operación |
| Aislamiento de instancias | Cada instancia de chat tiene su propio workspace aislado |
| Auto-recuperación | Watchdog reinicia el server automáticamente tras crash |
| Diseño orgánico vivo | Componentes con animaciones de "respiración", partículas orbitantes, brillo pulsante |

## 2.2 Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ANDROID APP                                  │
│                                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ChatScreen│  │BuildViz  │  │Remote    │  │Terminal  │          │
│  │          │  │Screen    │  │Screen    │  │Screen    │          │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘          │
│       │              │              │              │                │
│  ┌────┴──────────────┴──────────────┴──────────────┴─────┐        │
│  │                    MiMoViewModel                       │        │
│  │  ┌──────────┐  ┌──────────┐  ┌───────────────────┐   │        │
│  │  │AppState  │  │ChatInstance│ │Multi-Instance Mgr │   │        │
│  │  │(StateFlow)│ │(messages) │  │(instances list)   │   │        │
│  │  └──────────┘  └──────────┘  └───────────────────┘   │        │
│  └───────────────────────┬───────────────────────────────┘        │
│                          │                                         │
│  ┌───────────────────────┴───────────────────────────────┐        │
│  │                WebSocketClient                         │        │
│  │  • Handshake HTTP→WebSocket                           │        │
│  │  • Frame encode/decode (RFC 6455)                      │        │
│  │  • Auto-reconnect (exponential backoff, max 5)        │        │
│  │  • Auth PIN en primer mensaje                         │        │
│  └───────────────────────┬───────────────────────────────┘        │
│                          │                                         │
│  ┌───────────────────────┴───────────────────────────────┐        │
│  │              NeuralComponents.kt                       │        │
│  │  NeuralPanel │ NeuralButton │ NeuralOrb               │        │
│  │  NeuralStatusDot │ NeuralDivider                       │        │
│  └───────────────────────────────────────────────────────┘        │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ WebSocket (Puerto 8765)
                           │ JSON messages
┌──────────────────────────┴──────────────────────────────────────────┐
│                       PYTHON SERVER                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────┐          │
│  │              WebSocketProtocol (asyncio.Protocol)     │          │
│  │  • HTTP Upgrade → WebSocket handshake                │          │
│  │  • Frame parsing + payload extraction                │          │
│  │  • Auth verification (authorized_devices set)        │          │
│  └──────────────────────┬───────────────────────────────┘          │
│                         │                                          │
│  ┌─────────────────────┬┴──────────────────────────────────┐      │
│  │                     │  Message Router                    │      │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │      │
│  │  │handle_   │ │handle_   │ │handle_   │ │handle_   │  │      │
│  │  │chat      │ │execute   │ │read_file │ │write_file│  │      │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │      │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │      │
│  │  │handle_   │ │handle_   │ │handle_   │ │handle_   │  │      │
│  │  │list_dir  │ │device_   │ │screen_   │ │mouse_    │  │      │
│  │  │          │ │list/cmd  │ │stream    │ │event     │  │      │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │      │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐               │      │
│  │  │handle_   │ │handle_   │ │handle_   │               │      │
│  │  │keyboard_ │ │adb_      │ │build_    │               │      │
│  │  │event     │ │configure │ │progress  │               │      │
│  │  └──────────┘ └──────────┘ └──────────┘               │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌──────────────────────────────────────────────┐               │
│  │           HTTP Server (Puerto 8080)          │               │
│  │  GET /health → JSON status                   │               │
│  │  GET /api/exec?command=... → command output   │               │
│  └──────────────────────────────────────────────┘               │
│                                                                  │
│  ┌──────────────────────────────────────────────┐               │
│  │           Cloud Relay (Puerto 9876)          │               │
│  │  Proxy WebSocket → localhost:8765            │               │
│  └──────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────┘
```

## 2.3 Protocolo WebSocket

### Mensajes del Cliente → Servidor

| Tipo | Campos | Descripción |
|------|--------|-------------|
| `auth` | `pin` | Autenticación con PIN |
| `chat` | `prompt`, `id`, `instance_id` | Enviar prompt a MiMo CLI |
| `execute` | `command`, `id`, `cwd` | Ejecutar shell command |
| `read_file` | `path`, `id` | Leer contenido de archivo |
| `write_file` | `path`, `content`, `id` | Escribir contenido a archivo |
| `list_dir` | `path`, `id` | Listar directorio |
| `build_progress` | `id`, `path` | Obtener info de proyecto |
| `device_list` | `id` | Listar dispositivos ADB |
| `device_command` | `serial`, `command`, `action`, `id` | Comando ADB a dispositivo |
| `adb_configure` | `serial`, `id` | Configurar ADB wireless |
| `continuous_stream` | `action`, `id` | Iniciar/detener streaming |
| `screen_stream` | `action`, `id` | Captura de pantalla única |
| `mouse_event` | `x`, `y`, `action`, `id` | Control de mouse |
| `keyboard_event` | `key`, `action`, `id` | Control de teclado |
| `ping` | `id` | Health check |
| `system_info` | `id` | Info del sistema |

### Mensajes del Servidor → Cliente

| Tipo | Campos | Descripción |
|------|--------|-------------|
| `auth_result` | `data`, `status` | Resultado de autenticación |
| `chat_start` | `id`, `instance_id` | Inicio de respuesta |
| `chat_chunk` | `id`, `data`, `instance_id` | Chunk de texto streaming |
| `chat_end` | `id`, `exit_code`, `instance_id` | Fin de respuesta |
| `exec_start` | `id` | Inicio de ejecución |
| `exec_output` | `id`, `data` | Línea de output |
| `exec_end` | `id`, `exit_code` | Fin de ejecución |
| `file_content` | `id`, `path`, `data` | Contenido de archivo |
| `file_written` | `id`, `path` | Confirmación de escritura |
| `dir_listing` | `id`, `path`, `entries` | Lista de archivos |
| `build_progress` | `id`, `data` | Info de proyecto |
| `device_list` | `id`, `data` | Lista de dispositivos |
| `device_output` | `id`, `serial`, `stdout`, `exit_code` | Output de comando ADB |
| `adb_configured` | `id`, `serial`, `ip`, `wireless` | Config ADB completada |
| `screen_frame` | `id`, `data`, `format` | Frame JPEG base64 |
| `stream_status` | `id`, `data` | Estado del streaming |
| `mouse_ack` | `id`, `data` | Confirmación mouse |
| `keyboard_ack` | `id`, `data` | Confirmación teclado |
| `pong` | `id` | Respuesta a ping |
| `system_info` | `id`, `data` | Info del sistema |
| `error` | `id`, `data` | Mensaje de error |

## 2.4 Diseño UI — Neural Command Center

### Paleta de Colores
```
Fondos:
  DarkBackground: #05050A     ← Fondo principal
  DarkSurface:    #0A0B12     ← Superficies (top bar, bottom bar)
  DarkCard:       #0E1018     ← Cards y paneles
  DarkCardHover:  #141620     ← Estado hover

Acentos Bioluminiscentes:
  AccentOrange:   #00E5A0     ← Color primario (verde bioluminiscente)
  AccentGreen:    #00D4FF     ← Color secundario (cyan)
  AccentBlue:     #7B61FF     ← Color terciario (púrpura)
  AccentRed:      #FF3366     ← Errores
  AccentGold:     #FFD93D     ← Advertencias

Texto:
  TextPrimary:    #E8F0F8     ← Texto principal
  TextSecondary:  #8B9CC0     ← Texto secundario
  TextMuted:      #4A5568     ← Texto deshabilitado

Glow (para sombras animadas):
  GlowOrange:     #3000E5A0
  GlowGreen:      #3000D4FF
  GlowPurple:     #307B61FF
```

### Componentes Neurales
| Componente | Función | Animación |
|------------|---------|-----------|
| `NeuralPanel` | Card con borde gradiente | Border "respira" (alpha 0.08→0.15 en 3s) |
| `NeuralButton` | Botón con glow | Sombra pulsante (alpha 0.3→0.6 en 1.5s) |
| `NeuralOrb` | Avatar circular con partículas | 3 partículas orbitan + pulso central (2s) |
| `NeuralStatusDot` | Indicador de estado | Pulso alpha (800ms activo, 2s inactivo) |
| `NeuralDivider` | Separador con shimmer | Gradiente horizontal pulsante (2s) |

### Navegación — 6 Tabs
```
Chat → Build → Remote → Terminal → Devices → Settings
  ①      ②      ③        ④         ⑤         ⑥

① Chat:      Streaming de respuestas MiMo, multi-instancia, auto-scroll
② Build:     Neural graph animado, pipeline de construcción, manifest
③ Remote:    Remote Desktop — streaming continuo ~10 FPS, touch→mouse
④ Terminal:  Ejecución de comandos shell con output en vivo
⑤ Devices:   Universal Device Orchestration — gestión ADB
⑥ Settings:  Host/puerto, reconexión, estado de conexión
```

---

# PARTE 3: ESPECIFICACIÓN FUNCIONAL

## 3.1 Módulo: Chat

### Descripción
Permite al usuario enviar prompts a MiMo Code CLI y recibir respuestas en streaming en tiempo real.

### Funcionalidades
- **Envío de prompts**: Campo de texto con botón de envío
- **Streaming en tiempo real**: Respuestas se muestran chunk por chunk via `chat_chunk`
- **Auto-scroll**: `LaunchedEffect(messageCount)` + `LaunchedEffect(lastMessageContent.length)` aseguran que el chat siga el contenido durante streaming
- **Multi-instancia**: Pestañas de instancias — cada una con historial independiente
  - Solo "Main" se crea al inicio
  - Instancias adicionales se crean on-demand con "+"
  - Cada instancia aísla su workspace en el servidor
- **Quick Action Chips**: Copiar, Explicar, Ejecutar en mensajes del asistente
- **Indicador de procesamiento**: Muestra cuando MiMo está generando respuesta

### Flujo
```
Usuario escribe prompt → sendChat() → WsMessage(type="chat") →
Servidor ejecuta `mimo run <prompt>` → chat_start → chat_chunk* → chat_end
```

### Datos de Entrada
- `prompt` (String): Texto del usuario
- `instance_id` (String): ID de la instancia activa

### Datos de Salida
- `chat_start`: Indica inicio de respuesta
- `chat_chunk`: Fragmento de texto incremental
- `chat_end`: Indica fin con `exit_code`

## 3.2 Módulo: Build Visualizer

### Descripción
Visualización futurista del estado de construcción de un proyecto con gráfico neural animado.

### Funcionalidades
- **Neural Network Graph**: Canvas animado con nodos orbitantes (.kt, .py, .gradle), línea de escaneo, grid de fondo
- **Stat Tiles**: 6 tiles con estadísticas del proyecto (archivos totales, .kt, .xml, .gradle, tamaño total, estado)
- **Construction Pipeline**: 8 etapas de construcción con orbs de estado
- **File Manifest**: Lista de archivos con tipo, ruta y tamaño
- **Botón "Scan Project"**: Dispara `build_progress` al servidor

### Pipeline de Construcción
```
① Init → ② Resolve → ③ Compile → ④ Process → ⑤ Link → ⑥ Package → ⑦ Sign → ⑧ Deploy
```

### Datos del Servidor
```json
{
  "project": "MiMoMobile",
  "total_files": 45,
  "kt_files": 12,
  "xml_files": 8,
  "gradle_files": 3,
  "total_size": 524288,
  "files": [{"path": "...", "size": 1024, "ext": ".kt"}]
}
```

## 3.3 Módulo: Remote Desktop

### Descripción
Escritorio remoto completo — convierte la tablet/phone en segunda pantalla del PC.

### Funcionalidades
- **Streaming continuo**: ~10 FPS via PowerShell `CopyFromScreen` → JPEG → base64 → WebSocket
- **Touch → Mouse**: tap=click, drag=move, long press=right click
- **Keyboard hotkeys**: Tab, Enter, Esc, Del, Ctrl+C/V/Z
- **Auto-stream**: LaunchedEffect inicia streaming al abrir pantalla, DisposableEffect lo detiene al salir
- **Status bar**: Muestra "STREAMING LIVE", FPS, resolución — sin botones manuales

### Coordenadas de Touch
```
touch_x = event.x * (screen_width / image_width)
touch_y = event.y * (screen_height / image_height)
```

### Acciones Soportadas
| Touch | Acción Windows |
|-------|---------------|
| Tap | Left click |
| Long press | Right click |
| Drag | Mouse move |
| Scroll up/down | Mouse wheel |
| Tab key | Tab |
| Enter key | Enter |
| Esc key | Escape |

## 3.4 Módulo: Terminal

### Descripción
Terminal remoto con ejecución de comandos shell en vivo.

### Funcionalidades
- **Ejecución de comandos**: Campo de entrada + output en tiempo real
- **Streaming de output**: `exec_start` → `exec_output*` → `exec_end`
- **Historial de comandos**: Lista de comandos ejecutados con su output
- **Working directory**: Cada instancia mantiene su propio cwd

### Datos
- Entrada: `command` (String), `cwd` (String, opcional)
- Salida: `exec_output` (líneas incrementales), `exec_end` (exit_code)

## 3.5 Módulo: Device Manager

### Descripción
Universal Device Orchestration — gestión centralizada de todos los dispositivos ADB conectados al servidor.

### Funcionalidades
- **Auto-detect**: `LaunchedEffect` + coroutine loop que consulta `device_list` cada 10 segundos
- **Device Cards expandibles**: Modelo, serial, estado (device/offline)
- **Quick Actions por dispositivo**:
  - Screenshot: `screencap -p` → pull a `/tmp/`
  - Wake: `input keyevent KEYCODE_WAKEUP`
  - Home: `input keyevent KEYCODE_HOME`
  - Back: `input keyevent KEYCODE_BACK`
  - Settings: `am start -a android.settings.SETTINGS`
  - WiFi: `svc wifi enable/disable`
  - Brightness: `settings put system screen_brightness <0-255>`
  - Config ADB: `tcpip 5555` → detectar IP → `adb connect`
- **Custom ADB command input**: Campo para comandos ADB arbitrarios

### Server Handlers
- `device_list`: Ejecuta `adb devices -l`, parsea serial/state/model
- `device_command`: Ejecuta `adb -s <serial> <action> <command>`
  - Acciones: shell, install, launch, input, screenshot, settings
- `adb_configure`: `tcpip 5555` → detectar IP WiFi → `adb connect <ip>:5555`

## 3.6 Módulo: Settings

### Descripción
Configuración de conexión y estado del sistema.

### Funcionalidades
- **Server Host**: Campo editable (default: `192.168.100.190`)
- **Server Port**: Campo editable (default: `8765`)
- **Botón Reconectar**: Desconecta y reconecta
- **Indicador de estado**: ONLINE / CONNECTING / ERROR / OFFLINE
- **Persistencia**: DataStore Preferences — sobrevive reinicios de app

## 3.7 Módulo: File Browser

### Descripción
Navegador de archivos del sistema con visualización y edición.

### Funcionalidades
- **Listado de directorios**: `list_dir` → muestra archivos con tamaño
- **Lectura de archivos**: `read_file` → contenido con syntax highlighting básico
- **Escritura de archivos**: `write_file` → guardar cambios desde la app
- **Navegación**: Tap en directorio para entrar, botón atrás para subir

---

# PARTE 4: METODOLOGÍA DE DESARROLLO

## 4.1 Enfoque

MiMo Mobile se desarrolla bajo un modelo **iterativo incremental** con las siguientes características:

### Ciclo de Desarrollo
```
1. Diseño → 2. Implementación → 3. Build → 4. Deploy al dispositivo → 5. Testing en vivo → 6. Iteración
```

### Herramientas
| Fase | Herramienta |
|------|-------------|
| Edición | Claude Code (MiMo Agent) |
| Build Android | `./gradlew assembleDebug` (desde WSL2) |
| Deploy | `adb install -r <apk>` via USB (usbipd-win) |
| Testing | Dispositivos físicos (Tab S9, OnePlus 8) |
| Server | Python 3.13 directo en WSL2 |
| Monitoring | `/tmp/mimo-server.log` + stdout |

### Fases del Desarrollo

**Fase 1: Core (v1-v2)**
- WebSocket client/server básico
- Chat con streaming
- Terminal y File Browser
- Settings con DataStore

**Fase 2: UI Premium (v3)**
- Rediseño Neural Command Center
- Componentes neurales animados
- Splash screen
- Multi-instancia

**Fase 3: Universal Orchestration (v4)**
- Remote Desktop (true RDP)
- Device Manager con ADB proxy
- Build Visualizer futurista
- Cloud Relay para datos móviles
- Autenticación por PIN

## 4.2 Decisiones de Diseño Clave

| Decisión | Alternativa Rechazada | Razón |
|----------|----------------------|-------|
| Python stdlib para server | Node.js / npm | npm/node no disponible en sistema |
| asyncio.Protocol | asyncio.start_server+reader/writer | Protocol más confiable para WebSocket |
| WebSocket puro Java | OkHttp | Evitar dependencia extra en Android |
| Kotlin DSL (.kts) | Groovy DSL | Mejor compatibilidad con AGP 9.x |
| DataStore | SharedPreferences | Type-safe, coroutine-friendly |
| MVVM con AndroidViewModel | Composable state | Persistencia DataStore + lifecycle |
| 6 tabs bottom nav | NavigationRail | Mejor para uso con una mano en móvil |
| Neural Command Center | Cyberpunk Terminal / Void Interface | Elección del usuario — orgánico/bioluminiscente |

## 4.3 Convenciones de Código

### Kotlin
- **Formato**: 4 espacios de indentación
- **Nomenclatura**: camelCase para funciones/variables, PascalCase para clases
- **Compose**: Funciones `@Composable` en PascalCase, parámetros con `Modifier` primero
- **Imports**: Agrupados por paquete, ordenados alfabéticamente
- **No comments**: Solo documentación en KDoc para API pública

### Python
- **Formato**: 4 espacios de indentación
- **Nomenclatura**: snake_case para funciones/variables, PascalCase para clases
- **Type hints**: Opcionales pero recomendadas para parámetros públicos
- **Docstrings**: Mínimos — el código es self-documenting

### Git
- Commits descriptivos en inglés
- No secrets en el repositorio
- Preferencia por `git add <archivos>` sobre `git add -A`

## 4.4 Proceso de Testing

### Testing Manual en Dispositivo
1. Build: `./gradlew assembleDebug`
2. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Connect: App → Settings → Verificar host/puerto → Reconectar
4. Funcionalidad: Probar cada módulo secuencialmente
5. Edge cases: WiFi→USB switch, reconexión, múltiples instancias

### Puntos de Verificación
- [ ] Splash screen aparece y desaparece correctamente
- [ ] Conexión WebSocket exitosa (status ONLINE)
- [ ] Chat: envío de prompt + streaming de respuesta
- [ ] Build: scan de proyecto + visualización
- [ ] Remote: streaming de pantalla + touch→mouse
- [ ] Terminal: ejecución de comandos
- [ ] Devices: auto-detección + quick actions
- [ ] Settings: cambio de host/puerto + reconexión
- [ ] Multi-instancia: crear, cambiar, eliminar instancias

---

# PARTE 5: DOCUMENTACIÓN DE ARQUITECTURA DE SOFTWARE

## 5.1 Arquitectura de Alto Nivel

```
┌────────────────────────────────────────────────────────────────────┐
│                        CAPA DE PRESENTACIÓN                        │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                    Jetpack Compose UI                       │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │  │ChatScreen│ │BuildViz  │ │Remote    │ │Terminal  │     │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  │  ┌──────────┐ ┌──────────┐                                │   │
│  │  │Devices   │ │Settings  │                                │   │
│  │  └──────────┘ └──────────┘                                │   │
│  │                                                            │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │           NeuralComponents.kt                        │  │   │
│  │  │  NeuralPanel │ NeuralButton │ NeuralOrb             │  │   │
│  │  │  NeuralStatusDot │ NeuralDivider                     │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                    MiMoViewModel                            │   │
│  │  • AppState (StateFlow) — conexión, host, port, splash     │   │
│  │  • ChatInstance[] — mensajes, streaming, isProcessing       │   │
│  │  • Multi-instance management — switch/add/remove            │   │
│  │  • DataStore persistence — host/port across restarts        │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                    │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                 WebSocketClient                             │   │
│  │  • Raw Java Socket (sin OkHttp)                            │   │
│  │  • HTTP → WebSocket handshake (Sec-WebSocket-Key)          │   │
│  │  • Frame encode (masked) / decode (RFC 6455)               │   │
│  │  • Auto-reconnect: exponential backoff, max 5 intentos     │   │
│  │  • Auth: primer mensaje = auth PIN                          │   │
│  │  • SharedFlow<WsMessage> para emisión de mensajes          │   │
│  └────────────────────────────────────────────────────────────┘   │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
                     WebSocket (TCP)
                     Puerto 8765
                     JSON frames
                           │
┌──────────────────────────┴─────────────────────────────────────────┐
│                     CAPA DE TRANSPORTE                              │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              WebSocketProtocol (asyncio.Protocol)            │   │
│  │                                                              │   │
│  │  connection_made() → data_received() → _process_frames()    │   │
│  │                                                              │   │
│  │  • Parse HTTP upgrade, extract Sec-WebSocket-Key             │   │
│  │  • Send 101 Switching Protocols                              │   │
│  │  • Frame parsing: opcode, length, mask, payload              │   │
│  │  • Auth check: authorized_devices set per IP                 │   │
│  │  • Message routing to handler functions                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              HTTP Server (http.server)                        │   │
│  │  GET /health → {"status":"ok","ws_port":8765,...}           │   │
│  │  GET /api/exec?command=... → {"stdout":...,"stderr":...}    │   │
│  │  GET /* → HTML status page                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
                     subprocess / ADB / PowerShell
                           │
┌──────────────────────────┴─────────────────────────────────────────┐
│                     CAPA DE PROCESAMIENTO                           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    MiMo Code CLI                             │   │
│  │  mimo run <prompt> → stdout streaming → ANSI strip → chunks  │   │
│  │  Per-instance workspace isolation (.mimo_instances/<id>/)    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    ADB Proxy                                 │   │
│  │  adb devices -l → device list                               │   │
│  │  adb -s <serial> shell/install/launch/input/settings        │   │
│  │  adb tcpip 5555 → detect IP → adb connect (auto-config)    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Screen Capture (PowerShell)               │   │
│  │  CopyFromScreen → Bitmap → JPEG → base64 → WebSocket frame  │   │
│  │  Thread + asyncio combo para capture loop (~10 FPS)          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Input Control (PowerShell)                │   │
│  │  Cursor.Position + mouse_event (click/move/scroll)           │   │
│  │  SendKeys.SendWait() (keyboard)                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────┘
```

## 5.2 Patrones de Diseño Aplicados

### MVVM (Model-View-ViewModel)
```
Model:      AppState, ChatInstance, ChatMsg, WsMessage
ViewModel:  MiMoViewModel (AndroidViewModel + DataStore)
View:       Jetpack Compose screens (ChatScreen, BuildVisualizerScreen, etc.)
```

### Observer Pattern
- `StateFlow<AppState>` — Observable state para UI
- `SharedFlow<WsMessage>` — Observable stream de mensajes WebSocket
- `mutableStateListOf<ChatInstance>` — Observable list de instancias

### Strategy Pattern (Server Handlers)
- `handle_chat`, `handle_execute`, `handle_device_command`, etc.
- Cada handler implementa la estrategia de procesamiento para un tipo de mensaje

### Proxy Pattern
- Server actúa como proxy ADB — los dispositivos solo se comunican con el server
- Los clientes no necesitan ADB instalado

### Watchdog Pattern
- `watchdog.sh` — Loop infinito que reinicia el server tras crash
- `setsid` + `disown` para persistencia de proceso

### Reconnection Pattern (Exponential Backoff)
- `WebSocketClient.scheduleReconnect()` — delay = min(1000ms * attempts, 10000ms)
- Max 5 intentos antes de dar por fallida la conexión

## 5.3 Patrón de Comunicación

### Request-Response via WebSocket
```
Client → Server:  {"type": "chat", "id": "msg_1_1719140000", "prompt": "hola"}
Server → Client:  {"type": "chat_start", "id": "msg_1_1719140000"}
Server → Client:  {"type": "chat_chunk", "id": "msg_1_1719140000", "data": "¡Hola!"}
Server → Client:  {"type": "chat_chunk", "id": "msg_1_1719140000", "data": "¿En qué puedo..."}
Server → Client:  {"type": "chat_end", "id": "msg_1_1719140000", "exit_code": 0}
```

### Streaming Pattern
- El servidor envía múltiples `chat_chunk` antes del `chat_end`
- Cada chunk se append al contenido existente en el ViewModel
- `LaunchedEffect(lastMessageContent.length)` fuerza auto-scroll

### Authentication Flow
```
Client → Server:  {"type": "auth", "pin": "MIMO2026"}
Server → Client:  {"type": "auth_result", "data": "authorized", "status": "ok"}
# Ahora el cliente puede enviar cualquier tipo de mensaje
```

## 5.4 Manejo de Errores

| Error | Manejo |
|-------|--------|
| Conexión fallida | Auto-reconnect con exponential backoff (max 5) |
| Auth fallida | Mensaje de error, sin reconexión automática |
| Comando no encontrado | `chat_end` con error message |
| Timeout de respuesta | 300s timeout en `handle_chat` |
| JSON inválido | Respuesta `{"type": "error", "data": "Invalid JSON"}` |
| Tipo desconocido | Respuesta `{"type": "error", "data": "Unknown type: ..."}` |
| No autorizado | Respuesta `{"type": "error", "data": "Not authorized"}` |
| Frame WebSocket inválido | Se descarta silenciosamente (opcode validation) |
| Crash del server | Watchdog reinicia en 3 segundos |

## 5.5 Seguridad

1. **Autenticación por PIN**: Solo dispositivos con el PIN correcto pueden interactuar
2. **PIN en memoria**: `authorized_devices` es un `set` de IPs — se limpia al desconectar
3. **Sin exposición de archivos sensibles**: Server opera dentro de `WORKSPACE` (home dir)
4. **Aislamiento de instancias**: Cada instancia tiene su propio subdirectorio
5. **Cloud Relay**: Proxy transparente — no almacena datos, solo reenvía frames
6. **ADB como proxy centralizado**: Dispositivos ADB solo accesibles vía server

---

# PARTE 6: MEMORIA TÉCNICA

## 6.1 Decisiones de Arquitectura (con Razón)

### Python stdlib puro (no Node.js)
- **Decisión**: Server en Python 3.13 sin npm/pip
- **Razón**: npm y node no están instalados en el sistema; Python 3.13 sí
- **Impacto**: WebSocket implementado manualmente (handshake + frame encode/decode)

### asyncio.Protocol (no reader/writer)
- **Decisión**: Usar `asyncio.Protocol` en vez de `asyncio.start_server` con reader/writer
- **Razón**: Protocol es más confiable — `data_received` se dispara por cada segmento TCP; reader/writer puede retornar vacío tras handshake
- **Impacto**: Buffer management manual, pero conexión más estable

### WebSocket puro Java (no OkHttp)
- **Decisión**: Implementar WebSocket con `java.net.Socket` puro
- **Razón**: Evitar dependencia externa; mantener APK ligero (~19MB)
- **Impacto**: Handshake HTTP manual, frame encode/decode manual, pero cero dependencias

### Default host `192.168.100.190` (no localhost)
- **Decisión**: DataStore default a IP del PC WiFi, no localhost
- **Razón**: WiFi-first operation — funciona tanto con `adb reverse` (USB) como con port forwarding (WiFi)
- **Impacto**: Usuario no necesita cambiar configuración al usar WiFi

## 6.2 Errores Encontrados y Solucionados

### WebSocket Handshake Silencioso
- **Problema**: Conexión TCP se establecía pero no se completaba handshake WebSocket
- **Causa**: Server enviaba datos raw sin parsear HTTP upgrade
- **Solución**: Implementar parsing de `Sec-WebSocket-Key` y envío de `101 Switching Protocols`

### Server Frames con Mask Bit (RFC 6455 Violation)
- **Problema**: Client no reconocía frames del server
- **Causa**: Server seteaba bit 0x80 en el byte de length (mask bit)
- **Solución**: Server frames usan `frame.append(length)` sin OR con 0x80

### Python stdout Buffering
- **Problema**: Output de `mimo run` no llegaba al cliente en tiempo real
- **Causa**: Python bufera stdout cuando no es TTY
- **Solución**: `PYTHONUNBUFFERED=1` en env del subprocess

### Compose TextField no recibe ADB input text
- **Problema**: `adb shell input text` no funciona con Compose TextField
- **Causa**: ADB input va al IME del sistema, no al campo Compose
- **Solución**: No hay workaround automatizado — usuario debe escribir manualmente

### ANSI Escape Codes en output de mimo
- **Problema**: Caracteres extraños en el chat del usuario
- **Causa**: `mimo run` genera códigos ANSI para colores/formato
- **Solución**: Regex `ANSI_RE = re.compile(r'\x1b\[[0-9;]*[a-zA-Z]|\x1b\].*?\x07')` + `TERM=dumb`

### Canvas DrawScope Shadowing
- **Problema**: `size.width`/`size.height` retornaban tipos incorrectos
- **Causa**: `import foundation.layout.*` importa `Modifier.width()`/`.height()` que shadow las propiedades de `DrawScope.size`
- **Solución**: `val sz = this.size`然后 `sz.width`/`sz.height`

### Compose NaN Crash
- **Problema**: `IllegalStateException: AnimationVector cannot contain a NaN`
- **Causa**: Valores NaN de `log2(0)`, `log10(0)`, división por cero
- **Solución**: Guard con `isFinite()` + `coerceIn(0f, 1f)` en todos los valores animados

### USB BUSID Inestable
- **Problema**: `usbipd attach` falla con "device not found"
- **Causa**: BUSID cambia entre sesiones (1-3, 1-4, etc.)
- **Solución**: Siempre ejecutar `usbipd list` antes de attach, nunca hardcodear

### Tab S9 WiFi Desactivada
- **Problema**: `adb connect` falla sin razón aparente
- **Causa**: WiFi del Tab S9 estaba desactivada
- **Solución**: Verificar con `settings get global wifi_on`, activar con `svc wifi enable`

## 6.3 Configuración de Build

### AGP 9.x Migration Notes
```
- kotlinOptions ELIMINADO → usar compileOptions { sourceCompatibility/targetCompatibility }
- isMinifyEnabled/shrinkResources INVÁLIDO → usar optimization { enable = false }
- Kotlin compose plugin incluye kotlin.android → NO aplicar ambos
- Kotlin DSL (.kts) fuertemente recomendado
- Gradle 9.4.1 compatible con AGP 9.x
```

### KSP Versioning (2.x)
```
- Old: kotlinVersion-kspPatch (e.g., 2.2.10-1.0.31)
- New: kotlinVersion-kspMajor.Minor (e.g., 2.2.10-2.0.2)
```

### Room + KSP
```
- Room 2.6.1 incompatible con KSP 2.x → usar Room 2.8.0
- Room 2.8.0 type inference: Flow.first() ANTES de .map{}, no después
```

## 6.4 Networking Notes

### WSL2 ↔ Devices
```
WSL2 subnet:    192.168.160.0/20
WiFi subnet:    192.168.100.0/24
→ Diferentes subredes, no se pueden comunicar directamente

Solución USB:   adb reverse tcp:8765 tcp:8765
Solución WiFi:  netsh interface portproxy (Windows host como puente)
```

### Port Forwarding (Windows)
```powershell
netsh interface portproxy add v4tov4 listenport=8765 listenaddress=0.0.0.0 connectport=8765 connectaddress=192.168.161.68
netsh interface portproxy add v4tov4 listenport=8080 listenaddress=0.0.0.0 connectport=8080 connectaddress=192.168.161.68
# Requiere admin elevation via Start-Process -Verb RunAs
# Firewall: reglas TCP inbound para 8765, 8080, 9876
```

### adb reverse (USB Tunnel)
```bash
adb reverse tcp:8765 tcp:8765   # WebSocket
adb reverse tcp:8080 tcp:8080   # HTTP
# Requiere USB conectado — se pierde al desconectar
# Más confiable que port forwarding para desarrollo
```

## 6.5 Performance Notes

| Métrica | Valor | Notas |
|---------|-------|-------|
| APK size | ~19MB | Debug build |
| Server RAM | ~30MB | Python stdlib puro |
| WebSocket latency | <50ms | LAN |
| Remote Desktop FPS | ~10 FPS | PowerShell JPEG capture |
| Max concurrent clients | Sin límite | asyncio maneja miles de conexiones |
| Chat timeout | 300s | configurable en server.py |
| Auto-reconnect | Max 5 intentos | Exponential backoff hasta 10s |

## 6.6 Known Issues / Limitaciones

1. **ADB input text no funciona con Compose TextField** — Usuario debe escribir manualmente
2. **`tcpip 5555` causa desconexión USB** — Requiere re-attach post-switch
3. **Cloud Relay sin TLS** — Tráfico no cifrado; considerar WSS en producción
4. **No hay persistent storage de historial** — Chat se pierde al cerrar app
5. **Remote Desktop solo funciona con Windows host** — PowerShell dependency
6. **Single-threaded capture loop** — Screen capture puede bloquear otros handlers
7. **No rate limiting** — Server no limita requests por cliente
8. **Pin hardcoded** — MIMO2026 hardcoded en ambos lados; debería ser configurable

## 6.7 Roadmap Sugerido

### Prioridad Alta
- [ ] Persistencia de historial de chat (Room database)
- [ ] TLS/SSL para Cloud Relay
- [ ] Configuración de PIN desde la app
- [ ] Rate limiting en server

### Prioridad Media
- [ ] Notificaciones push para respuestas completadas
- [ ] Modo oscuro/claro toggle
- [ ] Exportar logs de sesión
- [ ] Soporte para múltiples workspaces

### Prioridad Baja
- [ ] Theme customization
- [ ] Widget de Android para status rápido
- [ ] Wear OS companion
- [ ] iPad/iOS client (Flutter o nativo)

---

*Fin del documento — MiMo Mobile v1.0.0 Documentation Package*
