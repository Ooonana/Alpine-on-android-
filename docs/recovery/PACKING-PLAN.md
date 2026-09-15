> Historical local-transfer plan. For the current public GitHub scope, see [GITHUB-TRANSFER.md](GITHUB-TRANSFER.md). The original Gemini archive is retained locally because it contains account credentials; only selected chats/tool history are uploaded.

# Alpine transfer plan — inventory review

Scope: Android Alpine APK projects only. Kindle Alpine, KUAL, Kindle filesystem images and scripts are excluded. Exclusion does not mean deletion.

Status: inventory and recovery notes only. No new transfer ZIP has been created. Original project/history files have not been moved. The user requested exploration and a reviewable packing list before packaging.

Sizes below are logical source bytes, including duplicate versions. They are NOT predicted ZIP sizes. Symlinks were inventoried without following them into shared storage. The inventory covers identified Alpine-related items in Termux home and Downloads, historical workspace archives, and selected toolchain installers. It is not a full-device backup.

| Group | Size (GiB) | Top-level items |
|---|---:|---:|
| 01-current | 0.660 | 2 |
| 02-support | 0.035 | 18 |
| 03-assembly-history | 3.642 | 12 |
| 04-bootstrap-history | 2.971 | 24 |
| 05-existing-archives | 4.026 | 8 |
| 06-download-apks | 7.213 | 102 |
| 07-download-sources | 0.354 | 14 |
| 09-history | 0.010 | 2 |
| 10-optional-termux-toolchain | 0.347 | 4 |

Total inventoried source-container bytes: 20,680,134,998. This includes optional Termux toolchain installers and the full size of the mixed old-other source archive; only selected Android project members of that archive are proposed for packaging. Inventory file records: 44,815. Initial scan reported zero read errors.

## Proposed packages

1. **Current project ZIP, first priority:** `alpine-build/` (all source modules, local embedded bootstrap, native libraries, Gradle wrapper/configuration, bundled test signing key, existing final APKs and metadata), `alpine-v66-work/`, relevant support inputs, original Downloads `alpine-app-master/`, recovered history, and handoff documentation. About 0.8–1 GiB of inputs depending on whether duplicate APK copies are retained. Keep the existing source tree unmodified; record desktop changes in instructions.
2. **Bootstrap and assembly history:** all 24 loose home `bootstrap-aarch64*.zip` files, v52–v61 assembly folders, plus the two partial directories under the literal `~/~/` directory. Preserve links and executable modes. Do not replace a real assembly with the tiny v66 overlay: v66-work is only 12 files.
3. **APK history:** all 102 candidate Android APKs in Downloads, including branded, fixed, guaranteed, ultimate, LXQt, other Alpine variants and the companion Termux:X11 APK. Confirm the count from the inventory if scope changes. Keep filenames and map any exact duplicate copies with SHA-256 before deciding whether to store one copy. No historical APKs have been deleted.
4. **Existing historical archives:** include the Alpine-workdirs, bootstrap-ZIPs and Gemini-history archives, with their inventories. From `old-other-workspaces-20260510.tar.zst`, extract ONLY `Alpineo-Project/` and `Vectras-VM-Android-master-alpineo/` into an Android-project archive. Those two trees total 835,852,937 logical bytes. The original mixed archive is a source container, not a proposed whole-file payload. Leave its unrelated projects on the tablet.
5. **Android source copies:** original AlpineTerminal/Alpineo/Vectras sources, Alpine logos/logcat notes and the aarch64 Alpine minirootfs used for the Android app. Exclude all Kindle/KUAL scripts, the `alpine.ext3` image and the five generic `alpine*.zip` images: inspection found Kindle launcher/image contents.
6. **Optional Termux toolchain reference:** downloaded Android-hosted NDK archive, SDK tools and `installer.sh`. These preserve provenance but do not substitute for SDK/NDK binaries built for the destination computer.

## Space and transfer method

