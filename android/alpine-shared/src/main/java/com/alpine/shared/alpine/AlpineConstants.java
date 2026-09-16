package com.alpine.shared.alpine;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.alpine.shared.shell.command.ExecutionCommand;
import com.alpine.shared.shell.command.ExecutionCommand.Runner;

import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/*
 * Version: v0.53.0
 * SPDX-License-Identifier: MIT
 *
 * Changelog
 *
 * - 0.1.0 (2021-03-08)
 *      - Initial Release.
 *
 * - 0.2.0 (2021-03-11)
 *      - Added `_DIR` and `_FILE` substrings to paths.
 *      - Added `INTERNAL_PRIVATE_APP_DATA_DIR*`, `ALPINE_CACHE_DIR*`, `ALPINE_DATABASES_DIR*`,
 *          `ALPINE_SHARED_PREFERENCES_DIR*`, `ALPINE_BIN_PREFIX_DIR*`, `ALPINE_ETC_DIR*`,
 *          `ALPINE_INCLUDE_DIR*`, `ALPINE_LIB_DIR*`, `ALPINE_LIBEXEC_DIR*`, `ALPINE_SHARE_DIR*`,
 *          `ALPINE_TMP_DIR*`, `ALPINE_VAR_DIR*`, `ALPINE_STAGING_PREFIX_DIR*`,
 *          `ALPINE_STORAGE_HOME_DIR*`, `ALPINE_DEFAULT_PREFERENCES_FILE_BASENAME*`,
 *          `ALPINE_DEFAULT_PREFERENCES_FILE`.
 *      - Renamed `DATA_HOME_PATH` to `ALPINE_DATA_HOME_DIR_PATH`.
 *      - Renamed `CONFIG_HOME_PATH` to `ALPINE_CONFIG_HOME_DIR_PATH`.
 *      - Updated javadocs and spacing.
 *
 * - 0.3.0 (2021-03-12)
 *      - Remove `ALPINE_CACHE_DIR_PATH*`, `ALPINE_DATABASES_DIR_PATH*`,
 *          `ALPINE_SHARED_PREFERENCES_DIR_PATH*` since they may not be consistent on all devices.
 *      - Renamed `ALPINE_DEFAULT_PREFERENCES_FILE_BASENAME` to
 *          `ALPINE_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`. This should be used for
 *           accessing shared preferences between Alpine app and its plugins if ever needed by first
 *           getting shared package context with {@link Context.createPackageContext(String,int}).
 *
 * - 0.4.0 (2021-03-16)
 *      - Added `BROADCAST_ALPINE_OPENED`,
 *          `ALPINE_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`
 *          `ALPINE_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `ALPINE_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `ALPINE_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `ALPINE_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `ALPINE_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`.
 *
 * - 0.5.0 (2021-03-16)
 *      - Renamed "Alpine Plugin app" labels to "Alpine:Tasker app".
 *
 * - 0.6.0 (2021-03-16)
 *      - Added `ALPINE_FILE_SHARE_URI_AUTHORITY`.
 *
 * - 0.7.0 (2021-03-17)
 *      - Fixed javadocs.
 *
 * - 0.8.0 (2021-03-18)
 *      - Fixed Intent extra types javadocs.
 *      - Added following to `ALPINE_SERVICE`:
 *          `EXTRA_PENDING_INTENT`, `EXTRA_RESULT_BUNDLE`,
 *          `EXTRA_STDOUT`, `EXTRA_STDERR`, `EXTRA_EXIT_CODE`,
 *          `EXTRA_ERR`, `EXTRA_ERRMSG`.
 *
 * - 0.9.0 (2021-03-18)
 *      - Fixed javadocs.
 *
 * - 0.10.0 (2021-03-19)
 *      - Added following to `ALPINE_SERVICE`:
 *          `EXTRA_SESSION_ACTION`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY`
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_SESSION_ACTION`.
 *
 * - 0.11.0 (2021-03-24)
 *      - Added following to `ALPINE_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`, `EXTRA_PLUGIN_API_HELP`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`.
 *      - Updated `RESULT_BUNDLE` related extras with `PLUGIN_RESULT_BUNDLE` prefixes.
 *
 * - 0.12.0 (2021-03-25)
 *      - Added following to `ALPINE_SERVICE`:
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH`,
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH`.
 *
 * - 0.13.0 (2021-03-25)
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_PENDING_INTENT`.
 *
 * - 0.14.0 (2021-03-25)
 *      - Added `FDROID_PACKAGES_BASE_URL`,
 *          `ALPINE_GITHUB_ORGANIZATION_NAME`, `ALPINE_GITHUB_ORGANIZATION_URL`,
 *          `ALPINE_GITHUB_REPO_NAME`, `ALPINE_GITHUB_REPO_URL`, `ALPINE_FDROID_PACKAGE_URL`,
 *          `ALPINE_API_GITHUB_REPO_NAME`,`ALPINE_API_GITHUB_REPO_URL`, `ALPINE_API_FDROID_PACKAGE_URL`,
 *          `ALPINE_BOOT_GITHUB_REPO_NAME`, `ALPINE_BOOT_GITHUB_REPO_URL`, `ALPINE_BOOT_FDROID_PACKAGE_URL`,
 *          `ALPINE_FLOAT_GITHUB_REPO_NAME`, `ALPINE_FLOAT_GITHUB_REPO_URL`, `ALPINE_FLOAT_FDROID_PACKAGE_URL`,
 *          `ALPINE_STYLING_GITHUB_REPO_NAME`, `ALPINE_STYLING_GITHUB_REPO_URL`, `ALPINE_STYLING_FDROID_PACKAGE_URL`,
 *          `ALPINE_TASKER_GITHUB_REPO_NAME`, `ALPINE_TASKER_GITHUB_REPO_URL`, `ALPINE_TASKER_FDROID_PACKAGE_URL`,
 *          `ALPINE_WIDGET_GITHUB_REPO_NAME`, `ALPINE_WIDGET_GITHUB_REPO_URL` `ALPINE_WIDGET_FDROID_PACKAGE_URL`.
 *
 * - 0.15.0 (2021-04-06)
 *      - Fixed some variables that had `PREFIX_` substring missing in their name.
 *      - Added `ALPINE_CRASH_LOG_FILE_PATH`, `ALPINE_CRASH_LOG_BACKUP_FILE_PATH`,
 *          `ALPINE_GITHUB_ISSUES_REPO_URL`, `ALPINE_API_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_BOOT_GITHUB_ISSUES_REPO_URL`, `ALPINE_FLOAT_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_STYLING_GITHUB_ISSUES_REPO_URL`, `ALPINE_TASKER_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_WIDGET_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_GITHUB_WIKI_REPO_URL`, `ALPINE_PACKAGES_GITHUB_WIKI_REPO_URL`,
 *          `ALPINE_PACKAGES_GITHUB_REPO_NAME`, `ALPINE_PACKAGES_GITHUB_REPO_URL`, `ALPINE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_GAME_PACKAGES_GITHUB_REPO_NAME`, `ALPINE_GAME_PACKAGES_GITHUB_REPO_URL`, `ALPINE_GAME_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_SCIENCE_PACKAGES_GITHUB_REPO_NAME`, `ALPINE_SCIENCE_PACKAGES_GITHUB_REPO_URL`, `ALPINE_SCIENCE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_ROOT_PACKAGES_GITHUB_REPO_NAME`, `ALPINE_ROOT_PACKAGES_GITHUB_REPO_URL`, `ALPINE_ROOT_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_UNSTABLE_PACKAGES_GITHUB_REPO_NAME`, `ALPINE_UNSTABLE_PACKAGES_GITHUB_REPO_URL`, `ALPINE_UNSTABLE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `ALPINE_X11_PACKAGES_GITHUB_REPO_NAME`, `ALPINE_X11_PACKAGES_GITHUB_REPO_URL`, `ALPINE_X11_PACKAGES_GITHUB_ISSUES_REPO_URL`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `RUN_COMMAND_API_HELP_URL`.
 *
 * - 0.16.0 (2021-04-06)
 *      - Added `ALPINE_SUPPORT_EMAIL`, `ALPINE_SUPPORT_EMAIL_URL`, `ALPINE_SUPPORT_EMAIL_MAILTO_URL`,
 *          `ALPINE_REDDIT_SUBREDDIT`, `ALPINE_REDDIT_SUBREDDIT_URL`.
 *      - The `ALPINE_SUPPORT_EMAIL_URL` value must be fixed later when email has been set up.
 *
 * - 0.17.0 (2021-04-07)
 *      - Added `ALPINE_APP_NOTIFICATION_CHANNEL_ID`, `ALPINE_APP_NOTIFICATION_CHANNEL_NAME`, `ALPINE_APP_NOTIFICATION_ID`,
 *          `ALPINE_RUN_COMMAND_NOTIFICATION_CHANNEL_ID`, `ALPINE_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME`, `ALPINE_RUN_COMMAND_NOTIFICATION_ID`,
 *          `ALPINE_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID`, `ALPINE_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME`,
 *          `ALPINE_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID`, `ALPINE_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME`.
 *      - Updated javadocs.
 *
 * - 0.18.0 (2021-04-11)
 *      - Updated `ALPINE_SUPPORT_EMAIL_URL` to a valid email.
 *      - Removed `ALPINE_SUPPORT_EMAIL`.
 *
 * - 0.19.0 (2021-04-12)
 *      - Added `ALPINE_ACTIVITY.ACTION_REQUEST_PERMISSIONS`.
 *      - Added `ALPINE_SERVICE.EXTRA_STDIN`.
 *      - Added `RUN_COMMAND_SERVICE.EXTRA_STDIN`.
 *      - Deprecated `ALPINE_ACTIVITY.EXTRA_RELOAD_STYLE`.
 *
 * - 0.20.0 (2021-05-13)
 *      - Added `ALPINE_WIKI`, `ALPINE_WIKI_URL`, `ALPINE_PLUGIN_APP_NAMES_LIST`, `ALPINE_PLUGIN_APP_PACKAGE_NAMES_LIST`.
 *      - Added `ALPINE_SETTINGS_ACTIVITY_NAME`.
 *
 * - 0.21.0 (2021-05-13)
 *      - Added `APK_RELEASE_FDROID`, `APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD`, `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GOOGLE_PLAYSTORE`, `APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.22.0 (2021-05-13)
 *      - Added `ALPINE_DONATE_URL`.
 *
 * - 0.23.0 (2021-06-12)
 *      - Rename `INTERNAL_PRIVATE_APP_DATA_DIR_PATH` to `ALPINE_INTERNAL_PRIVATE_APP_DATA_DIR_PATH`.
 *
 * - 0.24.0 (2021-06-27)
 *      - Add `COMMA_NORMAL`, `COMMA_ALTERNATIVE`.
 *      - Added following to `ALPINE_APP.ALPINE_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`.
 *      - Added following to `ALPINE_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`,
 *          `EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`, `EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`.
 *      - Added following to `RESULT_SENDER`:
 *           `FORMAT_SUCCESS_STDOUT`, `FORMAT_SUCCESS_STDOUT__EXIT_CODE`, `FORMAT_SUCCESS_STDOUT__STDERR__EXIT_CODE`
 *           `FORMAT_FAILED_ERR__ERRMSG__STDOUT__STDERR__EXIT_CODE`,
 *           `RESULT_FILE_ERR_PREFIX`, `RESULT_FILE_ERRMSG_PREFIX` `RESULT_FILE_STDOUT_PREFIX`,
 *           `RESULT_FILE_STDERR_PREFIX`, `RESULT_FILE_EXIT_CODE_PREFIX`.
 *
 * - 0.25.0 (2021-08-19)
 *      - Added following to `ALPINE_APP.ALPINE_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *      - Added following to `ALPINE_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *
 * - 0.26.0 (2021-08-25)
 *      - Changed `ALPINE_ACTIVITY.ACTION_FAILSAFE_SESSION` to `ALPINE_ACTIVITY.EXTRA_FAILSAFE_SESSION`.
 *
 * - 0.27.0 (2021-09-02)
 *      - Added `ALPINE_FLOAT_APP_NOTIFICATION_CHANNEL_ID`, `ALPINE_FLOAT_APP_NOTIFICATION_CHANNEL_NAME`,
 *          `ALPINE_FLOAT_APP.ALPINE_FLOAT_SERVICE_NAME`.
 *      - Added following to `ALPINE_FLOAT_APP.ALPINE_FLOAT_SERVICE`:
 *          `ACTION_STOP_SERVICE`, `ACTION_SHOW`, `ACTION_HIDE`.
 *
 * - 0.28.0 (2021-09-02)
 *      - Added `ALPINE_FLOAT_PROPERTIES_PRIMARY_FILE*` and `ALPINE_FLOAT_PROPERTIES_SECONDARY_FILE*`.
 *
 * - 0.29.0 (2021-09-04)
 *      - Added `ALPINE_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME`, `ALPINE_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME`,
 *          `ALPINE_SHORTCUT_SCRIPT_ICONS_DIR_PATH`, `ALPINE_SHORTCUT_SCRIPT_ICONS_DIR`.
 *      - Added following to `ALPINE_WIDGET.ALPINE_WIDGET_PROVIDER`:
 *          `ACTION_WIDGET_ITEM_CLICKED`, `ACTION_REFRESH_WIDGET`, `EXTRA_FILE_CLICKED`.
 *      - Changed naming convention of `ALPINE_FLOAT_APP.ALPINE_FLOAT_SERVICE.ACTION_*`.
 *      - Fixed wrong path set for `ALPINE_SHORTCUT_SCRIPTS_DIR_PATH`.
 *
 * - 0.30.0 (2021-09-08)
 *      - Changed `APK_RELEASE_GITHUB_DEBUG_BUILD`to `APK_RELEASE_GITHUB` and
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST` to
 *          `APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.31.0 (2021-09-09)
 *      - Added following to `ALPINE_APP.ALPINE_SERVICE`:
 *          `MIN_VALUE_EXTRA_SESSION_ACTION` and `MAX_VALUE_EXTRA_SESSION_ACTION`.
 *
 * - 0.32.0 (2021-09-23)
 *      - Added `ALPINE_API.ALPINE_API_ACTIVITY_NAME`, `ALPINE_TASKER.ALPINE_TASKER_ACTIVITY_NAME`
 *          and `ALPINE_WIDGET.ALPINE_WIDGET_ACTIVITY_NAME`.
 *
 * - 0.33.0 (2021-10-08)
 *      - Added `ALPINE_PROPERTIES_FILE_PATHS_LIST` and `ALPINE_FLOAT_PROPERTIES_FILE_PATHS_LIST`.
 *
 * - 0.34.0 (2021-10-26)
 *      - Move `RESULT_SENDER` to `com.alpine.shared.shell.command.ShellCommandConstants`.
 *
 * - 0.35.0 (2022-01-28)
 *      - Add `ALPINE_APP.ALPINE_ACTIVITY.EXTRA_RECREATE_ACTIVITY`.
 *
 * - 0.36.0 (2022-03-10)
 *      - Added `ALPINE_APP.ALPINE_SERVICE.EXTRA_RUNNER` and `ALPINE_APP.RUN_COMMAND_SERVICE.EXTRA_RUNNER`
 *
 * - 0.37.0 (2022-03-15)
 *  - Added `ALPINE_API_APT_*`.
 *
 * - 0.38.0 (2022-03-16)
 *      - Added `ALPINE_APP.ALPINE_ACTIVITY.ACTION_NOTIFY_APP_CRASH`.
 *
 * - 0.39.0 (2022-03-18)
 *      - Added `ALPINE_APP.ALPINE_SERVICE.EXTRA_SESSION_NAME`, `ALPINE_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_NAME`,
 *          `ALPINE_APP.ALPINE_SERVICE.EXTRA_SESSION_CREATE_MODE` and `ALPINE_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_CREATE_MODE`.
 *
 * - 0.40.0 (2022-04-17)
 *      - Added `ALPINE_APPS_DIR_PATH` and `ALPINE_APP.APPS_DIR_PATH`.
 *
 * - 0.41.0 (2022-04-17)
 *      - Added `ALPINE_APP.ALPINE_AM_SOCKET_FILE_PATH`.
 *
 * - 0.42.0 (2022-04-29)
 *      - Added `APK_RELEASE_ALPINE_DEVS` and `APK_RELEASE_ALPINE_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.43.0 (2022-05-29)
 *      - Changed `ALPINE_SUPPORT_EMAIL_URL` to support@termux.dev.
 *
 * - 0.44.0 (2022-05-29)
 *      - Changed `ALPINE_APP.APPS_DIR_PATH` basename from `alpine-app` to `com.alpine`.
 *
 * - 0.45.0 (2022-06-01)
 *      - Added `ALPINE_APP.BUILD_CONFIG_CLASS_NAME`.
 *
 * - 0.46.0 (2022-06-03)
 *      - Rename `ALPINE_APP.ALPINE_SERVICE.EXTRA_SESSION_NAME` to `*.EXTRA_SHELL_NAME`,
 *          `ALPINE_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_NAME` to `*.EXTRA_SHELL_NAME`,
 *          `ALPINE_APP.ALPINE_SERVICE.EXTRA_SESSION_CREATE_MODE` to `*.EXTRA_SHELL_CREATE_MODE` and
 *          `ALPINE_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_CREATE_MODE` to `*.EXTRA_SHELL_CREATE_MODE`.
 *
 * - 0.47.0 (2022-06-04)
 *      - Added `ALPINE_SITE` and `ALPINE_SITE_URL`.
 *      - Changed `ALPINE_DONATE_URL`.
 *
 * - 0.48.0 (2022-06-04)
 *      - Removed `ALPINE_GAME_PACKAGES_GITHUB_*`, `ALPINE_SCIENCE_PACKAGES_GITHUB_*`,
 *          `ALPINE_ROOT_PACKAGES_GITHUB_*`, `ALPINE_UNSTABLE_PACKAGES_GITHUB_*`
 *
 * - 0.49.0 (2022-06-11)
 *      - Added `ALPINE_ENV_PREFIX_ROOT`.
 *
 * - 0.50.0 (2022-06-11)
 *      - Added `ALPINE_CONFIG_PREFIX_DIR_PATH`, `ALPINE_ENV_FILE_PATH` and `ALPINE_ENV_TEMP_FILE_PATH`.
 *
 * - 0.51.0 (2022-06-13)
 *      - Added `ALPINE_APP.FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME` and `ALPINE_APP.FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME`.
 *
 * - 0.52.0 (2022-06-18)
 *      - Added `ALPINE_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY`.
 *
 * - 0.53.0 (2025-01-12)
 *      - Renamed `ALPINE_API`, `ALPINE_STYLING`, `ALPINE_TASKER`, `ALPINE_WIDGET` classes with `_APP` suffix added.
 *      - Added `ALPINE_*_MAIN_ACTIVITY_NAME` and `ALPINE_*_LAUNCHER_ACTIVITY_NAME` constants to each app class.
 */

