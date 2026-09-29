package com.alpine.shared.data;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

public class IntentUtils {

    private static final String LOG_TAG = "IntentUtils";

    /**
     * Safely read a parcelable extra supplied by another component/process.
     *
     * Raw {@link Intent#getParcelableExtra(String)} calls can throw while unparcelling malformed
     * extras and, before API 33, rely on an unchecked caller-side cast.  External intent entry
     * points should fail closed instead of crashing the receiving Activity/Service.
     */
    @Nullable
    public static <T extends Parcelable> T getParcelableExtraIfSet(@NonNull Intent intent,
                                                                    @NonNull String key,
                                                                    @NonNull Class<T> clazz) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                return intent.getParcelableExtra(key, clazz);

            Parcelable value = intent.getParcelableExtra(key);
            return clazz.isInstance(value) ? clazz.cast(value) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }


    /**
     * Get a {@link String} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key The {@link String} key name.
     * @param def The default value if extra is not set.
     * @param throwExceptionIfNotSet If set to {@code true}, then an exception will be thrown if extra
     *                               is not set.
     * @return Returns the {@link String} extra if set, otherwise {@code null}.
     */
    public static String getStringExtraIfSet(@NonNull Intent intent, String key, String def, boolean throwExceptionIfNotSet) throws Exception {
        String value = getStringExtraIfSet(intent, key, def);
        if (value == null && throwExceptionIfNotSet)
            throw new Exception("The \"" + key + "\" key string value is null or empty");
        return value;
    }

    /**
     * Get a {@link String} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key The {@link String} key name.
     * @param def The default value if extra is not set.
     * @return Returns the {@link String} extra if set, otherwise {@code null}.
     */
    public static String getStringExtraIfSet(@NonNull Intent intent, String key, String def) {
        String value = intent.getStringExtra(key);
        if (value == null || value.isEmpty()) {
            if (def != null && !def.isEmpty())
                return def;
            else
                return null;
        }
        return value;
    }

    /**
     * Get an {@link Integer} from an {@link Intent} stored as a {@link String} extra if its not
     * {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key The {@link String} key name.
     * @param def The default value if extra is not set.
     * @return Returns the {@link Integer} extra if set, otherwise {@code null}.
     */
    public static Integer getIntegerExtraIfSet(@NonNull Intent intent, String key, Integer def) {
        try {
            String value = intent.getStringExtra(key);
            if (value == null || value.isEmpty()) {
                return def;
            }

            return Integer.parseInt(value);
        }
        catch (Exception e) {
            return def;
        }
    }



    /**
     * Get a {@link String[]} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key The {@link String} key name.
     * @param def The default value if extra is not set.
     * @param throwExceptionIfNotSet If set to {@code true}, then an exception will be thrown if extra
     *                               is not set.
     * @return Returns the {@link String[]} extra if set, otherwise {@code null}.
     */
    public static String[] getStringArrayExtraIfSet(@NonNull Intent intent, String key, String[] def, boolean throwExceptionIfNotSet) throws Exception {
        String[] value = getStringArrayExtraIfSet(intent, key, def);
        if (value == null && throwExceptionIfNotSet)
            throw new Exception("The \"" + key + "\" key string array is null or empty");
        return value;
    }

    /**
     * Get a {@link String[]} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key The {@link String} key name.
     * @param def The default value if extra is not set.
     * @return Returns the {@link String[]} extra if set, otherwise {@code null}.
     */
    public static String[] getStringArrayExtraIfSet(Intent intent, String key, String[] def) {
        String[] value = intent.getStringArrayExtra(key);
        if (value == null || value.length == 0) {
            if (def != null && def.length != 0)
                return def;
            else
                return null;
        }
        return value;
    }

    public static String getIntentString(Intent intent) {
        if (intent == null) return null;

        return intent.toString() + "\n" + getBundleString(intent.getExtras());
    }

    public static String getBundleString(Bundle bundle) {
        if (bundle == null || bundle.size() == 0) return "Bundle[]";

        StringBuilder bundleString = new StringBuilder("Bundle[\n");
        boolean first = true;
        for (String key : bundle.keySet()) {
            if (!first)
                bundleString.append("\n");

            bundleString.append(key).append(": `");

            Object value = bundle.get(key);
            if (value instanceof int[]) {
                bundleString.append(Arrays.toString((int[]) value));
            } else if (value instanceof byte[]) {
                bundleString.append(Arrays.toString((byte[]) value));
            } else if (value instanceof boolean[]) {
                bundleString.append(Arrays.toString((boolean[]) value));
            } else if (value instanceof short[]) {
                bundleString.append(Arrays.toString((short[]) value));
            } else if (value instanceof long[]) {
                bundleString.append(Arrays.toString((long[]) value));
            } else if (value instanceof float[]) {
                bundleString.append(Arrays.toString((float[]) value));
            } else if (value instanceof double[]) {
                bundleString.append(Arrays.toString((double[]) value));
            } else if (value instanceof String[]) {
                bundleString.append(Arrays.toString((String[]) value));
            } else if (value instanceof CharSequence[]) {
                bundleString.append(Arrays.toString((CharSequence[]) value));
            } else if (value instanceof Parcelable[]) {
                bundleString.append(Arrays.toString((Parcelable[]) value));
            } else if (value instanceof Bundle) {
                bundleString.append(getBundleString((Bundle) value));
            } else {
                bundleString.append(value);
            }

            bundleString.append("`");

            first = false;
        }

        bundleString.append("\n]");
        return bundleString.toString();
    }

}
