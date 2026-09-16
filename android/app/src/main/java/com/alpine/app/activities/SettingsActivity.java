package com.alpine.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alpine.R;
import com.alpine.shared.activities.ReportActivity;
import com.alpine.shared.file.FileUtils;
import com.alpine.shared.models.ReportInfo;
import com.alpine.app.models.UserAction;
import com.alpine.shared.alpine.settings.preferences.AlpineAPIAppSharedPreferences;
import com.alpine.shared.alpine.settings.preferences.AlpineFloatAppSharedPreferences;
import com.alpine.shared.alpine.settings.preferences.AlpineTaskerAppSharedPreferences;
import com.alpine.shared.alpine.settings.preferences.AlpineWidgetAppSharedPreferences;
import com.alpine.shared.android.AndroidUtils;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.AlpineUtils;
import com.alpine.shared.activity.media.AppCompatActivityUtils;
import com.alpine.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.alpine.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureAlpineAPIPreference(context);
                    configureAlpineFloatPreference(context);
                    configureAlpineTaskerPreference(context);
                    configureAlpineWidgetPreference(context);
                    configureAboutPreference(context);
                }
            }.start();
        }

        private void configureAlpineAPIPreference(@NonNull Context context) {
            Preference alpineAPIPreference = findPreference("alpine_api");
            if (alpineAPIPreference != null) {
                AlpineAPIAppSharedPreferences preferences = AlpineAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                alpineAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureAlpineFloatPreference(@NonNull Context context) {
            Preference alpineFloatPreference = findPreference("alpine_float");
            if (alpineFloatPreference != null) {
                AlpineFloatAppSharedPreferences preferences = AlpineFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                alpineFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureAlpineTaskerPreference(@NonNull Context context) {
            Preference alpineTaskerPreference = findPreference("alpine_tasker");
            if (alpineTaskerPreference != null) {
                AlpineTaskerAppSharedPreferences preferences = AlpineTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                alpineTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureAlpineWidgetPreference(@NonNull Context context) {
            Preference alpineWidgetPreference = findPreference("alpine_widget");
            if (alpineWidgetPreference != null) {
                AlpineWidgetAppSharedPreferences preferences = AlpineWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                alpineWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(AlpineUtils.getAppInfoMarkdownString(context, AlpineUtils.AppInfoMode.ALPINE_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(AlpineUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                AlpineConstants.ALPINE_APP.ALPINE_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(AlpineConstants.ALPINE_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }

    }

}
