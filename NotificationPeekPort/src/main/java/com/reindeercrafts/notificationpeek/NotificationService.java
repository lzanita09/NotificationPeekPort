package com.reindeercrafts.notificationpeek;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.reindeercrafts.notificationpeek.blacklist.AppList;
import com.reindeercrafts.notificationpeek.peek.NotificationPeek;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;
import com.reindeercrafts.notificationpeek.utils.AccessChecker;


/**
 * Notification listener service. Implemented onNotificationPosted
 * method so that whenever a notification is posted, it'll be saved
 * into database.
 * <p/>
 * Created by zhelu on 2/21/14.
 */
public class NotificationService extends NotificationListenerService {

    private static final String TAG = NotificationService.class.getSimpleName();
    private static final int INVAID_ID = -1;

    public static final String ACTION_DISMISS_NOTIFICATION =
            NotificationActionReceiver.class.getSimpleName() + ".dismiss_notification";

    public static final String ACTION_PREFERENCE_CHANGED =
            NotificationService.class.getSimpleName() + ".preference_changed";


    public static final String EXTRA_PACKAGE_NAME = "PackageName";
    public static final String EXTRA_NOTIFICATION_ID = "NotificationId";
    public static final String EXTRA_NOTIFICATION_TAG = "NotificationTag";

    private int mPeekTimeoutMultiplier;
    private int mSensorTimeoutMultiplier;
    private boolean mShowContent;

    private NotificationHub mNotificationHub;
    private NotificationPeek mNotificationPeek;

    private NotificationActionReceiver mReceiver;

    private AppList mAppList;


    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationHub = NotificationHub.getInstance();
        mNotificationPeek = new NotificationPeek(mNotificationHub, this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Retrieve user specified peek timeout.
        mPeekTimeoutMultiplier =
                Integer.parseInt(preferences.getString(PreferenceKeys.PREF_PEEK_TIMEOUT, "1"));

        // Does user select always listening?
        mSensorTimeoutMultiplier =
                Integer.parseInt(preferences.getString(PreferenceKeys.PREF_SENSOR_TIMEOUT, "1"));

        // Does user select always show content?
        mShowContent = preferences.getBoolean(PreferenceKeys.PREF_ALWAYS_SHOW_CONTENT, false);

        mAppList = AppList.getInstance(this);

        registerNotificationActionReceiver();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        Log.i(TAG, sbn.getPackageName() + " Notification received:" +
                sbn.getNotification().tickerText);

        Notification postedNotification = sbn.getNotification();

        if (postedNotification.tickerText == null ||
                sbn.isOngoing() || !sbn.isClearable() ||
                isInBlackList(sbn)) {
            return;
        }

        mNotificationHub.addNotification(sbn);

        if (AccessChecker.isDeviceAdminEnabled(this)) {
            mNotificationPeek
                    .showNotification(sbn, false, mPeekTimeoutMultiplier, mSensorTimeoutMultiplier,
                            mShowContent);
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        mNotificationHub.removeNotification(sbn);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationPeek.unregisterScreenReceiver();
        unregisterReceiver(mReceiver);
    }

    /**
     * Check if the given package name is in black list or its priority is low.
     * There are some notifications that contain ticker text or content,
     * but they are actually not useful and not from user applications.
     *
     * @param notification Given StatusBarNotification object.
     * @return True if the package is in black list, false otherwise.
     */
    private boolean isInBlackList(StatusBarNotification notification) {

        return notification.getNotification().priority < Notification.PRIORITY_DEFAULT ||
                mAppList.isInBlackList(notification.getPackageName());

    }

    private void registerNotificationActionReceiver() {
        mReceiver = new NotificationActionReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DISMISS_NOTIFICATION);
        intentFilter.addAction(ACTION_PREFERENCE_CHANGED);

        registerReceiver(mReceiver, intentFilter);
    }

    public class NotificationActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_DISMISS_NOTIFICATION)) {
                String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                String tag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG);
                int id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, INVAID_ID);
                cancelNotification(packageName, tag, id);
            } else if (action.equals(ACTION_PREFERENCE_CHANGED)) {

                // Update user preferences.
                String changedKey = intent.getStringExtra(PreferenceKeys.INTENT_ACTION_KEY);
                String newValue = intent.getStringExtra(PreferenceKeys.INTENT_ACTION_NEW_VALUE);

                Log.d(TAG, "Key: " + changedKey + ", value: " + newValue);

                if (changedKey.equals(PreferenceKeys.PREF_PEEK_TIMEOUT)) {
                    mPeekTimeoutMultiplier = Integer.parseInt(newValue);
                } else if (changedKey.equals(PreferenceKeys.PREF_SENSOR_TIMEOUT)) {
                    mSensorTimeoutMultiplier = Integer.parseInt(newValue);
                } else if (changedKey.equals(PreferenceKeys.PREF_ALWAYS_SHOW_CONTENT)) {
                    mShowContent = Boolean.parseBoolean(newValue);
                }
            }
        }
    }

}
