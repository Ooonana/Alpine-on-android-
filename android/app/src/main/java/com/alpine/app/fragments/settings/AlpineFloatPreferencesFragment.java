package com.alpine.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alpine.R;
import com.alpine.shared.alpine.settings.preferences.AlpineFloatAppSharedPreferences;

@Keep
public class AlpineFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(AlpineFloatPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.alpine_float_preferences, rootKey);
    }

}

class AlpineFloatPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final AlpineFloatAppSharedPreferences mPreferences;

    private static AlpineFloatPreferencesDataStore mInstance;

    private AlpineFloatPreferencesDataStore(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = AlpineFloatAppSharedPreferences.build(mContext, true);
    }

    public static synchronized AlpineFloatPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlpineFloatPreferencesDataStore(context);
        }
        return mInstance;
    }

}
