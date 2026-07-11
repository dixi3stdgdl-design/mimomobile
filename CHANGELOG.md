# Changelog

All notable changes to MiMo Mobile (Android) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

## [1.2.0] - 2026-07-06

### Added
- Connection anchoring toggle with pairing
- HTTP ADB client fallback for device commands

### Changed
- Default port updated to 8765 with conflict avoidance

## [1.1.0] - 2026-07-05

### Added
- Theme selector (dark/light) with immersive background animation
- Professional UI — Chat with code blocks, Live Code Viewer, Live Code Writer
- UI refinements — BuildVisualizer, Files, Remote, PerspectiveGrid
- Release signing config with keystore and minification

### Fixed
- Abstract class compilation error in NetworkMonitor
- `kotlin.random.Random` usage

## [1.0.0] - 2026-07-04

### Added
- Initial release — Android Kotlin companion app for MiMo Mobile Server
- Exponential backoff, network interceptor, PIN persistence, chat history
- Premium subscription plans UI
- Mercado Pago monetization integration
- PerspectiveGridBackground animation
- GitHub Actions CI/CD
