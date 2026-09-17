package com.alpine.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alpine.R;
import com.alpine.shared.alpine.settings.preferences.AlpineWidgetAppSharedPreferences;

@Keep
public class AlpineWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(AlpineWidgetPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.alpine_widget_preferences, rootKey);
    }

}

class AlpineWidgetPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final AlpineWidgetAppSharedPreferences mPreferences;

    private static AlpineWidgetPreferencesDataStore mInstance;

    private AlpineWidgetPreferencesDataStore(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = AlpineWidgetAppSharedPreferences.build(mContext, true);
    }

    public static synchronized AlpineWidgetPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlpineWidgetPreferencesDataStore(context);
        }
        return mInstance;
    }

}
