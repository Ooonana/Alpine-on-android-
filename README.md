# Alpine on Android

An Android terminal app that automatically starts Alpine Linux through proot, with Termux:X11 integrated into the same APK. This repository now includes the recovered Termux development source and handoff documentation.

**Recovery snapshot: v65 was the last successfully built APK. v66 is unfinished work, not a tested release.** The installer and patched launcher say v66, but the embedded bootstrap's two marker files still say v65. Existing build-output APKs match the saved v65 APK. GUI/DBus/font problems remain under investigation.

## Source and history

- [`android/`](android/): the recovered app, terminal, shared-library and embedded X11 modules. Original licenses and the existing debug test key are retained.
- [`bootstrap/v66-overlay/`](bootstrap/v66-overlay/): the small unfinished v66 patch overlay. It is not a complete rootfs.
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
```

The downloader verifies size and SHA-256 and restores the bootstrap to `android/app/src/main/cpp/bootstrap-aarch64.zip`. It refuses to overwrite a different local file. The large bootstrap is a release asset because it exceeds GitHub's regular Git file limit.

Before building, follow the [handoff](docs/recovery/HANDOFF.md). Configure a desktop SDK/NDK in `android/local.properties`, and remove or replace the Termux-specific `android.aapt2FromMavenOverride` in your working copy. Check the recorded JDK/Gradle/AGP compatibility and resolve the v65/v66 marker mismatch. The transferred source has not been validated with a desktop build.

The recorded project uses arm64-v8a, Gradle 9.2.1, Android Gradle Plugin 8.13.2, compileSdk 34, targetSdk 28, minSdk 21, and NDK setting 29.0.14206865. Android-hosted Termux toolchain archives are historical references, not desktop SDK replacements.

## APKs and historical assets

Existing releases are retained. The `termux-recovery-2026-09-14` recovery release is an archive snapshot, not a new stable application version. Its v65 APK still has the recorded desktop/DBus issues; v66 has no completed APK.

List the recovery assets without downloading:

```sh
python3 scripts/fetch-assets.py --list
```

Download a specific asset with `--name NAME`, or use `--all` for the complete historical set. These files can require many gigabytes; consult the manifest first.

## Development history

`android-development-history.tar.gz` includes the original Codex JSONL/Markdown and the Gemini development chats and tool outputs. The original local files remain preserved. Credential files from the old Gemini directory are excluded from the public copy; any pattern-based redactions are recorded in [the publication review](docs/recovery/publication-review.json).

The Codex raw log parsed successfully, but the broken interactive session has not been repaired. The handoff summarizes recorded work and distinguishes previous claims from checks performed during recovery.
