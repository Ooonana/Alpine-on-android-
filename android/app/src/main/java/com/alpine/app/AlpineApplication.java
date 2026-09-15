package com.alpine.app;

import android.app.Application;
import android.content.Context;

import com.alpine.BuildConfig;
import com.alpine.shared.errors.Error;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.alpine.AlpineBootstrap;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.crash.AlpineCrashUtils;
import com.alpine.shared.alpine.file.AlpineFileUtils;
import com.alpine.shared.alpine.settings.preferences.AlpineAppSharedPreferences;
import com.alpine.shared.alpine.settings.properties.AlpineAppSharedProperties;
import com.alpine.shared.alpine.shell.command.environment.AlpineShellEnvironment;
import com.alpine.shared.alpine.shell.am.AlpineAmSocketServer;
import com.alpine.shared.alpine.shell.AlpineShellManager;
import com.alpine.shared.alpine.theme.AlpineThemeUtils;

public class AlpineApplication extends Application {

    private static final String LOG_TAG = "AlpineApplication";

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        AlpineCrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set AlpineBootstrap.ALPINE_APP_PACKAGE_MANAGER and AlpineBootstrap.ALPINE_APP_PACKAGE_VARIANT
        AlpineBootstrap.setAlpinePackageManagerAndVariant(BuildConfig.ALPINE_PACKAGE_VARIANT);

        // Init app wide SharedProperties loaded from alpine.properties
        AlpineAppSharedProperties properties = AlpineAppSharedProperties.init(context);

        // Init app wide shell manager
        AlpineShellManager shellManager = AlpineShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        AlpineThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create alpine files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = AlpineFileUtils.isAlpineFilesDirectoryAccessible(this, true, true);
        boolean isAlpineFilesDirectoryAccessible = error == null;
        if (isAlpineFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "Alpine files directory is accessible");

            error = AlpineFileUtils.isAppsAlpineAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/alpine-app directory failed\n" + error);
                return;
            }

            // Setup alpine-am-socket server
            AlpineAmSocketServer.setupAlpineAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "Alpine files directory is not accessible\n" + error);
        }

        // Init AlpineShellEnvironment constants and caches after everything has been setup including alpine-am-socket server
        AlpineShellEnvironment.init(this);

        if (isAlpineFilesDirectoryAccessible) {
            AlpineShellEnvironment.writeEnvironmentToFile(this);
        }
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(AlpineConstants.ALPINE_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        AlpineAppSharedPreferences preferences = AlpineAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

}
