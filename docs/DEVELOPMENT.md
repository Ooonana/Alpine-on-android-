# Development layout

This repository is intentionally split into the Android source tree, a tracked V66 compatibility overlay, recovery evidence, and reproducible bootstrap helper scripts.

## Active source

- `android/app/` — the `com.alpine` Android application, bootstrap installer, terminal UI, and the bridge that opens the integrated display.
- `android/alpine-shared/` — shared Android utilities and Alpine-specific constants.
- `android/terminal-emulator/` and `android/terminal-view/` — terminal implementation inherited from the Termux-derived codebase.
- `android/x11/` — embedded Termux:X11-derived display implementation and `libXlorie.so`.
- `android/x11-stub/` — compile-only stubs required by the X11 module for hidden Android framework interfaces. This is build input, not a second app.

The X11 Java/JNI implementation intentionally keeps technical `com.termux.x11` identifiers where upstream code, Binder descriptors, actions, or native symbols depend on them. User-facing integration belongs to Alpine and should use Alpine/Display wording without mechanically renaming those implementation identifiers.

## Bootstrap work

- `bootstrap/v66-overlay/` — tracked host-prefix and nested-rootfs compatibility files applied during V66 preparation. It is not a complete root filesystem.
- `android/app/src/main/cpp/bootstrap-aarch64.zip` — ignored complete build input. It preserves the recovered host prefix while the nested rootfs is regenerated from pinned Alpine 3.24.1 inputs.
- `scripts/fetch-assets.py` — restores checksummed recovery assets.
- `scripts/v66_rootfs.py` — rebuilds the nested Alpine 3.24.1 rootfs from pinned minirootfs, XKB, and static apk-tools inputs.
- `scripts/prepare-v66-bootstrap.py` — applies the V66 overlay, regenerates rootfs symlinks/markers, and verifies the final bootstrap atomically.

Prepare the build input from the repository root with:

```sh
python3 scripts/fetch-assets.py --bootstrap
python3 scripts/prepare-v66-bootstrap.py
```

Do not place downloaded `.deb`, `.apk`, APK files, temporary extracted root filesystems, build outputs, Gradle caches, or duplicate test binaries in the tracked source tree.

## Recovery and releases

- `docs/recovery/` — historical evidence, checksums, and the handoff from the original Android/Termux development environment.
- `docs/releases/` — compact immutable artifact-freeze manifests for known milestones such as V65.

The V65 freeze records known-good artifacts; it must not be interpreted as proof that the recovered current source exactly equals the historical V65 source tree.

## Development order

1. Keep the tree clean and verify no generated output is tracked.
2. Preserve/freeze the last known successful V65 artifacts.
3. Restore the recovered bootstrap and regenerate the V66 Alpine 3.24.1 build input with the preparation script.
4. Apply V66 fixes to tracked Android source or `bootstrap/v66-overlay/`; rerun preparation after overlay changes.
5. Before building, run the Python tests, shell syntax/shellcheck checks, bootstrap verifier, and `git diff --check`.
6. Build from a clean native/Gradle state and verify the APK contains the exact prepared bootstrap and expected X11/native libraries.
7. Runtime-test terminal startup/exit, `apk update/add/upgrade`, account lookup, DBus, package triggers, X11 socket/display startup, fonts, desktop startup, and app background/resume separately from build success.