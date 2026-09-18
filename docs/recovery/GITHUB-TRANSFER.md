# GitHub transfer layout

This replaces the earlier local ZIP-transfer proposal. The original packing plan is retained as inventory evidence, not a claim that a tablet ZIP or every upload already exists.

## Git-tracked files

`android/` is a source snapshot of `~/alpine-build`, excluding generated `build`, `.gradle` and `.cxx` directories, local SDK path settings and the large embedded bootstrap. `docs/recovery/local.properties.termux-reference` preserves the original SDK/NDK paths as a reference. No app behavior was changed during transfer preparation. The original source README is in `android/README.md`; it contains inherited/rebranded upstream text and should not be treated as authoritative release documentation.

The repository's original README is preserved in `original-repository-README.md`. Original source licenses remain in place. `bootstrap/v66-overlay/` preserves the partial v66 work separately.

## Release assets

The asset manifest maps each asset to its source and SHA-256. Exact byte-identical files are uploaded once and all their source aliases are listed. APK and bootstrap filenames are retained where possible; prefix changes avoid name collisions. Assets remain under GitHub's 2 GiB per-file release limit.

The current 205 MB bootstrap is named `bootstrap-aarch64-v66-incomplete.zip`. Its version-marker inconsistency is preserved rather than silently corrected. `scripts/fetch-assets.py --bootstrap` restores it to the source tree.

Historical assemblies and support/source directories are tar.gz archives so executable modes and symlinks survive the transfer. Old Alpine/bootstrap tar.zst archives are existing compressed history. Only the Alpineo/Vectras Android directories are repackaged from the mixed old-other archive. Generated Gradle caches and nested Git stores are omitted from that mixed-project export; those original stores remain on the tablet. The standalone Termux:X11 reference checkout is preserved in support assets.

Private signing-key formats (`.jks`, `.keystore`, `.p12`, `.pfx`) found in older source snapshots are deliberately omitted from the public release assets. The active app source retains its inherited `testkey_untrusted.jks`, which is an upstream debug key rather than a release credential.

## Conversations

The full original Gemini archive contains account credentials and extension configuration. It is retained locally and is not a public asset. The public history asset selects eight Android-related Gemini sessions, their tool outputs and a user-message log filtered by those session IDs, plus the recovered Codex session JSONL and Markdown. Public copies are scanned for known token/private-key patterns; redactions and omitted file paths are documented separately. This does not repair the interactive Codex session.

The `session-validation.json` hash refers to the original Codex raw session. If publication redactions affect its public copy, use the history archive checksum for transport verification and the publication review to understand differences. Do not claim a sanitized copy is byte-for-byte identical to the local original.

## Limits

Installed `com.alpine` app-private data could not be accessed from Termux. The source, embedded rootfs and APK history are preserved; files created later inside the installed app are not included. Kindle projects, unrelated old workspaces and global account credentials are outside the upload scope.

The source snapshot has not been rebuilt on a computer. Previous build/runtime diagnoses are historical evidence, not a newly successful end-to-end test. Uploaded bytes and checksums must be verified separately before declaring the transfer complete.
