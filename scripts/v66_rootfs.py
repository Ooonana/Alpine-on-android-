#!/usr/bin/env python3
"""Build the Alpine rootfs portion of the V66 bootstrap from pinned upstream inputs."""
from __future__ import annotations

import hashlib
import os
import posixpath
from pathlib import Path
import tarfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
DOWNLOADS = ROOT / "downloads"
ROOTFS_PREFIX = "var/lib/proot-distro/installed-rootfs/alpine/"

ALPINE_VERSION = "3.24.1"
ALPINE_MINIROOTFS_NAME = f"alpine-minirootfs-{ALPINE_VERSION}-aarch64.tar.gz"
ALPINE_MINIROOTFS_URL = f"https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/{ALPINE_MINIROOTFS_NAME}"
ALPINE_MINIROOTFS_BYTES = 4_023_732
ALPINE_MINIROOTFS_SHA256 = "f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259"

XKB_NAME = "xkeyboard-config-2.47-r0.apk"
XKB_URL = f"https://dl-cdn.alpinelinux.org/alpine/v3.24/main/aarch64/{XKB_NAME}"
XKB_BYTES = 565_349
XKB_SHA256 = "a78c94324d215f874a6e6f29c3aa889f0755596ea952865f98ef64c06806a1c1"

APK_STATIC_NAME = "apk-tools-static-3.0.8-r0.apk"
APK_STATIC_URL = f"https://dl-cdn.alpinelinux.org/alpine/v3.24/main/aarch64/{APK_STATIC_NAME}"
APK_STATIC_BYTES = 2_541_447
APK_STATIC_SHA256 = "7e86f8258f3a97ea1c279105950ebf6c9af960891eb499f731b7e5f0af26ff63"
APK_STATIC_PAYLOAD_SHA256 = "0eb67be74a8894867cc7985e07457ad6b255da16ed79ec51a39c18618b5b31f9"

REMOTE_REPOSITORIES = (
    "https://dl-cdn.alpinelinux.org/alpine/v3.24/main\n"
    "https://dl-cdn.alpinelinux.org/alpine/v3.24/community\n"
).encode()
LOCAL_REPOSITORIES = b"/var/cache/apk-mirror/main\n/var/cache/apk-mirror/community\n"
RESOLV_CONF = b"nameserver 8.8.8.8\nnameserver 8.8.4.4\n"
ENVIRONMENT = (
    b"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n"
    b"HOME=/root\nTERM=xterm-256color\nLANG=C.UTF-8\n"
)

# These official Alpine links cannot safely be flattened to file bytes. They
# must be recreated by AlpineInstaller through SYMLINKS.txt after extraction.
EXPECTED_RUNTIME_SYMLINKS = {
    "etc/mtab": "../proc/mounts",
    "etc/ssl1.1/certs": "/etc/ssl/certs",
    "var/run": "../run",
    "var/lock": "../run/lock",
    "var/spool/mail": "../mail",
    "var/spool/cron/crontabs": "../../../etc/crontabs",
    "usr/share/xkeyboard-config-2": "X11/xkb",
}


def digest_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def digest_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def _verified_download(name: str, url: str, size: int, sha256: str) -> Path:
    DOWNLOADS.mkdir(parents=True, exist_ok=True)
    target = DOWNLOADS / name
    if target.exists():
        if target.stat().st_size == size and digest_file(target) == sha256:
            return target
        raise RuntimeError(f"Refusing mismatched cached input: {target}")

    partial = target.with_name(target.name + ".part")
    partial.unlink(missing_ok=True)
    print("Downloading pinned V66 input:", name, flush=True)
    try:
        with urllib.request.urlopen(url, timeout=90) as response, partial.open("wb") as output:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                output.write(chunk)
        if partial.stat().st_size != size or digest_file(partial) != sha256:
            raise RuntimeError(f"Downloaded size/SHA-256 mismatch: {name}")
        os.replace(partial, target)
    except BaseException:
        partial.unlink(missing_ok=True)
        raise
    return target


def ensure_inputs() -> tuple[Path, Path, Path]:
    return (
        _verified_download(ALPINE_MINIROOTFS_NAME, ALPINE_MINIROOTFS_URL, ALPINE_MINIROOTFS_BYTES, ALPINE_MINIROOTFS_SHA256),
        _verified_download(XKB_NAME, XKB_URL, XKB_BYTES, XKB_SHA256),
        _verified_download(APK_STATIC_NAME, APK_STATIC_URL, APK_STATIC_BYTES, APK_STATIC_SHA256),
    )


def _normalized_name(name: str) -> str:
    return name.lstrip("./").rstrip("/")


