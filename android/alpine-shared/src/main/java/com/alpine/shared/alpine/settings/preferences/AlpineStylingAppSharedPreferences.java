package com.alpine.shared.alpine.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.logger.Logger;
import com.alpine.shared.android.PackageUtils;
import com.alpine.shared.settings.preferences.AppSharedPreferences;
import com.alpine.shared.settings.preferences.SharedPreferenceUtils;
import com.alpine.shared.alpine.AlpineUtils;
import com.alpine.shared.alpine.settings.preferences.AlpinePreferenceConstants.ALPINE_STYLING_APP;
import com.alpine.shared.alpine.AlpineConstants;

public class AlpineStylingAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "AlpineStylingAppSharedPreferences";

    private AlpineStylingAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                AlpineConstants.ALPINE_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                AlpineConstants.ALPINE_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link AlpineStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_STYLING_PACKAGE_NAME}.
     * @return Returns the {@link AlpineStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static AlpineStylingAppSharedPreferences build(@NonNull final Context context) {
        Context alpineStylingPackageContext = PackageUtils.getContextForPackage(context, AlpineConstants.ALPINE_STYLING_PACKAGE_NAME);
        if (alpineStylingPackageContext == null)
            return null;
        else
            return new AlpineStylingAppSharedPreferences(alpineStylingPackageContext);
    }

    /**
     * Get {@link AlpineStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_STYLING_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link AlpineStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static AlpineStylingAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context alpineStylingPackageContext = AlpineUtils.getContextForPackageOrExitApp(context, AlpineConstants.ALPINE_STYLING_PACKAGE_NAME, exitAppOnError);
        if (alpineStylingPackageContext == null)
            return null;
        else
            return new AlpineStylingAppSharedPreferences(alpineStylingPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, ALPINE_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, ALPINE_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, ALPINE_STYLING_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
