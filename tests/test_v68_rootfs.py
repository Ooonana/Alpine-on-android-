from pathlib import Path
import importlib.util
import tarfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("v68_rootfs_test_module", ROOT / "scripts/v68_rootfs.py")
v68_rootfs = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(v68_rootfs)


class FakeArchive:
    def __init__(self, names):
        self._members = [tarfile.TarInfo(name) for name in names]

    def getmembers(self):
        return list(self._members)


class V68RootfsPathSafetyTest(unittest.TestCase):
    def test_normalized_name_accepts_normal_relative_paths(self):
        self.assertEqual(v68_rootfs._normalized_name("./usr/bin/env/"), "usr/bin/env")
        self.assertEqual(v68_rootfs._normalized_name(".PKGINFO"), ".PKGINFO")
        self.assertEqual(v68_rootfs._normalized_name("."), "")
        self.assertEqual(v68_rootfs._normalized_name("./"), "")

    def test_normalized_name_rejects_archive_path_escape_forms(self):
        for name in (
            "../etc/passwd",
            "usr/../etc/passwd",
            "/etc/passwd",
            "usr\\bin\\env",
            "usr//bin/env",
            "usr/./bin/env",
            "usr/bin/evil\x00suffix",
        ):
            with self.subTest(name=name):
                with self.assertRaises(RuntimeError):
                    v68_rootfs._normalized_name(name)

    def test_members_by_name_rejects_normalized_collisions(self):
        archive = FakeArchive(["./usr/bin/env", "usr/bin/env"])
        with self.assertRaisesRegex(RuntimeError, "Duplicate normalized archive member"):
            v68_rootfs._members_by_name(archive)


if __name__ == "__main__":
    unittest.main()
