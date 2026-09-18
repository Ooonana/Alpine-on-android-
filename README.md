# Alpine on Android

An Android terminal app that automatically starts Alpine Linux through proot, with a Termux:X11-derived display integrated into the same APK. This repository includes the recovered development source, reproducible V67 bootstrap tooling, and historical handoff documentation.

**V65 remains the frozen known artifact baseline. V67 is active development and is not yet a runtime-tested release.** V67 uses a reproducibly prepared Alpine 3.24.1 rootfs, the integrated Alpine Display, and stock Alpine `/sbin/apk` with the official HTTPS repositories. The V66 custom `apk` compatibility wrapper is preserved only as historical development evidence and is not part of the V67 rootfs.

## Source and history

- [`android/`](android/): the recovered app, terminal, shared-library and embedded X11 modules. Original licenses and the existing debug test key are retained.
- [`bootstrap/v67-overlay/`](bootstrap/v67-overlay/): tracked V67 Android/proot integration overrides. It is not a complete rootfs.
- [`bootstrap/v66-overlay/`](bootstrap/v66-overlay/): preserved V66 compatibility experiment, including the custom package-manager wrapper; not used by V67.
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
python3 scripts/prepare-v67-bootstrap.py
```

The downloader verifies size and SHA-256 and restores the recovered host-prefix bootstrap to `android/app/src/main/cpp/bootstrap-aarch64.zip`. The preparation step then deterministically rebuilds the nested Alpine rootfs from pinned Alpine 3.24.1 inputs, applies the tracked V67 integration overlay, repairs rootfs symlinks, and verifies the result before replacing the build input.

The current Windows development checkout has been validated with JDK 17, Android Gradle Plugin 8.13.2, compileSdk 36, Gradle 9.3.1, and NDK 27.1.12297006. The project still targets Android 28 and arm64-v8a for compatibility with the recovered app/runtime architecture. Local SDK/NDK paths remain machine-specific and should not be committed.

Historical toolchain pins and Android-hosted Termux build paths remain documented under `docs/recovery/`; they are recovery evidence, not requirements for the current desktop build.

## Optional desktop environments

V67 keeps the APK/bootstrap small and installs a desktop only when requested from inside Alpine. Desktop packages are installed by stock Alpine `apk`. Run:

```sh
install-desktop
```

The interactive installer offers XFCE, LXQt, LXDE, Openbox, MATE, and Plasma. The selected desktop is saved as the default and can then be launched with:

```sh
start-desktop
```

`start-desktop` starts the integrated Alpine Display/X11 bridge, prepares a D-Bus session and PRoot-friendly X11 environment, and launches the selected desktop. A desktop can also be selected directly, for example `install-desktop xfce` or `start-desktop mate`. Plasma is experimental on Alpine 3.24 because current packages may not include the historical `startplasma-x11` session launcher; the project provides a best-effort `kwin_x11` + `plasmashell` fallback when those binaries exist.

## APKs and historical assets

Existing releases are retained. The `termux-recovery-2026-09-14` recovery release is an archive snapshot, not a new stable application version. V65 artifact identities are frozen under `docs/releases/v65/`. A V67 APK should not be published as stable until the Android runtime checks in `docs/DEVELOPMENT.md` pass on-device.

List the recovery assets without downloading:

```sh
python3 scripts/fetch-assets.py --list
```

Download a specific asset with `--name NAME`, or use `--all` for the complete historical set. These files can require many gigabytes; consult the manifest first.

## Development history

`android-development-history.tar.gz` includes the original Codex JSONL/Markdown and the Gemini development chats and tool outputs. The original local files remain preserved. Credential files from the old Gemini directory are excluded from the public copy; any pattern-based redactions are recorded in [the publication review](docs/recovery/publication-review.json).

The Codex raw log parsed successfully, but the broken interactive session has not been repaired. The handoff summarizes recorded work and distinguishes previous claims from checks performed during recovery.
