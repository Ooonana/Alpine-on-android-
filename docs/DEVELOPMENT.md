# Development layout

This repository is intentionally split into a small Android source tree, a small V66 bootstrap overlay, recovery evidence, and helper scripts.

## Active source

- `android/app/` — the `com.alpine` Android application, bootstrap installer, terminal UI, and the bridge that opens the integrated display.
- `android/alpine-shared/` — shared Android utilities and Alpine-specific constants.
- `android/terminal-emulator/` and `android/terminal-view/` — terminal implementation inherited from the Termux-derived codebase.
- `android/x11/` — embedded Termux:X11-derived display implementation and `libXlorie.so`.
- `android/x11-stub/` — compile-only stubs required by the X11 module for hidden Android framework interfaces. This is build input, not a second app.

The X11 Java/JNI implementation intentionally keeps technical `com.termux.x11` identifiers where upstream code, Binder descriptors, actions, or native symbols depend on them. User-facing integration belongs to Alpine and should use Alpine/Display wording without mechanically renaming those implementation identifiers.

## Bootstrap work

- `bootstrap/v66-overlay/` — only the files changed for unfinished V66 work. It is not a complete root filesystem.
- `android/app/src/main/cpp/bootstrap-aarch64.zip` — the complete embedded bootstrap used for an actual APK build. It is not stored in Git because of its size and is restored from a checksummed recovery release asset.
- `scripts/fetch-assets.py` — restores large recovery assets and verifies exact size and SHA-256.

Do not place downloaded `.deb`, `.apk`, APK files, temporary extracted root filesystems, build outputs, Gradle caches, or duplicate test binaries in the tracked source tree.

## Recovery and releases

- `docs/recovery/` — historical evidence, checksums, and the handoff from the original Android/Termux development environment.
- `docs/releases/` — compact immutable artifact-freeze manifests for known milestones such as V65.

The V65 freeze records known-good artifacts; it must not be interpreted as proof that the recovered current source exactly equals the historical V65 source tree.

## Development order

1. Keep the tree clean and verify no generated output is tracked.
2. Preserve/freeze the last known successful V65 artifacts.
3. Restore the incomplete V66 bootstrap only when it is needed for a build.
4. Apply V66 fixes to source and bootstrap content deliberately.
5. Build from a clean native/Gradle state and verify the APK contains the intended bootstrap and X11 library.
6. Runtime-test terminal startup, `apk` operations, account lookup/DBus, X11 socket/display startup, fonts, and the intended desktop session separately from build success.