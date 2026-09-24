package com.alpine.app.fragments.settings.alpine;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alpine.R;
import com.alpine.shared.alpine.settings.preferences.AlpineAppSharedPreferences;
import com.alpine.shared.android.PermissionUtils;
import com.alpine.shared.android.PhantomProcessUtils;
import com.alpine.shared.logger.Logger;

@Keep
public class DebuggingPreferencesFragment extends PreferenceFragmentCompat {

    private static final String PHANTOM_PROCESS_ADB_COMMAND =
        "adb shell settings put global settings_enable_monitor_phantom_procs false";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(DebuggingPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.alpine_debugging_preferences, rootKey);

        configureLoggingPreferences(context);
        configureProcessProtectionPreference(context);
    }

    private void configureLoggingPreferences(@NonNull Context context) {
        PreferenceCategory loggingCategory = findPreference("logging");
        if (loggingCategory == null) return;

        ListPreference logLevelListPreference = findPreference("log_level");
        if (logLevelListPreference != null) {
            AlpineAppSharedPreferences preferences = AlpineAppSharedPreferences.build(context, true);
            if (preferences == null) return;

            setLogLevelListPreferenceData(logLevelListPreference, context, preferences.getLogLevel());
            loggingCategory.addPreference(logLevelListPreference);
        }
    }

    private void configureProcessProtectionPreference(@NonNull Context context) {
        PreferenceCategory category = findPreference("process_protection");
        Preference preference = findPreference("android_process_protection");
        if (category == null || preference == null) return;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            category.setVisible(false);
            return;
        }

        Context appContext = context.getApplicationContext();
        preference.setOnPreferenceClickListener(clicked -> {
            clicked.setEnabled(false);
            new Thread(() -> {
                boolean batteryExempt = PermissionUtils.checkIfBatteryOptimizationsDisabled(appContext);
                String monitor = PhantomProcessUtils.getFeatureFlagMonitorPhantomProcsValueString(appContext).getName();
                Integer maxProcesses = PhantomProcessUtils.getActivityManagerMaxPhantomProcesses(appContext);
                String maxText = maxProcesses == null
                    ? appContext.getString(R.string.alpine_process_protection_unknown)
                    : String.valueOf(maxProcesses);
                String batteryText = appContext.getString(batteryExempt
                    ? R.string.alpine_process_protection_battery_exempt
                    : R.string.alpine_process_protection_battery_not_exempt);
                String message = appContext.getString(
                    R.string.alpine_process_protection_dialog_message,
                    batteryText, monitor, maxText);

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    clicked.setEnabled(true);
                    if (!isAdded()) return;
                    showProcessProtectionDialog(message);
                });
            }, "AlpineProcessProtectionCheck").start();
            return true;
        });
    }

    private void showProcessProtectionDialog(@NonNull String message) {
        Context context = getContext();
        if (context == null) return;

        new AlertDialog.Builder(context)
            .setTitle(R.string.alpine_process_protection_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.alpine_process_protection_request_battery,
                (dialog, which) -> PermissionUtils.requestDisableBatteryOptimizations(context))
            .setNeutralButton(R.string.alpine_process_protection_copy_adb, (dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB command", PHANTOM_PROCESS_ADB_COMMAND));
                    Logger.showToast(context, context.getString(R.string.alpine_process_protection_adb_copied), false);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    public static ListPreference setLogLevelListPreferenceData(ListPreference logLevelListPreference, Context context, int logLevel) {
        if (logLevelListPreference == null)
            logLevelListPreference = new ListPreference(context);

        CharSequence[] logLevels = Logger.getLogLevelsArray();
        CharSequence[] logLevelLabels = Logger.getLogLevelLabelsArray(context, logLevels, true);

        logLevelListPreference.setEntryValues(logLevels);
        logLevelListPreference.setEntries(logLevelLabels);

        logLevelListPreference.setValue(String.valueOf(logLevel));
        logLevelListPreference.setDefaultValue(Logger.DEFAULT_LOG_LEVEL);

        return logLevelListPreference;
    }

}

class DebuggingPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final AlpineAppSharedPreferences mPreferences;

    private static DebuggingPreferencesDataStore mInstance;

    private DebuggingPreferencesDataStore(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = AlpineAppSharedPreferences.build(mContext, true);
    }

    public static synchronized DebuggingPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DebuggingPreferencesDataStore(context);
        }
        return mInstance;
    }



    @Override
    @Nullable
    public String getString(String key, @Nullable String defValue) {
        if (mPreferences == null) return null;
        if (key == null) return null;

        switch (key) {
            case "log_level":
                return String.valueOf(mPreferences.getLogLevel());
            default:
                return null;
        }
    }

    @Override
    public void putString(String key, @Nullable String value) {
        if (mPreferences == null) return;
        if (key == null) return;

        switch (key) {
            case "log_level":
                if (value != null) {
                    mPreferences.setLogLevel(mContext, Integer.parseInt(value));
                }
                break;
            default:
                break;
        }
    }



    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null) return;
        if (key == null) return;

        switch (key) {
            case "terminal_view_key_logging_enabled":
                    mPreferences.setTerminalViewKeyLoggingEnabled(value);
                break;
            case "plugin_error_notifications_enabled":
                mPreferences.setPluginErrorNotificationsEnabled(value);
                break;
            case "crash_report_notifications_enabled":
                mPreferences.setCrashReportNotificationsEnabled(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null) return false;
        switch (key) {
            case "terminal_view_key_logging_enabled":
                return mPreferences.isTerminalViewKeyLoggingEnabled();
            case "plugin_error_notifications_enabled":
                return mPreferences.arePluginErrorNotificationsEnabled(false);
            case "crash_report_notifications_enabled":
                return mPreferences.areCrashReportNotificationsEnabled(false);
            default:
                return false;
        }
    }

}
