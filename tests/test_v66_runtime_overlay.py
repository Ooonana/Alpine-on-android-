from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
OVERLAY = ROOT / "bootstrap" / "v66-overlay"
BASHRC = OVERLAY / "etc" / "bash.bashrc"
APK_WRAPPER = (
    OVERLAY
    / "var/lib/proot-distro/installed-rootfs/alpine/usr/local/sbin/apk"
)
START_X11 = (
    OVERLAY
    / "var/lib/proot-distro/installed-rootfs/alpine/usr/local/bin/start-x11"
)
PROOT = OVERLAY / "bin" / "proot"


class V66RuntimeOverlayTest(unittest.TestCase):
    def test_shell_sources_are_lf_only(self):
        for path in (BASHRC, APK_WRAPPER, START_X11):
            data = path.read_bytes()
            self.assertNotIn(b"\r", data, path)
            self.assertTrue(data.endswith(b"\n"), path)

    def test_proot_uses_alpine_application_prefix(self):
        data = PROOT.read_bytes()
        self.assertNotIn(b"/data/data/com.termux", data)
        self.assertIn(b"/data/data/com.alpine/files/usr/lib", data)
        self.assertIn(b"/data/data/com.alpine/files/usr/tmp/", data)

    def test_launcher_keeps_android_proot_defaults(self):
        text = BASHRC.read_text(encoding="utf-8")
        self.assertIn("ALPINE_DISABLE_LINK2SYMLINK", text)
        self.assertIn("ALPINE_DISABLE_SYSVIPC", text)
        # These options may appear only in the explicit diagnostic opt-out blocks.
        self.assertEqual(text.count("--no-link2symlink"), 1)
        self.assertEqual(text.count("--no-sysvipc"), 1)
        self.assertIn("/system/bin/getprop", text)
        self.assertIn("ALPINE_DNS_SERVERS", text)

    def test_apk_wrapper_replays_suppressed_maintenance(self):
        text = APK_WRAPPER.read_text(encoding="utf-8")
        self.assertIn('"$action" --no-scripts', text)
        self.assertIn("run_commit_hooks pre-commit", text)
        self.assertIn("run_registered_triggers", text)
        self.assertIn("run_commit_hooks post-commit", text)
        self.assertIn('APK_SKIP_COMMIT_HOOKS', text)
        self.assertIn('APK_SKIP_TRIGGERS', text)
        self.assertIn('grep -E "\\\\.X1${script_hex}\\\\.trigger$"', text)


if __name__ == "__main__":
    unittest.main()
