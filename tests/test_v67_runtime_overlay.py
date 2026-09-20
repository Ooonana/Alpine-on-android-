from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
OVERLAY = ROOT / "bootstrap" / "v67-overlay"
BASHRC = OVERLAY / "etc" / "bash.bashrc"
ROOTFS = OVERLAY / "var/lib/proot-distro/installed-rootfs/alpine"
START_X11 = ROOTFS / "usr/local/bin/start-x11"
INSTALL_DESKTOP = ROOTFS / "usr/local/bin/install-desktop"
START_DESKTOP = ROOTFS / "usr/local/bin/start-desktop"
NSSWITCH = ROOTFS / "etc/nsswitch.conf"
INSTALLER = ROOT / "android/app/src/main/java/com/alpine/app/AlpineInstaller.java"
APP_BUILD = ROOT / "android/app/build.gradle"
GRADLE_PROPERTIES = ROOT / "android/gradle.properties"
GRADLE_WRAPPER_PROPERTIES = ROOT / "android/gradle/wrapper/gradle-wrapper.properties"
JITPACK = ROOT / "android/jitpack.yml"
ROOTFS_BUILDER = ROOT / "scripts/v67_rootfs.py"
PREPARE = ROOT / "scripts/prepare-v67-bootstrap.py"


class V67RuntimeOverlayTest(unittest.TestCase):
    def test_shell_sources_are_lf_only(self):
        for path in (BASHRC, START_X11, INSTALL_DESKTOP, START_DESKTOP, NSSWITCH):
            data = path.read_bytes()
            self.assertNotIn(b"\r", data, path)
            self.assertTrue(data.endswith(b"\n"), path)

    def test_v67_uses_stock_alpine_apk(self):
        installer = INSTALLER.read_text(encoding="utf-8")
        desktop = INSTALL_DESKTOP.read_text(encoding="utf-8")
        launcher = BASHRC.read_text(encoding="utf-8")
        builder = ROOTFS_BUILDER.read_text(encoding="utf-8")
        prepare = PREPARE.read_text(encoding="utf-8")

        self.assertFalse((ROOTFS / "usr/local/sbin/apk").exists())
        self.assertIn('ROOTFS_RELATIVE_PATH + "/sbin/apk"', installer)
        self.assertNotIn('ROOTFS_RELATIVE_PATH + "/sbin/apk.static"', installer)
        self.assertNotIn('ROOTFS_RELATIVE_PATH + "/usr/local/sbin/apk"', installer)
        self.assertIn('APK_CMD="${APK_CMD:-/sbin/apk}"', desktop)
        self.assertNotIn("apk.static", desktop)
        self.assertNotIn("apk-mirror", launcher)
        self.assertNotIn("remote-repositories", launcher)
        self.assertIn("https://dl-cdn.alpinelinux.org/alpine/v3.24/main", launcher)
        self.assertIn("OFFICIAL_REPOSITORIES", builder)
        self.assertNotIn("APK_STATIC_NAME", builder)
        self.assertIn('"etc/apk/repositories"', builder)
        self.assertIn("Legacy V66 apk wrapper is still embedded", prepare)
        self.assertIn("Stock Alpine /sbin/apk is missing", prepare)
        self.assertIn("Stock Alpine /sbin/apk SHA-256 mismatch", prepare)
        self.assertIn("STOCK_APK_SHA256", builder)

    def test_v67_historical_bootstrap_identity(self):
        builder = ROOTFS_BUILDER.read_text(encoding="utf-8")
        prepare = PREPARE.read_text(encoding="utf-8")
        self.assertIn('b"v67\\n"', builder)
        self.assertIn('b"v67\\n"', prepare)

    def test_v67_build_toolchain_defaults_match_validated_build(self):
        properties = GRADLE_PROPERTIES.read_text(encoding="utf-8")
        wrapper = GRADLE_WRAPPER_PROPERTIES.read_text(encoding="utf-8")
        jitpack = JITPACK.read_text(encoding="utf-8")
        self.assertIn("compileSdkVersion=36", properties)
        self.assertIn("ndkVersion=27.1.12297006", properties)
        self.assertIn("gradle-9.3.1-bin.zip", wrapper)
        self.assertIn('JITPACK_NDK_VERSION: "27.1.12297006"', jitpack)

    def test_desktop_installer_still_supports_all_choices(self):
        installer = INSTALL_DESKTOP.read_text(encoding="utf-8")
        launcher = START_DESKTOP.read_text(encoding="utf-8")
        for desktop in ("xfce", "lxqt", "lxde", "openbox", "mate", "plasma"):
            self.assertIn(desktop, installer)
            self.assertIn(desktop, launcher)
        self.assertIn('start-x11 "$DISPLAY"', launcher)
        self.assertIn("dbus-run-session", launcher)
        self.assertIn("LIBGL_ALWAYS_SOFTWARE", launcher)

    def test_launcher_keeps_proot_compatibility_without_package_manager_shim(self):
        text = BASHRC.read_text(encoding="utf-8")
        self.assertIn("ALPINE_DISABLE_LINK2SYMLINK", text)
        self.assertIn("ALPINE_DISABLE_SYSVIPC", text)
        self.assertEqual(text.count("--no-link2symlink"), 1)
        self.assertEqual(text.count("--no-sysvipc"), 1)
        self.assertIn("/system/bin/getprop", text)
        self.assertIn("ALPINE_DNS_SERVERS", text)
        self.assertIn("ALPINE_DNS_FORCE", text)
        self.assertIn('XDG_RUNTIME_DIR=/tmp/alpine-runtime-0', text)
        self.assertIn('RESOLV_CONF = b""', ROOTFS_BUILDER.read_text(encoding="utf-8"))
        self.assertIn("pid_identity_file_alive", text)
        self.assertIn("write_pid_identity", text)


if __name__ == "__main__":
    unittest.main()
