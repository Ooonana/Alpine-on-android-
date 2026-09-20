#!/usr/bin/env python3
"""Prepare the reproducible Alpine-on-Android V67 bootstrap.

The recovered 205 MB bootstrap is the trusted host-prefix source. V67 keeps that
Termux-derived host prefix, rebuilds only the embedded Alpine rootfs from pinned
Alpine 3.24.1 inputs, applies the tracked compatibility overlay, repairs rootfs
symlinks through SYMLINKS.txt, and verifies the result before replacing the
build input atomically.
"""
from __future__ import annotations

import copy
import hashlib
import os
from pathlib import Path
import struct
import sys
import zipfile

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))
import v67_rootfs

ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP = ROOT / "android/app/src/main/cpp/bootstrap-aarch64.zip"
OVERLAY = ROOT / "bootstrap/v67-overlay"
ROOTFS_PREFIX = v67_rootfs.ROOTFS_PREFIX

RECOVERED_BYTES = 204_984_759
RECOVERED_SHA256 = "af4107177ffa0f0e4dd16b5f5d33f0543ef1b342e654ce4a7a42187b5bbf77b4"
# Hash of every preserved host-prefix ZIP entry, excluding the Alpine rootfs,
# SYMLINKS.txt, and tracked V67 host-overlay paths. This permits safe reruns after
# rootfs/overlay edits without retaining a second 205 MB recovery copy.
RECOVERED_HOST_LINEAGE_SHA256 = "7d9a47e4757d9f405348e095384ba3eaf756e014071176187f920052cba31c2b"

# Obsolete V65-era host helpers that are no longer part of the V67 launch path.
# They are preserved in Git/recovery history, but omitted from new V67 bootstraps.
LEGACY_HOST_PATHS = {"bin/start-alpine.sh"}

TOP_VERSION_MARKER = "etc/alpine-bootstrap-version"
ROOTFS_VERSION_MARKER = ROOTFS_PREFIX + "etc/alpine-bootstrap-version"
SYMLINKS_NAME = "SYMLINKS.txt"
MODES_NAME = "MODES.txt"
SYMLINK_DELIMITER = "←"

TEXT_OVERLAY_SUFFIXES = {".sh", ".conf", ".rc"}
TEXT_OVERLAY_NAMES = {"bash.bashrc", "nsswitch.conf", "start-x11", "install-desktop", "start-desktop"}
FIXED_ZIP_TIME = (2026, 6, 13, 0, 0, 0)


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def overlay_bytes(path: Path) -> bytes:
    data = path.read_bytes()
    if path.name in TEXT_OVERLAY_NAMES or path.suffix in TEXT_OVERLAY_SUFFIXES:
        data = data.replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    return data


def overlay_entries() -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    for path in sorted(OVERLAY.rglob("*")):
        if path.is_file():
            result[path.relative_to(OVERLAY).as_posix()] = overlay_bytes(path)
    result[TOP_VERSION_MARKER] = b"v67\n"
    return result


def _host_mutable_names() -> set[str]:
    return (
        {name for name in overlay_entries() if not name.startswith(ROOTFS_PREFIX)}
        | {SYMLINKS_NAME, MODES_NAME}
        | LEGACY_HOST_PATHS
    )


def host_lineage_digest(path: Path) -> str:
    h = hashlib.sha256()
    mutable = _host_mutable_names()
    with zipfile.ZipFile(path) as archive:
        infos = archive.infolist()
        if len(infos) != len({info.filename for info in infos}):
            raise RuntimeError("Bootstrap contains duplicate ZIP entry names")
        for info in sorted(infos, key=lambda item: item.filename):
            if info.filename.startswith(ROOTFS_PREFIX) or info.filename in mutable:
                continue
            data = archive.read(info.filename)
            h.update(info.filename.encode("utf-8"))
            h.update(b"\0")
            h.update(struct.pack("<QII6H", info.external_attr, info.compress_type, len(data), *info.date_time))
            h.update(hashlib.sha256(data).digest())
    return h.hexdigest()


