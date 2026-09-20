# Development layout

This repository is intentionally split into the Android source tree, a tracked V68 integration overlay, recovery evidence, and reproducible bootstrap helper scripts.

## Active source

- `android/app/` — the `com.alpine` Android application, bootstrap installer, terminal UI, and the bridge that opens the integrated display.
- `android/alpine-shared/` — shared Android utilities and Alpine-specific constants.
- `android/terminal-emulator/` and `android/terminal-view/` — terminal implementation inherited from the Termux-derived codebase.
- `android/x11/` — embedded Termux:X11-derived display implementation and `libXlorie.so`.
- `android/x11-stub/` — compile-only stubs required by the X11 module for hidden Android framework interfaces. This is build input, not a second app.

The X11 Java/JNI implementation intentionally keeps technical `com.termux.x11` identifiers where upstream code, Binder descriptors, actions, or native symbols depend on them. User-facing integration belongs to Alpine and should use Alpine/Display wording without mechanically renaming those implementation identifiers. V68's refreshed Termux PRoot runtime raises the practical and manifest minimum to Android 7.0 / API 24; the integrated Alpine Display itself requires Android 8.0 / API 26 or newer.

## Bootstrap work

- `bootstrap/v68-overlay/` — active host-prefix and nested-rootfs integration files applied during V68 preparation. It refreshes PRoot to 5.1.107.92 and adds its required `libandroid-shmem` runtime while keeping Alpine package management stock.
- `bootstrap/v67-overlay/` — preserved V67 stock-`apk` experiment using Alpine 3.24.1 and the older recovered PRoot runtime.
- `bootstrap/v66-overlay/` — preserved V66 compatibility experiment; V68 does not use its custom `apk` wrapper.
- `android/app/src/main/cpp/bootstrap-aarch64.zip` — ignored complete build input. It preserves the recovered host prefix while the nested rootfs is regenerated from pinned Alpine 3.23.6 inputs.
- `scripts/fetch-assets.py` — restores checksummed recovery assets.
- `scripts/v68_rootfs.py` — rebuilds the nested Alpine 3.23.6 rootfs from the pinned official minirootfs plus XKB data. Stock Alpine `/sbin/apk` is retained.
- `scripts/prepare-v68-bootstrap.py` — applies the V68 overlay, refreshes the host PRoot runtime, regenerates rootfs symlinks/markers, rejects V66 package-manager shims, and verifies the final bootstrap atomically.
- `scripts/v67_rootfs.py` / `scripts/prepare-v67-bootstrap.py` — preserved V67 tooling for historical reproducibility.
- `scripts/v66_rootfs.py` / `scripts/prepare-v66-bootstrap.py` — preserved V66 tooling for historical reproducibility.

Prepare the build input from the repository root with:

```sh
python3 scripts/fetch-assets.py --bootstrap
python3 scripts/prepare-v68-bootstrap.py
```

Do not place downloaded `.deb`, `.apk`, APK files, temporary extracted root filesystems, build outputs, Gradle caches, or duplicate test binaries in the tracked source tree.

The validated V68 desktop build defaults are compileSdk 36, NDK 27.1.12297006, Gradle 9.3.1, JDK 17, and Android Gradle Plugin 8.13.2. Machine-specific SDK/JDK paths remain local.

V68 leaves modern PRoot's seccomp accelerator enabled by default. `PROOT_NO_SECCOMP=1` is retained only as an explicit device-specific diagnostic workaround; it is not forced by the launcher.

V68.1 keeps PRoot-Distro 4.38.0 only for the known-good runtime login path and read-only discovery. Its embedded command dispatcher allows `login`, `list`, and `help` only; destructive or mutating management commands such as install, remove, reset, restore, rename, copy, backup, and clear-cache are blocked.

## Recovery and releases

- `docs/recovery/` — historical evidence, checksums, and the handoff from the original Android/Termux development environment.
- `docs/releases/` — compact immutable artifact-freeze manifests for known milestones such as V65.

The V65 freeze records known-good artifacts; it must not be interpreted as proof that the recovered current source exactly equals the historical V65 source tree.

## Development order

1. Keep the tree clean and verify no generated output is tracked.
2. Preserve/freeze the last known successful V65 artifacts.
3. Restore the recovered bootstrap and regenerate the V68 Alpine 3.23.6 + PRoot 5.1.107.92 build input with the preparation script.
4. Apply V68 fixes to tracked Android source or `bootstrap/v68-overlay/`; rerun preparation after overlay changes.
5. Before building, run the Python tests, shell syntax/shellcheck checks, bootstrap verifier, and `git diff --check`.
6. Build from a clean native/Gradle state and verify the APK contains the exact prepared bootstrap and expected X11/native libraries.
7. Runtime-test terminal startup/exit, `apk update/add/upgrade`, account lookup, DBus, package triggers, X11 socket/display startup, fonts, desktop startup, and app background/resume separately from build success.
