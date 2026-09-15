package com.alpine.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.alpine.app.AlpineActivity;
import com.alpine.app.terminal.AlpineTerminalSessionActivityClient;
import com.alpine.app.terminal.AlpineTerminalViewClient;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.alpine.extrakeys.ExtraKeysConstants;
import com.alpine.shared.alpine.extrakeys.ExtraKeysInfo;
import com.alpine.shared.alpine.settings.properties.AlpinePropertyConstants;
import com.alpine.shared.alpine.settings.properties.AlpineSharedProperties;
import com.alpine.shared.alpine.terminal.io.TerminalExtraKeys;
import com.alpine.view.TerminalView;

import org.json.JSONException;

public class AlpineTerminalExtraKeys extends TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final AlpineActivity mActivity;
    final AlpineTerminalViewClient mAlpineTerminalViewClient;
    final AlpineTerminalSessionActivityClient mAlpineTerminalSessionActivityClient;

    private static final String LOG_TAG = "AlpineTerminalExtraKeys";

    public AlpineTerminalExtraKeys(AlpineActivity activity, @NonNull TerminalView terminalView,
                                   AlpineTerminalViewClient alpineTerminalViewClient,
                                   AlpineTerminalSessionActivityClient alpineTerminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mAlpineTerminalViewClient = alpineTerminalViewClient;
        mAlpineTerminalSessionActivityClient = alpineTerminalSessionActivityClient;

        setExtraKeys();
    }


    /**
     * Set the terminal extra keys and style.
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        try {
            // The mMap stores the extra key and style string values while loading properties
            // Check {@link #getExtraKeysInternalPropertyValueFromValue(String)} and
            // {@link #getExtraKeysStyleInternalPropertyValueFromValue(String)}
            String extrakeys = (String) mActivity.getProperties().getInternalPropertyValue(AlpinePropertyConstants.KEY_EXTRA_KEYS, true);
            String extraKeysStyle = (String) mActivity.getProperties().getInternalPropertyValue(AlpinePropertyConstants.KEY_EXTRA_KEYS_STYLE, true);

            ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle);
            if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY.equals(extraKeyDisplayMap) && !AlpinePropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE.equals(extraKeysStyle)) {
                Logger.logError(AlpineSharedProperties.LOG_TAG, "The style \"" + extraKeysStyle + "\" for the key \"" + AlpinePropertyConstants.KEY_EXTRA_KEYS_STYLE + "\" is invalid. Using default style instead.");
                extraKeysStyle = AlpinePropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
            }

            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            Logger.showToast(mActivity, "Could not load and set the \"" + AlpinePropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: " + e.toString(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not load and set the \"" + AlpinePropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(AlpinePropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS, AlpinePropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (JSONException e2) {
                Logger.showToast(mActivity, "Can't create default extra keys",true);
                Logger.logStackTraceWithMessage(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mAlpineTerminalViewClient != null)
                mAlpineTerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mAlpineTerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mAlpineTerminalSessionActivityClient != null)
                mAlpineTerminalSessionActivityClient.onPasteTextFromClipboard(null);
        }  else if ("SCROLL".equals(key)) {
            TerminalView terminalView = mAlpineTerminalViewClient.getActivity().getTerminalView();
            if (terminalView != null && terminalView.mEmulator != null)
                terminalView.mEmulator.toggleAutoScrollDisabled();
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
