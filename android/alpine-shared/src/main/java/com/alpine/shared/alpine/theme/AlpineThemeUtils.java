package com.alpine.shared.alpine.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.alpine.settings.properties.AlpinePropertyConstants;
import com.alpine.shared.alpine.settings.properties.AlpineSharedProperties;
import com.alpine.shared.theme.NightMode;

public class AlpineThemeUtils {

    /** Get the {@link AlpinePropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk
     * and set it to app wide night mode value. */
    public static void setAppNightMode(@NonNull Context context) {
        NightMode.setAppNightMode(AlpineSharedProperties.getNightMode(context));
    }

    /** Set name as app wide night mode value. */
    public static void setAppNightMode(@Nullable String name) {
        NightMode.setAppNightMode(name);
    }

}
