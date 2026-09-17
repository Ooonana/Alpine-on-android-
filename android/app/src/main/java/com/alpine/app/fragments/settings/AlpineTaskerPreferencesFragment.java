package com.alpine.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alpine.R;
import com.alpine.shared.alpine.settings.preferences.AlpineTaskerAppSharedPreferences;

@Keep
public class AlpineTaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(AlpineTaskerPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.alpine_tasker_preferences, rootKey);
    }

}

class AlpineTaskerPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final AlpineTaskerAppSharedPreferences mPreferences;

    private static AlpineTaskerPreferencesDataStore mInstance;

    private AlpineTaskerPreferencesDataStore(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = AlpineTaskerAppSharedPreferences.build(mContext, true);
    }

    public static synchronized AlpineTaskerPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlpineTaskerPreferencesDataStore(context);
        }
        return mInstance;
    }

}
