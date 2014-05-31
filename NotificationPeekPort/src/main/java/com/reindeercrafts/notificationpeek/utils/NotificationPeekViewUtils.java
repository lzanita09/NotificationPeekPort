package com.reindeercrafts.notificationpeek.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.peek.NotificationHelper;

/**
 * Utility class that provides methods to get icons and texts from StatusBarNotification.
 * <p/>
 * Created by zhelu on 5/17/14.
 */
public class NotificationPeekViewUtils {

    /**
     * Get rounded icon from the Bitmap object, with shade. The shade will only be drawn if
     * the Bitmap is larger than the ImageView's size.
     *
     * @param resources         Resources object for getting size and color.
     * @param scaleBitmapImage  Source Bitmap.
     * @return                  Rounded BitmapDrawable with shade (if possible).
     */
    public static Drawable getRoundedShape(Resources resources, Bitmap scaleBitmapImage) {
        final int shadowSize = resources.getDimensionPixelSize(R.dimen.shadow_size);
        final int shadowColor = resources.getColor(R.color.background_color);

        int targetWidth = scaleBitmapImage.getWidth();
        int targetHeight = scaleBitmapImage.getHeight();
        Bitmap targetBitmap =
                Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        Paint shadowPaint = new Paint(paint);
        RectF rectF = new RectF(0, 0, targetWidth, targetHeight);

        Canvas canvas = new Canvas(targetBitmap);

        final BitmapShader shader =
                new BitmapShader(scaleBitmapImage, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        // Only apply shadow if the icon is large enough.
        if (scaleBitmapImage.getWidth() >
                resources.getDimensionPixelSize(R.dimen.notification_icon_size)) {
            rectF.inset(shadowSize, shadowSize);
            shadowPaint.setShadowLayer(shadowSize, 0f, 0f, shadowColor);
            shadowPaint.setColor(Color.BLACK);
            canvas.drawOval(rectF, shadowPaint);
        }

        canvas.drawOval(rectF, paint);

        return new BitmapDrawable(resources, targetBitmap);
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
