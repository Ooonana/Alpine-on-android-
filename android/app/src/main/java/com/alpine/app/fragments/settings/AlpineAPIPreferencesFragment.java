package com.alpine.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alpine.R;
import com.alpine.shared.alpine.settings.preferences.AlpineAPIAppSharedPreferences;

@Keep
public class AlpineAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(AlpineAPIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.alpine_api_preferences, rootKey);
    }

}

class AlpineAPIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final AlpineAPIAppSharedPreferences mPreferences;

    private static AlpineAPIPreferencesDataStore mInstance;

    private AlpineAPIPreferencesDataStore(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = AlpineAPIAppSharedPreferences.build(mContext, true);
    }

    public static synchronized AlpineAPIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlpineAPIPreferencesDataStore(context);
        }
        return mInstance;
    }

}
