package com.alpine.shared.alpine.shell.command.environment;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.android.PackageUtils;
import com.alpine.shared.shell.command.environment.ShellEnvironmentUtils;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.AlpineUtils;

import java.util.HashMap;

/**
 * Environment for {@link AlpineConstants#ALPINE_API_PACKAGE_NAME} app.
 */
public class AlpineAPIShellEnvironment {

    /** Environment variable prefix for the Alpine:API app. */
    public static final String ALPINE_API_APP_ENV_PREFIX = AlpineConstants.ALPINE_ENV_PREFIX_ROOT + "_API_APP__";

    /** Environment variable for the Alpine:API app version. */
    public static final String ENV_ALPINE_API_APP__VERSION_NAME = ALPINE_API_APP_ENV_PREFIX + "VERSION_NAME";

    /** Get shell environment for Alpine:API app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        if (AlpineUtils.isAlpineAPIAppInstalled(currentPackageContext) != null) return null;

        String packageName = AlpineConstants.ALPINE_API_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return null;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_ALPINE_API_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));

        return environment;
    }

}
