from pathlib import Path
import hashlib
import unittest


ROOT = Path(__file__).resolve().parents[1]
OVERLAY = ROOT / "bootstrap" / "v68-overlay"
BASHRC = OVERLAY / "etc" / "bash.bashrc"
PROOT_DISTRO = OVERLAY / "bin" / "proot-distro"
ROOTFS = OVERLAY / "var/lib/proot-distro/installed-rootfs/alpine"
START_X11 = ROOTFS / "usr/local/bin/start-x11"
INSTALL_DESKTOP = ROOTFS / "usr/local/bin/install-desktop"
START_DESKTOP = ROOTFS / "usr/local/bin/start-desktop"
NSSWITCH = ROOTFS / "etc/nsswitch.conf"
INSTALLER = ROOT / "android/app/src/main/java/com/alpine/app/AlpineInstaller.java"
APP_BUILD = ROOT / "android/app/build.gradle"
ALPINE_SHARED_BUILD = ROOT / "android/alpine-shared/build.gradle"
APP_MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"
REPORT_ACTIVITY = ROOT / "android/alpine-shared/src/main/java/com/alpine/shared/activities/ReportActivity.java"
TEXT_IO_ACTIVITY = ROOT / "android/alpine-shared/src/main/java/com/alpine/shared/activities/TextIOActivity.java"
GRADLE_PROPERTIES = ROOT / "android/gradle.properties"
GRADLE_WRAPPER_PROPERTIES = ROOT / "android/gradle/wrapper/gradle-wrapper.properties"
GRADLE_WRAPPER_JAR = ROOT / "android/gradle/wrapper/gradle-wrapper.jar"
ALPINE_ACTIVITY = ROOT / "android/app/src/main/java/com/alpine/app/AlpineActivity.java"
HELP_ACTIVITY = ROOT / "android/app/src/main/java/com/alpine/app/activities/HelpActivity.java"
X11_MAIN_ACTIVITY = ROOT / "android/x11/src/main/java/com/termux/x11/MainActivity.java"
X11_PREFERENCES = ROOT / "android/x11/src/main/java/com/termux/x11/LoriePreferences.java"
X11_VIEW = ROOT / "android/x11/src/main/java/com/termux/x11/LorieView.java"
X11_TOUCH = ROOT / "android/x11/src/main/java/com/termux/x11/input/TouchInputHandler.java"
JITPACK = ROOT / "android/jitpack.yml"
ROOTFS_BUILDER = ROOT / "scripts/v68_rootfs.py"
PREPARE = ROOT / "scripts/prepare-v68-bootstrap.py"


