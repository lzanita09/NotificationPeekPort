/*
 * Copyright (C) 2014 ParanoidAndroid Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reindeercrafts.notificationpeek.peek;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

public class SensorActivityHandler {

    public final static String ACTION_UPDATE_SENSOR_USE = "NotificationPeek.update_sensor_use";

    private final static String TAG = "NotificationPeek.SensorActivityHandler";

    private final static int INCREMENTS_TO_DISABLE = 5;
    private final static float NOISE_THRESHOLD = 0.5f;

    // Minimum proximity detected distance from object. Some devices use 0/1 to indicate
    // "near"/"far", others use actual values.
    private static final float MIN_PROX_DISTANCE = 3.0f;

    private SensorManager mSensorManager;
    private SensorEventListener mProximityEventListener;
    private SensorEventListener mGyroscopeEventListener;
    private Sensor mProximityLightSensor;
    private Sensor mGyroscopeSensor;

    private ScreenReceiver mScreenReceiver;

    private SensorChangedCallback mCallback;
    private Context mContext;

    private float mLastX = 0, mLastY = 0, mLastZ = 0;
    private int mSensorIncrement = 0;

    private boolean mWaitingForMovement;
    private boolean mHasInitialValues;
    private boolean mScreenReceiverRegistered;
    private boolean mProximityRegistered;
    private boolean mGyroscopeRegistered;

    private boolean mInPocket;
    private boolean mOnTable;

    // User preferences of using sensors or not.
    private boolean mUseGyroSensor;
    private boolean mUseProxLightSensor;

    public SensorActivityHandler(Context context, SensorChangedCallback callback) {
        mContext = context;
        mCallback = callback;

        mScreenReceiver = new ScreenReceiver();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        mUseProxLightSensor = preferences.getBoolean(PreferenceKeys.PREF_PROX_LIGHT_SENSOR, true);
        mUseGyroSensor = preferences.getBoolean(PreferenceKeys.PREF_GYRO_SENSOR, true);

        initSensors(context);

    }

    private void initSensors(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initProximityLightSensor();
        initGyroscopeSensor();
    }

    private void initGyroscopeSensor() {
        // get gyroscope sensor for on-table detection
        if (mUseGyroSensor) {
            mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        if (mGyroscopeSensor != null) {
            mGyroscopeEventListener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

                @Override
                public void onSensorChanged(SensorEvent event) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[1];
                    boolean storeValues = false;
                    if (mHasInitialValues) {
                        float dX = Math.abs(mLastX - x);
                        float dY = Math.abs(mLastY - y);
                        float dZ = Math.abs(mLastZ - z);
                        if (dX >= NOISE_THRESHOLD ||
                                dY >= NOISE_THRESHOLD || dZ >= NOISE_THRESHOLD) {
                            if (mWaitingForMovement) {
                                if (NotificationPeek.DEBUG) {
                                    Log.d(TAG, "On table: false");
                                }
                                mOnTable = false;

                                // If proximity/light sensor is not used, set mInPocket to the same
                                // as mOnTable to synchronize status.
                                if (!mUseProxLightSensor) {
                                    mInPocket = mOnTable;
                                }

                                mCallback.onTableModeChanged(mOnTable);
                                registerEventListeners();
                                mWaitingForMovement = false;
                                mSensorIncrement = 0;
                            }
                            storeValues = true;
                        } else {
                            if (mSensorIncrement < INCREMENTS_TO_DISABLE) {
                                mSensorIncrement++;
                                if (mSensorIncrement == INCREMENTS_TO_DISABLE) {
                                    unregisterProximityLightEvent();
                                    if (NotificationPeek.DEBUG) {
                                        Log.d(TAG, "On table: true");
                                    }
                                    mOnTable = true;

                                    if (!mUseProxLightSensor) {
                                        mInPocket = mOnTable;
                                    }

                                    mCallback.onTableModeChanged(mOnTable);
                                    mWaitingForMovement = true;
                                }
                            }
                        }
                    }

                    if (!mHasInitialValues || storeValues) {
                        mHasInitialValues = true;
                        mLastX = x;
                        mLastY = y;
                        mLastZ = z;
                    }
                }
            };
        } else {
            // no accelerometer? time to buy a nexus
        }
    }

    private void initProximityLightSensor() {
        // get proximity sensor for in-pocket detection, if no proximity sensor detected, try
        // light sensor.
        if (mUseProxLightSensor) {
            mProximityLightSensor =
                    mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null ?
                            mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) :
                            mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        if (mProximityLightSensor != null) {
            mProximityEventListener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }

                @Override
                public void onSensorChanged(SensorEvent event) {
                    boolean inPocket = event.values[0] == 0 || event.values[0] <= MIN_PROX_DISTANCE;
                    if (inPocket) {
                        if (mUseGyroSensor) {
                            mOnTable =
                                    false; // we can't have phone on table and pocket at the same time
                        }
                        unregisterGyroscopeEvent();
                    } else {
                        // Only register gyroscope sensor listener if user chooses to use it.
                        if (!mGyroscopeRegistered && mUseGyroSensor) {
                            registerEventListeners();
                        }
                    }
                    if (NotificationPeek.DEBUG) {
                        Log.d(TAG, "In pocket: " + inPocket + ", old: " + mInPocket);
                    }
                    boolean oldInPocket = mInPocket;
                    mInPocket = inPocket;
                    if (!mUseGyroSensor) {
                        mOnTable = mInPocket;
                    }
                    if (oldInPocket != inPocket) {
                        mCallback.onPocketModeChanged(mInPocket);
                    }
                }
            };
        } else {
            // ugh, that's bad, run now that you can.
        }
    }

    public void updateUseSensors() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useGyro = preferences.getBoolean(PreferenceKeys.PREF_GYRO_SENSOR, true);
        boolean useProxLight = preferences.getBoolean(PreferenceKeys.PREF_PROX_LIGHT_SENSOR, true);

        if (useGyro == mUseGyroSensor && useProxLight == mUseProxLightSensor) {
            // Same as current, return.
            return;
        }

        mUseGyroSensor = useGyro;
        mUseProxLightSensor = useProxLight;

        if (!mUseGyroSensor) {
            unregisterGyroscopeEvent();
            mGyroscopeSensor = null;
            mOnTable = false;
        } else {
            initGyroscopeSensor();
        }

        if (!mUseProxLightSensor) {
            unregisterProximityLightEvent();
            mProximityLightSensor = null;
            mInPocket = false;
        } else {
            initProximityLightSensor();
        }

    }

    public boolean isInPocket() {
        return mInPocket;
    }

    public boolean isOnTable() {
        return mOnTable;
    }

    public void registerScreenReceiver() {
        if (!mScreenReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);

            // Add intent filter for listening to sensor use changes.
            intentFilter.addAction(ACTION_UPDATE_SENSOR_USE);

            mContext.registerReceiver(mScreenReceiver, intentFilter);
            mScreenReceiverRegistered = true;
        }
    }

    public void unregisterScreenReceiver() {
        if (mScreenReceiverRegistered) {
            mContext.unregisterReceiver(mScreenReceiver);
            mScreenReceiverRegistered = false;
        }
    }

    public void registerEventListeners() {
        if (mProximityLightSensor != null && !mProximityRegistered && mUseProxLightSensor) {
            if (NotificationPeek.DEBUG) {
                Log.d(TAG, "Registering proximity polling");
            }
            mSensorManager.registerListener(mProximityEventListener, mProximityLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mProximityRegistered = true;
        }
        if (mGyroscopeSensor != null && !mGyroscopeRegistered && mUseGyroSensor) {
            if (NotificationPeek.DEBUG) {
                Log.d(TAG, "Registering gyroscope polling");
            }
            mSensorManager.registerListener(mGyroscopeEventListener, mGyroscopeSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mGyroscopeRegistered = true;
        }
    }

    public void unregisterEventListeners() {
        unregisterProximityLightEvent();
        unregisterGyroscopeEvent();
    }

    private void unregisterProximityLightEvent() {
        if (mProximityLightSensor != null && mProximityRegistered) {
            if (NotificationPeek.DEBUG) {
                Log.d(TAG, "Unregistering proximity polling");
            }
            mSensorManager.unregisterListener(mProximityEventListener);
            mProximityRegistered = false;
        }
    }

    private void unregisterGyroscopeEvent() {
        if (mGyroscopeSensor != null && mGyroscopeRegistered) {
            if (NotificationPeek.DEBUG) {
                Log.d(TAG, "Unregistering gyroscope polling");
            }
            mSensorManager.unregisterListener(mGyroscopeEventListener);
            mLastX = mLastY = mLastZ = 0;
            mSensorIncrement = 0;
            mGyroscopeRegistered = false;
            mHasInitialValues = false;
        }
    }

    public interface SensorChangedCallback {
        public abstract void onPocketModeChanged(boolean inPocket);

        public abstract void onTableModeChanged(boolean onTable);

        public abstract void onScreenStateChanged(boolean screenOn);
    }

    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mCallback.onScreenStateChanged(false);
                registerEventListeners();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mCallback.onScreenStateChanged(true);
                unregisterEventListeners();

            } else if (intent.getAction().equals(ACTION_UPDATE_SENSOR_USE)) {
                // Update sensor use preferences.
                Log.d(TAG, "Update sensor uses");
                updateUseSensors();
            }
        }
    }

}
