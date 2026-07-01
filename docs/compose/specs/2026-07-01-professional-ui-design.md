# MiMo Mobile Professional UI Enhancement

## [S1] Problem
The current MiMo Mobile UI needs to feel professional and fluid like MiMo Code CLI. The Chat, Terminal, and Build tabs should work together as a cohesive development environment where users can see code being written in real-time across all views.

## [S2] Solution Overview
Three interconnected tabs that share real-time code state:

| Tab | Experience | Purpose |
|-----|------------|---------|
| Chat | Hybrid bubbles + code blocks | Communication with AI, professional code display |
| Terminal | Live code viewer | See code MiMo is writing in real-time |
| Build | Live code writer | Source code appearing line by line |

## [S3] Chat Screen Enhancement
- Clean message bubbles with Material3 design
- Code blocks with syntax highlighting (dark background, colored keywords)
- Collapsible code sections for long snippets
- Copy button on code blocks
- Streaming indicator during AI response
- Auto-scroll to latest message

## [S4] Terminal Screen Enhancement
- Live code viewer showing what MiMo is currently writing
- Syntax highlighting for Kotlin, Python, XML, etc.
- Line numbers on left side
- File path header showing which file is being edited
- Diff view for changes (old vs new)
- Color-coded by language

## [S5] Build Screen Enhancement
- Live code writer showing source code appearing line by line
- Character-by-character animation
- Syntax highlighting applied as code appears
- File tabs showing multiple files being worked on
- Progress indicator for overall build status

## [S6] Data Flow
All three tabs receive data from the same WebSocket messages:
- `chat_chunk` → Chat tab (display in bubble)
- `exec_output` → Terminal tab (show command output)
- `code_write` → Build tab (show code being written)

## [S7] Persistence
- Messages saved to DataStore on chat_end
- Terminal output saved per session
- Build state preserved across app restarts

## [S8] Animations
- Smooth transitions between tabs
- Code appearing with typing animation
- Subtle pulse on active processing indicators
- Background animation (Matplotlib) synced with activity level
