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

V69.6 builds on V69.5's managed multi-desktop support and fixes two device-found integration bugs: the interactive desktop installer no longer captures its printed menu as package-group names, and the manager removes only malformed `.alpine-desktop-*` virtual groups created by that V69.5 bug. Alpine still ships with password authentication locked and no universal default password. It keeps the embedded PRoot-Distro 4.38.0 login path and Alpine 3.23.6 runtime, and the compact Alpine Display input dock remains below the X11 surface. The dock provides software-keyboard toggle, right click, Esc, Tab, sticky Ctrl/Alt/Meta, arrow keys, clipboard paste, and a compact hide/show control without overlaying the desktop. `desktop-manager` can install several of XFCE, LXQt, Openbox, MATE, Plasma Desktop, LXDE compatibility, Plasma Mobile, and Phosh at the same time, switch the default without reinstalling, repair a desktop, start a specific desktop, and remove managed desktops. New V69.5+ installs are tracked as separate Alpine `apk` virtual package groups, so removing one group keeps dependencies still required by another group or by packages explicitly owned by the user. Older desktop installs remain detectable as legacy installs and are not silently claimed by the manager. V69.1 fixed nested-Xwayland socket permissions and Plasma support packages; V69.2 forced Phosh/phoc to use the wlroots pixman renderer; V69.3 reduced Android-inapplicable Phosh background services and added Android process diagnostics; V69.4 added live `xrandr`/`wlr-randr` Phosh resizing and user-chosen mobile lock-screen password setup; V69.5 added managed multi-desktop package groups. V69.6 also makes the desktop networking model explicit: Android continues to own Wi-Fi/cellular/VPN, GUI applications observe the inherited connection, and `android-network-settings` hands network-setting requests back to Android instead of running NetworkManager inside PRoot. V69.6 migrates V68.1/V68.2/V68.3/V68.4/V69/V69.1/V69.2/V69.3/V69.4/V69.5 installations in place: only bootstrap-owned launcher/display/banner/manager/network-integration files and version markers are replaced, so packages installed with `apk`, Alpine `/etc` changes outside those owned files, and files under `/root` are preserved. The public PRoot-Distro CLI remains restricted to `login`, `list`, and `help`.

The current Windows development checkout has been validated with JDK 17, Android Gradle Plugin 8.13.2, compileSdk 36, Gradle 9.3.1, and NDK 27.1.12297006. Those validated versions remain the V69.6 build defaults. The project still targets Android 28 and arm64-v8a for compatibility with the recovered app/runtime architecture. Local SDK/NDK paths remain machine-specific and should not be committed.

V68 requires Android 7.0 / API 24 or newer because its refreshed Termux PRoot runtime is built for the current Termux API-24+ package baseline. The integrated Alpine Display requires Android 8.0 / API 26 or newer because its embedded X11 native/runtime stack depends on API-26-era platform components.

Historical toolchain pins and Android-hosted Termux build paths remain documented under `docs/recovery/`; they are recovery evidence, not requirements for the current desktop build.

## Optional desktop environments

V68 keeps the APK/bootstrap small and installs desktops only when requested from inside Alpine. Desktop packages are installed by stock Alpine `apk`. The primary interface is:

```sh
desktop-manager
```

The manager lists installed/default state and can install one or several desktops, remove managed desktops, change the default, repair package groups, or start a specific desktop. For example:

```sh
desktop-manager install xfce phosh
desktop-manager default phosh
desktop-manager start xfce
desktop-manager remove xfce
```

`install-desktop` remains as a compatibility wrapper and also accepts multiple choices, for example `install-desktop xfce phosh`. The last successfully installed choice becomes the default. New V69.5+ installs use per-desktop `apk` virtual groups so shared dependencies remain when another installed desktop still needs them. Pre-V69.5 installations are reported as `legacy`; legacy removal requires an explicit confirmation because the manager cannot prove whether every old explicit world entry came from Alpine-on-Android or was manually installed by the user. User home/config files are not deleted by desktop removal.

`start-desktop` starts the integrated Alpine Display/X11 bridge, prepares a D-Bus session and PRoot-friendly environment, and launches the default or a desktop named on the command line. Plasma Desktop and Plasma Mobile use nested KWin/Wayland on the existing X11 display. Phosh uses phoc/wlroots with the X11 backend and pixman renderer; V69.3 masks GNOME autostarts that depend on unavailable phone hardware/systemd/logind only for that temporary session, while V69.4 synchronizes the nested Phosh output with Alpine Display size changes. V69.6 uses GIO's Linux netlink network monitor for inherited connectivity and provides an **Android Network Settings** app-grid entry plus the `android-network-settings` command. These open Android's actual network controls through the bundled app-context activity-manager bridge; no guest NetworkManager is used to control the physical device interface.

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
