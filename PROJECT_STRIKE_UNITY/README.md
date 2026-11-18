# PROJECT STRIKE — Unity 6.3 LTS migration

This is the new Unity implementation. The Godot project remains preserved in the sibling `PROJECT_STRIKE/` folder; do not delete it until the Unity build has been validated on a real Android device.

## Engine
- Unity 6.3 LTS, editor version `6000.3.28f1`
- Android ARM64 target, landscape orientation, minimum Android API 24
- Built-in Standard rendering pipeline for broad Android device compatibility

## Current Unity foundation
- Runtime-generated compact arena with collision and cover
- First-person controller, camera, basic rifle mesh and hitscan damage
- Mobile touch movement/look/fire/reload/jump input and desktop keyboard/mouse input
- 5v5 BLUE vs RED round loop, bots, health/armor/ammo, score, timer and first-to-five match flow

## Open
Open this folder in Unity Hub using Unity 6.3 LTS. Android Build Support, Android SDK & NDK modules are required for local APK export.

## GitHub Actions Android build
The workflow uses GameCI and requires a valid Unity Personal license secret named `UNITY_LICENSE`. To obtain it, run the `Unity license activation` workflow, download the generated `.alf` artifact, submit it at https://license.unity3d.com/, then save the returned `.ulf` file contents as the repository Actions secret `UNITY_LICENSE`. The build workflow then exports an Android APK artifact.

## Important
This is a playable systems foundation, not a finished Standoff 2-level game. Character and weapon meshes are still procedural blockout art. Production character rigs, animation, high-quality licensed environment/weapon assets, audio, pathfinding, multiplayer networking, optimization, device testing and release signing remain future work. Never treat a successful editor compile as proof of real-device performance.
