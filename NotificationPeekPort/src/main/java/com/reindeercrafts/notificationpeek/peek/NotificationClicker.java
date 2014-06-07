package com.reindeercrafts.notificationpeek.peek;

import android.app.PendingIntent;
import android.view.View;

/**
 * OnClickListener class that performs launching apps from the notification.
 *
 * Created by zhelu on 5/18/14.
 */
public class NotificationClicker implements View.OnClickListener {

    private PendingIntent mPendingIntent;

    private NotificationPeek mPeek;


    public NotificationClicker(PendingIntent contentIntent, NotificationPeek peek) {
        this.mPendingIntent = contentIntent;
        this.mPeek = peek;
    }


    @Override
    public void onClick(View v) {
        try {
            mPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        mPeek.onPostClick();
    }
}
