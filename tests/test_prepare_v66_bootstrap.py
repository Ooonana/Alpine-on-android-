import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile


spec = importlib.util.spec_from_file_location(
    "prepare_v66",
    Path(__file__).resolve().parents[1] / "scripts/prepare-v66-bootstrap.py",
)
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)


class PrepareV66BootstrapTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.overlay = self.root / "bootstrap/v66-overlay"
        (self.overlay / "etc").mkdir(parents=True)
        (self.overlay / "lib").mkdir(parents=True)
        (self.overlay / "bin").mkdir(parents=True)
        root_overlay = self.overlay / prepare.ROOTFS_PREFIX / "etc"
        root_overlay.mkdir(parents=True)

        (self.overlay / "etc/bash.bashrc").write_bytes(b"#!/bin/sh\r\necho v66\r\n")
        for name in ("libtalloc.so", "libtalloc.so.2", "libtalloc.so.2.4.3"):
            (self.overlay / "lib" / name).write_bytes(b"ELF/data/data/com.alpine/files/usr/lib")
        (self.overlay / "bin/proot").write_bytes(b"ELF/data/data/com.alpine/files/usr/lib")
        (root_overlay / "nsswitch.conf").write_bytes(b"hosts: files dns\r\n")

        self.static_apk = b"fake-current-static-apk"
        prefix = prepare.ROOTFS_PREFIX
        self.fake_rootfs = {
            prefix + "etc/": (None, True),
            prefix + "etc/alpine-release": (b"3.24.1\n", False),
            prefix + "etc/os-release": (b"VERSION_ID=3.24.1\n", False),
            prefix + "etc/apk/remote-repositories": (b"https://example.invalid/alpine/v3.24/main\n", False),
            prefix + "etc/alpine-bootstrap-version": (b"v66\n", False),
            prefix + "sbin/apk.static": (self.static_apk, False),
            prefix + "usr/share/X11/xkb/rules/base": (b"xkb", False),
        }
        self.fake_symlinks = {"var/run": "../run"}

        self.zip_path = self.root / "bootstrap.zip"
        with zipfile.ZipFile(self.zip_path, "w") as z:
            files = {
                "etc/bash.bashrc": b"#!/bin/sh\necho old\n",
                "lib/libtalloc.so": b"ELF/data/data/com.termux/files/usr/lib",
                "lib/libtalloc.so.2": b"ELF/data/data/com.termux/files/usr/lib",
                "lib/libtalloc.so.2.4.3": b"ELF/data/data/com.termux/files/usr/lib",
                "bin/proot": b"ELF/data/data/com.alpine/files/usr/lib",
                "etc/alpine-bootstrap-version": b"v65\n",
                "SYMLINKS.txt": "host-target←./host-link\n".encode(),
                prefix + "old-stale-file": b"remove me",
                "keep": b"unchanged",
            }
            for name, data in files.items():
                info = zipfile.ZipInfo(name, (2025, 1, 1, 0, 0, 0))
                info.create_system = 3
                info.external_attr = 0o100700 << 16
                z.writestr(info, data)

        self.saved = {
            "OVERLAY": prepare.OVERLAY,
            "RECOVERED_BYTES": prepare.RECOVERED_BYTES,
            "RECOVERED_SHA256": prepare.RECOVERED_SHA256,
            "RECOVERED_HOST_LINEAGE_SHA256": prepare.RECOVERED_HOST_LINEAGE_SHA256,
            "build_rootfs": prepare.v66_rootfs.build_rootfs,
            "APK_STATIC_PAYLOAD_SHA256": prepare.v66_rootfs.APK_STATIC_PAYLOAD_SHA256,
        }
        prepare.OVERLAY = self.overlay
        prepare.RECOVERED_BYTES = self.zip_path.stat().st_size
        prepare.RECOVERED_SHA256 = hashlib.sha256(self.zip_path.read_bytes()).hexdigest()
        prepare.v66_rootfs.build_rootfs = lambda: (dict(self.fake_rootfs), dict(self.fake_symlinks))
        prepare.v66_rootfs.APK_STATIC_PAYLOAD_SHA256 = hashlib.sha256(self.static_apk).hexdigest()
        prepare.RECOVERED_HOST_LINEAGE_SHA256 = prepare.host_lineage_digest(self.zip_path)

    def tearDown(self):
        for name, value in self.saved.items():
            if name == "build_rootfs":
                prepare.v66_rootfs.build_rootfs = value
            elif name == "APK_STATIC_PAYLOAD_SHA256":
                prepare.v66_rootfs.APK_STATIC_PAYLOAD_SHA256 = value
            else:
                setattr(prepare, name, value)

    def test_rebuilds_rootfs_and_repairs_symlink_manifest(self):
        prepare.prepare(self.zip_path)
        prefix = prepare.ROOTFS_PREFIX
        with zipfile.ZipFile(self.zip_path) as z:
            self.assertEqual(z.read("etc/bash.bashrc"), b"#!/bin/sh\necho v66\n")
            self.assertEqual(z.read("etc/alpine-bootstrap-version"), b"v66\n")
            self.assertEqual(z.read(prefix + "etc/alpine-release"), b"3.24.1\n")
            self.assertEqual(z.read(prefix + "etc/nsswitch.conf"), b"hosts: files dns\n")
            self.assertNotIn(prefix + "old-stale-file", z.namelist())
            self.assertNotIn(prefix + "var/run", z.namelist())
            lines = z.read("SYMLINKS.txt").decode().splitlines()
            self.assertIn("host-target←./host-link", lines)
            self.assertIn(f"../run←./{prefix}var/run", lines)
            self.assertEqual(z.read("keep"), b"unchanged")
            self.assertNotIn(b"com.termux", z.read("lib/libtalloc.so"))

    def test_second_run_is_idempotent(self):
        prepare.prepare(self.zip_path)
        first = hashlib.sha256(self.zip_path.read_bytes()).hexdigest()
        prepare.prepare(self.zip_path)
        second = hashlib.sha256(self.zip_path.read_bytes()).hexdigest()
        self.assertEqual(first, second)

    def test_can_update_prepared_archive_after_overlay_change(self):
        prepare.prepare(self.zip_path)
        (self.overlay / "etc/bash.bashrc").write_bytes(b"#!/bin/sh\r\necho newer-v66\r\n")
        prepare.prepare(self.zip_path)
        with zipfile.ZipFile(self.zip_path) as z:
            self.assertEqual(z.read("etc/bash.bashrc"), b"#!/bin/sh\necho newer-v66\n")

    def test_refuses_unknown_host_lineage(self):
        prepare.prepare(self.zip_path)
        with zipfile.ZipFile(self.zip_path, "a") as z:
            z.writestr("unexpected-host-file", b"tamper")
        with self.assertRaises(SystemExit):
            prepare.prepare(self.zip_path)


if __name__ == "__main__":
    unittest.main()