Cleanup freed 4,076,679,168 physical bytes (3.797 GiB); free space immediately afterward was 15,228,223,488 bytes (14.183 GiB). The recovered notes consume a small additional amount. See `cleanup.json` for the exact six removed cache/intermediate directories.

The complete candidate set exceeds current free space before compression. APKs, bootstrap ZIPs and old archives are already compressed, so a single conventional ZIP cannot be assumed to fit. First make the current-project package. Transfer historical packages in batches, or write them directly to a computer/external destination. Keep a reserve of at least 1 GiB on the tablet. Do not delete source/history to make room.

## Rechecked findings

- Main active tree: `~/alpine-build`, package `com.alpine`, arm64-v8a. It has no Git history of its own; preserving the entire source snapshot is essential.
- Original Termux fork: `Download/alpine-app-master/alpine-app-master`. Alpineo/Vectras was an earlier, distinct attempt and survives in the old-other archive and Downloads.
- Last recorded successful build: v65, Android version 0.133.0 / code 133. The two build-output APKs and Downloads v65 have the same SHA-256: `2f68fc373dc445f4df7196339b2652216b7d34792bbfb22945c868ac724cee3c`. This is an exact duplicate check, not a fresh runtime/signature test.
- Unfinished v66: installer constant and embedded launcher say v66; both embedded `alpine-bootstrap-version` files still say v65. The embedded ZIP differs from standalone v65. Preserve both. No v66 APK was found in the inventory.
- The original session reports DBus/user lookup/syscall and font failures, then a failed v66 build with Gradle/aapt2 problems. Do not label v66 working.
- `termux-x11-src` retains Git metadata at commit `d04adbc355d6b573085f00a64b65e1776c7c3f2f`; the app also contains integrated `x11` and `x11-stub` modules and imported native library.
- The four existing history archives were read through using zstd/tar successfully. Their top-level contents and sizes are recorded alongside this plan.

## History recovery and limits

- Raw Codex session `01a09b4d-15c7-7383-92af-1d60f1ac277e`: 6,343 JSON records, zero malformed JSON lines. Resume functionality has NOT been repaired or tested. Valid JSON alone does not prove the session is internally resumable.
- Original readable Markdown export and raw JSONL are copied under `history/`. Raw events preserve material omitted from the human-readable export, including recorded tool activity and compaction events.
- Gemini archive yielded 14 chat/log files under `recovered-gemini/` with original directory paths preserved and SHA-256 verified. Its full archive also preserves recorded tool-output files. Main chat: `.gemini/tmp/home/chats/session-2026-02-25T02-00-1c0d7989.json` and its JSONL continuation; recall sessions include `2f992b49` and `b66f872b`; earlier Alpineo work includes `f33b3601`.
- Reviewed the Codex user/assistant development timeline and the original Gemini request history. This is not a claim to have manually read every line of every historical tool log or to have proved all previous assistant diagnoses. Raw history is retained so those claims can be revisited.
- Installed app private data at `/data/data/com.alpine/files` is not visible from this Termux session. The plan backs up source and embedded rootfs, not any additional files installed or created inside the running Alpine app.

## Deliberate exclusions

- Kindle/KUAL items: `Download/alpine/`, `Download/alpine (1)/`, `alpine.zip`, `alpine (1).zip` through `alpine (4).zip`, `alpine_kindle_kual/`, `alpine_kindle_kual.zip`, and `create_kindle_alpine_image.sh`. These 10 items total 4,937,812,160 bytes (4.599 GiB). Their originals were not deleted. See `excluded-kindle-items.json`.

- Unrelated personal Downloads/photos, SSH keys, GitHub recovery codes and global Codex authentication/configuration.
- Regenerable npm/Node headers, generated Gradle caches, app intermediate outputs (already removed as authorized). Final APKs, sources and signing files were retained.
- Installed Debian/Fedora/Ubuntu/Ooonana proot environments; Ooonana identifies itself as Debian 13 and is a separate project.
- Installed Termux SDK (about 821 MiB) and NDK (about 1.8 GiB) are not required in the desktop source package. Record versions and retain downloaded installers as optional references. Do not delete these installed tools.
- `developer-tools/` and `workspace/` are separate Gemini extension projects, not the Alpine Android source.
- The mixed old-other archive also contains unrelated projects. It remains untouched on the tablet; package only the two selected Android project directories. No unrelated Boox, WebView, Debian or Kindle project is intended as payload.

