# MiMo Mobile v2 - App Builder Environment

## [S1] Problem
MiMoMobile's current Build section shows code being written line-by-line, but the vision is for it to be a **visual app constructor** where users see their app being built in real-time (wireframes → UI). Files section needs unified dual-device management, Remote needs immersive fullscreen, and the background must feel like "construction from nothing" with constant motion.

## [S2] Solution Overview
Five interconnected improvements:

| Section | Change | Impact |
|---------|--------|--------|
| **Build** | Text input + visual construction canvas (wireframe → Material3 UI) | Complete rewrite of BuildVisualizerScreen |
| **Files** | Unified phone+PC explorer with full file operations | Rewrite of FileBrowserScreen |
| **Remote** | Fullscreen stream with tap-to-show controls | Modify RemoteScreen |
| **Background** | Construction-style particles with constant motion | Rewrite PerspectiveGridBackground |
| **Chat** | No changes needed | Already professional |

## [S3] Build Section - Visual App Constructor

### [S3.1] Input Widget
- TextField at bottom of Build screen (same style as Chat's "Escribe tu mensaje")
- Send button (FilledIconButton) on the right
- When user types a prompt (e.g., "Construye una app de venta de tecnologia"), it triggers visual construction
- Input sends via WebSocket as `chat` message to MiMo Code server

### [S3.2] Visual Construction Canvas
Main area shows app being built visually:

**Phase 1 - Wireframe (0-3s):**
- Gray dashed lines forming panel outlines
- Placeholder rectangles for content areas
- Wireframe buttons and input fields
- Layout grid visible

**Phase 2 - UI Transformation (3s+):**
- Wireframes transform into real Material3 components
- Colors, shadows, typography applied
- Components: App Bar → Cards → Buttons → Lists → Forms
- Each component appears with fadeIn + slideUp animation (200ms delay between each)

### [S3.3] Construction Sequence
1. App Bar appears at top
2. Navigation drawer/bottom bar forms
3. Content cards slide in from sides
4. Product images fade in
5. Action buttons pulse into place
6. Form fields appear with labels
7. Final polish: shadows, spacing adjust

### [S3.4] Data Flow
```
User types in Build → WebSocket "chat" → MiMo Code server
                                        ↓
                          "build_progress" with visual structure JSON
                                        ↓
                          Build Canvas renders wireframe → UI
```

## [S4] Files Section - Unified Dual-Device Explorer

### [S4.1] Unified View
- Single file explorer showing both phone and PC files together
- Differentiated by icon badge (phone icon / monitor icon)
- Path shows device prefix: `[Phone] /sdcard/...` or `[PC] /home/user/...`

### [S4.2] File Operations
- **Share**: Opens Android Share Sheet with file content
- **Copy**: Copies to clipboard or another location
- **Move**: Moves to another folder with destination selector
- **Delete**: Deletes with confirmation dialog
- **Rename**: Renames with dialog
- **New Folder**: Creates folder with name input

### [S4.3] Multi-Select Mode
- Long-press to enter selection mode
- Top bar shows selected count and batch operations
- Select All / Deselect All buttons
- Batch delete, batch move, batch share

### [S4.4] WebSocket Messages
- Existing: `list_dir`, `read_file`, `write_file`, `delete_file`
- New: `copy_file`, `move_file`, `share_file`, `rename_file`

## [S5] Remote Section - Fullscreen Immersive

### [S5.1] Fullscreen Mode
- Stream fills entire tab area (remove header padding)
- No permanent header or bottom bar
- Canvas fills max available space

### [S5.2] Floating Controls
- Minimal floating bar at bottom with mode chips (Touch/Keyboard)
- Auto-hides after 3 seconds of inactivity
- Semi-transparent background

### [S5.3] Tap-to-Show
- Single tap on screen: shows/hides controls
- In Touch mode: drag controls mouse cursor
- In Keyboard mode: tap opens text input for typing

## [S6] Background - Construction from Nothing

### [S6.1] Core Concept
Rewrite PerspectiveGridBackground with construction theme:
- **Building blocks**: Rectangles falling from top and placing on grid (like LEGO)
- **Construction lines**: Horizontal lines drawing left-to-right
- **Progress particles**: Light dots flowing downward (work in progress)
- **Scan line**: Horizontal line sweeping continuously

### [S6.2] Animation Specifications
- All animations: `infiniteRepeatable` with NO pauses
- Speed: 2x faster than current implementation
- Particle count: 60+ (up from 40)
- Block count: 24+ (up from 18)

### [S6.3] Visual Elements
- Grid scrolling infinitely downward (construction feel)
- Blocks that pulse and shift position
- Particles that flow in one direction (building blocks falling into place)
- Orbs that pulse and move
- Scan line that sweeps continuously
- Vignette edges for depth

### [S6.4] Color Palette
- Background: `#0B0E17` (dark)
- Grid lines: `#1A2744` → `#243556` (blue-gray)
- Blocks: `#1E4D8C` with glow (blue)
- Particles: `#4FC3F7` (light blue)
- Scan line: `#00D4FF` (cyan)
- Maintain existing palette but brighter/more saturated

## [S7] Data Flow Architecture

### [S7.1] WebSocket Protocol
All tabs receive data from the same WebSocket messages:
- `chat` → Chat tab (user messages)
- `chat_chunk` → Chat tab (AI response streaming)
- `build_progress` → Build tab (visual construction data)
- `exec_output` → Terminal tab (command output)
- `screen_frame` → Remote tab (screen stream)
- `dir_listing` → Files tab (directory contents)
- `file_content` → Files tab (file viewer)

### [S7.2] Build Progress JSON Structure
```json
{
  "project": "TechStore",
  "phase": "wireframe|ui",
  "components": [
    {
      "type": "appbar|card|button|list|form|nav",
      "position": {"x": 0, "y": 0, "w": 1, "h": 0.1},
      "style": {"color": "#primary", "elevation": 2}
    }
  ]
}
```

## [S8] Transitions and Animations

### [S8.1] Tab Transitions
- Maintain current: fadeIn + slideInHorizontally
- Direction based on tab index (left/right)

### [S8.2] Build Construction
- Sequential appearance: each component 200ms after previous
- Wireframe phase: dashed lines animate drawing
- UI phase: components scale from 0.8 to 1.0 with spring animation

### [S8.3] Background
- No transitions - constant infinite motion
- Speed variations create organic feel

### [S8.4] File Operations
- Smooth fade for items appearing/disappearing
- Slide animation for new folders

## [S9] Implementation Priority

1. **Background rewrite** (Task 1) - Foundation, affects all screens
2. **Build section** (Task 2) - Major new feature
3. **Files section** (Task 3) - Important functionality
4. **Remote section** (Task 4) - Enhancement
5. **Verification** (Task 5) - Final testing

## [S10] Success Criteria

- [ ] Background has CONSTANT motion - never static
- [ ] Build shows visual construction (wireframe → UI), NO code
- [ ] Files manages both phone and PC files with all operations
- [ ] Remote fills entire tab with tap-to-show controls
- [ ] All animations smooth at 60fps on OnePlus 8
- [ ] App builds and installs without errors