/**
 * A class that defines shared constants of the Alpine app and its plugins.
 * This class will be hosted by alpine-shared lib and should be imported by other alpine plugin
 * apps as is instead of copying constants to random classes. The 3rd party apps can also import
 * it for interacting with alpine apps. If changes are made to this file, increment the version number
 * and add an entry in the Changelog section above.
 *
 * Alpine app default package name is "com.alpine" and is used in {@link #ALPINE_PREFIX_DIR_PATH}.
 * The binaries compiled for alpine have {@link #ALPINE_PREFIX_DIR_PATH} hardcoded in them but it
 * can be changed during compilation.
 *
 * The {@link #ALPINE_PACKAGE_NAME} must be the same as the applicationId of alpine-app build.gradle
 * since its also used by {@link #ALPINE_FILES_DIR_PATH}.
 * If {@link #ALPINE_PACKAGE_NAME} is changed, then binaries, specially used in bootstrap need to be
 * compiled appropriately. Check https://github.com/termux/termux-packages/wiki/Building-packages
 * for more info.
 *
 * Ideally the only places where changes should be required if changing package name are the following:
 * - The {@link #ALPINE_PACKAGE_NAME} in {@link AlpineConstants}.
 * - The "applicationId" in "build.gradle" of alpine-app. This is package name that android and app
 *      stores will use and is also the final package name stored in "AndroidManifest.xml".
 * - The "manifestPlaceholders" values for {@link #ALPINE_PACKAGE_NAME} and *_APP_NAME in
 *      "build.gradle" of alpine-app.
 * - The "ENTITY" values for {@link #ALPINE_PACKAGE_NAME} and *_APP_NAME in "strings.xml" of
 *      alpine-app and of alpine-shared.
 * - The "shortcut.xml" and "*_preferences.xml" files of alpine-app since dynamic variables don't
 *      work in it.
 * - Optionally the "package" in "AndroidManifest.xml" if modifying project structure of alpine-app.
 *      This is package name for java classes project structure and is prefixed if activity and service
 *      names use dot (.) notation. This is currently not advisable since this will break lot of
 *      stuff, including alpine-* packages.
 * - Optionally the *_PATH variables in {@link AlpineConstants} containing the string "alpine".
 *
 * Check https://developer.android.com/studio/build/application-id for info on "package" in
 * "AndroidManifest.xml" and "applicationId" in "build.gradle".
 *
 * The {@link #ALPINE_PACKAGE_NAME} must be used in source code of Alpine app and its plugins instead
 * of hardcoded "com.alpine" paths.
 */
