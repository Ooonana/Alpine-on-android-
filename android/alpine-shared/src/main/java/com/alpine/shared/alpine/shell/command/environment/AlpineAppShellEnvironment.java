package com.alpine.shared.alpine.shell.command.environment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.android.PackageUtils;
import com.alpine.shared.android.SELinuxUtils;
import com.alpine.shared.data.DataUtils;
import com.alpine.shared.shell.command.environment.ShellEnvironmentUtils;
import com.alpine.shared.alpine.AlpineBootstrap;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.AlpineUtils;
import com.alpine.shared.alpine.shell.am.AlpineAmSocketServer;

import java.util.HashMap;
import java.util.Locale;

/**
 * Environment for {@link AlpineConstants#ALPINE_PACKAGE_NAME} app.
 */
public class AlpineAppShellEnvironment {

    /** Alpine app environment variables. */
    public static HashMap<String, String> alpineAppEnvironment;

    /** Environment variable for the Alpine app version. */
    public static final String ENV_ALPINE_VERSION = AlpineConstants.ALPINE_ENV_PREFIX_ROOT + "_VERSION";

    /** Environment variable prefix for the Alpine app. */
    public static final String ALPINE_APP_ENV_PREFIX = AlpineConstants.ALPINE_ENV_PREFIX_ROOT + "_APP__";

    /** Environment variable for the Alpine app version name. */
    public static final String ENV_ALPINE_APP__VERSION_NAME = ALPINE_APP_ENV_PREFIX + "VERSION_NAME";
    /** Environment variable for the Alpine app version code. */
    public static final String ENV_ALPINE_APP__VERSION_CODE = ALPINE_APP_ENV_PREFIX + "VERSION_CODE";
    /** Environment variable for the Alpine app package name. */
    public static final String ENV_ALPINE_APP__PACKAGE_NAME = ALPINE_APP_ENV_PREFIX + "PACKAGE_NAME";
    /** Environment variable for the Alpine app process id. */
    public static final String ENV_ALPINE_APP__PID = ALPINE_APP_ENV_PREFIX + "PID";
    /** Environment variable for the Alpine app uid. */
    public static final String ENV_ALPINE_APP__UID = ALPINE_APP_ENV_PREFIX + "UID";
    /** Environment variable for the Alpine app targetSdkVersion. */
    public static final String ENV_ALPINE_APP__TARGET_SDK = ALPINE_APP_ENV_PREFIX + "TARGET_SDK";
    /** Environment variable for the Alpine app is debuggable apk build. */
    public static final String ENV_ALPINE_APP__IS_DEBUGGABLE_BUILD = ALPINE_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD";
    /** Environment variable for the Alpine app {@link AlpineConstants} APK_RELEASE_*. */
    public static final String ENV_ALPINE_APP__APK_RELEASE = ALPINE_APP_ENV_PREFIX + "APK_RELEASE";
    /** Environment variable for the Alpine app install path. */
    public static final String ENV_ALPINE_APP__APK_PATH = ALPINE_APP_ENV_PREFIX + "APK_PATH";
    /** Environment variable for the Alpine app is installed on external/portable storage. */
    public static final String ENV_ALPINE_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE = ALPINE_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE";

    /** Environment variable for the Alpine app process selinux context. */
    public static final String ENV_ALPINE_APP__SE_PROCESS_CONTEXT = ALPINE_APP_ENV_PREFIX + "SE_PROCESS_CONTEXT";
    /** Environment variable for the Alpine app data files selinux context. */
    public static final String ENV_ALPINE_APP__SE_FILE_CONTEXT = ALPINE_APP_ENV_PREFIX + "SE_FILE_CONTEXT";
    /** Environment variable for the Alpine app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
    public static final String ENV_ALPINE_APP__SE_INFO = ALPINE_APP_ENV_PREFIX + "SE_INFO";
    /** Environment variable for the Alpine app user id. */
    public static final String ENV_ALPINE_APP__USER_ID = ALPINE_APP_ENV_PREFIX + "USER_ID";
    /** Environment variable for the Alpine app profile owner. */
    public static final String ENV_ALPINE_APP__PROFILE_OWNER = ALPINE_APP_ENV_PREFIX + "PROFILE_OWNER";

    /** Environment variable for the Alpine app {@link AlpineBootstrap#ALPINE_APP_PACKAGE_MANAGER}. */
    public static final String ENV_ALPINE_APP__PACKAGE_MANAGER = ALPINE_APP_ENV_PREFIX + "PACKAGE_MANAGER";
    /** Environment variable for the Alpine app {@link AlpineBootstrap#ALPINE_APP_PACKAGE_VARIANT}. */
    public static final String ENV_ALPINE_APP__PACKAGE_VARIANT = ALPINE_APP_ENV_PREFIX + "PACKAGE_VARIANT";
    /** Environment variable for the Alpine app files directory. */
    public static final String ENV_ALPINE_APP__FILES_DIR = ALPINE_APP_ENV_PREFIX + "FILES_DIR";


