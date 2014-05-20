package com.reindeercrafts.notificationpeek;

import android.service.notification.StatusBarNotification;

import com.reindeercrafts.notificationpeek.peek.NotificationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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

    // Use HashMap to store only the latest notification for a given app.
    private HashMap<String, StatusBarNotification> mNotifications;

    private NotificationHub() {
        mNotifications = new HashMap<String, StatusBarNotification>();
    }

    public static NotificationHub getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NotificationHub();
        }

        return INSTANCE;
    }

    /**
     * Sort the StatusBarNotifications stored in the HashMap with their posted time.
     *
     * @return A sorted List of StatusBarNotification.
     */
    public List<StatusBarNotification> getNotifications() {
        List<StatusBarNotification> list = new ArrayList<StatusBarNotification>(mNotifications.values());
        Collections.sort(list, new NotificationTimeComparator());
        return list;
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
        mNotifications.put(notification.getPackageName(), notification);
        setCurrentNotification(notification);
    }

    public void removeNotification(StatusBarNotification notification) {
        mNotifications.remove(notification.getPackageName());

        // If the notification we are to remove is equal to mCurrentNotification, remove the
        // reference.
        if (NotificationHelper.getContentDescription(notification)
                .equals(NotificationHelper.getContentDescription(mCurrentNotification))) {
            mCurrentNotification = null;
        }

    }


    private class NotificationTimeComparator implements Comparator<StatusBarNotification> {
        @Override
        public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {

            return lhs.getPostTime() > rhs.getPostTime() ? 1 : -1;
        }

    }

}