class V68RuntimeOverlayTest(unittest.TestCase):
    def test_shell_sources_are_lf_only(self):
        for path in (BASHRC, PROOT_DISTRO, START_X11, INSTALL_DESKTOP, START_DESKTOP, NSSWITCH):
            data = path.read_bytes()
            self.assertNotIn(b"\r", data, path)
            self.assertTrue(data.endswith(b"\n"), path)

    def test_v68_uses_stock_alpine_apk(self):
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
        self.assertIn("https://dl-cdn.alpinelinux.org/alpine/v3.23/main", launcher)
        self.assertIn("OFFICIAL_REPOSITORIES", builder)
        self.assertNotIn("APK_STATIC_NAME", builder)
        self.assertIn('"etc/apk/repositories"', builder)
        self.assertIn("Legacy V66 apk wrapper is still embedded", prepare)
        self.assertIn("Stock Alpine /sbin/apk is missing", prepare)
        self.assertIn("Stock Alpine /sbin/apk SHA-256 mismatch", prepare)
        self.assertIn("STOCK_APK_SHA256", builder)

    def test_v68_version_identity(self):
        installer = INSTALLER.read_text(encoding="utf-8")
        build = APP_BUILD.read_text(encoding="utf-8")
        builder = ROOTFS_BUILDER.read_text(encoding="utf-8")
        prepare = PREPARE.read_text(encoding="utf-8")
        self.assertIn('BOOTSTRAP_VERSION = "v68.2"', installer)
        self.assertIn("versionCode 138", build)
        self.assertIn('versionName "0.135.2-v68-dev"', build)
        self.assertIn('ALPINE_VERSION = "3.23.6"', builder)
        self.assertIn('b"v68.2\\n"', builder)
        self.assertIn('b"v68.2\\n"', prepare)

    def test_v68_build_toolchain_defaults_match_validated_build(self):
        properties = GRADLE_PROPERTIES.read_text(encoding="utf-8")
        wrapper = GRADLE_WRAPPER_PROPERTIES.read_text(encoding="utf-8")
        jitpack = JITPACK.read_text(encoding="utf-8")
        self.assertIn("compileSdkVersion=36", properties)
        self.assertIn("minSdkVersion=24", properties)
        self.assertIn("ndkVersion=27.1.12297006", properties)
        self.assertIn("gradle-9.3.1-bin.zip", wrapper)
        self.assertIn("distributionSha256Sum=b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06", wrapper)
        self.assertEqual(
            hashlib.sha256(GRADLE_WRAPPER_JAR.read_bytes()).hexdigest(),
            "b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13",
        )
        self.assertIn('JITPACK_NDK_VERSION: "27.1.12297006"', jitpack)

    def test_security_sensitive_dependencies_and_report_deserialization_are_hardened(self):
        app_build = APP_BUILD.read_text(encoding="utf-8")
        shared_build = ALPINE_SHARED_BUILD.read_text(encoding="utf-8")
        manifest = APP_MANIFEST.read_text(encoding="utf-8")
        report = REPORT_ACTIVITY.read_text(encoding="utf-8")

        for build in (app_build, shared_build):
            self.assertIn("com.google.guava:guava:33.3.1-android", build)
            self.assertIn("com.android.tools:desugar_jdk_libs_nio:2.0.4", build)
            self.assertNotIn("com.google.guava:guava:24.1-jre", build)
            self.assertNotIn("com.android.tools:desugar_jdk_libs:1.1.5", build)
        self.assertIn("commons-io:commons-io:2.16.1", shared_build)
        self.assertIn('android:name=".shared.activities.ReportActivity"', manifest)
        report_manifest_block = manifest[manifest.index('android:name=".shared.activities.ReportActivity"'):]
        self.assertIn('android:exported="false"', report_manifest_block.split('/>')[0])
        self.assertNotIn("android.permission.REQUEST_INSTALL_PACKAGES", manifest)
        self.assertNotIn("com.android.alarm.permission.SET_ALARM", manifest)
        self.assertIn("getSafeReportInfoFilePath", report)
        self.assertIn("Refusing to deserialize ReportInfo", report)
        file_utils = (ROOT / "android/alpine-shared/src/main/java/com/alpine/shared/file/FileUtils.java").read_text(encoding="utf-8")
        self.assertNotIn("RecursiveDeleteOption", file_utils)
        self.assertNotIn("com.google.common.io.MoreFiles", file_utils)

    def test_help_webview_is_remote_content_hardened(self):
        help_activity = HELP_ACTIVITY.read_text(encoding="utf-8")
        self.assertIn("settings.setJavaScriptEnabled(false)", help_activity)
        self.assertIn("settings.setAllowFileAccess(false)", help_activity)
        self.assertIn("settings.setAllowContentAccess(false)", help_activity)
        self.assertIn("WebSettings.MIXED_CONTENT_NEVER_ALLOW", help_activity)
        self.assertNotIn("addJavascriptInterface", help_activity)

    def test_text_io_extra_namespace_uses_its_own_activity(self):
        text_io = TEXT_IO_ACTIVITY.read_text(encoding="utf-8")
        self.assertIn("TextIOActivity.class.getCanonicalName()", text_io)
        self.assertNotIn("ReportActivity.class.getCanonicalName()", text_io)

    def test_api24_terminal_floor_does_not_launch_api26_display(self):
        activity = ALPINE_ACTIVITY.read_text(encoding="utf-8")
        guard = "if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)"
        self.assertIn(guard, activity)
        self.assertIn("error_display_requires_android_8", activity)
        self.assertLess(activity.index(guard), activity.index("sendStartX11CommandToCurrentSession()"))

        start_x11 = START_X11.read_text(encoding="utf-8")
        self.assertIn("ro.build.version.sdk", start_x11)
        self.assertIn('sdk="${ALPINE_ANDROID_SDK:-}"', start_x11)
        self.assertIn("/data/data/com.alpine/files/usr/bin/getprop", start_x11)
        self.assertIn('[ "$android_sdk" -lt 26 ]', start_x11)
        self.assertIn("Alpine Display requires Android 8.0 (API 26) or newer", start_x11)
        for path in (X11_MAIN_ACTIVITY, X11_PREFERENCES, X11_VIEW, X11_TOUCH):
            text = path.read_text(encoding="utf-8")
            self.assertNotIn("@RequiresApi(Build.VERSION_CODES.O)", text, path)

    def test_desktop_installer_still_supports_all_choices(self):
        installer = INSTALL_DESKTOP.read_text(encoding="utf-8")
        launcher = START_DESKTOP.read_text(encoding="utf-8")
        for desktop in ("xfce", "lxqt", "openbox", "mate", "plasma"):
            self.assertIn(desktop, installer)
            self.assertIn(desktop, launcher)
        self.assertNotIn("lxde", installer)
        self.assertNotIn("lxde", launcher)
        self.assertIn('start-x11 "$DISPLAY"', launcher)
        self.assertIn("dbus-run-session", launcher)
        self.assertIn("LIBGL_ALWAYS_SOFTWARE", launcher)

    def test_launcher_keeps_proot_compatibility_without_package_manager_shim(self):
        text = BASHRC.read_text(encoding="utf-8")
        self.assertIn("# Alpine Auto-Launch (v68)", text)
        self.assertNotIn("export PROOT_NO_SECCOMP=1", text)
        self.assertNotIn("export PROOT_FORCE_NO_SECCOMP=1", text)
        self.assertIn("PROOT_NO_SECCOMP=1", text)
        self.assertIn("ALPINE_DISABLE_LINK2SYMLINK", text)
        self.assertIn("ALPINE_DISABLE_SYSVIPC", text)
        self.assertEqual(text.count("--no-link2symlink"), 1)
        self.assertEqual(text.count("--no-sysvipc"), 1)
        self.assertIn("/system/bin/getprop", text)
        self.assertIn('"$PREFIX/bin/getprop" ro.build.version.sdk', text)
        self.assertIn('ALPINE_ANDROID_SDK="$(detect_android_sdk', text)
        self.assertIn('--env ALPINE_ANDROID_SDK="$ALPINE_ANDROID_SDK"', text)
        self.assertIn('ALPINE_ANDROID_SDK="${ALPINE_ANDROID_SDK:-}"', text)
        self.assertIn("ALPINE_DNS_SERVERS", text)
        self.assertIn("ALPINE_DNS_FORCE", text)
        self.assertIn('XDG_RUNTIME_DIR=/tmp/alpine-runtime-0', text)
        self.assertIn('RESOLV_CONF = b""', ROOTFS_BUILDER.read_text(encoding="utf-8"))
        self.assertIn("pid_identity_file_alive", text)
        self.assertIn("write_pid_identity", text)

    def test_v68_host_proot_runtime_is_current_and_prefix_patched(self):
        proot = (OVERLAY / "bin/proot").read_bytes()
        shmem = (OVERLAY / "lib/libandroid-shmem.so").read_bytes()
        self.assertIn(b"5.1.107.92", proot)
        self.assertIn(b"libandroid-shmem.so", proot)
        self.assertNotIn(b"/data/data/com.termux", proot)
        self.assertIn(b"/data/data/com.alpine/files/usr/lib", proot)
        self.assertNotIn(b"/data/data/com.termux", shmem)
        self.assertIn(b"/data/data/com.alpine/files/usr/lib", shmem)
        self.assertTrue((OVERLAY / "libexec/proot/loader").is_file())
        self.assertTrue((OVERLAY / "libexec/proot/loader32").is_file())

    def test_embedded_proot_distro_cli_is_restricted(self):
        text = PROOT_DISTRO.read_text(encoding="utf-8")
        self.assertIn('PROGRAM_VERSION="4.38.0"', text)
        self.assertIn("# ALPINE_ON_ANDROID_COMMAND_GATE", text)
        self.assertIn("Allowed commands: help, list, login.", text)

        dispatch = text[text.rfind("if [ $# -ge 1 ]; then"):]
        for allowed in ("command_help", "command_list", "command_login"):
            self.assertIn(allowed, dispatch)
        for blocked in (
            "command_backup", "command_install", "command_remove", "command_rename",
            "command_reset", "command_restore", "command_clear_cache", "command_copy",
        ):
            self.assertNotIn(blocked, dispatch)

        help_block = text[text.index("command_help() {"):text.index("\nshow_version() {")]
        for blocked_label in ("backup", "install", "remove", "rename", "reset", "restore", "clear-cache", "copy"):
            self.assertNotIn(f"  ${{GREEN}}{blocked_label}", help_block)

    def test_embedded_proot_distro_preserves_known_good_login_path(self):
        text = PROOT_DISTRO.read_text(encoding="utf-8")
        start = text.index("command_login() {")
        end = text.index("\ncommand_login_help() {", start)
        login = (text[start:end].rstrip("\n") + "\n").encode()
        self.assertEqual(
            hashlib.sha256(login).hexdigest(),
            "9ba2ea50600a84e87d6849c1611b9f2978acfc6de7e94a267d3c56d189203b2f",
        )

    def test_installer_requires_modern_proot_runtime_dependencies(self):
        installer = INSTALLER.read_text(encoding="utf-8")
        for required in (
            '"lib/libandroid-shmem.so"',
            '"lib/libtalloc.so.2"',
            '"libexec/proot/loader"',
        ):
            self.assertIn(required, installer)
        self.assertGreaterEqual(installer.count('"libexec/proot/loader"'), 2)


if __name__ == "__main__":
    unittest.main()
