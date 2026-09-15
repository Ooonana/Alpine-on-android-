package com.alpine.shared.alpine.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.alpine.shared.alpine.AlpineConstants;

public class AlpineAppSharedProperties extends AlpineSharedProperties {

    private static AlpineAppSharedProperties properties;


    private AlpineAppSharedProperties(@NonNull Context context) {
        super(context, AlpineConstants.ALPINE_APP_NAME,
            AlpineConstants.ALPINE_PROPERTIES_FILE_PATHS_LIST, AlpinePropertyConstants.ALPINE_APP_PROPERTIES_LIST,
            new AlpineSharedProperties.SharedPropertiesParserClient());
    }

    /**
     * Initialize the {@link #properties} and load properties from disk.
     *
     * @param context The {@link Context} for operations.
     * @return Returns the {@link AlpineAppSharedProperties}.
     */
    public static AlpineAppSharedProperties init(@NonNull Context context) {
        if (properties == null)
            properties = new AlpineAppSharedProperties(context);

        return properties;
    }

    /**
     * Get the {@link #properties}.
     *
     * @return Returns the {@link AlpineAppSharedProperties}.
     */
    public static AlpineAppSharedProperties getProperties() {
        return properties;
    }

}
