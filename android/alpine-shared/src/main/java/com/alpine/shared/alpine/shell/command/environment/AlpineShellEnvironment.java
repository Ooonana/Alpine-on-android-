package com.alpine.shared.alpine.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.alpine.shared.errors.Error;
import com.alpine.shared.file.FileUtils;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.shell.command.ExecutionCommand;
import com.alpine.shared.shell.command.environment.AndroidShellEnvironment;
import com.alpine.shared.shell.command.environment.ShellEnvironmentUtils;
import com.alpine.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.alpine.shared.alpine.AlpineBootstrap;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.shell.AlpineShellUtils;

import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Environment for Alpine.
 */
public class AlpineShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "AlpineShellEnvironment";

    /** Environment variable for the alpine {@link AlpineConstants#ALPINE_PREFIX_DIR_PATH}. */
    public static final String ENV_PREFIX = "PREFIX";

    public AlpineShellEnvironment() {
        super();
        shellCommandShellEnvironment = new AlpineShellCommandShellEnvironment();
    }


    /** Init {@link AlpineShellEnvironment} constants and caches. */
    public synchronized static void init(@NonNull Context currentPackageContext) {
        AlpineAppShellEnvironment.setAlpineAppEnvironment(currentPackageContext);
    }

    /** Init {@link AlpineShellEnvironment} constants and caches. */
    public synchronized static void writeEnvironmentToFile(@NonNull Context currentPackageContext) {
        HashMap<String, String> environmentMap = new AlpineShellEnvironment().getEnvironment(currentPackageContext, false);
        String environmentString = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(environmentMap);

        // Write environment string to temp file and then move to final location since otherwise
        // writing may happen while file is being sourced/read
        Error error = FileUtils.writeTextToFile("alpine.env.tmp", AlpineConstants.ALPINE_ENV_TEMP_FILE_PATH,
            Charset.defaultCharset(), environmentString, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return;
        }

        error = FileUtils.moveRegularFile("alpine.env.tmp", AlpineConstants.ALPINE_ENV_TEMP_FILE_PATH, AlpineConstants.ALPINE_ENV_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    /** Get shell environment for Alpine. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {

        // Alpine environment builds upon the Android environment
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, isFailSafe);

        HashMap<String, String> alpineAppEnvironment = AlpineAppShellEnvironment.getEnvironment(currentPackageContext);
        if (alpineAppEnvironment != null)
            environment.putAll(alpineAppEnvironment);

        HashMap<String, String> alpineApiAppEnvironment = AlpineAPIShellEnvironment.getEnvironment(currentPackageContext);
        if (alpineApiAppEnvironment != null)
            environment.putAll(alpineApiAppEnvironment);

        environment.put(ENV_HOME, AlpineConstants.ALPINE_HOME_DIR_PATH);
        environment.put(ENV_PREFIX, AlpineConstants.ALPINE_PREFIX_DIR_PATH);

        // If failsafe is not enabled, then we keep default PATH and TMPDIR so that system binaries can be used
        if (!isFailSafe) {
            environment.put(ENV_TMPDIR, AlpineConstants.ALPINE_TMP_PREFIX_DIR_PATH);
            if (AlpineBootstrap.isAppPackageVariantAPTAndroid5()) {
                // Alpine in android 5/6 era shipped busybox binaries in applets directory
                environment.put(ENV_PATH, AlpineConstants.ALPINE_BIN_PREFIX_DIR_PATH + ":" + AlpineConstants.ALPINE_BIN_PREFIX_DIR_PATH + "/applets");
                environment.put(ENV_LD_LIBRARY_PATH, AlpineConstants.ALPINE_LIB_PREFIX_DIR_PATH);
            } else {
                // Alpine binaries on Android 7+ rely on DT_RUNPATH, so LD_LIBRARY_PATH should be unset by default
                environment.put(ENV_PATH, AlpineConstants.ALPINE_BIN_PREFIX_DIR_PATH);
                environment.remove(ENV_LD_LIBRARY_PATH);
            }
        }

        return environment;
    }


    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return AlpineConstants.ALPINE_HOME_DIR_PATH;
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return AlpineConstants.ALPINE_BIN_PREFIX_DIR_PATH;
    }

    @NonNull
    @Override
    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
        return AlpineShellUtils.setupShellCommandArguments(executable, arguments);
    }

}