public final class AlpineConstants {


    /*
     * Alpine organization variables.
     */

    /** Alpine GitHub organization name */
    public static final String ALPINE_GITHUB_ORGANIZATION_NAME = "Ooonana";
    /** Alpine GitHub organization url */
    public static final String ALPINE_GITHUB_ORGANIZATION_URL = "https://github.com" + "/" + ALPINE_GITHUB_ORGANIZATION_NAME;

    /** F-Droid packages base url */
    public static final String FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages"; // Default: "https://f-droid.org/en/packages"





    /*
     * Alpine and its plugin app and package names and urls.
     */

    /** Alpine app name */
    public static final String ALPINE_APP_NAME = "Alpine"; // Default: "Alpine"
    /** Alpine package name */
    public static final String ALPINE_PACKAGE_NAME = "com.alpine"; // Default: "com.alpine"
    /** Alpine GitHub repo name */
    public static final String ALPINE_GITHUB_REPO_NAME = "Alpine-on-android-";
    /** Alpine GitHub repo url */
    public static final String ALPINE_GITHUB_REPO_URL = ALPINE_GITHUB_ORGANIZATION_URL + "/" + ALPINE_GITHUB_REPO_NAME;
    /** Alpine GitHub issues repo url */
    public static final String ALPINE_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_REPO_URL + "/issues";
    /** Alpine F-Droid package url */
    public static final String ALPINE_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine:API app name */
    public static final String ALPINE_API_APP_NAME = "Alpine:API"; // Default: "Alpine:API"
    /** Alpine:API app package name */
    public static final String ALPINE_API_PACKAGE_NAME = ALPINE_PACKAGE_NAME + ".api"; // Default: "com.alpine.api"
    /** Alpine:API GitHub repo name */
    public static final String ALPINE_API_GITHUB_REPO_NAME = "alpine-api"; // Default: "alpine-api"
    /** Alpine:API GitHub repo url */
    public static final String ALPINE_API_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine:API GitHub issues repo url */
    public static final String ALPINE_API_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;
    /** Alpine:API F-Droid package url */
    public static final String ALPINE_API_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine:Boot app name */
    public static final String ALPINE_BOOT_APP_NAME = "Alpine:Boot"; // Default: "Alpine:Boot"
    /** Alpine:Boot app package name */
    public static final String ALPINE_BOOT_PACKAGE_NAME = ALPINE_PACKAGE_NAME + ".boot"; // Default: "com.alpine.boot"
    /** Alpine:Boot GitHub repo name */
    public static final String ALPINE_BOOT_GITHUB_REPO_NAME = "alpine-boot"; // Default: "alpine-boot"
    /** Alpine:Boot GitHub repo url */
    public static final String ALPINE_BOOT_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine:Boot GitHub issues repo url */
    public static final String ALPINE_BOOT_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;
    /** Alpine:Boot F-Droid package url */
    public static final String ALPINE_BOOT_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine:Float app name */
    public static final String ALPINE_FLOAT_APP_NAME = "Alpine:Float"; // Default: "Alpine:Float"
    /** Alpine:Float app package name */
    public static final String ALPINE_FLOAT_PACKAGE_NAME = ALPINE_PACKAGE_NAME + ".window"; // Default: "com.alpine.window"
    /** Alpine:Float GitHub repo name */
    public static final String ALPINE_FLOAT_GITHUB_REPO_NAME = "alpine-float"; // Default: "alpine-float"
    /** Alpine:Float GitHub repo url */
    public static final String ALPINE_FLOAT_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine:Float GitHub issues repo url */
    public static final String ALPINE_FLOAT_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;
    /** Alpine:Float F-Droid package url */
    public static final String ALPINE_FLOAT_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine:Styling app name */
    public static final String ALPINE_STYLING_APP_NAME = "Alpine:Styling"; // Default: "Alpine:Styling"
    /** Alpine:Styling app package name */
    public static final String ALPINE_STYLING_PACKAGE_NAME = ALPINE_PACKAGE_NAME + ".styling"; // Default: "com.alpine.styling"
    /** Alpine:Styling GitHub repo name */
    public static final String ALPINE_STYLING_GITHUB_REPO_NAME = "alpine-styling"; // Default: "alpine-styling"
    /** Alpine:Styling GitHub repo url */
    public static final String ALPINE_STYLING_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine:Styling GitHub issues repo url */
    public static final String ALPINE_STYLING_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;
    /** Alpine:Styling F-Droid package url */
    public static final String ALPINE_STYLING_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine:Tasker app name */
    public static final String ALPINE_TASKER_APP_NAME = "Alpine:Tasker"; // Default: "Alpine:Tasker"
    /** Alpine:Tasker app package name */
    public static final String ALPINE_TASKER_PACKAGE_NAME = ALPINE_PACKAGE_NAME + ".tasker"; // Default: "com.alpine.tasker"
    /** Alpine:Tasker GitHub repo name */
    public static final String ALPINE_TASKER_GITHUB_REPO_NAME = "alpine-tasker"; // Default: "alpine-tasker"
    /** Alpine:Tasker GitHub repo url */
    public static final String ALPINE_TASKER_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine:Tasker GitHub issues repo url */
    public static final String ALPINE_TASKER_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;
    /** Alpine:Tasker F-Droid package url */
    public static final String ALPINE_TASKER_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine:Widget app name */
    public static final String ALPINE_WIDGET_APP_NAME = "Alpine:Widget"; // Default: "Alpine:Widget"
    /** Alpine:Widget app package name */
    public static final String ALPINE_WIDGET_PACKAGE_NAME = ALPINE_PACKAGE_NAME + ".widget"; // Default: "com.alpine.widget"
    /** Alpine:Widget GitHub repo name */
    public static final String ALPINE_WIDGET_GITHUB_REPO_NAME = "alpine-widget"; // Default: "alpine-widget"
    /** Alpine:Widget GitHub repo url */
    public static final String ALPINE_WIDGET_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine:Widget GitHub issues repo url */
    public static final String ALPINE_WIDGET_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;
    /** Alpine:Widget F-Droid package url */
    public static final String ALPINE_WIDGET_FDROID_PACKAGE_URL = ALPINE_GITHUB_REPO_URL;





    /*
     * Alpine plugin apps lists.
     */

