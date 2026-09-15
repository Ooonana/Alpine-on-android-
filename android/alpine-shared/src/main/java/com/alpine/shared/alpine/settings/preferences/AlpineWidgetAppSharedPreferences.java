package com.alpine.shared.alpine.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.logger.Logger;
import com.alpine.shared.android.PackageUtils;
import com.alpine.shared.settings.preferences.AppSharedPreferences;
import com.alpine.shared.settings.preferences.SharedPreferenceUtils;
import com.alpine.shared.alpine.AlpineUtils;
import com.alpine.shared.alpine.settings.preferences.AlpinePreferenceConstants.ALPINE_WIDGET_APP;
import com.alpine.shared.alpine.AlpineConstants;

import java.util.UUID;

public class AlpineWidgetAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "AlpineWidgetAppSharedPreferences";

    private AlpineWidgetAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                AlpineConstants.ALPINE_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                AlpineConstants.ALPINE_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link AlpineWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_WIDGET_PACKAGE_NAME}.
     * @return Returns the {@link AlpineWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static AlpineWidgetAppSharedPreferences build(@NonNull final Context context) {
        Context alpineWidgetPackageContext = PackageUtils.getContextForPackage(context, AlpineConstants.ALPINE_WIDGET_PACKAGE_NAME);
        if (alpineWidgetPackageContext == null)
            return null;
        else
            return new AlpineWidgetAppSharedPreferences(alpineWidgetPackageContext);
    }

    /**
     * Get the {@link AlpineWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link AlpineConstants#ALPINE_WIDGET_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link AlpineWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static AlpineWidgetAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context alpineWidgetPackageContext = AlpineUtils.getContextForPackageOrExitApp(context, AlpineConstants.ALPINE_WIDGET_PACKAGE_NAME, exitAppOnError);
        if (alpineWidgetPackageContext == null)
            return null;
        else
            return new AlpineWidgetAppSharedPreferences(alpineWidgetPackageContext);
    }



    public static String getGeneratedToken(@NonNull Context context) {
        AlpineWidgetAppSharedPreferences preferences = AlpineWidgetAppSharedPreferences.build(context, true);
        if (preferences == null) return null;
        return preferences.getGeneratedToken();
    }

    public String getGeneratedToken() {
        String token =  SharedPreferenceUtils.getString(mSharedPreferences, ALPINE_WIDGET_APP.KEY_TOKEN, null, true);
        if (token == null) {
            token = UUID.randomUUID().toString();
            SharedPreferenceUtils.setString(mSharedPreferences, ALPINE_WIDGET_APP.KEY_TOKEN, token, true);
        }
        return token;
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, ALPINE_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, ALPINE_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, ALPINE_WIDGET_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
