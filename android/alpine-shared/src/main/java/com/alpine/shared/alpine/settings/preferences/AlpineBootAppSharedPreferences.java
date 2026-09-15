package com.alpine.shared.alpine.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.logger.Logger;
import com.alpine.shared.android.PackageUtils;
import com.alpine.shared.settings.preferences.AppSharedPreferences;
import com.alpine.shared.settings.preferences.SharedPreferenceUtils;
import com.alpine.shared.alpine.AlpineUtils;
import com.alpine.shared.alpine.settings.preferences.AlpinePreferenceConstants.ALPINE_BOOT_APP;
import com.alpine.shared.alpine.AlpineConstants;

public class AlpineBootAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "AlpineBootAppSharedPreferences";

    private AlpineBootAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                AlpineConstants.ALPINE_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                AlpineConstants.ALPINE_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link AlpineBootAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_BOOT_PACKAGE_NAME}.
     * @return Returns the {@link AlpineBootAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static AlpineBootAppSharedPreferences build(@NonNull final Context context) {
        Context alpineBootPackageContext = PackageUtils.getContextForPackage(context, AlpineConstants.ALPINE_BOOT_PACKAGE_NAME);
        if (alpineBootPackageContext == null)
            return null;
        else
            return new AlpineBootAppSharedPreferences(alpineBootPackageContext);
    }

    /**
     * Get {@link AlpineBootAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_BOOT_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link AlpineBootAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static AlpineBootAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context alpineBootPackageContext = AlpineUtils.getContextForPackageOrExitApp(context, AlpineConstants.ALPINE_BOOT_PACKAGE_NAME, exitAppOnError);
        if (alpineBootPackageContext == null)
            return null;
        else
            return new AlpineBootAppSharedPreferences(alpineBootPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, ALPINE_BOOT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, ALPINE_BOOT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, ALPINE_BOOT_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
