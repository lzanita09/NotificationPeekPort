package com.reindeercrafts.notificationpeek.peek;

import android.app.PendingIntent;
import android.service.notification.StatusBarNotification;
import android.view.View;

/**
 * OnClickListener class that performs launching apps from the notification.
 *
 * Created by zhelu on 5/18/14.
 */
public class NotificationClicker implements View.OnClickListener {

    private PendingIntent mPendingIntent;

    private StatusBarNotification mNotification;
    private NotificationPeek mPeek;


    public NotificationClicker(StatusBarNotification notification, NotificationPeek peek) {
        this.mPendingIntent = notification.getNotification().contentIntent;
        this.mNotification = notification;
        this.mPeek = peek;
    }


    @Override
    public void onClick(View v) {
        if (mPendingIntent == null) {
            // There is no content intent in this notification. Or the recent touch event is from
            // press & hold for displaying content.
            return;
        }
        try {
            mPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        mPeek.dismissNotification();
        mPeek.removeNotification(mNotification);
        mPeek.onPostClick();
    }
}
