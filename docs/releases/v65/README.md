# V65 artifact freeze

V65 is the last recorded successfully built Alpine on Android APK from the recovered development history.

This freeze preserves the **identity of the known V65 artifacts** without storing another ~423 MB of duplicate binary data in Git. The large APK and bootstrap remain checksummed release assets and can be fetched on demand.

This is an artifact freeze, not a claim that the current recovered source tree is byte-for-byte the historical V65 source tree. The recovered Git history begins from a later recovery snapshot and the exact old source commit was not preserved as a normal Git commit.

## Frozen identity

- Android version name: `0.133.0`
- Android version code: `133`
- APK asset: `alpine-ultimate-v65-apk-commitfix.apk`
- APK size: `218417722` bytes
- APK SHA-256: `2f68fc373dc445f4df7196339b2652216b7d34792bbfb22945c868ac724cee3c`
- Bootstrap asset: `historical-bootstrap-aarch64-v65.zip`
- Bootstrap size: `204982812` bytes
- Bootstrap SHA-256: `384457c8d5bb87a561e8846fa2b5f9c3ebbc03e18aab477c4fe273cea11ac1a2`
- Bootstrap markers: both recorded as `v65`
- APK signing certificate SHA-256: `B6:DA:01:48:0E:EF:D5:FB:F2:CD:37:71:B8:D1:02:1E:C7:91:30:4B:DD:6C:4B:F4:1D:3F:AA:BA:D4:8E:E5:E1`
- Recovery release tag containing the assets: `termux-recovery-2026-09-14`

The historical build output APK copies and the saved V65 APK were independently recorded with the same size and SHA-256. See `docs/recovery/current-artifact-checks.json` for the recovery evidence.

## On-demand retrieval

The files are intentionally not downloaded during normal development. To retrieve them only when needed:

```sh
python3 scripts/fetch-assets.py --name alpine-ultimate-v65-apk-commitfix.apk
python3 scripts/fetch-assets.py --name historical-bootstrap-aarch64-v65.zip
```

The downloader verifies the manifest size and SHA-256 before accepting the file.

## Freeze rule

Do not overwrite, rename, or republish a different binary under either frozen V65 asset name. Any future rebuild is a new reproducibility result and must be recorded separately, even if its visible Android version is set to V65.