    /** Environment variable for the Alpine app {@link AlpineAmSocketServer#getAlpineAppAMSocketServerEnabled(Context)}. */
    public static final String ENV_ALPINE_APP__AM_SOCKET_SERVER_ENABLED = ALPINE_APP_ENV_PREFIX + "AM_SOCKET_SERVER_ENABLED";



    /** Get shell environment for Alpine app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        setAlpineAppEnvironment(currentPackageContext);
        return alpineAppEnvironment;
    }

    /** Set Alpine app environment variables in {@link #alpineAppEnvironment}. */
    public synchronized static void setAlpineAppEnvironment(@NonNull Context currentPackageContext) {
        boolean isAlpineApp = AlpineConstants.ALPINE_PACKAGE_NAME.equals(currentPackageContext.getPackageName());

        // If current package context is of alpine app and its environment is already set, then no need to set again since it won't change
        // Other apps should always set environment again since alpine app may be installed/updated/deleted in background
        if (alpineAppEnvironment != null && isAlpineApp)
            return;

        alpineAppEnvironment = null;

        String packageName = AlpineConstants.ALPINE_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return;
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName);
        if (applicationInfo == null || !applicationInfo.enabled) return;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_VERSION, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__VERSION_CODE, String.valueOf(PackageUtils.getVersionCodeForPackage(packageInfo)));

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__PACKAGE_NAME, packageName);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__PID, AlpineUtils.getAlpineAppPID(currentPackageContext));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__UID, String.valueOf(PackageUtils.getUidForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__TARGET_SDK, String.valueOf(PackageUtils.getTargetSDKForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__APK_PATH, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo));

        putAlpineAPKSignature(currentPackageContext, environment);

        Context alpinePackageContext = AlpineUtils.getAlpinePackageContext(currentPackageContext);
        if (alpinePackageContext != null) {
            // An app that does not have the same sharedUserId as alpine app will not be able to get
            // get alpine context's classloader to get BuildConfig.ALPINE_PACKAGE_VARIANT via reflection.
            // Check AlpineBootstrap.setAlpinePackageManagerAndVariantFromAlpineApp()
            if (AlpineBootstrap.ALPINE_APP_PACKAGE_MANAGER != null)
                environment.put(ENV_ALPINE_APP__PACKAGE_MANAGER, AlpineBootstrap.ALPINE_APP_PACKAGE_MANAGER.getName());
            if (AlpineBootstrap.ALPINE_APP_PACKAGE_VARIANT != null)
                environment.put(ENV_ALPINE_APP__PACKAGE_VARIANT, AlpineBootstrap.ALPINE_APP_PACKAGE_VARIANT.getName());

            // Will not be set for plugins
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__AM_SOCKET_SERVER_ENABLED,
                AlpineAmSocketServer.getAlpineAppAMSocketServerEnabled(currentPackageContext));

            String filesDirPath = currentPackageContext.getFilesDir().getAbsolutePath();
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__FILES_DIR, filesDirPath);

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__SE_PROCESS_CONTEXT, SELinuxUtils.getContext());
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(filesDirPath));

            String seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                (DataUtils.isNullOrEmpty(seInfoUser) ? "" : seInfoUser));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__USER_ID, String.valueOf(PackageUtils.getUserIdForPackage(currentPackageContext)));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext));
        }

        alpineAppEnvironment = environment;
    }

    /** Put {@link #ENV_ALPINE_APP__APK_RELEASE} in {@code environment}. */
    public static void putAlpineAPKSignature(@NonNull Context currentPackageContext,
                                             @NonNull HashMap<String, String> environment) {
        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext,
            AlpineConstants.ALPINE_PACKAGE_NAME);
        if (signingCertificateSHA256Digest != null) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_APP__APK_RELEASE,
                AlpineUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase(Locale.ROOT));
        }
    }

    /** Update {@link #ENV_ALPINE_APP__AM_SOCKET_SERVER_ENABLED} value in {@code environment}. */
    public synchronized static void updateAlpineAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        if (alpineAppEnvironment == null) return;
        alpineAppEnvironment.remove(ENV_ALPINE_APP__AM_SOCKET_SERVER_ENABLED);
        ShellEnvironmentUtils.putToEnvIfSet(alpineAppEnvironment, ENV_ALPINE_APP__AM_SOCKET_SERVER_ENABLED,
            AlpineAmSocketServer.getAlpineAppAMSocketServerEnabled(currentPackageContext));
    }

}