def _resolve_tar_symlink(members: dict[str, tarfile.TarInfo], name: str, seen: set[str] | None = None) -> tarfile.TarInfo | None:
    seen = set() if seen is None else seen
    if name in seen:
        raise RuntimeError(f"Symlink cycle in Alpine minirootfs: {name}")
    seen.add(name)
    member = members[name]
    if not member.issym():
        return member
    target = member.linkname
    if target.startswith("/"):
        target = target.lstrip("/")
    else:
        target = posixpath.normpath(posixpath.join(posixpath.dirname(name), target))
    if target not in members:
        return None
    return _resolve_tar_symlink(members, target, seen)


def build_rootfs() -> tuple[dict[str, tuple[bytes | None, bool]], dict[str, str]]:
    """Return ZIP entries and post-extraction symlinks for the V66 Alpine rootfs.

    Resolvable file symlinks are deliberately flattened to file contents to
    preserve the historical Alpine-on-Android bootstrap behavior. Directory or
    runtime/dangling symlinks are omitted from the ZIP and returned separately
    so AlpineInstaller can recreate them with Os.symlink().
    """
    minirootfs, xkb_apk, apk_static_apk = ensure_inputs()
    entries: dict[str, tuple[bytes | None, bool]] = {}
    symlinks: dict[str, str] = {}

    with tarfile.open(minirootfs, "r:gz") as archive:
        members = {_normalized_name(m.name): m for m in archive.getmembers() if _normalized_name(m.name)}
        for name, member in members.items():
            full = ROOTFS_PREFIX + name
            if member.isdir():
                entries[full + "/"] = (None, True)
                continue
            if member.isfile():
                stream = archive.extractfile(member)
                if stream is None:
                    raise RuntimeError(f"Cannot read minirootfs file: {name}")
                entries[full] = (stream.read(), False)
                continue
            if member.issym():
                resolved = _resolve_tar_symlink(members, name)
                if resolved is not None and resolved.isfile():
                    stream = archive.extractfile(resolved)
                    if stream is None:
                        raise RuntimeError(f"Cannot resolve minirootfs symlink: {name}")
                    entries[full] = (stream.read(), False)
                else:
                    symlinks[name] = member.linkname
                continue
            raise RuntimeError(f"Unsupported minirootfs tar member type: {name} {member.type!r}")

    # Refresh the XKB payload used by the embedded Termux:X11 server. Do not
    # register it in Alpine's apk database; it is bootstrap support data.
    with tarfile.open(xkb_apk, "r:gz") as archive:
        for member in archive.getmembers():
            name = _normalized_name(member.name)
            if not name.startswith("usr/share/X11/xkb"):
                continue
            full = ROOTFS_PREFIX + name
            if member.isdir():
                entries[full + "/"] = (None, True)
            elif member.isfile():
                stream = archive.extractfile(member)
                if stream is None:
                    raise RuntimeError(f"Cannot read XKB payload: {name}")
                entries[full] = (stream.read(), False)
        symlinks["usr/share/xkeyboard-config-2"] = "X11/xkb"

    # Current apk-tools static binary is used only against downloaded local
    # payloads. This avoids pairing Alpine 3.24 repositories with the old V52
    # static apk binary.
    with tarfile.open(apk_static_apk, "r:gz") as archive:
        stream = archive.extractfile("sbin/apk.static")
        if stream is None:
            raise RuntimeError("apk-tools-static package is missing sbin/apk.static")
        static_apk = stream.read()
    if digest_bytes(static_apk) != APK_STATIC_PAYLOAD_SHA256:
        raise RuntimeError("Unexpected sbin/apk.static payload hash")
    entries[ROOTFS_PREFIX + "sbin/apk.static"] = (static_apk, False)

    # Bootstrap-owned runtime configuration. Package/user-specific files are
    # intentionally not preseeded here; package maintainer scripts should
    # create their own users/groups and machine-id when the packages install.
    entries[ROOTFS_PREFIX + "etc/apk/repositories"] = (LOCAL_REPOSITORIES, False)
    entries[ROOTFS_PREFIX + "etc/apk/remote-repositories"] = (REMOTE_REPOSITORIES, False)
    entries[ROOTFS_PREFIX + "etc/resolv.conf"] = (RESOLV_CONF, False)
    entries[ROOTFS_PREFIX + "etc/environment"] = (ENVIRONMENT, False)
    entries[ROOTFS_PREFIX + "etc/alpine-bootstrap-version"] = (b"v66\n", False)

    if symlinks != EXPECTED_RUNTIME_SYMLINKS:
        raise RuntimeError(f"Unexpected Alpine runtime symlink set: {symlinks!r}")
    return entries, symlinks


def rootfs_symlink_lines(symlinks: dict[str, str]) -> list[str]:
    return [f"{target}←./{ROOTFS_PREFIX}{path}" for path, target in sorted(symlinks.items())]
