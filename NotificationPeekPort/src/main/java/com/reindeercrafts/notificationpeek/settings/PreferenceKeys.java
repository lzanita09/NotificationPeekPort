package com.reindeercrafts.notificationpeek.settings;

/**
 * Static fields for preference keys
 * <p/>
 * Created by zhelu on 5/3/14.
 */
public class PreferenceKeys {

    // Intent's extra key for retrieving changed preference's key.
    public static final String INTENT_ACTION_KEY = "preferences_changed_key";

    // Intent's extra key for retrieving changed preference's new value.
    public static final String INTENT_ACTION_NEW_VALUE = "preferences_changed_new_value";

    // General settings preference keys.
    public static final String PREF_CLOCK = "show_clock";
    public static final String PREF_PEEK_TIMEOUT = "peek_time_out";
    public static final String PREF_SENSOR_TIMEOUT = "sensor_time_out";
    public static final String PREF_GYRO_SENSOR = "gyro_sensor";
    public static final String PREF_PROX_LIGHT_SENSOR = "prox_light_sensor";
    public static final String PREF_ALWAYS_SHOW_CONTENT = "always_show_content";

    // Appearance settings preference keys.
    public static final String PREF_APPEARANCE = "appearance";
    public static final String PREF_BACKGROUND = "background";
    public static final String PREF_RADIUS = "radius";
    public static final String PREF_DIM = "dim";
}
