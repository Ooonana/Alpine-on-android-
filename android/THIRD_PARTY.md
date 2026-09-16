# Third-party provenance

Alpine on Android includes code and binaries derived from other open-source projects. Rebranding user-facing text does not remove those origins or their license obligations.

## Termux app

- Upstream: https://github.com/termux/termux-app
- Role here: base Android terminal/application architecture plus shared, terminal-emulator, and terminal-view derived code.
- License: the Termux app is primarily GPLv3, with per-file/module exceptions documented by upstream (including MIT-licensed shared utilities and separately licensed terminal components where applicable).

The recovered fork changed package/application identifiers to `com.alpine`, but upstream copyright and license information must remain intact.

## Termux:X11

- Upstream: https://github.com/termux/termux-x11
- Recovered upstream commit: `d04adbc355d6b573085f00a64b65e1776c7c3f2f`
- Role here: embedded Android X server/display implementation in `x11/`, including the recovered `libXlorie.so` native library.
- License: GPLv3.

The embedded module intentionally retains technical identifiers such as the Java namespace `com.termux.x11`, upstream class names, intent/action names, and JNI symbol names. These identifiers are part of the integrated implementation and are not claims that Alpine on Android is an official Termux:X11 build.

User-facing labels may say **Alpine Display**, while help/source attribution continues to point to the Termux:X11 upstream project where appropriate.

## Terminal Emulator for Android

- Upstream: https://github.com/jackpal/Android-Terminal-Emulator
- Role here: terminal-view / terminal-emulator ancestry inherited through the Termux-derived codebase.
- License: Apache License 2.0 for the applicable upstream code.

When adding or replacing third-party code or binaries, record the exact upstream source/revision and license here instead of removing upstream namespaces or attribution.