# Prism Pulse 3D

Native Android puzzle game. No WebView, HTML, JavaScript, Compose, or third-party game engine.

## Gameplay
Rotate the 3D structure by dragging, pinch to zoom, and tap a cube to flip it plus its orthogonally adjacent neighbours. Turn every cube off to complete the level.

## Version 1 feature set
- 50 deterministic solvable levels across five difficulty worlds
- Native Android Canvas 3D projection and touch hit-testing
- Drag rotation and pinch zoom
- Undo, restart, and mathematically generated hints
- 1–3 star scoring and par targets
- Saved best moves, stars, and unlocked levels
- Haptic and sound feedback
- Offline play
- GitHub Actions APK build

## Build
Push to `main`, then open the GitHub **Actions** tab. The `Build Android APK` workflow uploads `PrismPulse3D-debug` containing `app-debug.apk`.
