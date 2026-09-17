#!/usr/bin/env python3
"""Prepare the reproducible Alpine-on-Android V66 bootstrap.

The recovered 205 MB bootstrap is the trusted host-prefix source. V66 keeps that
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
import v66_rootfs

ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP = ROOT / "android/app/src/main/cpp/bootstrap-aarch64.zip"
OVERLAY = ROOT / "bootstrap/v66-overlay"
ROOTFS_PREFIX = v66_rootfs.ROOTFS_PREFIX

RECOVERED_BYTES = 204_984_759
RECOVERED_SHA256 = "af4107177ffa0f0e4dd16b5f5d33f0543ef1b342e654ce4a7a42187b5bbf77b4"
# Hash of every preserved host-prefix ZIP entry, excluding the Alpine rootfs,
# SYMLINKS.txt, and tracked V66 host-overlay paths. This permits safe reruns after
# rootfs/overlay edits without retaining a second 205 MB recovery copy.
RECOVERED_HOST_LINEAGE_SHA256 = "b6f43d4fe206c3feb8387e9f927f3d9c1b3e5f73798d0325ab83756bf8f07c6b"

TOP_VERSION_MARKER = "etc/alpine-bootstrap-version"
ROOTFS_VERSION_MARKER = ROOTFS_PREFIX + "etc/alpine-bootstrap-version"
SYMLINKS_NAME = "SYMLINKS.txt"
SYMLINK_DELIMITER = "←"

TEXT_OVERLAY_SUFFIXES = {".sh", ".conf", ".rc"}
TEXT_OVERLAY_NAMES = {"bash.bashrc", "apk", "nsswitch.conf", "start-x11"}
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
    result[TOP_VERSION_MARKER] = b"v66\n"
    return result


def _host_mutable_names() -> set[str]:
    return {name for name in overlay_entries() if not name.startswith(ROOTFS_PREFIX)} | {SYMLINKS_NAME}


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


def _rootfs_zip_info(name: str, is_dir: bool) -> zipfile.ZipInfo:
    actual_name = name if not is_dir or name.endswith("/") else name + "/"
    info = zipfile.ZipInfo(actual_name, FIXED_ZIP_TIME)
    info.create_system = 3
    info.compress_type = zipfile.ZIP_DEFLATED
    if is_dir:
        info.external_attr = (0o040700 << 16) | 0x10
    else:
        lower = "/" + actual_name.lower()
        executable = any(part in lower for part in ("/bin/", "/sbin/", "/lib/", "/libexec/"))
        info.external_attr = ((0o100700 if executable else 0o100600) << 16)
    return info


def _prepared_payload() -> tuple[dict[str, tuple[bytes | None, bool]], dict[str, bytes], list[str]]:
    rootfs, rootfs_symlinks = v66_rootfs.build_rootfs()
    overlays = overlay_entries()

    # Rootfs overlay wins over the pristine Alpine/XKB/static-apk generated data.
    for name, data in overlays.items():
        if name.startswith(ROOTFS_PREFIX):
            rootfs[name] = (data, False)
    rootfs[ROOTFS_VERSION_MARKER] = (b"v66\n", False)
    return rootfs, overlays, v66_rootfs.rootfs_symlink_lines(rootfs_symlinks)


def _updated_symlinks(existing: bytes, rootfs_lines: list[str]) -> bytes:
    text = existing.decode("utf-8")
    kept = []
    root_dest_prefix = "./" + ROOTFS_PREFIX
    for line in text.splitlines():
        if not line:
            continue
        parts = line.split(SYMLINK_DELIMITER, 1)
        if len(parts) != 2:
            raise RuntimeError(f"Malformed SYMLINKS.txt line: {line!r}")
        if parts[1].startswith(root_dest_prefix):
            continue
        kept.append(line)
    kept.extend(rootfs_lines)
    return ("\n".join(kept) + "\n").encode("utf-8")


def verify_prepared(path: Path) -> None:
    rootfs, overlays, rootfs_symlink_lines = _prepared_payload()
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise RuntimeError("Bootstrap contains duplicate ZIP entry names")
        name_set = set(names)

        for name, (expected, is_dir) in rootfs.items():
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
        root_lines = [line for line in symlinks if line.split(SYMLINK_DELIMITER, 1)[-1].startswith("./" + ROOTFS_PREFIX)]
        if root_lines != rootfs_symlink_lines:
            raise RuntimeError(f"Incorrect rootfs symlink manifest: {root_lines!r}")
        for line in rootfs_symlink_lines:
            destination = line.split(SYMLINK_DELIMITER, 1)[1][2:]
            if destination in name_set:
                raise RuntimeError(f"Rootfs symlink is incorrectly stored as a regular ZIP entry: {destination}")

        if archive.read(ROOTFS_PREFIX + "etc/alpine-release") != b"3.24.1\n":
            raise RuntimeError("Prepared rootfs is not Alpine 3.24.1")
        os_release = archive.read(ROOTFS_PREFIX + "etc/os-release")
        if b"VERSION_ID=3.24.1" not in os_release:
            raise RuntimeError("Prepared rootfs os-release is not Alpine 3.24.1")
        remote = archive.read(ROOTFS_PREFIX + "etc/apk/remote-repositories")
        if b"/v3.24/" not in remote or b"v3.23" in remote:
            raise RuntimeError("Prepared rootfs repository branch is incorrect")
        if any(name.startswith(ROOTFS_PREFIX + "var/cache/apk-mirror/") and not name.endswith("/") for name in names):
            raise RuntimeError("Stale local APK mirror payload is embedded")
        if any(name.startswith(ROOTFS_PREFIX + "var/cache/apk/") and not name.endswith("/") for name in names):
            raise RuntimeError("Stale Alpine APK cache payload is embedded")

        static_apk = archive.read(ROOTFS_PREFIX + "sbin/apk.static")
        if hashlib.sha256(static_apk).hexdigest() != v66_rootfs.APK_STATIC_PAYLOAD_SHA256:
            raise RuntimeError("Prepared bootstrap has the wrong apk.static")
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
        print("Verified already prepared V66 bootstrap:", source)
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
    temporary = source.with_name(source.name + ".v66.tmp")
    temporary.unlink(missing_ok=True)
    try:
        with zipfile.ZipFile(source, "r") as src, zipfile.ZipFile(temporary, "w", allowZip64=True) as dst:
            if len(src.namelist()) != len(set(src.namelist())):
                raise RuntimeError("Source bootstrap contains duplicate ZIP entry names")
            existing_symlinks = src.read(SYMLINKS_NAME)
            new_symlinks = _updated_symlinks(existing_symlinks, rootfs_symlink_lines)

            for info in src.infolist():
                name = info.filename
                if name.startswith(ROOTFS_PREFIX):
                    continue
                new_info = copy.copy(info)
                if name == SYMLINKS_NAME:
                    dst.writestr(new_info, new_symlinks)
                elif name in overlays:
                    dst.writestr(new_info, overlays[name])
                else:
                    dst.writestr(new_info, src.read(name))

            for name in sorted(rootfs):
                data, is_dir = rootfs[name]
                info = _rootfs_zip_info(name, is_dir)
                dst.writestr(info, b"" if data is None else data)

        verify_prepared(temporary)
        os.replace(temporary, source)
    except BaseException:
        temporary.unlink(missing_ok=True)
        raise

    print("Prepared V66 bootstrap:", source)
    print("Bytes:", source.stat().st_size)
    print("SHA-256:", digest(source))


if __name__ == "__main__":
    prepare()
