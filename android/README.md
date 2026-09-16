# Alpine on Android — recovered Android source

This directory contains the recovered Android application source for **Alpine on Android**.
The application keeps the package identity `com.alpine` for compatibility with existing installs and with the recovered bootstrap layout.

This is a fork/recovery project, not the upstream Alpine Linux Android application and not an official Termux project.

## Upstream-derived components

The Android terminal application is derived from the Termux app codebase and retains terminal/shared components from that project. The embedded graphical display is derived from **Termux:X11**.

The embedded X11 module intentionally retains implementation identifiers such as `com.termux.x11`, upstream class names, intent/action names, and native JNI symbols. Those are technical compatibility details and must not be blindly renamed. The user-facing embedded display is branded **Alpine Display**.

See [`THIRD_PARTY.md`](THIRD_PARTY.md) for provenance and license information.

## Recovery and build status

The root [`README.md`](../README.md) describes the recovered project state. [`docs/recovery/HANDOFF.md`](../docs/recovery/HANDOFF.md) contains the development history, V65/V66 status, bootstrap recovery details, and desktop build notes.

V65 is the last recorded successfully built APK. V66 is unfinished development work and must not be treated as a tested release merely because it compiles.

The embedded bootstrap is intentionally not downloaded by Gradle from legacy Termux/fork URLs. Restore the checksummed recovery asset from the repository root with:

```sh
python3 scripts/fetch-assets.py --bootstrap
```

Before building on a desktop, configure the local Android SDK/NDK for that machine and verify the recovered version/bootstrap markers.

## Project links

- Repository: https://github.com/Ooonana/Alpine-on-android-
- Issues: https://github.com/Ooonana/Alpine-on-android-/issues

Do not use legacy project, wiki, support, or donation links from the historical search-and-replace fork. They do not represent this project.