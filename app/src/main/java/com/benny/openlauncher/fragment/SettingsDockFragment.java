package com.benny.openlauncher.fragment;

import android.os.Bundle;

import net.skywall.openlauncher.R;

public class SettingsDockFragment extends SettingsBaseFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_dock);
    }
}
