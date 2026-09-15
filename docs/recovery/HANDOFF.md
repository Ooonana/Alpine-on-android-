# Alpine Android app — development handoff

This is a recovered working-state description, not a report of a newly successful build. Scope is Android Alpine APK development only; Kindle Alpine/KUAL files are excluded. Read PACKING-PLAN.md for the proposed transfer scope and inventory. No transfer ZIP has been created yet.

## Intended product

A modified Termux-style Android app named Alpine (`com.alpine`) that starts an embedded Alpine Linux environment with proot automatically. A Display button should open an embedded Termux:X11 screen in the same APK. Alpine and the Android-side X server share tmp; package commands (`apk update`, `apk add`, `apk upgrade`) should work normally. The user wanted a functioning GUI and package upgrades, not commands that are simply disabled.

Android APK files and Alpine Linux .apk package files are different formats despite the shared suffix. The four small loose home .apk files are Alpine Linux packages; Downloads contains the Android app version history.

## Current files that matter most

- `alpine-build/app/src/main/java/com/alpine/app/AlpineInstaller.java`: embedded bootstrap installation and version refresh; currently requests v66.
- `alpine-build/app/src/main/cpp/bootstrap-aarch64.zip`: current modified bootstrap, 204,984,759 bytes; preserve this exact file. It includes v66 launcher changes but both on-disk marker files still read v65.
- `alpine-build/app/src/main/cpp/alpine-bootstrap-zip.S`, `alpine-bootstrap.c`, `Android.mk`: native embedding of the bootstrap into `libalpine-bootstrap.so`. Prior history demonstrated stale embedded payloads when native outputs were reused.
- `alpine-v66-work/`: 12-file patch overlay, NOT a complete rootfs. Contains the launcher, package wrapper, service-account changes and related patch inputs. Use the embedded ZIP plus this overlay as evidence of the unfinished work.
- Inside the bootstrap: `etc/bash.bashrc`; Alpine rootfs `usr/local/sbin/apk`, `usr/local/bin/start-x11`, `etc/passwd`, `etc/group`, `etc/shadow`, `etc/nsswitch.conf`; host `bin/proot`, `bin/proot-distro`, `bin/termux-x11` and libtalloc.
- `alpine-build/x11/`, `x11-stub/`, `terminal-emulator/`, `terminal-view/`, `alpine-shared/`: all required source modules. `x11` uses the `com.termux.x11` Java namespace inside the `com.alpine` APK.
- `termux-x11-src/`: upstream source checkout, commit d04adbc355d6b573085f00a64b65e1776c7c3f2f. Preserve its Git metadata and imported native library provenance.
- `alpine-build/app/testkey_untrusted.jks`: the bundled signing key referenced by debug build configuration; retain it for continuity. Do not replace signing configuration blindly.

## Version history recovered from the conversations

This table summarizes recorded development and user feedback. Historical claims of successful tests are not newly rerun tests.

| Stage | Recorded work and outcome |
|---|---|
| Alpineo/Vectras, February | Earlier VM-based app experiment, package com.alpineo.vm, display drawer work and signing/Firebase changes; Termux build tool problems. Separate from the later Termux fork. |
| Termux-to-Alpine fork | Original source under Downloads/alpine-app-master/alpine-app-master; renamed packages/paths and copied into alpine-build. |
| Early bootstraps through v47 | Package-prefix changes, login permissions, EEXIST symlinks, launch hooks, libtalloc/proot issues; moved toward proot-distro and an embedded rootfs. |
| v48/v49 | Rootfs extraction and symlink work; isolated launcher. Later discovered APK native bootstrap could be stale despite a newer standalone ZIP. |
| v50 | Forced native bootstrap regeneration and supplied missing etc/environment. Alpine launched; package-index network operations still failed with Function not implemented. |
| v51 | DNS/repository setup and installer bootstrap-version refresh. Switching HTTPS to HTTP did not eliminate the failure. |
| v52 | Static apk-tools v2.14.9 plus a wget-fed local package mirror; update/add worked in user reports, upgrade still hit maintainer-script fchdir failures. |
| v53 | Manual maintainer-script runner around apk --no-scripts; its own cd / failed. |
| v54 | Removed the cd / dependency using temporary root-directory symlinks. User reported success with the core Alpine side. |
| v55 | Drawer X11 button and external Termux:X11 companion command. |
| v56 | XKB data and default display :1; still external app. |
| v57 | Embedded X11 and x11-stub modules, libXlorie.so, internal Activity and command entry point; Display button. |
| v58 | X11 cwd/environment fixes and socket wait; clean-build fixes for generated Prefs.java and preservation of local bootstrap ZIP. Runtime cwd errors persisted. |
| v59 | Shared-tmp request file and host-side X11 launcher outside proot. |
| v60 | App-side Activity request watcher, removed cmd/am activity launch path, set host XKB_CONFIG_ROOT. |
| v61 | Fixed extra-keys layout resource collision/ClassCastException and stale socket cleanup. User showed a live X socket; xterm had font errors. |
| v62 | X server font paths and font-index preparation. |
| v63 | Visible wget progress and corrected launcher label. An older hardcoded v60 label had previously led to a wrong diagnosis about installed version. |
| v64 | Clean cached downloaded package payloads after successful transactions. |
| v65 | Avoid repeated .PKGINFO scans by resolving package filenames directly; print transaction phases. Last recorded successful build: 0.133.0/code 133. Later feedback reported DBus, service-account and syscall failures affecting desktop startup. |
| v66 unfinished | Service accounts/runtime dirs and nsswitch repairs, proot option changes, newer local proot/libtalloc, best-effort manual scripts. Build failed on Gradle transform cache/aapt2 execution. Current installer says v66; embedded marker files remain v65; existing APK outputs still exactly match v65. |

