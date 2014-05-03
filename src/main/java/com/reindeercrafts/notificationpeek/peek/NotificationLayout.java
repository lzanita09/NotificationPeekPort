package com.reindeercrafts.notificationpeek.peek;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by zhelu on 4/29/14.
 */
public class NotificationLayout extends LinearLayout {

    private NotificationPeek mNotificationPeek;

    public NotificationLayout(Context context) {
        super(context);
    }

    public NotificationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public NotificationPeek getNotificationPeek() {
        return mNotificationPeek;
    }

    public void setNotificationPeek(NotificationPeek mNotificationPeek) {
        this.mNotificationPeek = mNotificationPeek;
    }
}