def _unix_zip_info(name: str, is_dir: bool, mode: int) -> zipfile.ZipInfo:
    actual_name = name if not is_dir or name.endswith("/") else name + "/"
    info = zipfile.ZipInfo(actual_name, FIXED_ZIP_TIME)
    info.create_system = 3
    info.compress_type = zipfile.ZIP_DEFLATED
    permissions = mode & 0o7777
    if is_dir:
        info.external_attr = ((0o040000 | permissions) << 16) | 0x10
    else:
        info.external_attr = ((0o100000 | permissions) << 16)
    return info


def _zip_permissions(info: zipfile.ZipInfo) -> int:
    raw_mode = (info.external_attr >> 16) & 0xFFFF
    if raw_mode == 0:
        return 0o700 if info.is_dir() else 0o600
    return raw_mode & 0o7777


def _validate_manifest_field(value: str, label: str, *, forbid_tab: bool = False, forbid_delimiter: bool = False) -> None:
    if not value or any(char in value for char in ("\0", "\n", "\r")):
        raise RuntimeError(f"Unsafe {label}: {value!r}")
    if forbid_tab and "\t" in value:
        raise RuntimeError(f"Unsafe {label}: {value!r}")
    if forbid_delimiter and SYMLINK_DELIMITER in value:
        raise RuntimeError(f"Unsafe {label}: {value!r}")


def _mode_manifest_bytes(modes: dict[str, int]) -> bytes:
    for name in modes:
        _validate_manifest_field(name, f"{MODES_NAME} path", forbid_tab=True)
    return "".join(f"{mode & 0o7777:04o}\t{name}\n" for name, mode in sorted(modes.items())).encode("utf-8")


def _parse_mode_manifest(data: bytes) -> dict[str, int]:
    result: dict[str, int] = {}
    for line in data.decode("utf-8").splitlines():
        if not line:
            continue
        parts = line.split("\t", 1)
        if len(parts) != 2 or not parts[0] or not parts[1]:
            raise RuntimeError(f"Malformed {MODES_NAME} line: {line!r}")
        mode_text, name = parts
        _validate_manifest_field(name, f"{MODES_NAME} path", forbid_tab=True)
        if name in result:
            raise RuntimeError(f"Duplicate {MODES_NAME} path: {name}")
        try:
            mode = int(mode_text, 8)
        except ValueError as exc:
            raise RuntimeError(f"Invalid mode in {MODES_NAME}: {mode_text!r}") from exc
        if mode < 0 or mode > 0o7777:
            raise RuntimeError(f"Out-of-range mode in {MODES_NAME}: {mode_text!r}")
        result[name] = mode
    return result


def _prepared_payload() -> tuple[dict[str, tuple[bytes | None, bool, int]], dict[str, bytes], list[str]]:
    rootfs, rootfs_symlinks = v67_rootfs.build_rootfs()
    overlays = overlay_entries()

    # Rootfs overlay wins over the pristine Alpine/XKB generated data.
    for name, data in overlays.items():
        if name.startswith(ROOTFS_PREFIX):
            existing = rootfs.get(name)
            if existing is not None:
                mode = existing[2]
            elif (
                name.endswith("/usr/local/bin/start-x11")
                or name.endswith("/usr/local/bin/install-desktop")
                or name.endswith("/usr/local/bin/start-desktop")
            ):
                mode = 0o755
            else:
                mode = 0o644
            rootfs[name] = (data, False, mode)
    rootfs[ROOTFS_VERSION_MARKER] = (b"v67\n", False, 0o644)
    return rootfs, overlays, v67_rootfs.rootfs_symlink_lines(rootfs_symlinks)


