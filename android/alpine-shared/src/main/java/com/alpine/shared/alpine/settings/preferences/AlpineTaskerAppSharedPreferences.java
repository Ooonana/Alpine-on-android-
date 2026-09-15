package com.alpine.shared.alpine.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.android.PackageUtils;
import com.alpine.shared.settings.preferences.AppSharedPreferences;
import com.alpine.shared.settings.preferences.SharedPreferenceUtils;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.AlpineUtils;
import com.alpine.shared.alpine.settings.preferences.AlpinePreferenceConstants.ALPINE_TASKER_APP;
import com.alpine.shared.logger.Logger;

public class AlpineTaskerAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "AlpineTaskerAppSharedPreferences";

    private  AlpineTaskerAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                AlpineConstants.ALPINE_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                AlpineConstants.ALPINE_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link AlpineTaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_TASKER_PACKAGE_NAME}.
     * @return Returns the {@link AlpineTaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static AlpineTaskerAppSharedPreferences build(@NonNull final Context context) {
        Context alpineTaskerPackageContext = PackageUtils.getContextForPackage(context, AlpineConstants.ALPINE_TASKER_PACKAGE_NAME);
        if (alpineTaskerPackageContext == null)
            return null;
        else
            return new AlpineTaskerAppSharedPreferences(alpineTaskerPackageContext);
    }

    /**
     * Get {@link AlpineTaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_TASKER_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link AlpineTaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static  AlpineTaskerAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context alpineTaskerPackageContext = AlpineUtils.getContextForPackageOrExitApp(context, AlpineConstants.ALPINE_TASKER_PACKAGE_NAME, exitAppOnError);
        if (alpineTaskerPackageContext == null)
            return null;
        else
            return new AlpineTaskerAppSharedPreferences(alpineTaskerPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, ALPINE_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, ALPINE_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, ALPINE_TASKER_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }



    public int getLastPendingIntentRequestCode() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, ALPINE_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, ALPINE_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE);
    }

    public void setLastPendingIntentRequestCode(int lastPendingIntentRequestCode) {
        SharedPreferenceUtils.setInt(mSharedPreferences, ALPINE_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, lastPendingIntentRequestCode, false);
    }

}
