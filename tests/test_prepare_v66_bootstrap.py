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
        rootfs = self.overlay / "var/lib/proot-distro/installed-rootfs/alpine/etc"
        rootfs.mkdir(parents=True)

        (self.overlay / "etc/bash.bashrc").write_bytes(b"#!/bin/sh\r\necho v66\r\n")
        for name in ("libtalloc.so", "libtalloc.so.2", "libtalloc.so.2.4.3"):
            (self.overlay / "lib" / name).write_bytes(b"ELF/data/data/com.alpine/files/usr/lib")
        (self.overlay / "bin/proot").write_bytes(b"ELF/data/data/com.alpine/files/usr/lib")

        self.zip_path = self.root / "bootstrap.zip"
        with zipfile.ZipFile(self.zip_path, "w") as z:
            for name, data in {
                "etc/bash.bashrc": b"#!/bin/sh\necho old\n",
                "lib/libtalloc.so": b"ELF/data/data/com.termux/files/usr/lib",
                "lib/libtalloc.so.2": b"ELF/data/data/com.termux/files/usr/lib",
                "lib/libtalloc.so.2.4.3": b"ELF/data/data/com.termux/files/usr/lib",
                "bin/proot": b"ELF/data/data/com.alpine/files/usr/lib",
                "etc/alpine-bootstrap-version": b"v65\n",
                "var/lib/proot-distro/installed-rootfs/alpine/etc/alpine-bootstrap-version": b"v65\n",
                "keep": b"unchanged",
            }.items():
                info = zipfile.ZipInfo(name)
                info.external_attr = 0o100700 << 16
                z.writestr(info, data)

        self.old_overlay = prepare.OVERLAY
        self.old_bytes = prepare.RECOVERED_BYTES
        self.old_hash = prepare.RECOVERED_SHA256
        self.old_lineage = prepare.RECOVERED_LINEAGE_SHA256
        prepare.OVERLAY = self.overlay
        prepare.RECOVERED_BYTES = self.zip_path.stat().st_size
        prepare.RECOVERED_SHA256 = hashlib.sha256(self.zip_path.read_bytes()).hexdigest()
        prepare.RECOVERED_LINEAGE_SHA256 = prepare.archive_lineage_digest(
            self.zip_path, set(prepare.replacements())
        )

    def tearDown(self):
        prepare.OVERLAY = self.old_overlay
        prepare.RECOVERED_BYTES = self.old_bytes
        prepare.RECOVERED_SHA256 = self.old_hash
        prepare.RECOVERED_LINEAGE_SHA256 = self.old_lineage

    def test_prepares_and_normalizes_bootstrap(self):
        prepare.prepare(self.zip_path)
        with zipfile.ZipFile(self.zip_path) as z:
            self.assertEqual(z.read("etc/bash.bashrc"), b"#!/bin/sh\necho v66\n")
            self.assertEqual(z.read("etc/alpine-bootstrap-version"), b"v66\n")
            self.assertEqual(
                z.read("var/lib/proot-distro/installed-rootfs/alpine/etc/alpine-bootstrap-version"),
                b"v66\n",
            )
            self.assertEqual(z.read("keep"), b"unchanged")
            self.assertNotIn(b"com.termux", z.read("lib/libtalloc.so"))

    def test_second_run_is_idempotent(self):
        prepare.prepare(self.zip_path)
        first = hashlib.sha256(self.zip_path.read_bytes()).hexdigest()
        prepare.prepare(self.zip_path)
        second = hashlib.sha256(self.zip_path.read_bytes()).hexdigest()
        self.assertEqual(first, second)

    def test_can_update_previously_prepared_archive_after_overlay_change(self):
        prepare.prepare(self.zip_path)
        (self.overlay / "etc/bash.bashrc").write_bytes(b"#!/bin/sh\r\necho newer-v66\r\n")
        prepare.prepare(self.zip_path)
        with zipfile.ZipFile(self.zip_path) as z:
            self.assertEqual(z.read("etc/bash.bashrc"), b"#!/bin/sh\necho newer-v66\n")

    def test_refuses_unknown_bootstrap(self):
        self.zip_path.write_bytes(b"not the recovered bootstrap")
        with self.assertRaises(SystemExit):
            prepare.prepare(self.zip_path)


if __name__ == "__main__":
    unittest.main()