def _updated_symlinks(existing: bytes, rootfs_lines: list[str]) -> bytes:
    text = existing.decode("utf-8")
    kept = []
    root_dest_prefix = "./" + ROOTFS_PREFIX
    for line in text.splitlines():
        if not line:
            continue
        if line.count(SYMLINK_DELIMITER) != 1:
            raise RuntimeError(f"Malformed SYMLINKS.txt line: {line!r}")
        parts = line.split(SYMLINK_DELIMITER, 1)
        _validate_manifest_field(parts[0], "symlink target", forbid_delimiter=True)
        _validate_manifest_field(parts[1], "symlink destination", forbid_delimiter=True)
        if parts[1].startswith(root_dest_prefix):
            continue
        kept.append(line)
    for line in rootfs_lines:
        if line.count(SYMLINK_DELIMITER) != 1:
            raise RuntimeError(f"Malformed generated SYMLINKS.txt line: {line!r}")
        target, destination = line.split(SYMLINK_DELIMITER, 1)
        _validate_manifest_field(target, "generated symlink target", forbid_delimiter=True)
        _validate_manifest_field(destination, "generated symlink destination", forbid_delimiter=True)
        kept.append(line)
    return ("\n".join(kept) + "\n").encode("utf-8")


def verify_prepared(path: Path) -> None:
    rootfs, overlays, rootfs_symlink_lines = _prepared_payload()
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise RuntimeError("Bootstrap contains duplicate ZIP entry names")
        name_set = set(names)
        stale_host_paths = sorted(LEGACY_HOST_PATHS & name_set)
        if stale_host_paths:
            raise RuntimeError("Legacy host helpers are still embedded: " + ", ".join(stale_host_paths))

        mode_manifest = _parse_mode_manifest(archive.read(MODES_NAME))
        expected_mode_names = {name for name in names if name not in {SYMLINKS_NAME, MODES_NAME}}
        if set(mode_manifest) != expected_mode_names:
            missing = sorted(expected_mode_names - set(mode_manifest))
            extra = sorted(set(mode_manifest) - expected_mode_names)
            raise RuntimeError(
                f"{MODES_NAME} does not cover archive entries; missing={missing[:10]!r} extra={extra[:10]!r}"
            )

        for info in archive.infolist():
            if info.filename in {SYMLINKS_NAME, MODES_NAME}:
                continue
            if mode_manifest[info.filename] != _zip_permissions(info):
                raise RuntimeError(f"{MODES_NAME} disagrees with ZIP metadata: {info.filename}")

        for name, (expected, is_dir, expected_mode) in rootfs.items():
            if is_dir:
                if name not in name_set:
                    raise RuntimeError(f"Prepared bootstrap is missing rootfs directory: {name}")
            else:
                try:
                    actual = archive.read(name)
                except KeyError as exc:
                    raise RuntimeError(f"Prepared bootstrap is missing rootfs file: {name}") from exc
                if actual != expected:
                    raise RuntimeError(f"Prepared rootfs content mismatch: {name}")
            if mode_manifest[name] != (expected_mode & 0o7777):
                raise RuntimeError(f"Prepared rootfs mode mismatch: {name}")

        generated_names = set(rootfs)
        unexpected_rootfs = [
            name for name in names
            if name.startswith(ROOTFS_PREFIX) and name not in generated_names
        ]
        if unexpected_rootfs:
            raise RuntimeError("Unexpected stale rootfs entries remain: " + ", ".join(unexpected_rootfs[:20]))

        for name, expected in overlays.items():
            if name.startswith(ROOTFS_PREFIX):
                continue
            if archive.read(name) != expected:
                raise RuntimeError(f"Prepared host overlay mismatch: {name}")

        symlinks = archive.read(SYMLINKS_NAME).decode("utf-8").splitlines()
        for line in symlinks:
            if not line:
                continue
            if line.count(SYMLINK_DELIMITER) != 1:
                raise RuntimeError(f"Malformed final SYMLINKS.txt line: {line!r}")
            target, destination = line.split(SYMLINK_DELIMITER, 1)
            _validate_manifest_field(target, "final symlink target", forbid_delimiter=True)
            _validate_manifest_field(destination, "final symlink destination", forbid_delimiter=True)
        root_lines = [line for line in symlinks if line.split(SYMLINK_DELIMITER, 1)[-1].startswith("./" + ROOTFS_PREFIX)]
        if root_lines != rootfs_symlink_lines:
            raise RuntimeError(f"Incorrect rootfs symlink manifest: {root_lines!r}")
        for line in rootfs_symlink_lines:
            destination = line.split(SYMLINK_DELIMITER, 1)[1][2:]
            if destination in name_set:
                raise RuntimeError(f"Rootfs symlink is incorrectly stored as a regular ZIP entry: {destination}")

        if archive.read(ROOTFS_PREFIX + "etc/alpine-release") != b"3.24.1\n":
            raise RuntimeError("Prepared rootfs is not Alpine 3.24.1")
        # Alpine 3.24 keeps /etc/os-release as a symlink to /usr/lib/os-release.
        # The symlink itself is already verified above through SYMLINKS.txt, so
        # validate the canonical payload rather than assuming the old flattened
        # bootstrap representation.
        os_release = archive.read(ROOTFS_PREFIX + "usr/lib/os-release")
        if b"VERSION_ID=3.24.1" not in os_release:
            raise RuntimeError("Prepared rootfs os-release is not Alpine 3.24.1")
        repositories = archive.read(ROOTFS_PREFIX + "etc/apk/repositories")
        if repositories != v67_rootfs.OFFICIAL_REPOSITORIES:
            raise RuntimeError("Prepared rootfs does not use the official Alpine repositories directly")
        if ROOTFS_PREFIX + "etc/apk/remote-repositories" in name_set:
            raise RuntimeError("Legacy V66 remote-repositories compatibility file is still embedded")
        if ROOTFS_PREFIX + "usr/local/sbin/apk" in name_set:
            raise RuntimeError("Legacy V66 apk wrapper is still embedded")
        if ROOTFS_PREFIX + "sbin/apk.static" in name_set:
            raise RuntimeError("Legacy V66 apk.static compatibility payload is still embedded")
        stock_apk_name = ROOTFS_PREFIX + "sbin/apk"
        if stock_apk_name not in name_set:
            raise RuntimeError("Stock Alpine /sbin/apk is missing")
        stock_apk_sha256 = hashlib.sha256(archive.read(stock_apk_name)).hexdigest()
        if stock_apk_sha256 != v67_rootfs.STOCK_APK_SHA256:
            raise RuntimeError("Stock Alpine /sbin/apk SHA-256 mismatch")
        if any(name.startswith(ROOTFS_PREFIX + "var/cache/apk-mirror/") and not name.endswith("/") for name in names):
            raise RuntimeError("Stale local APK mirror payload is embedded")
        if any(name.startswith(ROOTFS_PREFIX + "var/cache/apk/") and not name.endswith("/") for name in names):
            raise RuntimeError("Stale Alpine APK cache payload is embedded")

        if not any(name.startswith(ROOTFS_PREFIX + "usr/share/X11/xkb/") for name in names):
            raise RuntimeError("Prepared bootstrap is missing XKB data")

        for name in ("lib/libtalloc.so", "lib/libtalloc.so.2", "lib/libtalloc.so.2.4.3"):
            data = archive.read(name)
            if b"/data/data/com.termux/files/usr/lib" in data:
                raise RuntimeError(f"Legacy Termux RUNPATH remains in {name}")
            if b"/data/data/com.alpine/files/usr/lib" not in data:
                raise RuntimeError(f"Alpine RUNPATH not found in {name}")
        proot = archive.read("bin/proot")
        if b"/data/data/com.alpine/files/usr/lib" not in proot:
            raise RuntimeError("Alpine RUNPATH not found in bin/proot")
        if b"/data/data/com.termux" in proot:
            raise RuntimeError("Legacy Termux application prefix remains in bin/proot")
        if b"/data/data/com.alpine/files/usr/tmp/" not in proot:
            raise RuntimeError("Alpine link2symlink temp prefix not found in bin/proot")

        for name, expected in overlays.items():
            if (name.endswith(".sh") or Path(name).name in TEXT_OVERLAY_NAMES) and b"\r\n" in expected:
                raise RuntimeError(f"CRLF remained in text overlay entry: {name}")


