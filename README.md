# MiMo Mobile

**Controla tu PC desde el celular. Asistente AI siempre disponible.**

App Android nativa que conecta con MiMo Code CLI vía WebSocket. Chat, remote desktop, gestión de archivos y ejecución de comandos — todo desde tu teléfono.

---

## Modelo de Negocio

| Tier | Precio | Features |
|------|--------|----------|
| **Free** | $0 | 1 dispositivo, chat básico |
| **Pro** | $9.99/mes | 5 dispositivos, remote desktop, file manager |
| **Team** | $29.99/mes | Ilimitado, prioridad, soporte |
| **Enterprise** | Custom | API, on-premise, SLA |

---

## Funcionalidades

- **Chat con AI** — Envía prompts y recibe respuestas en tiempo real streaming
- **Build Visualizer** — Pipeline animado de construcción de proyectos
- **Remote Desktop** — Vista y control del escritorio del PC desde el celular
- **File Browser** — Navega y edita archivos del proyecto
- **Terminal** — Ejecuta comandos shell remotamente
- **Device Manager** — Controla dispositivos Android conectados vía ADB
- **Settings** — Configuración de servidor y preferencias

---

## Arquitectura

```
┌─────────────────────────────────┐
│    MiMo Mobile (Android)        │
│    Kotlin + Jetpack Compose     │
└───────────┬─────────────────────┘
            │ WebSocket (8765)
┌───────────▼─────────────────────┐
│    MiMo Server (Python)         │
│    asyncio.Protocol, stdlib     │
└───────────┬─────────────────────┘
            │ Subprocess
┌───────────▼─────────────────────┐
│    MiMo Code CLI                │
└─────────────────────────────────┘
```

---

## Stack Técnico

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material3 |
| Arquitectura | MVVM |
| WebSocket | Implementación raw TCP (sin OkHttp) |
| Persistencia | DataStore Preferences |
| Server | Python 3.13, cero dependencias externas |
| Comunicación | WebSocket (asyncio.Protocol) |

---

## Instalación

### Server (PC)
```bash
cd mimo-mobile-server
python3 server.py
```

### App (Android)
1. Instala el APK desde `app/build/outputs/apk/debug/app-debug.apk`
2. Ingresa la IP de tu PC en Settings
3. Conecta con PIN: `MIMO2026`

---

## Configuración del Server

Variables de entorno (`.env`):
```bash
MIMO_CMD=~/.mimocode/bin/mimo    # Ruta al CLI
MIMO_AUTH_PIN=MIMO2026           # PIN de autenticación
MIMO_WORKSPACE=~                 # Directorio de trabajo
MIMO_WS_PORT=8765                # Puerto WebSocket
MIMO_HTTP_PORT=8080              # Puerto HTTP
```

---

## Requisitos

- Android 8.0+ (API 26)
- Python 3.10+
- MiMo Code CLI instalado

---

## Soporte el desarrollo

Si MiMo Mobile te es útil, considera suscribirte para apoyar el desarrollo continuo:

[![Pro](https://img.shields.io/badge/Pro-$9.99%2Fmes-2563EB?style=for-the-badge&logo=stripe)](https://buy.stripe.com/your-pro-link)
[![Team](https://img.shields.io/badge/Team-$29.99%2Fmes-16A34A?style=for-the-badge&logo=stripe)](https://buy.stripe.com/your-team-link)

---

## Contacto

- **GitHub**: [@dixi3stdgdl-design](https://github.com/dixi3stdgdl-design)

---

## Licencia

MIT License
