#!/usr/bin/env python3
"""Build the Alpine rootfs portion of the V68 bootstrap from pinned upstream inputs."""
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

ALPINE_VERSION = "3.23.6"
ALPINE_MINIROOTFS_NAME = f"alpine-minirootfs-{ALPINE_VERSION}-aarch64.tar.gz"
ALPINE_MINIROOTFS_URL = f"https://dl-cdn.alpinelinux.org/alpine/v3.23/releases/aarch64/{ALPINE_MINIROOTFS_NAME}"
ALPINE_MINIROOTFS_BYTES = 4_026_314
ALPINE_MINIROOTFS_SHA256 = "b17a57958e29735ff0e6e64254d958e65903687a70ca30cc430b33a965ad49d7"
STOCK_APK_SHA256 = "d77b885bef835f8cade6fe3feba2fe504896e16ecfe599fa54352fef68a11d4d"

XKB_NAME = "xkeyboard-config-2.46-r0.apk"
XKB_URL = f"https://dl-cdn.alpinelinux.org/alpine/v3.23/main/aarch64/{XKB_NAME}"
XKB_BYTES = 562_889
XKB_SHA256 = "a2b701a8825f0b2d39b130649ba8c8e174e6db79b13738fe76af260d51097099"

OFFICIAL_REPOSITORIES = (
    "https://dl-cdn.alpinelinux.org/alpine/v3.23/main\n"
    "https://dl-cdn.alpinelinux.org/alpine/v3.23/community\n"
).encode()
# Leave DNS empty in the prepared rootfs so the first Android launch can seed
# it from the device. The launcher then preserves a user's non-empty file unless
# an explicit ALPINE_DNS_SERVERS/ALPINE_DNS_FORCE override is requested.
RESOLV_CONF = b""
ENVIRONMENT = (
    b"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n"
    b"HOME=/root\nTERM=xterm-256color\nLANG=C.UTF-8\n"
)