## Architecture and remaining checks

The Android app installs a Termux-compatible host prefix at `/data/data/com.alpine/files/usr`. The embedded Alpine rootfs is under `var/lib/proot-distro/installed-rootfs/alpine`. The host launcher enters it with shared tmp, using DISPLAY=:1. The Alpine helper writes a request in tmp; an Android-side bridge launches the X server, while app code opens the display Activity. Avoid assuming Android app_process or activity-shell commands will work when invoked inside proot.

The package wrapper feeds a local mirror with wget and runs static apk against local files. It manually executes package scripts to work around observed syscall failures. That is a significant behavioral workaround: successful package unpacking does not prove all service setup/triggers completed. v66 makes script failures best-effort; verify the actual DBus, account lookup and font behavior rather than treating suppressed errors as a fix.

The installer deletes and replaces its prefix when its bootstrap version check fails. That can replace packages/data inside the embedded rootfs. Preserve any important installed-app data before testing a refreshed bootstrap. Such private app data is not accessible from this Termux view and is not included in this source inventory.

## Destination computer setup — proposed, not executed

1. Extract the current source and history into a new directory. Keep the unmodified transferred copy as a reference. Initialize Git on a working copy because alpine-build currently has no repository history.
2. Install the Android build tools for the computer's operating system. The current local.properties points at Termux-specific SDK/NDK locations; replace those paths only in the working copy. The downloaded Android-hosted NDK is not a desktop toolchain.
3. Remove or replace the Termux-specific `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` in the working copy. Check the JDK/Gradle/AGP compatibility on the destination before building.
4. Recorded settings: wrapper Gradle 9.2.1; Android Gradle Plugin 8.13.2; compileSdk 34, targetSdk 28, minSdk 21; ndkVersion 29.0.14206865; ABI arm64-v8a; Android app code 133/name 0.133.0. Some prior builds reported NDK path/version warnings. These are observed pins, not a claim that the desktop build has been validated.
5. Resolve the v65/v66 marker inconsistency deliberately. Compare the current embedded bootstrap with standalone v65 and the v66 overlay. Do not discard the modified embedded ZIP or assume the overlay can regenerate it alone.
6. Preserve disabled automatic bootstrap downloading and the clean-task fix that keeps the embedded ZIP. A native clean rebuild is needed when testing new bootstrap content. Proposed build task is `./gradlew clean :app:assembleDebug` after desktop setup; it has not been run on the computer.
7. Verify that the resulting APK actually embeds the intended bootstrap and X11 native library; compare version markers, ZIP integrity, signing and alignment. Then test on the target Android device.
8. Runtime checks should cover launch, account lookup, DBus session startup, apk update/add/upgrade, X11 socket creation, simple X clients and font rendering, then the desktop session. Record failures separately from build success.

## Evidence and preservation status

`current-artifact-checks.json` records freshly calculated hashes for the current bootstrap, standalone v65 and all three matching v65 APK copies. Raw Codex JSONL validation passed for all 6,343 records. No Codex session repair has been attempted. The readable export is convenient, but the original JSONL and full Gemini archive should travel with the source because they preserve recorded tool calls, logs and compaction context.

The historical tar.zst archives were streamed and their contents enumerated successfully. Extracted Gemini chat/log copies were checked against SHA-256. The packing plan and inventory do not constitute a finished transfer backup; packaging and restored-file verification remain pending the user's requested review of the list.
