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
HOST_MOTD = OVERLAY / "etc/motd"
ROOTFS_MOTD = ROOTFS / "etc/motd"
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
X11_LAYOUT = ROOT / "android/x11/src/main/res/layout/main_activity.xml"
X11_ERROR_LAYOUT = ROOT / "android/x11/src/main/res/layout/main_activity_error.xml"
X11_STYLES = ROOT / "android/x11/src/main/res/values/styles.xml"
X11_STRINGS = ROOT / "android/x11/src/main/res/values/strings.xml"
X11_COLORS = ROOT / "android/x11/src/main/res/values/colors.xml"
X11_MANIFEST = ROOT / "android/x11/src/main/AndroidManifest.xml"
X11_EXTRA_KEYS_LAYOUT = ROOT / "android/x11/src/main/res/layout/extra_keys_config.xml"
X11_DISPLAY_LOGO = ROOT / "android/x11/src/main/res/drawable/ic_alpine_display.xml"
X11_NOTIFICATION_LOGO = ROOT / "android/x11/src/main/res/drawable/ic_alpine_display_notification.xml"
TERMINAL_LOGO = ROOT / "android/app/src/main/res/drawable/ic_foreground.xml"
X11_CMD_ENTRYPOINT = ROOT / "android/x11/src/main/java/com/termux/x11/CmdEntryPoint.java"
X11_NATIVE_LIB = ROOT / "android/x11/src/main/jniLibs/arm64-v8a/libXlorie.so"
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
        self.assertIn('BOOTSTRAP_VERSION = "v68.3"', installer)
        self.assertIn("versionCode 139", build)
        self.assertIn('versionName "0.135.3-v68-dev"', build)
        self.assertIn('ALPINE_VERSION = "3.23.6"', builder)
        self.assertIn('b"v68.3\\n"', builder)
        self.assertIn('b"v68.3\\n"', prepare)

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

    def test_v68_3_hotfix_migrates_v68_1_and_v68_2_in_place(self):
        installer = INSTALLER.read_text(encoding="utf-8")
        self.assertIn('PATCHABLE_BOOTSTRAP_VERSIONS = { "v68.1", "v68.2" }', installer)
        self.assertIn("V68_HOTFIX_PATCH_FILES", installer)
        for path in (
            '"etc/bash.bashrc"',
            'ROOTFS_RELATIVE_PATH + "/usr/local/bin/start-x11"',
            '"etc/motd"',
            'ROOTFS_RELATIVE_PATH + "/etc/motd"',
            'ROOTFS_RELATIVE_PATH + "/etc/alpine-bootstrap-version"',
            '"etc/alpine-bootstrap-version"',
        ):
            self.assertIn(path, installer)
        self.assertIn("applyV68HotfixInPlacePatch", installer)
        self.assertIn("replacePatchFile", installer)
        self.assertIn("installedBootstrapLooksUsable() || isV68HotfixInPlacePatchCandidate()", installer)
        self.assertIn("v68HotfixUnchangedRuntimeLooksUsable()", installer)
        self.assertIn("cleanupV68HotfixPatchArtifacts()", installer)
        self.assertIn("packages, user configuration, and /root data", installer)
        self.assertLess(
            installer.index("applyV68HotfixInPlacePatch();"),
            installer.index('deletePathOrThrow("staging", ALPINE_STAGING_PREFIX_DIR_PATH, true)'),
        )

    def test_v68_3_startup_banner_and_gui_hint_are_tracked(self):
        host = HOST_MOTD.read_text(encoding="utf-8")
        rootfs = ROOTFS_MOTD.read_text(encoding="utf-8")
        self.assertEqual(host, rootfs)
        self.assertIn("Alpine on Android by Ooonana", rootfs)
        self.assertIn("Made with Gemini 3 (base) + GPT-5.5 + GPT-5.6 Sol (final touches)", rootfs)
        self.assertTrue(
            rootfs.rstrip().endswith(
                "GUI desktop: run install-desktop to install one, then start-desktop to launch it."
            )
        )

    def test_display_shell_matches_terminal_and_has_startup_fallback(self):
        activity = X11_MAIN_ACTIVITY.read_text(encoding="utf-8")
        layout = X11_LAYOUT.read_text(encoding="utf-8")
        fallback = X11_ERROR_LAYOUT.read_text(encoding="utf-8")
        styles = X11_STYLES.read_text(encoding="utf-8")
        strings = X11_STRINGS.read_text(encoding="utf-8")

        self.assertIn("@color/alpine_display_black", layout)
        self.assertIn("@drawable/ic_alpine_display", layout)
        self.assertIn("@style/AlpineDisplay.ActionButton", layout)
        self.assertIn("@style/AlpineDisplay.PrimaryButton", layout)
        self.assertNotIn("@drawable/ic_x11_icon", layout)
        self.assertIn("R.drawable.ic_alpine_display_notification", activity)
        self.assertFalse((ROOT / "android/x11/src/main/res/drawable/ic_x11_icon.xml").exists())
        self.assertIn('<item name="android:windowBackground">@color/alpine_display_black</item>', styles)
        self.assertIn('<item name="android:colorAccent">@color/alpine_display_green</item>', styles)
        self.assertIn("$ start-desktop", strings)
        self.assertIn("Back to terminal", strings)

        self.assertNotIn("Integer.parseInt(prefs.touchMode.get())", activity)
        self.assertIn("catch (android.view.InflateException | LinkageError e)", activity)
        self.assertIn("R.layout.main_activity_error", activity)
        self.assertIn("receiverRegistered", activity)
        self.assertIn("updateDisplayNotification()", activity)
        self.assertNotIn("com.termux.x11.LorieView", fallback)
        self.assertIn("display_startup_failed", fallback)

    def test_display_branding_matches_terminal_identity(self):
        terminal_logo = TERMINAL_LOGO.read_text(encoding="utf-8")
        display_logo = X11_DISPLAY_LOGO.read_text(encoding="utf-8")
        notification_logo = X11_NOTIFICATION_LOGO.read_text(encoding="utf-8")
        colors = X11_COLORS.read_text(encoding="utf-8")

        # Display keeps Alpine's terminal visual language: black, white and green,
        # and reuses the exact >_ prompt geometry instead of an X11/window badge.
        for path_data in (
            'android:pathData="M34,38',
            'android:pathData="M56,66',
        ):
            self.assertIn(path_data, terminal_logo)
            self.assertIn(path_data, display_logo)
            self.assertIn(path_data, notification_logo)
        self.assertNotIn("strokeColor", display_logo)
        self.assertNotIn("strokeColor", notification_logo)
        self.assertIn('<color name="alpine_display_black">#FF000000</color>', colors)
        self.assertIn('<color name="alpine_display_white">#FFFFFFFF</color>', colors)
        self.assertIn('<color name="alpine_display_green">#FF00FF66</color>', colors)
        self.assertNotIn("alpine_display_red", colors)
        self.assertNotIn("#FFFF0000", colors)

    def test_display_backend_handoff_is_complete_and_pinned(self):
        launcher = BASHRC.read_text(encoding="utf-8")
        start_x11 = START_X11.read_text(encoding="utf-8")
        alpine_activity = ALPINE_ACTIVITY.read_text(encoding="utf-8")
        cmd_entry = X11_CMD_ENTRYPOINT.read_text(encoding="utf-8")
        activity = X11_MAIN_ACTIVITY.read_text(encoding="utf-8")
        view = X11_VIEW.read_text(encoding="utf-8")
        prepare = PREPARE.read_text(encoding="utf-8")

        # Guest /tmp is intentionally the host-prefix tmp through --shared-tmp,
        # so start-x11's request is consumed by the host bridge.
        self.assertIn("--shared-tmp", launcher)
        self.assertIn('request_file="/tmp/alpine-x11-request"', start_x11)
        self.assertIn('request_file="$TMPDIR/alpine-x11-request"', launcher)
        self.assertIn('"$PREFIX/bin/termux-x11" "$display"', launcher)
        self.assertIn(r'START_DISPLAY_COMMAND = "start-x11 :1 --no-open\n"', alpine_activity)

        # The embedded launcher enters CmdEntryPoint, broadcasts its Binder, and
        # MainActivity transfers the X connection fd into native LorieView.
        self.assertIn('ACTION_START = "com.termux.x11.CmdEntryPoint.ACTION_START"', cmd_entry)
        self.assertIn("bundle.putBinder(null, this)", cmd_entry)
        self.assertIn("sendBroadcastDelayed()", cmd_entry)
        self.assertIn("onReceiveConnection(intent)", activity)
        self.assertIn("service.getXConnection()", activity)
        self.assertIn("LorieView.connect(fd.detachFd())", activity)
        self.assertIn("LorieView.requestConnection()", activity)
        self.assertIn('System.loadLibrary("Xlorie")', view)

        # libXlorie is prebuilt, so pin the exact blob and the host app_process
        # launcher that selects com.alpine and CmdEntryPoint.
        self.assertEqual(
            hashlib.sha256(X11_NATIVE_LIB.read_bytes()).hexdigest(),
            "5ad7e186ae39af3e3ff2babab80e6c599ce71376a786d1d413f20a271073afb1",
        )
        self.assertIn(
            'TERMUX_X11_LAUNCHER_SHA256 = "596ed18e0b6896b8293ddb4e2bcc0705ae07b8c82266f6bca5f1041e3b2e737e"',
            prepare,
        )
        self.assertIn("Embedded termux-x11 launcher changed unexpectedly", prepare)
        self.assertIn("b'TERMUX_X11_OVERRIDE_PACKAGE=\"com.alpine\"'", prepare)
        self.assertIn('b"com.termux.x11.CmdEntryPoint"', prepare)

    def test_display_settings_use_green_alpine_theme(self):
        manifest = X11_MANIFEST.read_text(encoding="utf-8")
        styles = X11_STYLES.read_text(encoding="utf-8")

        self.assertIn('android:theme="@style/AlpineDisplay.PreferencesTheme"', manifest)
        self.assertIn('<style name="AlpineDisplay.PreferencesTheme"', styles)
        self.assertIn('<item name="colorAccent">@color/alpine_display_green</item>', styles)
        self.assertIn('<item name="colorControlActivated">@color/alpine_display_green</item>', styles)
        self.assertIn('<item name="android:colorControlActivated">@color/alpine_display_green</item>', styles)
        self.assertIn('<item name="actionBarStyle">@style/AlpineDisplay.ActionBar</item>', styles)
        self.assertIn('<item name="alertDialogTheme">@style/AlpineDisplay.DialogTheme</item>', styles)
        self.assertNotIn('android:theme="@style/Theme.AppCompat.DayNight"', manifest)
        preferences = X11_PREFERENCES.read_text(encoding="utf-8")
        extra_keys_layout = X11_EXTRA_KEYS_LAYOUT.read_text(encoding="utf-8")
        self.assertIn("new AlertDialog.Builder(requireContext())", preferences)
        self.assertNotIn("new android.app.AlertDialog.Builder", preferences)
        self.assertIn('android:backgroundTint="@color/alpine_display_green"', extra_keys_layout)
        self.assertIn('android:textColor="@color/alpine_display_white"', extra_keys_layout)

    def test_display_never_overlays_extra_key_toolbar(self):
        activity = X11_MAIN_ACTIVITY.read_text(encoding="utf-8")

        method = activity[
            activity.index("private void setTerminalToolbarView()"):
            activity.index("public void toggleExtraKeys(boolean visible", activity.index("private void setTerminalToolbarView()"))
        ]
        self.assertIn("pager.setVisibility(View.GONE);", method)
        self.assertIn("pager.setAdapter(null);", method)
        self.assertIn("layoutParams.height = 0;", method)
        self.assertIn("frm.setPadding(0, 0, 0, 0);", method)
        self.assertNotIn("View.VISIBLE", method)
        self.assertNotIn("X11ToolbarViewPager", method)

    def test_display_preferences_are_crash_hardened(self):
        activity = X11_MAIN_ACTIVITY.read_text(encoding="utf-8")
        view = X11_VIEW.read_text(encoding="utf-8")
        preferences = X11_PREFERENCES.read_text(encoding="utf-8")

        self.assertNotIn("Integer.parseInt(prefs.touchMode.get())", activity)
        self.assertNotIn("Integer.parseInt(prefs.touchMode.get())", preferences)
        self.assertIn("safeResolution", view)
        self.assertIn("Math.max(30, Math.min(300, prefs.displayScale.get()))", view)
        self.assertIn("parts.length != 2", view)
        self.assertIn("width > maxDimension || height > maxDimension", view)
        self.assertIn('value.split("x", -1)', preferences)
        self.assertIn("width > 8192 || height > 8192", preferences)

    def test_start_x11_can_reopen_activity_when_server_is_already_running(self):
        start_x11 = START_X11.read_text(encoding="utf-8")
        activity_request = 'if [ "$open_activity" = "1" ]; then'
        existing_socket = 'if [ -S "$socket_path" ]; then'
        self.assertIn(activity_request, start_x11)
        self.assertIn(existing_socket, start_x11)
        self.assertLess(start_x11.index(activity_request), start_x11.index(existing_socket))
        self.assertIn('mv "$activity_request_tmp" "$activity_request_file"', start_x11)

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
