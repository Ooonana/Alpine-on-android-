package com.alpine.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alpine.R;
import com.alpine.shared.alpine.settings.preferences.AlpineAppSharedPreferences;

@Keep
public class AlpinePreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(AlpinePreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.alpine_preferences, rootKey);
    }

}

class AlpinePreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final AlpineAppSharedPreferences mPreferences;

    private static AlpinePreferencesDataStore mInstance;

    private AlpinePreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = AlpineAppSharedPreferences.build(context, true);
    }

    public static synchronized AlpinePreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlpinePreferencesDataStore(context);
        }
        return mInstance;
    }

}