    public static final List<String> ALPINE_PLUGIN_APP_NAMES_LIST = Arrays.asList(
        ALPINE_API_APP_NAME,
        ALPINE_BOOT_APP_NAME,
        ALPINE_FLOAT_APP_NAME,
        ALPINE_STYLING_APP_NAME,
        ALPINE_TASKER_APP_NAME,
        ALPINE_WIDGET_APP_NAME);

    public static final List<String> ALPINE_PLUGIN_APP_PACKAGE_NAMES_LIST = Arrays.asList(
        ALPINE_API_PACKAGE_NAME,
        ALPINE_BOOT_PACKAGE_NAME,
        ALPINE_FLOAT_PACKAGE_NAME,
        ALPINE_STYLING_PACKAGE_NAME,
        ALPINE_TASKER_PACKAGE_NAME,
        ALPINE_WIDGET_PACKAGE_NAME);





    /*
     * Alpine APK releases.
     */

    /** F-Droid APK release */
    public static final String APK_RELEASE_FDROID = "F-Droid"; // Default: "F-Droid"

    /** F-Droid APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST = "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"; // Default: "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"

    /** GitHub APK release */
    public static final String APK_RELEASE_GITHUB = "Recovery debug";

    /** GitHub APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST = "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"; // Default: "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"

    /** Google Play Store APK release */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE = "Google Play Store"; // Default: "Google Play Store"

    /** Google Play Store APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST = "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"; // Default: "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"

    /** Alpine Devs APK release */
    public static final String APK_RELEASE_ALPINE_DEVS = "Alpine Devs"; // Default: "Alpine Devs"

    /** Alpine Devs APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_ALPINE_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST = "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"; // Default: "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"





    /*
     * Alpine packages urls.
     */

    /** Alpine Packages GitHub repo name */
    public static final String ALPINE_PACKAGES_GITHUB_REPO_NAME = "alpine-packages"; // Default: "alpine-packages"
    /** Alpine Packages GitHub repo url */
    public static final String ALPINE_PACKAGES_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine Packages GitHub issues repo url */
    public static final String ALPINE_PACKAGES_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;


    /** Alpine API apt package name */
    public static final String ALPINE_API_APT_PACKAGE_NAME = "alpine-api"; // Default: "alpine-api"
    /** Alpine API apt GitHub repo name */
    public static final String ALPINE_API_APT_GITHUB_REPO_NAME = "alpine-api-package"; // Default: "alpine-api-package"
    /** Alpine API apt GitHub repo url */
    public static final String ALPINE_API_APT_GITHUB_REPO_URL = ALPINE_GITHUB_REPO_URL;
    /** Alpine API apt GitHub issues repo url */
    public static final String ALPINE_API_APT_GITHUB_ISSUES_REPO_URL = ALPINE_GITHUB_ISSUES_REPO_URL;





    /*
     * Alpine miscellaneous urls.
     */

    /** Alpine Site */
    public static final String ALPINE_SITE = ALPINE_APP_NAME + " Site"; // Default: "Alpine Site"

    /** Alpine Site url */
    public static final String ALPINE_SITE_URL = ALPINE_GITHUB_REPO_URL;

    /** Alpine Wiki */
    public static final String ALPINE_WIKI = ALPINE_APP_NAME + " Wiki"; // Default: "Alpine Wiki"

    /** Alpine Wiki url */
    public static final String ALPINE_WIKI_URL = ALPINE_GITHUB_REPO_URL;

    /** Alpine GitHub wiki repo url */
    public static final String ALPINE_GITHUB_WIKI_REPO_URL = ALPINE_GITHUB_REPO_URL;

    /** Alpine Packages wiki repo url */
    public static final String ALPINE_PACKAGES_GITHUB_WIKI_REPO_URL = ALPINE_GITHUB_REPO_URL;


    /** Alpine support email url */
    public static final String ALPINE_SUPPORT_EMAIL_URL = "";

    /** Alpine support email mailto url */
    public static final String ALPINE_SUPPORT_EMAIL_MAILTO_URL = "";


    /** Alpine Reddit subreddit */
    public static final String ALPINE_REDDIT_SUBREDDIT = "";

    /** Alpine Reddit subreddit url */
    public static final String ALPINE_REDDIT_SUBREDDIT_URL = "";


    /** Alpine donate url */
    public static final String ALPINE_DONATE_URL = "";





    /*
     * Alpine app core directory paths.
     */

    /** Alpine app internal private app data directory path */
    @SuppressLint("SdCardPath")
    public static final String ALPINE_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + ALPINE_PACKAGE_NAME; // Default: "/data/data/com.alpine"
    /** Alpine app internal private app data directory */
    public static final File ALPINE_INTERNAL_PRIVATE_APP_DATA_DIR = new File(ALPINE_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);



    /** Alpine app Files directory path */
    public static final String ALPINE_FILES_DIR_PATH = ALPINE_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"; // Default: "/data/data/com.alpine/files"
    /** Alpine app Files directory */
    public static final File ALPINE_FILES_DIR = new File(ALPINE_FILES_DIR_PATH);



