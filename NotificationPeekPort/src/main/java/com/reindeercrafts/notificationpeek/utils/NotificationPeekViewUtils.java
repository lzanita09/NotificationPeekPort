package com.reindeercrafts.notificationpeek.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.ImageView;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.peek.NotificationHelper;
import com.reindeercrafts.notificationpeek.views.RoundedAvatarDrawable;

/**
 * Utility class that provides methods to get icons and texts from StatusBarNotification.
 *
 * Created by zhelu on 5/17/14.
 */
public class NotificationPeekViewUtils {

    public static Drawable getRoundedShape(ImageView imageView, Bitmap scaleBitmapImage) {
        final int shadowSize = imageView.getContext().getResources().getDimensionPixelSize(
                R.dimen.shadow_size);
        final int shadowColor = imageView.getContext().getResources().getColor(R.color.background_color);

        // Use RoundedAvatarDrawable to convert bitmap to rounded icon with shadow.
        Drawable rounded = new RoundedAvatarDrawable(scaleBitmapImage, shadowSize, shadowColor);
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        return rounded;
    }

    public static Drawable getIconFromResource(Context context, StatusBarNotification n) {
        Drawable icon;
        String packageName = n.getPackageName();
        int resource = n.getNotification().icon;
        try {
            Context remotePackageContext = context.createPackageContext(packageName, 0);
            icon = remotePackageContext.getResources().getDrawable(resource);
        } catch (PackageManager.NameNotFoundException nnfe) {
            icon = new BitmapDrawable(context.getResources(), n.getNotification().largeIcon);
        }

        return icon;
    }

    public static String getNotificationDisplayText(Context context, StatusBarNotification n) {
        String text = null;
        if (n.getNotification().tickerText != null) {
            text = n.getNotification().tickerText.toString();
        }
        PackageManager pm = context.getPackageManager();
        if (n != null) {
            if (text == null) {
                text = NotificationHelper.getNotificationTitle(n);
                if (text == null) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(n.getPackageName(), 0);
                        text = (String) pm.getApplicationLabel(ai);
                    } catch (PackageManager.NameNotFoundException e) {
                        // application is uninstalled, run away
                        text = "";
                    }
                }
            }
        }
        return text;
    }
}
