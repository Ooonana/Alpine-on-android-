# Alpine on Android — recovered Android source

This directory contains the recovered Android application source for **Alpine on Android**.
The application keeps the package identity `com.alpine` for compatibility with existing installs and with the recovered bootstrap layout.

This is a fork/recovery project, not the upstream Alpine Linux Android application and not an official Termux project.

## Upstream-derived components

The Android terminal application is derived from the Termux app codebase and retains terminal/shared components from that project. The embedded graphical display is derived from **Termux:X11**.

The embedded X11 module intentionally retains implementation identifiers such as `com.termux.x11`, upstream class names, intent/action names, and native JNI symbols. Those are technical compatibility details and must not be blindly renamed. The user-facing embedded display is branded **Alpine Display**.

See [`THIRD_PARTY.md`](THIRD_PARTY.md) for provenance and license information.

## Recovery and build status

The root [`README.md`](../README.md) describes the recovered project state. [`docs/recovery/HANDOFF.md`](../docs/recovery/HANDOFF.md) contains the development history, V65/V66/V67 status, bootstrap recovery details, and desktop build notes.

V65 remains the frozen known artifact baseline. V70 is the active development line. It uses the reproducibly prepared Alpine 3.23.6 rootfs, Termux PRoot 5.1.107.92, stock Alpine `/sbin/apk`, and the integrated display. V69 through V70 retain the compact black/white/green input dock below the X11 surface with IME toggle, right click, Esc, Tab, sticky Ctrl/Alt/Meta, arrows, clipboard paste, and hide/show. `install-desktop` now offers XFCE, LXQt, Openbox, MATE, Plasma Desktop, LXDE compatibility, Plasma Mobile, Phosh, and GNOME; Plasma and Phosh use experimental nested Wayland sessions on the X11 display, while GNOME uses Alpine's GNOME Flashback/Metacity X11 session directly on Alpine Display. The embedded PRoot-Distro 4.38.0 CLI remains restricted to `login`, `list`, and `help`. V69.1 fixed nested-Xwayland socket permissions and added Plasma support packages. V69.2 forced the Phosh nested compositor to use the pixman renderer. V69.3 added lean Phosh session masks and Android process-protection diagnostics. V69.4 added live nested-Phosh resizing plus user-chosen mobile lock-screen password setup. V69.5 added `desktop-manager`, multi-desktop installs, per-desktop `apk` virtual package groups, default switching, and repair. V69.6 fixed the V69.5 interactive-parser bug and added Android-owned network handoff. V69.7 added managed DNS refresh, X11 session isolation, and a fallback background for missing stock GNOME wallpaper files. V70 removes V69.7's duplicate manual Squeekboard launch so `phosh-session` alone owns `sm.puri.OSK0`, adds GNOME as desktop profile 9, and adds targeted `desktop-manager upgrade` / `upgrade all` actions that upgrade selected desktop package groups without intentionally upgrading the whole Alpine rootfs. V70 migrates V68.1 through V69.7 in place while preserving installed packages and user data. Android runtime validation is still required for package transactions, D-Bus, inherited connectivity, network UI handoff, audio, OSK, GNOME Flashback, and the embedded display.

The embedded bootstrap is intentionally not downloaded by Gradle from legacy Termux/fork URLs. Restore the checksummed recovery asset from the repository root with:

```sh
python3 scripts/fetch-assets.py --bootstrap
python3 scripts/prepare-v68-bootstrap.py
```

Before building on another desktop, configure the local Android SDK/NDK for that machine and verify the prepared bootstrap. Do not reintroduce historical Termux-host SDK paths or blindly rename technical `com.termux.x11` identifiers.

## Project links

- Repository: https://github.com/Ooonana/Alpine-on-android-
- Issues: https://github.com/Ooonana/Alpine-on-android-/issues

Do not use legacy project, wiki, support, or donation links from the historical search-and-replace fork. They do not represent this project.
