package com.reindeercrafts.notificationpeek.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.reindeercrafts.notificationpeek.NotificationService;
import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.peek.SensorActivityHandler;
import com.reindeercrafts.notificationpeek.settings.appearance.AppearanceSettings;
import com.reindeercrafts.notificationpeek.utils.SensorHelper;

/**
 * General settings fragment.
 * <p/>
 * Created by zhelu on 5/3/14.
 */
public class GeneralSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        // Appearance settings entrance.
        Preference appearancePref = findPreference(PreferenceKeys.PREF_APPEARANCE);
        appearancePref.setOnPreferenceClickListener(this);

        // Clock preference.
        CheckBoxPreference clockPref =
                (CheckBoxPreference) findPreference(PreferenceKeys.PREF_CLOCK);
        clockPref.setOnPreferenceChangeListener(this);



        // Always show content preference.
        CheckBoxPreference alwaysShowContentPref =
                (CheckBoxPreference) findPreference(PreferenceKeys.PREF_ALWAYS_SHOW_CONTENT);
        alwaysShowContentPref.setOnPreferenceChangeListener(this);

        // Notification Peek timeout preference.
        ListPreference peekTimeoutPref =
                (ListPreference) findPreference(PreferenceKeys.PREF_PEEK_TIMEOUT);
        peekTimeoutPref.setOnPreferenceChangeListener(this);
        bindPreferenceSummaryToValue(peekTimeoutPref);

        // Listen forever preference.
        ListPreference sensorTimeoutPref =
                (ListPreference) findPreference(PreferenceKeys.PREF_SENSOR_TIMEOUT);
        sensorTimeoutPref.setOnPreferenceChangeListener(this);
        bindPreferenceSummaryToValue(sensorTimeoutPref);

        // Gyroscope sensor enable/disable preference.
        CheckBoxPreference gyroPref =
                (CheckBoxPreference) findPreference(PreferenceKeys.PREF_GYRO_SENSOR);
        if (!SensorHelper.checkSensorStatus(getActivity(), SensorHelper.SENSOR_GYRO, false)) {
            // No gyroscope sensor found.
            gyroPref.setEnabled(false);
        } else {
            gyroPref.setEnabled(true);
            gyroPref.setOnPreferenceChangeListener(this);
        }


        // Proximity/Light sensor enable/disable preference.
        CheckBoxPreference proxPref =
                (CheckBoxPreference) findPreference(PreferenceKeys.PREF_PROX_LIGHT_SENSOR);
        if (!SensorHelper
                .checkSensorStatus(getActivity(), SensorHelper.SENSOR_PROXIMITY_LIGHT, false)) {
            // No proximity or light sensor found.
            proxPref.setEnabled(false);
        } else {
            proxPref.setEnabled(true);
            proxPref.setOnPreferenceChangeListener(this);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String key = preference.getKey();
        if (key.equals(PreferenceKeys.PREF_PEEK_TIMEOUT) ||
                key.equals(PreferenceKeys.PREF_SENSOR_TIMEOUT)) {
            sharedPref.edit().putString(key, (String) newValue).apply();
            bindPreferenceSummaryToValue(preference);

        } else if (key.equals(PreferenceKeys.PREF_ALWAYS_SHOW_CONTENT)) {
            sharedPref.edit().putBoolean(key, (Boolean) newValue).apply();

        } else if (key.equals(PreferenceKeys.PREF_CLOCK)) {
            sharedPref.edit().putBoolean(key, (Boolean) newValue).apply();
            return true;
        } else if (key.equals(PreferenceKeys.PREF_GYRO_SENSOR) ||
                key.equals(PreferenceKeys.PREF_PROX_LIGHT_SENSOR)) {
            sharedPref.edit().putBoolean(key, (Boolean) newValue).apply();

            // Send broadcast to request update sensor use changes.
            getActivity().sendBroadcast(new Intent(SensorActivityHandler.ACTION_UPDATE_SENSOR_USE));
            return true;
        }

        // Send broadcast to NotificationService to update preferences.
        Intent intent = new Intent(NotificationService.ACTION_PREFERENCE_CHANGED);
        intent.putExtra(PreferenceKeys.INTENT_ACTION_KEY, key);
        intent.putExtra(PreferenceKeys.INTENT_ACTION_NEW_VALUE, newValue.toString());
        getActivity().sendBroadcast(intent);

        return true;
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (preference instanceof ListPreference) {
            int index = ((ListPreference) preference)
                    .findIndexOfValue(sharedPref.getString(preference.getKey(), "1"));
            CharSequence newSum = ((ListPreference) preference).getEntries()[index];
            preference.setSummary(newSum);

        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PreferenceKeys.PREF_APPEARANCE)) {
            // Launch appearance settings activity.
            Intent intent = new Intent(getActivity(), AppearanceSettings.class);
            startActivity(intent);
            return true;
        }
        return false;
    }
}
