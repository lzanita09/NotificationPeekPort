package com.reindeercrafts.notificationpeek.peek;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;

/**
 * OnClickListener class that performs launching apps from the notification.
 * <p/>
 * Created by zhelu on 5/18/14.
 */
public class NotificationClicker implements View.OnClickListener {

    private Context mContext;

    private PendingIntent mPendingIntent;

    private NotificationPeek mPeek;


    public NotificationClicker(Context context, PendingIntent contentIntent,
                               NotificationPeek peek) {
        this.mContext = context;
        this.mPendingIntent = contentIntent;
        this.mPeek = peek;
    }


    @Override
    public void onClick(View v) {
        try {
            mPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            // Something is wrong while sending the PendingIntent, just launch the app.
            PackageManager packageManager = mContext.getPackageManager();
            Intent intent =
                    packageManager.getLaunchIntentForPackage(mPendingIntent.getCreatorPackage());
            mContext.startActivity(intent);
        }

        mPeek.onPostClick();
    }
}
