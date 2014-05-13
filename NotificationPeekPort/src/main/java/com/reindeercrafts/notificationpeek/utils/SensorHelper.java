package com.reindeercrafts.notificationpeek.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

/**
 * Helper class for checking if the device contains specific sensors:
 * - Proximity sensor (or Light sensor).
 * - Gyroscope sensor.
 * <p/>
 * Created by zhelu on 5/12/14.
 */
public class SensorHelper {

    /**
     * Sensor type to check: proxmity/light sensor.
     */
    public static final int SENSOR_PROXIMITY_LIGHT = 1;

    /**
     * Sensor type to check: gyroscope sensor.
     */
    public static final int SENSOR_GYRO = 2;

    /**
     * Check if the given sensor is presented in the device and/or the user choose to use it.
     *
     * @param context           Context instance.
     * @param sensor            Sensor type, can be {@link SensorHelper#SENSOR_GYRO}
     *                          or {@link SensorHelper#SENSOR_PROXIMITY_LIGHT}
     * @param combinePreference Boolean value for whether we need to check the preference or not.
     * @return
     */
    public static boolean checkSensorStatus(Context context, int sensor,
                                            boolean combinePreference) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        switch (sensor) {
            case SENSOR_PROXIMITY_LIGHT:
                Sensor proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) !=
                        null ? sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) : sensorManager
                        .getDefaultSensor(Sensor.TYPE_LIGHT);


                return proxSensor != null &&
                        (preferences.getBoolean(PreferenceKeys.PREF_PROX_LIGHT_SENSOR, true) ||
                                !combinePreference);

            case SENSOR_GYRO:
                Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

                return gyroSensor != null &&
                        (preferences.getBoolean(PreferenceKeys.PREF_GYRO_SENSOR, true) ||
                                !combinePreference);

            default:
                return false;

        }
    }
}
