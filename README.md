# Alpine on Android

An Android terminal app that automatically starts Alpine Linux through proot, with a Termux:X11-derived display integrated into the same APK. This repository includes the recovered development source, reproducible V68 bootstrap tooling, and historical handoff documentation.

**V65 remains the frozen known artifact baseline. V68 is active development and is not yet a runtime-tested release.** V68 uses a reproducibly prepared Alpine 3.23.6 rootfs, current Termux PRoot 5.1.107.92, the integrated Alpine Display, and stock Alpine `/sbin/apk` with the official HTTPS repositories. The V66 custom `apk` compatibility wrapper and the V67 Alpine 3.24.1/older-PRoot experiment are preserved only as historical development evidence and are not part of the V68 runtime.

## Source and history

- [`android/`](android/): the recovered app, terminal, shared-library and embedded X11 modules. Original licenses and the existing debug test key are retained.
- [`bootstrap/v68-overlay/`](bootstrap/v68-overlay/): active V68 Android/proot integration overrides. It is not a complete rootfs.
- [`bootstrap/v67-overlay/`](bootstrap/v67-overlay/): preserved V67 stock-`apk` experiment with Alpine 3.24.1 and the older recovered PRoot runtime.
- [`bootstrap/v66-overlay/`](bootstrap/v66-overlay/): preserved V66 compatibility experiment, including the custom package-manager wrapper; not used by V68.
- [Development handoff](docs/recovery/HANDOFF.md): architecture, version history, reported failures and desktop setup considerations.
- [Recovery asset manifest](docs/recovery/release-assets.json): original-path mappings, sizes and SHA-256 checksums for large files.
- [Upload layout](docs/recovery/GITHUB-TRANSFER.md): what is in Git versus release assets, and the history-preservation limits.

The recovery covers **Android Alpine APK development only**. Kindle Alpine/KUAL projects and unrelated personal files are excluded.

## Continue on a computer

Clone the repository and download the exact bootstrap from the recovery release:

```sh
git clone https://github.com/Ooonana/Alpine-on-android-.git
cd Alpine-on-android-
python3 scripts/fetch-assets.py --bootstrap
python3 scripts/prepare-v68-bootstrap.py
```

The downloader verifies size and SHA-256 and restores the recovered host-prefix bootstrap to `android/app/src/main/cpp/bootstrap-aarch64.zip`. The preparation step then refreshes the host PRoot runtime to pinned Termux PRoot 5.1.107.92, deterministically rebuilds the nested Alpine rootfs from pinned Alpine 3.23.6 inputs, applies the tracked V68 integration overlay, repairs rootfs symlinks, and verifies the result before replacing the build input.

V68.2 retains the embedded PRoot-Distro 4.38.0 login path, but its public CLI is restricted to `login`, `list`, and `help`; install/reset/restore/copy and the other mutating management commands are disabled because Alpine-on-Android ships and upgrades its own prepared rootfs.

The current Windows development checkout has been validated with JDK 17, Android Gradle Plugin 8.13.2, compileSdk 36, Gradle 9.3.1, and NDK 27.1.12297006. Those validated versions remain the V68 build defaults. The project still targets Android 28 and arm64-v8a for compatibility with the recovered app/runtime architecture. Local SDK/NDK paths remain machine-specific and should not be committed.

V68 requires Android 7.0 / API 24 or newer because its refreshed Termux PRoot runtime is built for the current Termux API-24+ package baseline. The integrated Alpine Display requires Android 8.0 / API 26 or newer because its embedded X11 native/runtime stack depends on API-26-era platform components.

Historical toolchain pins and Android-hosted Termux build paths remain documented under `docs/recovery/`; they are recovery evidence, not requirements for the current desktop build.

## Optional desktop environments

V68 keeps the APK/bootstrap small and installs a desktop only when requested from inside Alpine. Desktop packages are installed by stock Alpine `apk`. Run:

```sh
install-desktop
```

The interactive installer offers XFCE, LXQt, Openbox, MATE, and Plasma. Alpine 3.23 does not publish the old `lxpanel`/`lxappearance` packages required by our LXDE launcher, so V68 removes that unsupported choice instead of presenting a broken install. The selected desktop is saved as the default and can then be launched with:

```sh
start-desktop
```

`start-desktop` starts the integrated Alpine Display/X11 bridge, prepares a D-Bus session and PRoot-friendly X11 environment, and launches the selected desktop. A desktop can also be selected directly, for example `install-desktop xfce` or `start-desktop mate`. Plasma is experimental on Alpine 3.23 because current packages may not include the historical `startplasma-x11` session launcher; the project provides a best-effort `kwin_x11` + `plasmashell` fallback when those binaries exist.

## APKs and historical assets

Existing releases are retained. The `termux-recovery-2026-09-14` recovery release is an archive snapshot, not a new stable application version. V65 artifact identities are frozen under `docs/releases/v65/`. A V68 APK should not be published as stable until the Android runtime checks in `docs/DEVELOPMENT.md` pass on-device.

List the recovery assets without downloading:

```sh
python3 scripts/fetch-assets.py --list
```

Download a specific asset with `--name NAME`, or use `--all` for the complete historical set. These files can require many gigabytes; consult the manifest first.

## Development history

`android-development-history.tar.gz` includes the original Codex JSONL/Markdown and the Gemini development chats and tool outputs. The original local files remain preserved. Credential files from the old Gemini directory are excluded from the public copy; any pattern-based redactions are recorded in [the publication review](docs/recovery/publication-review.json).

The Codex raw log parsed successfully, but the broken interactive session has not been repaired. The handoff summarizes recorded work and distinguishes previous claims from checks performed during recovery.
