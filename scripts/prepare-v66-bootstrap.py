#!/usr/bin/env python3
"""Prepare the build bootstrap from the checksummed recovered V66 bootstrap.

The large recovered ZIP is intentionally kept out of Git.  `fetch-assets.py
--bootstrap` restores it.  This script then applies the small tracked V66
overlay, normalizes tracked shell/text files to Unix newlines, updates both
bootstrap version markers, and verifies the result before replacing the build
input atomically.
"""

from __future__ import annotations

import copy
import hashlib
from pathlib import Path
import os
import struct
import zipfile


ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP = ROOT / "android/app/src/main/cpp/bootstrap-aarch64.zip"
OVERLAY = ROOT / "bootstrap/v66-overlay"

RECOVERED_BYTES = 204_984_759
RECOVERED_SHA256 = "af4107177ffa0f0e4dd16b5f5d33f0543ef1b342e654ce4a7a42187b5bbf77b4"
RECOVERED_LINEAGE_SHA256 = "94f069e07de0271c4d7afe03ae185de7b16aab50bbce90224c4bdd55fe0ca6da"

VERSION_MARKERS = (
    "etc/alpine-bootstrap-version",
    "var/lib/proot-distro/installed-rootfs/alpine/etc/alpine-bootstrap-version",
)

TEXT_OVERLAY_SUFFIXES = {".sh", ".conf", ".rc"}
TEXT_OVERLAY_NAMES = {"bash.bashrc", "apk", "passwd", "group", "shadow", "nsswitch.conf", "start-x11"}


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def overlay_bytes(path: Path) -> bytes:
    data = path.read_bytes()
    if path.name in TEXT_OVERLAY_NAMES or path.suffix in TEXT_OVERLAY_SUFFIXES:
        # Git may check text files out as CRLF on Windows. Android shell files
        # in the bootstrap must stay LF-only, especially their shebang lines.
        data = data.replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    return data


def replacements() -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    for path in sorted(OVERLAY.rglob("*")):
        if path.is_file():
            result[path.relative_to(OVERLAY).as_posix()] = overlay_bytes(path)
    for marker in VERSION_MARKERS:
        result[marker] = b"v66\n"
    return result


def archive_lineage_digest(path: Path, mutable_names: set[str]) -> str:
    """Hash every immutable ZIP entry, including relevant ZIP metadata.

    This lets a previously prepared bootstrap be updated after an overlay edit
    without retaining a second 205 MB copy of the original archive. Only paths
    intentionally controlled by the V66 overlay/markers are excluded.
    """
    h = hashlib.sha256()
    with zipfile.ZipFile(path) as archive:
        infos = archive.infolist()
        if len(infos) != len({info.filename for info in infos}):
            raise RuntimeError("Bootstrap contains duplicate ZIP entry names")
        for info in sorted(infos, key=lambda item: item.filename):
            if info.filename in mutable_names:
                continue
            data = archive.read(info.filename)
            h.update(info.filename.encode("utf-8"))
            h.update(b"\0")
            h.update(
                struct.pack(
                    "<QII6H",
                    info.external_attr,
                    info.compress_type,
                    len(data),
                    *info.date_time,
                )
            )
            h.update(hashlib.sha256(data).digest())
    return h.hexdigest()


def verify_prepared(path: Path, wanted: dict[str, bytes]) -> None:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise RuntimeError("Bootstrap contains duplicate ZIP entry names")

        for name, expected in wanted.items():
            try:
                actual = archive.read(name)
            except KeyError as exc:
                raise RuntimeError(f"Prepared bootstrap is missing {name}") from exc
            if actual != expected:
                raise RuntimeError(f"Prepared bootstrap content mismatch: {name}")

        for marker in VERSION_MARKERS:
            if archive.read(marker) != b"v66\n":
                raise RuntimeError(f"Incorrect V66 marker: {marker}")

        for name in ("lib/libtalloc.so", "lib/libtalloc.so.2", "lib/libtalloc.so.2.4.3"):
            data = archive.read(name)
            if b"/data/data/com.termux/files/usr/lib" in data:
                raise RuntimeError(f"Legacy Termux RUNPATH remains in {name}")
            if b"/data/data/com.alpine/files/usr/lib" not in data:
                raise RuntimeError(f"Alpine RUNPATH not found in {name}")

        proot = archive.read("bin/proot")
        if b"/data/data/com.alpine/files/usr/lib" not in proot:
            raise RuntimeError("Alpine RUNPATH not found in bin/proot")

        for name, expected in wanted.items():
            if name in VERSION_MARKERS:
                continue
            if (name.endswith(".sh") or Path(name).name in TEXT_OVERLAY_NAMES) and b"\r\n" in expected:
                raise RuntimeError(f"CRLF remained in text overlay entry: {name}")


def is_prepared(path: Path, wanted: dict[str, bytes]) -> bool:
    try:
        verify_prepared(path, wanted)
        return True
    except (OSError, RuntimeError, zipfile.BadZipFile):
        return False


def prepare(source: Path = BOOTSTRAP) -> None:
    if not source.is_file():
        raise SystemExit("Bootstrap is missing. Run: python scripts/fetch-assets.py --bootstrap")

    wanted = replacements()
    if is_prepared(source, wanted):
        print("Verified already prepared V66 bootstrap:", source)
        print("SHA-256:", digest(source))
        return

    size = source.stat().st_size
    sha256 = digest(source)
    exact_recovered_asset = size == RECOVERED_BYTES and sha256 == RECOVERED_SHA256
    try:
        same_recovered_lineage = (
            archive_lineage_digest(source, set(wanted)) == RECOVERED_LINEAGE_SHA256
        )
    except (OSError, RuntimeError, zipfile.BadZipFile):
        same_recovered_lineage = False

    if not exact_recovered_asset and not same_recovered_lineage:
        raise SystemExit(
            "Refusing to modify an unknown bootstrap. It is neither the exact "
            "recovered asset nor a previously prepared archive with the verified "
            f"recovery lineage. Got {size} bytes / {sha256}."
        )

    temporary = source.with_name(source.name + ".v66.tmp")
    temporary.unlink(missing_ok=True)
    replaced: set[str] = set()

    try:
        with zipfile.ZipFile(source, "r") as src, zipfile.ZipFile(temporary, "w") as dst:
            if len(src.namelist()) != len(set(src.namelist())):
                raise RuntimeError("Recovered bootstrap contains duplicate ZIP entry names")

            for info in src.infolist():
                new_info = copy.copy(info)
                if info.filename in wanted:
                    data = wanted[info.filename]
                    replaced.add(info.filename)
                else:
                    data = src.read(info.filename)
                dst.writestr(new_info, data)

        missing = set(wanted) - replaced
        if missing:
            raise RuntimeError("Overlay paths missing from recovered bootstrap: " + ", ".join(sorted(missing)))

        verify_prepared(temporary, wanted)
        os.replace(temporary, source)
    except BaseException:
        temporary.unlink(missing_ok=True)
        raise

    print("Prepared V66 bootstrap:", source)
    print("Bytes:", source.stat().st_size)
    print("SHA-256:", digest(source))


if __name__ == "__main__":
    prepare()