# These links are required for the minimal rootfs to function. The pinned
# minirootfs contains many more symlinks (primarily BusyBox applets); all of
# them are preserved through SYMLINKS.txt instead of being flattened into
# duplicate file payloads.
REQUIRED_RUNTIME_SYMLINKS = {
    "etc/mtab": "../proc/mounts",
    "var/run": "../run",
    "var/lock": "../run/lock",
    "var/spool/mail": "../mail",
    "var/spool/cron/crontabs": "../../../etc/crontabs",
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
    print("Downloading pinned V68 input:", name, flush=True)
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


def ensure_inputs() -> tuple[Path, Path]:
    return (
        _verified_download(ALPINE_MINIROOTFS_NAME, ALPINE_MINIROOTFS_URL, ALPINE_MINIROOTFS_BYTES, ALPINE_MINIROOTFS_SHA256),
        _verified_download(XKB_NAME, XKB_URL, XKB_BYTES, XKB_SHA256),
    )


def _normalized_name(name: str) -> str:
    if name in ("", ".", "./"):
        return ""
    if "\x00" in name or "\\" in name or name.startswith("/"):
        raise RuntimeError(f"Unsafe archive member path: {name!r}")
    while name.startswith("./"):
        name = name[2:]
    name = name.rstrip("/")
    if not name:
        return ""
    parts = name.split("/")
    if any(part in ("", ".", "..") for part in parts):
        raise RuntimeError(f"Unsafe archive member path: {name!r}")
    return "/".join(parts)


def _members_by_name(archive: tarfile.TarFile) -> dict[str, tarfile.TarInfo]:
    members: dict[str, tarfile.TarInfo] = {}
    for member in archive.getmembers():
        name = _normalized_name(member.name)
        if not name:
            continue
        if name in members:
            raise RuntimeError(f"Duplicate normalized archive member: {name}")
        members[name] = member
    return members


def build_rootfs() -> tuple[dict[str, tuple[bytes | None, bool, int]], dict[str, str]]:
    """Return ZIP entries and post-extraction symlinks for the V68 Alpine rootfs.

    Official Alpine symlinks are deliberately preserved instead of flattened
    to file contents. Alpine's minirootfs uses hundreds of BusyBox applet links;
    flattening them would duplicate the same binary hundreds of times and make
    installation slower and much larger. AlpineInstaller recreates every link
    from SYMLINKS.txt after regular files/directories have been extracted.
    """
    minirootfs, xkb_apk = ensure_inputs()
    entries: dict[str, tuple[bytes | None, bool, int]] = {}
    symlinks: dict[str, str] = {}

    with tarfile.open(minirootfs, "r:gz") as archive:
        members = _members_by_name(archive)
        for name, member in members.items():
            full = ROOTFS_PREFIX + name
            if member.isdir():
                entries[full + "/"] = (None, True, member.mode & 0o7777)
                continue
            if member.isfile():
                stream = archive.extractfile(member)
                if stream is None:
                    raise RuntimeError(f"Cannot read minirootfs file: {name}")
                entries[full] = (stream.read(), False, member.mode & 0o7777)
                continue
            if member.issym():
                if not member.linkname or "\0" in member.linkname or "\n" in member.linkname or "\r" in member.linkname:
                    raise RuntimeError(f"Unsafe Alpine symlink target: {name} -> {member.linkname!r}")
                target = member.linkname
                # Alpine uses many absolute links such as /bin/busybox. Convert
                # them to equivalent relative links so they are valid both in
                # the Android app's physical rootfs directory and after proot
                # makes that directory appear as '/'.
                if target.startswith("/"):
                    target = posixpath.relpath(target.lstrip("/"), posixpath.dirname(name) or ".")
                symlinks[name] = target
                continue
            raise RuntimeError(f"Unsupported minirootfs tar member type: {name} {member.type!r}")

    # Refresh the XKB payload used by the embedded Termux:X11 server. Do not
    # register it in Alpine's apk database; it is bootstrap support data.
    with tarfile.open(xkb_apk, "r:gz") as archive:
        for name, member in _members_by_name(archive).items():
            if not name.startswith("usr/share/X11/xkb"):
                continue
            full = ROOTFS_PREFIX + name
            if member.isdir():
                entries[full + "/"] = (None, True, member.mode & 0o7777)
            elif member.isfile():
                stream = archive.extractfile(member)
                if stream is None:
                    raise RuntimeError(f"Cannot read XKB payload: {name}")
                entries[full] = (stream.read(), False, member.mode & 0o7777)
        symlinks["usr/share/xkeyboard-config-2"] = "X11/xkb"

    # Bootstrap-owned runtime configuration. Package/user-specific files are
    # intentionally not preseeded here; package maintainer scripts should
    # create their own users/groups and machine-id when the packages install.
    # Keep Alpine's normal package manager path. V68 deliberately uses the
    # stock /sbin/apk from the official minirootfs and points it directly at
    # Alpine's HTTPS repositories instead of shadowing apk with an Android
    # compatibility wrapper or routing transactions through a local mirror.
    entries[ROOTFS_PREFIX + "etc/apk/repositories"] = (OFFICIAL_REPOSITORIES, False, 0o644)
    entries[ROOTFS_PREFIX + "etc/resolv.conf"] = (RESOLV_CONF, False, 0o644)
    entries[ROOTFS_PREFIX + "etc/environment"] = (ENVIRONMENT, False, 0o600)
    entries[ROOTFS_PREFIX + "etc/alpine-bootstrap-version"] = (b"v68.4\n", False, 0o644)

    for name, expected_target in REQUIRED_RUNTIME_SYMLINKS.items():
        if symlinks.get(name) != expected_target:
            raise RuntimeError(
                f"Required Alpine runtime symlink changed: {name} -> {symlinks.get(name)!r}; "
                f"expected {expected_target!r}"
            )
    if symlinks.get("usr/share/xkeyboard-config-2") != "X11/xkb":
        raise RuntimeError("XKB compatibility symlink is missing or incorrect")
    return entries, symlinks


def rootfs_symlink_lines(symlinks: dict[str, str]) -> list[str]:
    return [f"{target}←./{ROOTFS_PREFIX}{path}" for path, target in sorted(symlinks.items())]
