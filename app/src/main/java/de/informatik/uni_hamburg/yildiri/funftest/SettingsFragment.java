package de.informatik.uni_hamburg.yildiri.funftest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

/**
 * Settings fragments to configure preferences of the app. The preferences are defined and loaded from a XML file in the resources (found at /res/xml/preferences.xml).
 * This also does implement an change listener for the preferences in order to update the summary of a preference, whose value has been changed.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Initialize the summary of each preference with their current respective values
        triggerSummaryInit(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference changedPreference = findPreference(key);
        updatePreferenceSummary(changedPreference);
    }

    /**
     * Trigger the initialization of the summary for the given preference
     * @param preference preference whose summary is to be initialized
     */
    private void triggerSummaryInit(Preference preference) {
        // If it is a preference group, call this method recursively on each of it's component preferences
        if(preference instanceof PreferenceGroup) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
            for(int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                triggerSummaryInit(preferenceGroup.getPreference(i));
            }
        } else {
            // if it is an atomic preference we can go on to update its summary
            updatePreferenceSummary(preference);
        }
    }

    /**
     * Update the summary for the given preference with its current value
     * @param preference preference whose summary is to be updated
     */
    private void updatePreferenceSummary(Preference preference) {
        if(preference instanceof EditTextPreference) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            editTextPreference.setSummary(editTextPreference.getText());
        }
    }
}
