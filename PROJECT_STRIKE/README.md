# PROJECT STRIKE — Android FPS prototype

A separate Godot 4 project inside the existing repository. The original TikTok X files remain outside this folder and are not part of the game.

## Current prototype
- Procedural greybox training arena (temporary blockout, not final environment art)
- First-person movement, mouse look, jump, hitscan training weapon and reload loop
- Android touch UI: virtual movement stick, swipe look, fire, reload and jump buttons
- Humanoid armored training targets assembled from 3D meshes
- Basic target movement and health
- HUD for health, armor and ammunition

## Open locally
Open project.godot from this folder in Godot 4.4.1 or a compatible Godot 4 release.

## Automated checks
GitHub Actions workflow: .github/workflows/project-strike-checks.yml.
It imports/parses the project and performs a short headless startup smoke test. A green workflow is required before calling the project validated.

## Current limitations
This is still an early prototype. It is not a finished commercial game and is not equivalent to Standoff 2 or Call of Duty. Final character rigs, animations, weapon models, audio, environment art, combat AI, menus, progression, optimization, device testing, Android export configuration and APK packaging remain to be implemented and tested. The current bots are procedural placeholders, not final character assets.

Use properly licensed assets only.
