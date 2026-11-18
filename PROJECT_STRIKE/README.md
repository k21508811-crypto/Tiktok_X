# PROJECT STRIKE — Android FPS prototype

A separate Godot 4 project inside the existing repository. The original TikTok X files remain outside this folder and are not part of the game.

## Implemented prototype systems
- Procedural greybox training arena (temporary blockout, not final environment art)
- First-person movement, mouse look, jump and a basic first-person rifle mesh
- Hitscan firing, magazine and reserve ammo, reload timer and weapon recoil
- Android landscape mode and touch UI: virtual movement stick, swipe look, fire, reload and jump
- Armored humanoid training targets assembled from 3D meshes
- Enemy detection, pursuit, line-of-sight attacks and damage
- Player health and armor, elimination and timed respawn
- Kill/death counter, wave progression and HUD

## Open locally
Open project.godot from this folder in Godot 4.4.1 or a compatible Godot 4 release.

## Automated checks
GitHub Actions workflow: .github/workflows/project-strike-checks.yml. It imports/parses the project and performs a short headless startup smoke test. A green workflow is required before calling the project validated.

## Current limitations
This is still an early prototype. It is not a finished commercial game and is not equivalent to Standoff 2 or Call of Duty. The character meshes are procedural blockout models, not production character assets. Final rigged characters, animation, detailed weapon models, audio, environment art, cover-aware AI/pathfinding, menus, progression, optimization, device testing, Android export configuration and APK packaging remain to be implemented and tested. No successful build or APK is claimed yet.

Use properly licensed assets only.