    /** Alpine app $PREFIX directory path */
    public static final String ALPINE_PREFIX_DIR_PATH = ALPINE_FILES_DIR_PATH + "/usr"; // Default: "/data/data/com.alpine/files/usr"
    /** Alpine app $PREFIX directory */
    public static final File ALPINE_PREFIX_DIR = new File(ALPINE_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/bin directory path */
    public static final String ALPINE_BIN_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/bin"; // Default: "/data/data/com.alpine/files/usr/bin"
    /** Alpine app $PREFIX/bin directory */
    public static final File ALPINE_BIN_PREFIX_DIR = new File(ALPINE_BIN_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/etc directory path */
    public static final String ALPINE_ETC_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/etc"; // Default: "/data/data/com.alpine/files/usr/etc"
    /** Alpine app $PREFIX/etc directory */
    public static final File ALPINE_ETC_PREFIX_DIR = new File(ALPINE_ETC_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/include directory path */
    public static final String ALPINE_INCLUDE_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/include"; // Default: "/data/data/com.alpine/files/usr/include"
    /** Alpine app $PREFIX/include directory */
    public static final File ALPINE_INCLUDE_PREFIX_DIR = new File(ALPINE_INCLUDE_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/lib directory path */
    public static final String ALPINE_LIB_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/lib"; // Default: "/data/data/com.alpine/files/usr/lib"
    /** Alpine app $PREFIX/lib directory */
    public static final File ALPINE_LIB_PREFIX_DIR = new File(ALPINE_LIB_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/libexec directory path */
    public static final String ALPINE_LIBEXEC_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/libexec"; // Default: "/data/data/com.alpine/files/usr/libexec"
    /** Alpine app $PREFIX/libexec directory */
    public static final File ALPINE_LIBEXEC_PREFIX_DIR = new File(ALPINE_LIBEXEC_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/share directory path */
    public static final String ALPINE_SHARE_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/share"; // Default: "/data/data/com.alpine/files/usr/share"
    /** Alpine app $PREFIX/share directory */
    public static final File ALPINE_SHARE_PREFIX_DIR = new File(ALPINE_SHARE_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/tmp and $TMPDIR directory path */
    public static final String ALPINE_TMP_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/tmp"; // Default: "/data/data/com.alpine/files/usr/tmp"
    /** Alpine app $PREFIX/tmp and $TMPDIR directory */
    public static final File ALPINE_TMP_PREFIX_DIR = new File(ALPINE_TMP_PREFIX_DIR_PATH);


    /** Alpine app $PREFIX/var directory path */
    public static final String ALPINE_VAR_PREFIX_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "/var"; // Default: "/data/data/com.alpine/files/usr/var"
    /** Alpine app $PREFIX/var directory */
    public static final File ALPINE_VAR_PREFIX_DIR = new File(ALPINE_VAR_PREFIX_DIR_PATH);



    /** Alpine app usr-staging directory path */
    public static final String ALPINE_STAGING_PREFIX_DIR_PATH = ALPINE_FILES_DIR_PATH + "/usr-staging"; // Default: "/data/data/com.alpine/files/usr-staging"
    /** Alpine app usr-staging directory */
    public static final File ALPINE_STAGING_PREFIX_DIR = new File(ALPINE_STAGING_PREFIX_DIR_PATH);



    /** Alpine app $HOME directory path */
    public static final String ALPINE_HOME_DIR_PATH = ALPINE_FILES_DIR_PATH + "/home"; // Default: "/data/data/com.alpine/files/home"
    /** Alpine app $HOME directory */
    public static final File ALPINE_HOME_DIR = new File(ALPINE_HOME_DIR_PATH);


    /** Alpine app config home directory path */
    public static final String ALPINE_CONFIG_HOME_DIR_PATH = ALPINE_HOME_DIR_PATH + "/.config/alpine"; // Default: "/data/data/com.alpine/files/home/.config/alpine"
    /** Alpine app config home directory */
    public static final File ALPINE_CONFIG_HOME_DIR = new File(ALPINE_CONFIG_HOME_DIR_PATH);

    /** Alpine app config $PREFIX directory path */
    public static final String ALPINE_CONFIG_PREFIX_DIR_PATH = ALPINE_ETC_PREFIX_DIR_PATH + "/alpine"; // Default: "/data/data/com.alpine/files/usr/etc/alpine"
    /** Alpine app config $PREFIX directory */
    public static final File ALPINE_CONFIG_PREFIX_DIR = new File(ALPINE_CONFIG_PREFIX_DIR_PATH);


    /** Alpine app data home directory path */
    public static final String ALPINE_DATA_HOME_DIR_PATH = ALPINE_HOME_DIR_PATH + "/.alpine"; // Default: "/data/data/com.alpine/files/home/.alpine"
    /** Alpine app data home directory */
    public static final File ALPINE_DATA_HOME_DIR = new File(ALPINE_DATA_HOME_DIR_PATH);


    /** Alpine app storage home directory path */
    public static final String ALPINE_STORAGE_HOME_DIR_PATH = ALPINE_HOME_DIR_PATH + "/storage"; // Default: "/data/data/com.alpine/files/home/storage"
    /** Alpine app storage home directory */
    public static final File ALPINE_STORAGE_HOME_DIR = new File(ALPINE_STORAGE_HOME_DIR_PATH);



    /** Alpine and plugin apps directory path */
    public static final String ALPINE_APPS_DIR_PATH = ALPINE_FILES_DIR_PATH + "/apps"; // Default: "/data/data/com.alpine/files/apps"
    /** Alpine and plugin apps directory */
    public static final File ALPINE_APPS_DIR = new File(ALPINE_APPS_DIR_PATH);


    /** Alpine app $PREFIX directory path ignored sub file paths to consider it empty */
    public static final List<String> ALPINE_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY = Arrays.asList(
        AlpineConstants.ALPINE_TMP_PREFIX_DIR_PATH, AlpineConstants.ALPINE_ENV_TEMP_FILE_PATH, AlpineConstants.ALPINE_ENV_FILE_PATH);



    /*
     * Alpine app and plugin preferences and properties file paths.
     */

    /** Alpine app default SharedPreferences file basename without extension */
    public static final String ALPINE_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_PACKAGE_NAME + "_preferences"; // Default: "com.alpine_preferences"

    /** Alpine:API app default SharedPreferences file basename without extension */
    public static final String ALPINE_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_API_PACKAGE_NAME + "_preferences"; // Default: "com.alpine.api_preferences"

    /** Alpine:Boot app default SharedPreferences file basename without extension */
    public static final String ALPINE_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_BOOT_PACKAGE_NAME + "_preferences"; // Default: "com.alpine.boot_preferences"

    /** Alpine:Float app default SharedPreferences file basename without extension */
    public static final String ALPINE_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_FLOAT_PACKAGE_NAME + "_preferences"; // Default: "com.alpine.window_preferences"

    /** Alpine:Styling app default SharedPreferences file basename without extension */
    public static final String ALPINE_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_STYLING_PACKAGE_NAME + "_preferences"; // Default: "com.alpine.styling_preferences"

    /** Alpine:Tasker app default SharedPreferences file basename without extension */
    public static final String ALPINE_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_TASKER_PACKAGE_NAME + "_preferences"; // Default: "com.alpine.tasker_preferences"

    /** Alpine:Widget app default SharedPreferences file basename without extension */
    public static final String ALPINE_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = ALPINE_WIDGET_PACKAGE_NAME + "_preferences"; // Default: "com.alpine.widget_preferences"



    /** Alpine app properties primary file path */
    public static final String ALPINE_PROPERTIES_PRIMARY_FILE_PATH = ALPINE_DATA_HOME_DIR_PATH + "/alpine.properties"; // Default: "/data/data/com.alpine/files/home/.alpine/alpine.properties"
    /** Alpine app properties primary file */
    public static final File ALPINE_PROPERTIES_PRIMARY_FILE = new File(ALPINE_PROPERTIES_PRIMARY_FILE_PATH);

    /** Alpine app properties secondary file path */
    public static final String ALPINE_PROPERTIES_SECONDARY_FILE_PATH = ALPINE_CONFIG_HOME_DIR_PATH + "/alpine.properties"; // Default: "/data/data/com.alpine/files/home/.config/alpine/alpine.properties"
    /** Alpine app properties secondary file */
    public static final File ALPINE_PROPERTIES_SECONDARY_FILE = new File(ALPINE_PROPERTIES_SECONDARY_FILE_PATH);

    /** Alpine app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> ALPINE_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        ALPINE_PROPERTIES_PRIMARY_FILE_PATH,
        ALPINE_PROPERTIES_SECONDARY_FILE_PATH);



    /** Alpine:Float app properties primary file path */
    public static final String ALPINE_FLOAT_PROPERTIES_PRIMARY_FILE_PATH = ALPINE_DATA_HOME_DIR_PATH + "/alpine.float.properties"; // Default: "/data/data/com.alpine/files/home/.alpine/alpine.float.properties"
    /** Alpine:Float app properties primary file */
    public static final File ALPINE_FLOAT_PROPERTIES_PRIMARY_FILE = new File(ALPINE_FLOAT_PROPERTIES_PRIMARY_FILE_PATH);

    /** Alpine:Float app properties secondary file path */
    public static final String ALPINE_FLOAT_PROPERTIES_SECONDARY_FILE_PATH = ALPINE_CONFIG_HOME_DIR_PATH + "/alpine.float.properties"; // Default: "/data/data/com.alpine/files/home/.config/alpine/alpine.float.properties"
    /** Alpine:Float app properties secondary file */
    public static final File ALPINE_FLOAT_PROPERTIES_SECONDARY_FILE = new File(ALPINE_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);

    /** Alpine:Float app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> ALPINE_FLOAT_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        ALPINE_FLOAT_PROPERTIES_PRIMARY_FILE_PATH,
        ALPINE_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);



    /** Alpine app and Alpine:Styling colors.properties file path */
    public static final String ALPINE_COLOR_PROPERTIES_FILE_PATH = ALPINE_DATA_HOME_DIR_PATH + "/colors.properties"; // Default: "/data/data/com.alpine/files/home/.alpine/colors.properties"
    /** Alpine app and Alpine:Styling colors.properties file */
    public static final File ALPINE_COLOR_PROPERTIES_FILE = new File(ALPINE_COLOR_PROPERTIES_FILE_PATH);

    /** Alpine app and Alpine:Styling font.ttf file path */
    public static final String ALPINE_FONT_FILE_PATH = ALPINE_DATA_HOME_DIR_PATH + "/font.ttf"; // Default: "/data/data/com.alpine/files/home/.alpine/font.ttf"
    /** Alpine app and Alpine:Styling font.ttf file */
    public static final File ALPINE_FONT_FILE = new File(ALPINE_FONT_FILE_PATH);


    /** Alpine app and plugins crash log file path */
    public static final String ALPINE_CRASH_LOG_FILE_PATH = ALPINE_HOME_DIR_PATH + "/crash_log.md"; // Default: "/data/data/com.alpine/files/home/crash_log.md"

    /** Alpine app and plugins crash log backup file path */
    public static final String ALPINE_CRASH_LOG_BACKUP_FILE_PATH = ALPINE_HOME_DIR_PATH + "/crash_log_backup.md"; // Default: "/data/data/com.alpine/files/home/crash_log_backup.md"


    /** Alpine app environment file path */
    public static final String ALPINE_ENV_FILE_PATH = ALPINE_CONFIG_PREFIX_DIR_PATH + "/alpine.env"; // Default: "/data/data/com.alpine/files/usr/etc/alpine/alpine.env"

    /** Alpine app environment temp file path */
    public static final String ALPINE_ENV_TEMP_FILE_PATH = ALPINE_CONFIG_PREFIX_DIR_PATH + "/alpine.env.tmp"; // Default: "/data/data/com.alpine/files/usr/etc/alpine/alpine.env.tmp"




    /*
     * Alpine app plugin specific paths.
     */

    /** Alpine app directory path to store scripts to be run at boot by Alpine:Boot */
    public static final String ALPINE_BOOT_SCRIPTS_DIR_PATH = ALPINE_DATA_HOME_DIR_PATH + "/boot"; // Default: "/data/data/com.alpine/files/home/.alpine/boot"
    /** Alpine app directory to store scripts to be run at boot by Alpine:Boot */
    public static final File ALPINE_BOOT_SCRIPTS_DIR = new File(ALPINE_BOOT_SCRIPTS_DIR_PATH);


    /** Alpine app directory path to store foreground scripts that can be run by the alpine launcher
     * widget provided by Alpine:Widget */
    public static final String ALPINE_SHORTCUT_SCRIPTS_DIR_PATH = ALPINE_HOME_DIR_PATH + "/.shortcuts"; // Default: "/data/data/com.alpine/files/home/.shortcuts"
    /** Alpine app directory to store foreground scripts that can be run by the alpine launcher widget provided by Alpine:Widget */
    public static final File ALPINE_SHORTCUT_SCRIPTS_DIR = new File(ALPINE_SHORTCUT_SCRIPTS_DIR_PATH);


    /** Alpine app directory basename that stores background scripts that can be run by the alpine
     * launcher widget provided by Alpine:Widget */
    public static final String ALPINE_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME =  "tasks"; // Default: "tasks"
    /** Alpine app directory path to store background scripts that can be run by the alpine launcher
     * widget provided by Alpine:Widget */
    public static final String ALPINE_SHORTCUT_TASKS_SCRIPTS_DIR_PATH = ALPINE_SHORTCUT_SCRIPTS_DIR_PATH + "/" + ALPINE_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME; // Default: "/data/data/com.alpine/files/home/.shortcuts/tasks"
    /** Alpine app directory to store background scripts that can be run by the alpine launcher widget provided by Alpine:Widget */
    public static final File ALPINE_SHORTCUT_TASKS_SCRIPTS_DIR = new File(ALPINE_SHORTCUT_TASKS_SCRIPTS_DIR_PATH);


    /** Alpine app directory basename that stores icons for the foreground and background scripts
     * that can be run by the alpine launcher widget provided by Alpine:Widget */
    public static final String ALPINE_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME =  "icons"; // Default: "icons"
    /** Alpine app directory path to store icons for the foreground and background scripts that can
     * be run by the alpine launcher widget provided by Alpine:Widget */
    public static final String ALPINE_SHORTCUT_SCRIPT_ICONS_DIR_PATH = ALPINE_SHORTCUT_SCRIPTS_DIR_PATH + "/" + ALPINE_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME; // Default: "/data/data/com.alpine/files/home/.shortcuts/icons"
    /** Alpine app directory to store icons for the foreground and background scripts that can be
     * run by the alpine launcher widget provided by Alpine:Widget */
    public static final File ALPINE_SHORTCUT_SCRIPT_ICONS_DIR = new File(ALPINE_SHORTCUT_SCRIPT_ICONS_DIR_PATH);


    /** Alpine app directory path to store scripts to be run by 3rd party twofortyfouram locale plugin
     * host apps like Tasker app via the Alpine:Tasker plugin client */
    public static final String ALPINE_TASKER_SCRIPTS_DIR_PATH = ALPINE_DATA_HOME_DIR_PATH + "/tasker"; // Default: "/data/data/com.alpine/files/home/.alpine/tasker"
    /** Alpine app directory to store scripts to be run by 3rd party twofortyfouram locale plugin host apps like Tasker app via the Alpine:Tasker plugin client */
    public static final File ALPINE_TASKER_SCRIPTS_DIR = new File(ALPINE_TASKER_SCRIPTS_DIR_PATH);





    /*
     * Alpine app and plugins notification variables.
     */

    /** Alpine app notification channel id used by {@link ALPINE_APP.ALPINE_SERVICE} */
    public static final String ALPINE_APP_NOTIFICATION_CHANNEL_ID = "alpine_notification_channel";
    /** Alpine app notification channel name used by {@link ALPINE_APP.ALPINE_SERVICE} */
    public static final String ALPINE_APP_NOTIFICATION_CHANNEL_NAME = AlpineConstants.ALPINE_APP_NAME + " App";
    /** Alpine app unique notification id used by {@link ALPINE_APP.ALPINE_SERVICE} */
    public static final int ALPINE_APP_NOTIFICATION_ID = 1337;

    /** Alpine app notification channel id used by {@link ALPINE_APP.RUN_COMMAND_SERVICE} */
    public static final String ALPINE_RUN_COMMAND_NOTIFICATION_CHANNEL_ID = "alpine_run_command_notification_channel";
    /** Alpine app notification channel name used by {@link ALPINE_APP.RUN_COMMAND_SERVICE} */
    public static final String ALPINE_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME = AlpineConstants.ALPINE_APP_NAME + " RunCommandService";
    /** Alpine app unique notification id used by {@link ALPINE_APP.RUN_COMMAND_SERVICE} */
    public static final int ALPINE_RUN_COMMAND_NOTIFICATION_ID = 1338;

    /** Alpine app notification channel id used for plugin command errors */
    public static final String ALPINE_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID = "alpine_plugin_command_errors_notification_channel";
    /** Alpine app notification channel name used for plugin command errors */
    public static final String ALPINE_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME = AlpineConstants.ALPINE_APP_NAME + " Plugin Commands Errors";

    /** Alpine app notification channel id used for crash reports */
    public static final String ALPINE_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID = "alpine_crash_reports_notification_channel";
    /** Alpine app notification channel name used for crash reports */
    public static final String ALPINE_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME = AlpineConstants.ALPINE_APP_NAME + " Crash Reports";


    /** Alpine app notification channel id used by {@link ALPINE_FLOAT_APP.ALPINE_FLOAT_SERVICE} */
    public static final String ALPINE_FLOAT_APP_NOTIFICATION_CHANNEL_ID = "alpine_float_notification_channel";
    /** Alpine app notification channel name used by {@link ALPINE_FLOAT_APP.ALPINE_FLOAT_SERVICE} */
    public static final String ALPINE_FLOAT_APP_NOTIFICATION_CHANNEL_NAME = AlpineConstants.ALPINE_FLOAT_APP_NAME + " App";
    /** Alpine app unique notification id used by {@link ALPINE_APP.ALPINE_SERVICE} */
    public static final int ALPINE_FLOAT_APP_NOTIFICATION_ID = 1339;





    /*
     * Alpine app and plugins miscellaneous variables.
     */

    /** Android OS permission declared by Alpine app in AndroidManifest.xml which can be requested by
     * 3rd party apps to run various commands in Alpine app context */
    public static final String PERMISSION_RUN_COMMAND = ALPINE_PACKAGE_NAME + ".permission.RUN_COMMAND"; // Default: "com.alpine.permission.RUN_COMMAND"

    /** Alpine property defined in alpine.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in Alpine app context */
    public static final String PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"; // Default: "allow-external-apps"
    /** Default value for {@link #PROP_ALLOW_EXTERNAL_APPS} */
    public static final String PROP_DEFAULT_VALUE_ALLOW_EXTERNAL_APPS = "false"; // Default: "false"

    /** The broadcast action sent when Alpine App opens */
    public static final String BROADCAST_ALPINE_OPENED = ALPINE_PACKAGE_NAME + ".app.OPENED";

    /** The Uri authority for Alpine app file shares */
    public static final String ALPINE_FILE_SHARE_URI_AUTHORITY = ALPINE_PACKAGE_NAME + ".files"; // Default: "com.alpine.files"

    /** The normal comma character (U+002C, &comma;, &#44;, comma) */
    public static final String COMMA_NORMAL = ","; // Default: ","

    /** The alternate comma character (U+201A, &sbquo;, &#8218;, single low-9 quotation mark) that
     * may be used instead of {@link #COMMA_NORMAL} */
    public static final String COMMA_ALTERNATIVE = "‚"; // Default: "‚"

    /** Environment variable prefix root for the Alpine app. */
    public static final String ALPINE_ENV_PREFIX_ROOT = "ALPINE";






    /**
     * Alpine app constants.
     */
    public static final class ALPINE_APP {

        /** Alpine apps directory path */
        public static final String APPS_DIR_PATH = ALPINE_APPS_DIR_PATH + "/" + ALPINE_PACKAGE_NAME; // Default: "/data/data/com.alpine/files/apps/com.alpine"

        /** alpine-am socket file path */
        public static final String ALPINE_AM_SOCKET_FILE_PATH = APPS_DIR_PATH + "/alpine-am/am.sock"; // Default: "/data/data/com.alpine/files/apps/com.alpine/alpine-am/am.sock"


        /** Alpine app BuildConfig class name */
        public static final String BUILD_CONFIG_CLASS_NAME = ALPINE_PACKAGE_NAME + ".BuildConfig"; // Default: "com.alpine.BuildConfig"

        /** Alpine app FileShareReceiverActivity class name */
        public static final String FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME = ALPINE_PACKAGE_NAME + ".app.api.file.FileShareReceiverActivity"; // Default: "com.alpine.app.api.file.FileShareReceiverActivity"

        /** Alpine app FileViewReceiverActivity class name */
        public static final String FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME = ALPINE_PACKAGE_NAME + ".app.api.file.FileViewReceiverActivity"; // Default: "com.alpine.app.api.file.FileViewReceiverActivity"


        /** Alpine app core activity name. */
        public static final String ALPINE_ACTIVITY_NAME = ALPINE_PACKAGE_NAME + ".app.AlpineActivity"; // Default: "com.alpine.app.AlpineActivity"

        /**
         * Alpine app core activity.
         */
        public static final class ALPINE_ACTIVITY {

            /** Intent extra for if alpine failsafe session needs to be started and is used by {@link ALPINE_ACTIVITY} and {@link ALPINE_SERVICE#ACTION_STOP_SERVICE} */
            public static final String EXTRA_FAILSAFE_SESSION = AlpineConstants.ALPINE_PACKAGE_NAME + ".app.failsafe_session"; // Default: "com.alpine.app.failsafe_session"


            /** Intent action to make alpine app notify user that a crash happened. */
            public static final String ACTION_NOTIFY_APP_CRASH = AlpineConstants.ALPINE_PACKAGE_NAME + ".app.notify_app_crash"; // Default: "com.alpine.app.notify_app_crash"


            /** Intent action to make alpine reload its alpine session styling */
            public static final String ACTION_RELOAD_STYLE = AlpineConstants.ALPINE_PACKAGE_NAME + ".app.reload_style"; // Default: "com.alpine.app.reload_style"
            /** Intent {@code String} extra for what to reload for the ALPINE_ACTIVITY.ACTION_RELOAD_STYLE intent. This has been deperecated. */
            @Deprecated
            public static final String EXTRA_RELOAD_STYLE = AlpineConstants.ALPINE_PACKAGE_NAME + ".app.reload_style"; // Default: "com.alpine.app.reload_style"

            /**  Intent {@code boolean} extra for whether to recreate activity for the ALPINE_ACTIVITY.ACTION_RELOAD_STYLE intent. */
            public static final String EXTRA_RECREATE_ACTIVITY = ALPINE_APP.ALPINE_ACTIVITY_NAME + ".EXTRA_RECREATE_ACTIVITY"; // Default: "com.alpine.app.AlpineActivity.EXTRA_RECREATE_ACTIVITY"


            /** Intent action to make alpine request storage permissions */
            public static final String ACTION_REQUEST_PERMISSIONS = AlpineConstants.ALPINE_PACKAGE_NAME + ".app.request_storage_permissions"; // Default: "com.alpine.app.request_storage_permissions"
        }





        /** Alpine app settings activity name. */
        public static final String ALPINE_SETTINGS_ACTIVITY_NAME = ALPINE_PACKAGE_NAME + ".app.activities.SettingsActivity"; // Default: "com.alpine.app.activities.SettingsActivity"





        /** Alpine app core service name. */
        public static final String ALPINE_SERVICE_NAME = ALPINE_PACKAGE_NAME + ".app.AlpineService"; // Default: "com.alpine.app.AlpineService"

        /**
         * Alpine app core service.
         */
        public static final class ALPINE_SERVICE {

            /** Intent action to stop ALPINE_SERVICE */
            public static final String ACTION_STOP_SERVICE = ALPINE_PACKAGE_NAME + ".service_stop"; // Default: "com.alpine.service_stop"


            /** Intent action to make ALPINE_SERVICE acquire a wakelock */
            public static final String ACTION_WAKE_LOCK = ALPINE_PACKAGE_NAME + ".service_wake_lock"; // Default: "com.alpine.service_wake_lock"


            /** Intent action to make ALPINE_SERVICE release wakelock */
            public static final String ACTION_WAKE_UNLOCK = ALPINE_PACKAGE_NAME + ".service_wake_unlock"; // Default: "com.alpine.service_wake_unlock"


            /** Intent action to execute command with ALPINE_SERVICE */
            public static final String ACTION_SERVICE_EXECUTE = ALPINE_PACKAGE_NAME + ".service_execute"; // Default: "com.alpine.service_execute"

            /** Uri scheme for paths sent via intent to ALPINE_SERVICE */
            public static final String URI_SCHEME_SERVICE_EXECUTE = ALPINE_PACKAGE_NAME + ".file"; // Default: "com.alpine.file"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_ARGUMENTS = ALPINE_PACKAGE_NAME + ".execute.arguments"; // Default: "com.alpine.execute.arguments"
            /** Intent {@code String} extra for stdin of the command for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_STDIN = ALPINE_PACKAGE_NAME + ".execute.stdin"; // Default: "com.alpine.execute.stdin"
            /** Intent {@code String} extra for command current working directory for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_WORKDIR = ALPINE_PACKAGE_NAME + ".execute.cwd"; // Default: "com.alpine.execute.cwd"
            /** Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            @Deprecated
            public static final String EXTRA_BACKGROUND = ALPINE_PACKAGE_NAME + ".execute.background"; // Default: "com.alpine.execute.background"
            /** Intent {@code String} extra for command the {@link Runner} for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RUNNER = ALPINE_PACKAGE_NAME + ".execute.runner"; // Default: "com.alpine.execute.runner"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.alpine.shared.logger.Logger} for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = ALPINE_PACKAGE_NAME + ".execute.background_custom_log_level"; // Default: "com.alpine.execute.background_custom_log_level"
            /** Intent {@code String} extra for session action for {@link Runner#TERMINAL_SESSION} commands for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SESSION_ACTION = ALPINE_PACKAGE_NAME + ".execute.session_action"; // Default: "com.alpine.execute.session_action"
            /** Intent {@code String} extra for shell name for commands for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SHELL_NAME = ALPINE_PACKAGE_NAME + ".execute.shell_name"; // Default: "com.alpine.execute.shell_name"
            /** Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent. */
            public static final String EXTRA_SHELL_CREATE_MODE = ALPINE_PACKAGE_NAME + ".execute.shell_create_mode"; // Default: "com.alpine.execute.shell_create_mode"
            /** Intent {@code String} extra for label of the command for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_LABEL = ALPINE_PACKAGE_NAME + ".execute.command_label"; // Default: "com.alpine.execute.command_label"
            /** Intent markdown {@code String} extra for description of the command for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = ALPINE_PACKAGE_NAME + ".execute.command_description"; // Default: "com.alpine.execute.command_description"
            /** Intent markdown {@code String} extra for help of the command for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_HELP = ALPINE_PACKAGE_NAME + ".execute.command_help"; // Default: "com.alpine.execute.command_help"
            /** Intent markdown {@code String} extra for help of the plugin API for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent (Internal Use Only) */
            public static final String EXTRA_PLUGIN_API_HELP = ALPINE_PACKAGE_NAME + ".execute.plugin_api_help"; // Default: "com.alpine.execute.plugin_help"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the
             * result of the execution command to the execute command caller for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_PENDING_INTENT = "pendingIntent"; // Default: "pendingIntent"
            /** Intent {@code String} extra for the directory path in which to write the result of the
             * execution command for the execute command caller for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_DIRECTORY = ALPINE_PACKAGE_NAME + ".execute.result_directory"; // Default: "com.alpine.execute.result_directory"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = ALPINE_PACKAGE_NAME + ".execute.result_single_file"; // Default: "com.alpine.execute.result_single_file"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = ALPINE_PACKAGE_NAME + ".execute.result_file_basename"; // Default: "com.alpine.execute.result_file_basename"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = ALPINE_PACKAGE_NAME + ".execute.result_file_output_format"; // Default: "com.alpine.execute.result_file_output_format"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = ALPINE_PACKAGE_NAME + ".execute.result_file_error_format"; // Default: "com.alpine.execute.result_file_error_format"
            /** Intent {@code String} extra for the optional suffix of the result files that should
             * be created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = ALPINE_PACKAGE_NAME + ".execute.result_files_suffix"; // Default: "com.alpine.execute.result_files_suffix"



            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session and will start {@link ALPINE_ACTIVITY} if its not running to bring
             * the new session to foreground.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY = 0;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session and will start {@link ALPINE_ACTIVITY} if its not running to
             * bring the existing session to foreground. The new session will be added to the left
             * sidebar in the sessions list.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY = 1;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session but will not start {@link ALPINE_ACTIVITY} if its not running
             * and session(s) will be seen in Alpine notification and can be clicked to bring new
             * session to foreground. If the {@link ALPINE_ACTIVITY} is already running, then this
             * will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY = 2;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session but will not start {@link ALPINE_ACTIVITY} if its not running
             * and session(s) will be seen in Alpine notification and can be clicked to bring
             * existing session to foreground. If the {@link ALPINE_ACTIVITY} is already running,
             * then this will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY = 3;

            /** The minimum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MIN_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY;

            /** The maximum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MAX_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY;


            /** Intent {@code Bundle} extra to store result of execute command that is sent back for the
             * ALPINE_SERVICE.ACTION_SERVICE_EXECUTE intent if the {@link #EXTRA_PENDING_INTENT} is not
             * {@code null} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE = "result"; // Default: "result"
            /** Intent {@code String} extra for stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"; // Default: "stdout"
            /** Intent {@code String} extra for original length of stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"; // Default: "stdout_original_length"
            /** Intent {@code String} extra for stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"; // Default: "stderr"
            /** Intent {@code String} extra for original length of stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"; // Default: "stderr_original_length"
            /** Intent {@code int} extra for exit code value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"; // Default: "exitCode"
            /** Intent {@code int} extra for err value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"; // Default: "err"
            /** Intent {@code String} extra for errmsg value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"; // Default: "errmsg"

        }





        /** Alpine app run command service name. */
        public static final String RUN_COMMAND_SERVICE_NAME = ALPINE_PACKAGE_NAME + ".app.RunCommandService"; // Alpine app service to receive commands from 3rd party apps "com.alpine.app.RunCommandService"

        /**
         * Alpine app run command service to receive commands sent by 3rd party apps.
         */
        public static final class RUN_COMMAND_SERVICE {

            /** Alpine RUN_COMMAND Intent help url */
            public static final String RUN_COMMAND_API_HELP_URL = ALPINE_GITHUB_REPO_URL;


            /** Intent action to execute command with RUN_COMMAND_SERVICE */
            public static final String ACTION_RUN_COMMAND = ALPINE_PACKAGE_NAME + ".RUN_COMMAND"; // Default: "com.alpine.RUN_COMMAND"

            /** Intent {@code String} extra for absolute path of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_PATH = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_PATH"; // Default: "com.alpine.RUN_COMMAND_PATH"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_ARGUMENTS = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_ARGUMENTS"; // Default: "com.alpine.RUN_COMMAND_ARGUMENTS"
            /** Intent {@code boolean} extra for whether to replace comma alternative characters in arguments with comma characters for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.alpine.RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
            /** Intent {@code String} extra for the comma alternative characters in arguments that should be replaced instead of the default {@link #COMMA_ALTERNATIVE} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.alpine.RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"

            /** Intent {@code String} extra for stdin of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_STDIN = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_STDIN"; // Default: "com.alpine.RUN_COMMAND_STDIN"
            /** Intent {@code String} extra for current working directory of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_WORKDIR = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_WORKDIR"; // Default: "com.alpine.RUN_COMMAND_WORKDIR"
            /** Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            @Deprecated
            public static final String EXTRA_BACKGROUND = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND"; // Default: "com.alpine.RUN_COMMAND_BACKGROUND"
            /** Intent {@code String} extra for command the {@link Runner} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RUNNER = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RUNNER"; // Default: "com.alpine.RUN_COMMAND_RUNNER"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.alpine.shared.logger.Logger} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"; // Default: "com.alpine.RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"
            /** Intent {@code String} extra for session action of {@link Runner#TERMINAL_SESSION} commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SESSION_ACTION = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_SESSION_ACTION"; // Default: "com.alpine.RUN_COMMAND_SESSION_ACTION"
            /** Intent {@code String} extra for shell name of commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SHELL_NAME = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_SHELL_NAME"; // Default: "com.alpine.RUN_COMMAND_SHELL_NAME"
            /** Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent. */
            public static final String EXTRA_SHELL_CREATE_MODE = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_SHELL_CREATE_MODE"; // Default: "com.alpine.RUN_COMMAND_SHELL_CREATE_MODE"
            /** Intent {@code String} extra for label of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_LABEL = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_LABEL"; // Default: "com.alpine.RUN_COMMAND_COMMAND_LABEL"
            /** Intent markdown {@code String} extra for description of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_DESCRIPTION"; // Default: "com.alpine.RUN_COMMAND_COMMAND_DESCRIPTION"
            /** Intent markdown {@code String} extra for help of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_HELP = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_HELP"; // Default: "com.alpine.RUN_COMMAND_COMMAND_HELP"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the result of the execution command to the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_PENDING_INTENT = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_PENDING_INTENT"; // Default: "com.alpine.RUN_COMMAND_PENDING_INTENT"
            /** Intent {@code String} extra for the directory path in which to write the result of
             * the execution command for the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_DIRECTORY = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RESULT_DIRECTORY"; // Default: "com.alpine.RUN_COMMAND_RESULT_DIRECTORY"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RESULT_SINGLE_FILE"; // Default: "com.alpine.RUN_COMMAND_RESULT_SINGLE_FILE"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_BASENAME"; // Default: "com.alpine.RUN_COMMAND_RESULT_FILE_BASENAME"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"; // Default: "com.alpine.RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"; // Default: "com.alpine.RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"
            /** Intent {@code String} extra for the optional suffix of the result files that should be
             * created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = ALPINE_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILES_SUFFIX"; // Default: "com.alpine.RUN_COMMAND_RESULT_FILES_SUFFIX"

        }
    }


    /**
     * Alpine:API app constants.
     */
    public static final class ALPINE_API_APP {

        /** Alpine:API app main activity name. */
        public static final String ALPINE_API_MAIN_ACTIVITY_NAME = ALPINE_API_PACKAGE_NAME + ".activities.AlpineAPIMainActivity"; // Default: "com.alpine.api.activities.AlpineAPIMainActivity"

        /** Alpine:API app launcher activity name. This is an `activity-alias` for {@link #ALPINE_API_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String ALPINE_API_LAUNCHER_ACTIVITY_NAME = ALPINE_API_PACKAGE_NAME + ".activities.AlpineAPILauncherActivity"; // Default: "com.alpine.api.activities.AlpineAPILauncherActivity"

    }





    /**
     * Alpine:Boot app constants.
     */
    public static final class ALPINE_BOOT_APP {

        /** Alpine:Boot app main activity name. */
        public static final String ALPINE_BOOT_MAIN_ACTIVITY_NAME = ALPINE_BOOT_PACKAGE_NAME + ".activities.AlpineBootMainActivity"; // Default: "com.alpine.boot.activities.AlpineBootMainActivity"

        /** Alpine:Boot app launcher activity name. This is an `activity-alias` for {@link #ALPINE_BOOT_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String ALPINE_BOOT_LAUNCHER_ACTIVITY_NAME = ALPINE_BOOT_PACKAGE_NAME + ".activities.AlpineBootLauncherActivity"; // Default: "com.alpine.boot.activities.AlpineBootLauncherActivity"

    }





    /**
     * Alpine:Float app constants.
     */
    public static final class ALPINE_FLOAT_APP {

        /** Alpine:Float app core activity name. */
        public static final String ALPINE_FLOAT_ACTIVITY_NAME = ALPINE_FLOAT_PACKAGE_NAME + ".AlpineFloatActivity"; // Default: "com.alpine.window.AlpineFloatActivity"

        /** Alpine:Float app core service name. */
        public static final String ALPINE_FLOAT_SERVICE_NAME = ALPINE_FLOAT_PACKAGE_NAME + ".AlpineFloatService"; // Default: "com.alpine.window.AlpineFloatService"

        /**
         * Alpine:Float app core service.
         */
        public static final class ALPINE_FLOAT_SERVICE {

            /** Intent action to stop ALPINE_FLOAT_SERVICE. */
            public static final String ACTION_STOP_SERVICE = ALPINE_FLOAT_PACKAGE_NAME + ".ACTION_STOP_SERVICE"; // Default: "com.alpine.float.ACTION_STOP_SERVICE"

            /** Intent action to show float window. */
            public static final String ACTION_SHOW = ALPINE_FLOAT_PACKAGE_NAME + ".ACTION_SHOW"; // Default: "com.alpine.float.ACTION_SHOW"

            /** Intent action to hide float window. */
            public static final String ACTION_HIDE = ALPINE_FLOAT_PACKAGE_NAME + ".ACTION_HIDE"; // Default: "com.alpine.float.ACTION_HIDE"

        }

    }





    /**
     * Alpine:Styling app constants.
     */
    public static final class ALPINE_STYLING_APP {

        /** Alpine:Styling app core activity name. */
        public static final String ALPINE_STYLING_ACTIVITY_NAME = ALPINE_STYLING_PACKAGE_NAME + ".AlpineStyleActivity"; // Default: "com.alpine.styling.AlpineStyleActivity"


        /** Alpine:Styling app main activity name. */
        public static final String ALPINE_STYLING_MAIN_ACTIVITY_NAME = ALPINE_STYLING_PACKAGE_NAME + ".activities.AlpineStylingMainActivity"; // Default: "com.alpine.styling.activities.AlpineStylingMainActivity"

        /** Alpine:Styling app launcher activity name. This is an `activity-alias` for {@link #ALPINE_STYLING_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String ALPINE_STYLING_LAUNCHER_ACTIVITY_NAME = ALPINE_STYLING_PACKAGE_NAME + ".activities.AlpineStylingLauncherActivity"; // Default: "com.alpine.styling.activities.AlpineStylingLauncherActivity"

    }





    /**
     * Alpine:Tasker app constants.
     */
    public static final class ALPINE_TASKER_APP {

        /** Alpine:Tasker app main activity name. */
        public static final String ALPINE_TASKER_MAIN_ACTIVITY_NAME = ALPINE_TASKER_PACKAGE_NAME + ".activities.AlpineTaskerMainActivity"; // Default: "com.alpine.tasker.activities.AlpineTaskerMainActivity"

        /** Alpine:Tasker app launcher activity name. This is an `activity-alias` for {@link #ALPINE_TASKER_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String ALPINE_TASKER_LAUNCHER_ACTIVITY_NAME = ALPINE_TASKER_PACKAGE_NAME + ".activities.AlpineTaskerLauncherActivity"; // Default: "com.alpine.tasker.activities.AlpineTaskerLauncherActivity"

    }





    /**
     * Alpine:Widget app constants.
     */
    public static final class ALPINE_WIDGET_APP {

        /** Alpine:Widget app main activity name. */
        public static final String ALPINE_WIDGET_MAIN_ACTIVITY_NAME = ALPINE_WIDGET_PACKAGE_NAME + ".activities.AlpineWidgetMainActivity"; // Default: "com.alpine.widget.activities.AlpineWidgetMainActivity"

        /** Alpine:Widget app launcher activity name. This is an `activity-alias` for {@link #ALPINE_WIDGET_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String ALPINE_WIDGET_LAUNCHER_ACTIVITY_NAME = ALPINE_WIDGET_PACKAGE_NAME + ".activities.AlpineWidgetLauncherActivity"; // Default: "com.alpine.widget.activities.AlpineWidgetLauncherActivity"


        /**  Intent {@code String} extra for the token of the Alpine:Widget app shortcuts. */
        public static final String EXTRA_TOKEN_NAME = ALPINE_PACKAGE_NAME + ".shortcut.token"; // Default: "com.alpine.shortcut.token"


        /**
         * Alpine:Widget app {@link android.appwidget.AppWidgetProvider} class.
         */
        public static final class ALPINE_WIDGET_PROVIDER {

            /** Intent action for if an item is clicked in the widget. */
            public static final String ACTION_WIDGET_ITEM_CLICKED = ALPINE_WIDGET_PACKAGE_NAME + ".ACTION_WIDGET_ITEM_CLICKED"; // Default: "com.alpine.widget.ACTION_WIDGET_ITEM_CLICKED"


            /** Intent action to refresh files in the widget. */
            public static final String ACTION_REFRESH_WIDGET = ALPINE_WIDGET_PACKAGE_NAME + ".ACTION_REFRESH_WIDGET"; // Default: "com.alpine.widget.ACTION_REFRESH_WIDGET"


            /**  Intent {@code String} extra for the file clicked for the ALPINE_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED intent. */
            public static final String EXTRA_FILE_CLICKED = ALPINE_WIDGET_PACKAGE_NAME + ".EXTRA_FILE_CLICKED"; // Default: "com.alpine.widget.EXTRA_FILE_CLICKED"

        }

    }

}