## Verification before declaring the backup complete

Record source-to-archive paths and SHA-256 checksums; verify ZIP CRCs and archive contents; test extraction of the current project and scripts with Unix modes/symlinks preserved; compare restored hashes; confirm all listed companion archives reached the computer. A completed inventory is not a completed backup.

## Exact candidate list

The `old-other-workspaces` archive row is a source container only: include its two selected Android project trees, not the entire archive.

| Group | Source path | Bytes | Files/links |
|---|---|---:|---:|
| 01-current | `/data/data/com.termux/files/home/alpine-build` | 708,466,354 | 1,132 |
| 02-support | `/data/data/com.termux/files/home/alpine-debug-packages` | 2,794,236 | 681 |
| 02-support | `/data/data/com.termux/files/home/alpine-downloads` | 12,534,072 | 7 |
| 02-support | `/data/data/com.termux/files/home/alpine-inject` | 8,952,449 | 425 |
| 02-support | `/data/data/com.termux/files/home/alpine-refine` | 471 | 1 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v52-assembly` | 387,894,156 | 3,841 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v53-assembly` | 387,897,920 | 3,841 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v54-assembly` | 387,898,438 | 3,841 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v55-assembly` | 387,907,037 | 3,845 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v56-assembly` | 391,790,330 | 4,140 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v57-assembly` | 391,787,507 | 4,139 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v58-assembly` | 391,788,744 | 4,139 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v59-assembly` | 391,790,723 | 4,139 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v60-assembly` | 391,790,912 | 4,139 |
| 03-assembly-history | `/data/data/com.termux/files/home/alpine-v61-assembly` | 391,794,718 | 4,139 |
| 01-current | `/data/data/com.termux/files/home/alpine-v66-work` | 548,911 | 12 |
| 02-support | `/data/data/com.termux/files/home/audit-sources` | 405,610 | 35 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-branded-v2.zip` | 30,747,593 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-branded-v8.zip` | 30,775,664 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-branded.zip` | 30,747,937 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-final.zip` | 34,960,908 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-fixed.zip` | 30,638,495 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-lite.zip` | 30,997,716 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-mega.zip` | 34,927,735 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-ultimate-v29.zip` | 35,117,008 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-ultimate.zip` | 35,159,653 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v52.zip` | 203,885,963 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v53.zip` | 203,886,741 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v54.zip` | 203,886,918 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v55.zip` | 203,893,293 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v56.zip` | 205,205,269 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v57.zip` | 204,981,124 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v58.zip` | 204,981,498 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v59.zip` | 204,981,976 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v60.zip` | 204,982,050 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v61.zip` | 204,982,174 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v62.zip` | 204,982,559 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v63.zip` | 204,982,641 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v64.zip` | 204,982,705 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64-v65.zip` | 204,982,812 | 1 |
| 04-bootstrap-history | `/data/data/com.termux/files/home/bootstrap-aarch64.zip` | 30,747,888 | 1 |
| 02-support | `/data/data/com.termux/files/home/dpkg-check` | 293,080 | 2 |
| 02-support | `/data/data/com.termux/files/home/libncursesw-6.5_p20251123-r0.apk` | 161,252 | 1 |
| 02-support | `/data/data/com.termux/files/home/musl-1.2.5-r23.apk` | 416,104 | 1 |
| 02-support | `/data/data/com.termux/files/home/nano-8.7-r0.apk` | 164,344 | 1 |
| 02-support | `/data/data/com.termux/files/home/ncurses-terminfo-base-6.5_p20251123-r0.apk` | 21,839 | 1 |
| 02-support | `/data/data/com.termux/files/home/pd-bundle` | 9,107,645 | 451 |
| 02-support | `/data/data/com.termux/files/home/termux-x11-Loader.java` | 2,438 | 1 |
| 02-support | `/data/data/com.termux/files/home/termux-x11-README.md` | 9,161 | 1 |
| 02-support | `/data/data/com.termux/files/home/termux-x11-deb-control` | 331 | 2 |
| 02-support | `/data/data/com.termux/files/home/termux-x11-deb-extract` | 7,212 | 3 |
| 02-support | `/data/data/com.termux/files/home/termux-x11-nightly-1.03.01-0-all.deb` | 5,242 | 1 |
| 02-support | `/data/data/com.termux/files/home/termux-x11-src` | 3,227,296 | 185 |
| 03-assembly-history | `/data/data/com.termux/files/home/~/alpine-v46-assembly` | 8,702,507 | 419 |
| 03-assembly-history | `/data/data/com.termux/files/home/~/alpine-v47-assembly` | 0 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/gemini-20260510.tar.zst` | 15,778,304 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-alpine-workdirs-20260510.list` | 678 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-alpine-workdirs-20260510.tar.zst` | 1,175,551,905 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-bootstrap-zips-20260510.list` | 806 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-bootstrap-zips-20260510.tar.zst` | 1,013,970,575 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-other-workspaces-20260510.existing.list` | 202 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-other-workspaces-20260510.list` | 202 | 1 |
| 05-existing-archives | `/data/data/com.termux/files/home/workspace-archives/old-other-workspaces-20260510.tar.zst` | 2,118,081,358 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/Alpine.apk` | 45,922,125 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/AlpineLXQt-1.apk` | 47,601,631 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/AlpineLXQt-2.apk` | 47,601,631 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/AlpineLXQt-3.apk` | 61,966,303 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/AlpineLXQt-funcpatch.apk` | 60,237,795 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/AlpineLXQt.apk` | 45,922,125 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/AlpineTerminal-source-1.zip` | 21,443,705 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/AlpineTerminal-source.zip` | 21,443,705 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/Alpinelogo.png` | 8,902 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/Alpineo (rename of vectrass source code).zip` | 27,322,250 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/Vectras-VM-Android-master-alpineo` | 180,705,993 | 746 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/Vectras-VM-Android-master-alpineo.zip` | 29,618,695 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/alpine-app-master` | 33,572,348 | 343 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/alpine-app-master(termux modification).zip` | 31,148,617 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-automated-final.apk` | 68,664,459 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-branded-final.apk` | 68,664,459 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-branded-v2.apk` | 68,664,459 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-branded-v6.apk` | 68,664,459 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-branded-v7.apk` | 42,916,047 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-branded-v8.apk` | 42,916,047 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-branded-v9.apk` | 42,917,326 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-debug.apk` | 38,511,397 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-dpkg-patched-v26.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-final-full.apk` | 68,664,459 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-final-v1.apk` | 68,664,389 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-final-v14.apk` | 42,915,788 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-fixed-v2.apk` | 38,766,125 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-fixed-v3.apk` | 38,766,125 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-fixed-v4.apk` | 38,522,955 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-fixed-v5.apk` | 38,574,897 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-guaranteed-v24.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-guaranteed-v27.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-guaranteed-v28.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-guaranteed-v29.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-1.apk` | 18,469,001 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-10.apk` | 27,382,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-11.apk` | 27,382,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-12.apk` | 27,382,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-17.apk` | 27,382,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-19.apk` | 27,382,702 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-2.apk` | 22,225,191 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-25.apk` | 27,382,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-3.apk` | 25,909,761 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-4.apk` | 27,382,789 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-5.apk` | 27,382,793 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-6.apk` | 27,382,793 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-7.apk` | 27,382,793 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-8.apk` | 27,382,793 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt-9.apk` | 27,382,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-lxqt.apk` | 22,010,398 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/alpine-minirootfs-3.23.3-aarch64.tar.gz` | 4,040,567 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-pd-patched-v25.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-preinstalled-v27.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-proot-distro-v22.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-proot-distro-v23.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v10.apk` | 42,917,538 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v11.apk` | 42,917,346 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v12.apk` | 42,915,954 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v13.apk` | 42,916,628 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v15.apk` | 43,003,302 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v16.apk` | 43,003,302 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v17.apk` | 42,953,505 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v18.apk` | 42,953,505 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v19.apk` | 42,953,505 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v20.apk` | 43,002,413 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v21.apk` | 43,002,413 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v24.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v25.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v27.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v28.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v29.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v30.apk` | 42,954,049 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v31-final-verified.apk` | 47,112,286 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v31-final.apk` | 47,097,435 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v31.apk` | 47,097,435 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v32.apk` | 47,111,668 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v33-verified.apk` | 43,088,384 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v33.apk` | 47,111,668 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v34.apk` | 47,111,998 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v36.apk` | 43,088,384 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v37.apk` | 47,023,566 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v38.apk` | 47,023,124 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v39.apk` | 47,023,124 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v40.apk` | 47,023,493 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v41.apk` | 47,023,493 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v43.apk` | 47,099,590 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v44.apk` | 47,147,553 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v45.apk` | 47,147,553 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v46.apk` | 48,104,998 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v47.apk` | 48,104,789 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v48.apk` | 204,560,880 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v49.apk` | 204,560,880 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v50.apk` | 396,300,349 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v51.apk` | 204,563,000 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v52.apk` | 215,657,972 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v53.apk` | 215,658,639 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v54.apk` | 215,658,660 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v55-x11-button.apk` | 215,668,740 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v56-x11-docfix.apk` | 216,906,543 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v57-embedded-x11.apk` | 218,908,164 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v58-x11-cwdfix.apk` | 218,413,719 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v59-x11-hostbridge.apk` | 218,413,880 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v60-x11-appbridge.apk` | 218,414,600 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v61-x11-layoutfix.apk` | 218,417,401 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v62-x11-fontpath.apk` | 218,417,700 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v63-apk-progress.apk` | 218,417,708 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v64-apk-progress-cleanup.apk` | 218,417,706 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/alpine-ultimate-v65-apk-commitfix.apk` | 218,417,722 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/bootstrap-aarch64.zip` | 30,542,758 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/com.alpinelinux.org_aee-signed.apk` | 201,173,635 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/logcat-c&&logcat__E_grep-A20_com.alpinelinux.org_.txt` | 5,962 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/logcat__Elogcat_grepalpine.txt` | 2,776 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/logcat__Elogcat_grepcom.alpine.txt` | 3,057 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/ooonana-alpine.apk` | 21,774,374 | 1 |
| 06-download-apks | `/data/data/com.termux/files/home/storage/downloads/termux-x11-arm64-v8a-nightly.apk` | 5,017,278 | 1 |
| 07-download-sources | `/data/data/com.termux/files/home/storage/downloads/vectras_apk_analysis.md` | 9,139 | 1 |
| 09-history | `/data/data/com.termux/files/home/codex-session-01a09b4d-15c7-7383-92af-1d60f1ac277e.md` | 344,312 | 1 |
| 09-history | `/data/data/com.termux/files/home/.codex/sessions/2026/09/13/rollout-2026-09-13T15-05-15-01a09b4d-15c7-7383-92af-1d60f1ac277e.jsonl` | 10,325,437 | 1 |
| 02-support | `/data/data/com.termux/files/home/etc/motd` | 323 | 1 |
| 10-optional-termux-toolchain | `/data/data/com.termux/files/home/storage/downloads/installer.sh` | 8,236 | 1 |
| 10-optional-termux-toolchain | `/data/data/com.termux/files/home/storage/downloads/android-ndk-r29-aarch64.7z` | 348,595,391 | 1 |
| 10-optional-termux-toolchain | `/data/data/com.termux/files/home/storage/downloads/termux-sdk-tools-34.0.0.tar.xz` | 4,874,212 | 1 |
| 10-optional-termux-toolchain | `/data/data/com.termux/files/home/storage/downloads/termux-sdk-tools-34.0.0` | 18,772,736 | 8 |