def is_prepared(path: Path) -> bool:
    try:
        verify_prepared(path)
        if host_lineage_digest(path) != RECOVERED_HOST_LINEAGE_SHA256:
            raise RuntimeError("Prepared bootstrap host lineage no longer matches recovery source")
        return True
    except (OSError, RuntimeError, KeyError, zipfile.BadZipFile):
        return False


def prepare(source: Path = BOOTSTRAP) -> None:
    if not source.is_file():
        raise SystemExit("Bootstrap is missing. Run: python scripts/fetch-assets.py --bootstrap")

    if is_prepared(source):
        print("Verified already prepared V67 bootstrap:", source)
        print("SHA-256:", digest(source))
        return

    size = source.stat().st_size
    sha256 = digest(source)
    exact_recovered_asset = size == RECOVERED_BYTES and sha256 == RECOVERED_SHA256
    try:
        same_recovered_host = host_lineage_digest(source) == RECOVERED_HOST_LINEAGE_SHA256
    except (OSError, RuntimeError, zipfile.BadZipFile):
        same_recovered_host = False
    if not exact_recovered_asset and not same_recovered_host:
        raise SystemExit(
            "Refusing to modify an unknown bootstrap. The preserved host prefix does not match "
            f"the recovered lineage. Got {size} bytes / {sha256}."
        )

    rootfs, overlays, rootfs_symlink_lines = _prepared_payload()
    temporary = source.with_name(source.name + ".v67.tmp")
    temporary.unlink(missing_ok=True)
    try:
        with zipfile.ZipFile(source, "r") as src, zipfile.ZipFile(temporary, "w", allowZip64=True) as dst:
            if len(src.namelist()) != len(set(src.namelist())):
                raise RuntimeError("Source bootstrap contains duplicate ZIP entry names")
            existing_symlinks = src.read(SYMLINKS_NAME)
            new_symlinks = _updated_symlinks(existing_symlinks, rootfs_symlink_lines)
            modes: dict[str, int] = {}

            for info in src.infolist():
                name = info.filename
                if name.startswith(ROOTFS_PREFIX):
                    continue
                if name in LEGACY_HOST_PATHS:
                    continue
                if name == MODES_NAME:
                    continue
                new_info = copy.copy(info)
                if name == SYMLINKS_NAME:
                    dst.writestr(new_info, new_symlinks)
                elif name in overlays:
                    dst.writestr(new_info, overlays[name])
                else:
                    dst.writestr(new_info, src.read(name))
                if name != SYMLINKS_NAME:
                    modes[name] = _zip_permissions(new_info)

            for name in sorted(rootfs):
                data, is_dir, mode = rootfs[name]
                info = _unix_zip_info(name, is_dir, mode)
                dst.writestr(info, b"" if data is None else data)
                modes[info.filename] = mode & 0o7777

            modes_info = _unix_zip_info(MODES_NAME, False, 0o600)
            dst.writestr(modes_info, _mode_manifest_bytes(modes))

        verify_prepared(temporary)
        os.replace(temporary, source)
    except BaseException:
        temporary.unlink(missing_ok=True)
        raise

    print("Prepared V67 bootstrap:", source)
    print("Bytes:", source.stat().st_size)
    print("SHA-256:", digest(source))


if __name__ == "__main__":
    prepare()
