package com.reindeercrafts.notificationpeek;

import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A data structure for storing currently unread notifications. The notification data
 * are from {@link NotificationService}.
 * <p/>
 * Created by zhelu on 4/29/14.
 */
public class NotificationHub {

    private static final boolean DEBUG = true;

    private static final String TAG = NotificationHub.class.getSimpleName();
    private static NotificationHub INSTANCE;
    private StatusBarNotification mCurrentNotification;
    private ArrayList<StatusBarNotification> mNotifications;

    private NotificationHub() {
        mNotifications = new ArrayList<StatusBarNotification>();
    }

    public static NotificationHub getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NotificationHub();
        }

        return INSTANCE;
    }

    public ArrayList<StatusBarNotification> getNotifications() {
        return mNotifications;
    }

    public int getNotificationCount() {
        return mNotifications.size();
    }

    public StatusBarNotification getCurrentNotification() {
        return mCurrentNotification;
    }

    public void setCurrentNotification(StatusBarNotification mCurrentNotification) {
        this.mCurrentNotification = mCurrentNotification;
    }

    public void addNotification(StatusBarNotification notification) {
        mNotifications.add(notification);
    }

    public void removeNotification(StatusBarNotification notification) {

        // Direct equalTo() doesn't work for StatusBarNotification. I have to
        // change to comparing package name. (This will remove all notification
        // having the same package name)
        Iterator<StatusBarNotification> it = mNotifications.iterator();
        while (it.hasNext()) {
            if (it.next().getPackageName().equals(notification.getPackageName())) {
                it.remove();
            }
        }

        if (DEBUG) {
            Log.d(TAG, "Removing notification, count: " + getNotificationCount());
        }
    }

}
