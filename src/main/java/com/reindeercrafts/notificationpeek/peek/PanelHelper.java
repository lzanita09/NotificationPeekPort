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

import android.app.Notification;
import android.graphics.LightingColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class PanelHelper {

    public final static String DELIMITER = "|";


    // Static methods

    public static boolean shouldDisplayNotification(StatusBarNotification oldNotif,
                                                    StatusBarNotification newNotif) {
        // First check for ticker text, if they are different, some other parameters will be
        // checked to determine if we should show the notification.
        CharSequence oldTickerText = oldNotif.getNotification().tickerText;
        CharSequence newTickerText = newNotif.getNotification().tickerText;
        if (newTickerText == null ? oldTickerText == null : newTickerText.equals(oldTickerText)) {
            // If old notification title isn't null, show notification if
            // new notification title is different. If it is null, show notification
            // if the new one isn't.
            String oldNotificationText = getNotificationTitle(oldNotif);
            String newNotificationText = getNotificationTitle(newNotif);
            if (newNotificationText == null ?
                    oldNotificationText != null : !newNotificationText.equals(oldNotificationText))
                return true;

            // Last chance, check when the notifications were posted. If times
            // are equal, we shouldn't display the new notification.
            if (oldNotif.getNotification().when != newNotif.getNotification().when)
                return true;
            return false;
        }
        return true;
    }

    public static String getNotificationTitle(StatusBarNotification n) {
        String text = null;
        if (n != null) {
            Notification notification = n.getNotification();
            Bundle extras = notification.extras;
            text = extras.getString(Notification.EXTRA_TITLE);
        }
        return text;
    }

    public static String getContentDescription(StatusBarNotification content) {
        if (content != null) {
            return content.getPackageName() + DELIMITER + content.getId();
        }
        return null;
    }

    public static View.OnTouchListener getHighlightTouchListener(final int color) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Drawable drawable = ((ImageView) view).getDrawable();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        LightingColorFilter lighten = new LightingColorFilter(color, color);
                        drawable.setColorFilter(lighten);
                        break;
                    case MotionEvent.ACTION_UP:
                        drawable.clearColorFilter();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Rect rect = new Rect();
                        view.getLocalVisibleRect(rect);
                        if (!rect.contains((int) event.getX(), (int) event.getY())) {
                            drawable.clearColorFilter();
                        }
                        break;
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_CANCEL:
                        drawable.clearColorFilter();
                        break;
                }
                return false;
            }
        };
    }
}
