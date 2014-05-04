package com.reindeercrafts.notificationpeek.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.reindeercrafts.notificationpeek.R;

/**
 * General settings fragment.
 * <p/>
 * Created by zhelu on 5/3/14.
 */
public class GeneralSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        // Clock preference.
        CheckBoxPreference clockPref =
                (CheckBoxPreference) findPreference(PreferenceKeys.PREF_CLOCK);
        clockPref.setOnPreferenceChangeListener(this);

        // Notification Peek timeout preference.
        ListPreference peekTimeoutPref =
                (ListPreference) findPreference(PreferenceKeys.PREF_PEEK_TIMEOUT);
        peekTimeoutPref.setOnPreferenceChangeListener(this);
        bindPreferenceSummaryToValue(peekTimeoutPref);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String key = preference.getKey();
        if (key.equals(PreferenceKeys.PREF_CLOCK)) {
            sharedPref.edit().putBoolean(key, (Boolean) newValue).apply();
        } else if (key.equals(PreferenceKeys.PREF_PEEK_TIMEOUT)) {
            sharedPref.edit().putString(key, (String) newValue).apply();
            bindPreferenceSummaryToValue(preference);
        }

        return true;
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (preference instanceof ListPreference) {
            int index = ((ListPreference) preference).findIndexOfValue(sharedPref.getString(
                    preference.getKey(), "1"));
            CharSequence newSum = ((ListPreference) preference).getEntries()[index];
            preference.setSummary(newSum);

        }
    }
}
