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
INSTALL_DESKTOP = (
    OVERLAY
    / "var/lib/proot-distro/installed-rootfs/alpine/usr/local/bin/install-desktop"
)
START_DESKTOP = (
    OVERLAY
    / "var/lib/proot-distro/installed-rootfs/alpine/usr/local/bin/start-desktop"
)
NSSWITCH = (
    OVERLAY
    / "var/lib/proot-distro/installed-rootfs/alpine/etc/nsswitch.conf"
)
PROOT = OVERLAY / "bin" / "proot"
INSTALLER = ROOT / "android/app/src/main/java/com/alpine/app/AlpineInstaller.java"
BOOTSTRAP_JNI = ROOT / "android/app/src/main/cpp/alpine-bootstrap.c"
X11_MAIN_ACTIVITY = ROOT / "android/x11/src/main/java/com/termux/x11/MainActivity.java"
ALPINE_ACTIVITY = ROOT / "android/app/src/main/java/com/alpine/app/AlpineActivity.java"
APP_MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"
ALPINE_CONSTANTS = ROOT / "android/alpine-shared/src/main/java/com/alpine/shared/alpine/AlpineConstants.java"
X11_STRINGS = ROOT / "android/x11/src/main/res/values/strings.xml"


class V66RuntimeOverlayTest(unittest.TestCase):
    def test_shell_sources_are_lf_only(self):
        for path in (BASHRC, APK_WRAPPER, START_X11, INSTALL_DESKTOP, START_DESKTOP, NSSWITCH):
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
        self.assertIn("pid_identity_file_alive", text)
        self.assertIn("write_pid_identity", text)
        self.assertIn("Removing stale X11 socket", text)
        self.assertIn('if [ -d /sdcard ] && [ -r /sdcard ]; then', text)
        self.assertIn('"${optional_binds[@]}"', text)
        self.assertNotIn("        -b /sdcard \\\n", text)

    def test_start_x11_rejects_bad_display_and_detects_pid_reuse(self):
        text = START_X11.read_text(encoding="utf-8")
        self.assertIn("ERROR: invalid X11 display", text)
        self.assertIn("pid_identity_file_alive", text)
        self.assertIn('/proc/$pid/stat', text)
        self.assertIn('rm -f "$socket_path" "$server_pid_file"', text)

    def test_desktop_installer_and_launcher_cover_supported_choices(self):
        installer = INSTALL_DESKTOP.read_text(encoding="utf-8")
        launcher = START_DESKTOP.read_text(encoding="utf-8")

        for desktop in ("xfce", "lxqt", "lxde", "openbox", "mate", "plasma"):
            self.assertIn(desktop, installer)
            self.assertIn(desktop, launcher)

        self.assertIn("xfce4 xfce4-terminal", installer)
        self.assertIn("lxqt-desktop qterminal qt6-qtbase-x11", installer)
        self.assertIn("lxsession openbox pcmanfm lxpanel lxterminal lxappearance", installer)
        self.assertIn("openbox pcmanfm lxterminal tint2", installer)
        self.assertIn("mate-desktop-environment mate-terminal", installer)
        self.assertIn("plasma-desktop plasma-workspace kwin konsole", installer)
        self.assertIn('printf \'%s\\n\' "$desktop" > "$CONFIG_FILE"', installer)
        self.assertIn("startplasma-x11", launcher)
        self.assertIn("kwin_x11", launcher)
        self.assertIn("plasmashell", launcher)
        self.assertIn("LIBGL_ALWAYS_SOFTWARE", launcher)
        self.assertIn("dbus-run-session", launcher)
        self.assertIn('start-x11 "$DISPLAY"', launcher)

    def test_embedded_display_connection_retry_is_single_and_cancellable(self):
        text = X11_MAIN_ACTIVITY.read_text(encoding="utf-8")
        self.assertIn("private final Runnable mConnectionRetry = this::tryConnect", text)
        self.assertIn("private void scheduleConnectionRetry()", text)
        self.assertIn("handler.removeCallbacks(mConnectionRetry)", text)
        self.assertNotIn("handler.postDelayed(this::tryConnect, 250)", text)
        self.assertIn("prefs.get().unregisterOnSharedPreferenceChangeListener(preferencesChangedListener)", text)
        self.assertIn("if (instance == this)", text)
        self.assertIn("private float clampToVisibleBounds", text)
        self.assertIn("public void onBackPressed() {\n        finish();", text)
        self.assertIn("if (isFinishing() || isDestroyed())", text)
        self.assertIn("SDK_INT >= VERSION_CODES.O", text)
        self.assertIn('m == 4 ? "R"', text)
        self.assertNotIn("Map.of(", text)

    def test_apk_wrapper_prefers_android_host_curl_for_https(self):
        text = APK_WRAPPER.read_text(encoding="utf-8")
        self.assertIn('host_curl="/data/data/com.alpine/files/usr/bin/curl"', text)
        self.assertIn('"$host_curl" --fail --location --show-error --output "$tmp" "$url"', text)
        self.assertLess(text.index('if [ -x "$host_curl" ]'), text.index('elif command -v wget'))
        self.assertIn('elif command -v curl >/dev/null 2>&1', text)

    def test_apk_wrapper_replays_suppressed_maintenance(self):
        text = APK_WRAPPER.read_text(encoding="utf-8")
        self.assertIn('"$action" --no-scripts', text)
        self.assertIn("run_commit_hooks pre-commit", text)
        self.assertIn("run_registered_triggers", text)
        self.assertIn("run_commit_hooks post-commit", text)
        self.assertIn('APK_SKIP_COMMIT_HOOKS', text)
        self.assertIn('APK_SKIP_TRIGGERS', text)
        self.assertIn('grep -E "\\\\.X1${script_hex}\\\\.trigger$"', text)

    def test_apk_wrapper_handles_removal_without_native_scripts(self):
        text = APK_WRAPPER.read_text(encoding="utf-8")
        self.assertIn("Purging \\([^ ]*\\)", text)
        self.assertIn("cache_deinstall_scripts", text)
        self.assertIn("pre-deinstall", text)
        self.assertIn("post-deinstall", text)
        self.assertIn('commit_with_manual_scripts del "$@"', text)
        self.assertIn("apk fix is disabled in Android compatibility mode", text)

    def test_apk_wrapper_fails_closed_and_serializes_mutations(self):
        text = APK_WRAPPER.read_text(encoding="utf-8")
        self.assertIn('TRANSACTION_LOCK_DIR="/tmp/alpine-apk-transaction.lock"', text)
        self.assertIn('PENDING_CONFIGURATION_DIR="/var/lib/apk/android-compat-pending"', text)
        self.assertGreaterEqual(text.count("acquire_transaction_lock || exit 1"), 3)
        self.assertGreaterEqual(text.count("recover_pending_configuration || exit 1"), 3)
        self.assertIn("persist_pending_configuration", text)
        self.assertIn("pending_plan_application_state", text)
        self.assertIn("finish_pending_configuration", text)
        self.assertIn("Previous package configuration recovered successfully.", text)
        self.assertIn("apk simulation returned an unsupported transaction format", text)
        self.assertIn("Downgrading", text)
        self.assertIn("Removing", text)
        self.assertIn('echo "Nothing to do."', text)
        self.assertNotIn('"$action" --no-scripts "$@"\n        return $?', text)
        for variable in ("HOME=/root", "TMPDIR=/tmp", "SHELL=/bin/sh", "USER=root", "LOGNAME=root"):
            self.assertGreaterEqual(text.count(variable), 4, variable)

    def test_installer_is_transactional_and_does_not_copy_bootstrap_to_heap(self):
        java = INSTALLER.read_text(encoding="utf-8")
        native = BOOTSTRAP_JNI.read_text(encoding="utf-8")
        self.assertIn("BOOTSTRAP_BACKUP_DIR_PATH", java)
        self.assertIn("recoverInterruptedBootstrapInstall", java)
        self.assertIn("rollbackBootstrapInstall", java)
        self.assertIn("MIN_BOOTSTRAP_FREE_BYTES", java)
        self.assertIn("safeStagingTarget", java)
        self.assertIn("getZipBuffer", java)
        self.assertIn("ALPINE_ENV_FILE_PATH", java)
        self.assertIn('entryName.equals("MODES.txt")', java)
        self.assertIn("seenModePaths.equals(extractedPaths)", java)
        self.assertIn("Os.chmod(mode.first, mode.second)", java)
        self.assertIn("REQUIRED_EXECUTABLE_FILES", java)
        self.assertIn("192L * 1024L * 1024L", java)
        self.assertNotIn('path.contains("/bin/")', java)
        self.assertNotIn("loadZipBytes", java)
        self.assertNotIn("native byte[] getZip", java)
        self.assertIn("NewDirectByteBuffer", native)
        self.assertNotIn("NewByteArray", native)

    def test_android_upgrade_and_project_identity_are_explicit(self):
        manifest = APP_MANIFEST.read_text(encoding="utf-8")
        activity = ALPINE_ACTIVITY.read_text(encoding="utf-8")
        constants = ALPINE_CONSTANTS.read_text(encoding="utf-8")
        x11_strings = X11_STRINGS.read_text(encoding="utf-8")

        # Keep upgrade compatibility for existing V65 installs while opting
        # new Android 13+ installs out of the deprecated shared-user model.
        self.assertIn('android:sharedUserId="${ALPINE_PACKAGE_NAME}"', manifest)
        self.assertIn('android:sharedUserMaxSdkVersion="32"', manifest)

        # Internal styling broadcasts must remain package-explicit on modern Android.
        self.assertIn("stylingIntent.setPackage(context.getPackageName())", activity)

        self.assertIn('ALPINE_DEVELOPER_NAME = "Ooonana"', constants)
        self.assertIn('ALPINE_GITHUB_REPO_NAME = "Alpine-on-android-"', constants)
        self.assertIn("https://github.com/Ooonana/Alpine-on-android-", x11_strings)


if __name__ == "__main__":
    unittest.main()
