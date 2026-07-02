# MiMo Mobile Major UI Upgrade Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task.

**Goal:** Transform MiMo Mobile into a professional app development environment with visual construction, immersive animations, and complete file management.

**Architecture:** Each tab becomes a specialized workspace: Chat for AI interaction, Build for visual app construction, Files for dual-device management, Terminal for code execution, Remote for fullscreen PC control. Background animations become truly dynamic with construction-from-nothing feel.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Canvas animations, WebSocket

---

## Task 1: Immersive Background Animation

**Covers:** S1 (Background must feel like construction from nothing)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/components/PerspectiveGridBackground.kt`

**Goal:** Background must have CONSTANT motion - particles flowing, grid scrolling, orbs pulsing, scan lines moving. Never static.

- [ ] **Step 1: Rewrite PerspectiveGridBackground with continuous animation**

Replace entire file with enhanced version that has:
- Grid lines that scroll infinitely downward (construction feel)
- Particles that flow in one direction (like building blocks falling into place)
- Orbs that pulse and move
- Scan line that sweeps continuously
- All animations must be `infiniteRepeatable` with NO pauses

- [ ] **Step 2: Build and install**

```bash
cd /home/DexTer/MiMoMobile
JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug
/home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify on OnePlus 8**

Take screenshot and verify ALL elements are moving constantly.

---

## Task 2: Build Section - Visual App Constructor

**Covers:** S2 (Build shows visual construction, not code)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/BuildVisualizerScreen.kt`

**Goal:** Build section becomes a visual app constructor. User types prompt, sees UI panels appearing, buttons forming, layouts assembling - like watching an app being built in real-time.

- [ ] **Step 1: Add input widget at bottom (like Chat)**

Add text input field at bottom of BuildScreen with send button. When user types "Construye una app de venta de tecnología", it triggers visual construction.

- [ ] **Step 2: Create VisualConstructionCanvas**

New composable that shows:
- Empty canvas → panels appearing one by one
- UI elements fading in (buttons, text fields, images)
- Layout grids forming
- Color themes applying
- Navigation bars appearing
- All with smooth animations

- [ ] **Step 3: Wire input to construction trigger**

When user sends message in Build, parse intent and show appropriate visual construction sequence.

- [ ] **Step 4: Build and install**

- [ ] **Step 5: Verify on OnePlus 8**

---

## Task 3: Files Section - Complete File Manager

**Covers:** S3 (Files with share, copy, delete operations)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/FileBrowserScreen.kt`

**Goal:** Full file manager with operations: share, copy, move, delete, rename, create folder. Shows both phone and PC files.

- [ ] **Step 1: Add file operation toolbar**

Add bottom toolbar with icons: Share, Copy, Move, Delete, Rename, New Folder

- [ ] **Step 2: Add multi-select mode**

Long-press to enter selection mode. Show selected count and batch operations.

- [ ] **Step 3: Add share functionality**

Share button opens Android share sheet with file content.

- [ ] **Step 4: Add confirmation dialogs**

Delete, move, rename operations show confirmation dialog.

- [ ] **Step 5: Build and install**

- [ ] **Step 6: Verify on OnePlus 8**

---

## Task 4: Remote Section - Fullscreen Mode

**Covers:** S4 (Remote fullscreen within tab margins)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/RemoteScreen.kt`

**Goal:** Remote desktop fills entire tab area. Minimize controls to overlay. Tap to show/hide controls.

- [ ] **Step 1: Make screen stream fill entire tab**

Remove padding, make Canvas fill max size.

- [ ] **Step 2: Add floating control bar**

Minimal floating bar at bottom with: Mouse mode, Keyboard mode, Scroll mode. Auto-hide after 3 seconds.

- [ ] **Step 3: Add tap-to-show controls**

Single tap shows controls, tap again hides.

- [ ] **Step 4: Build and install**

- [ ] **Step 5: Verify on OnePlus 8**

---

## Task 5: Chat Section - Operational Hub

**Covers:** S5 (Chat as operational section for MiMo Code)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/ChatScreen.kt`

**Goal:** Chat remains clean operational interface. No changes needed - already professional.

- [ ] **Step 1: Verify current ChatScreen meets requirements**

Check: bubbles, code blocks, streaming, auto-scroll all working.

- [ ] **Step 2: Minor polish if needed**

Only touch if something is broken.

---

## Execution Order

1. Task 1 (Background) - Foundation, affects all screens
2. Task 2 (Build) - Major new feature
3. Task 3 (Files) - Important functionality
4. Task 4 (Remote) - Enhancement
5. Task 5 (Chat) - Verification only

## Verification

After all tasks:
1. Build complete app
2. Install on OnePlus 8
3. Test each tab:
   - Chat: Send message, see response with code blocks
   - Build: Send "Construye una app", see visual construction
   - Files: Navigate, select file, try share/copy/delete
   - Terminal: Execute command, see output
   - Remote: Fullscreen stream with floating controls
4. Verify background animation is ALWAYS moving
5. Take final screenshots
6. Commit and push to GitHub
