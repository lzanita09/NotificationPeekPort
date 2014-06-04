package com.reindeercrafts.notificationpeek.peek;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.utils.ContactHelper;
import com.reindeercrafts.notificationpeek.utils.NotificationPeekViewUtils;

/**
 * Factory class for creating components for Notification Peek layout:
 * - Clock
 * - Notification content.
 * <p/>
 * More custom layouts are added here instead of messing up with NotificationPeek class.
 * <p/>
 * Created by zhelu on 5/1/14.
 */
public class PeekLayoutFactory {

    // Layout type: clock.
    public static final int LAYOUT_TYPE_CLOCK = 0;

    // Layout type: notification content & contact.
    public static final int LAYOUT_TYPE_CONTENT = 1;

    private static final int MIN_IMAGE_WIDTH = 120; // px

    public static View createPeekLayout(Context context, int layoutType, Object data) {
        switch (layoutType) {
            case LAYOUT_TYPE_CLOCK:
                return createClockLayout(context);

            case LAYOUT_TYPE_CONTENT:
                return createContentLayout(context, (StatusBarNotification) data);
        }

        return null;
    }

    private static View createClockLayout(Context context) {
        TextView clockText = new TextView(context);
        clockText.setTextAppearance(context, android.R.style.TextAppearance_Holo_Large);
        clockText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32);
        clockText.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        clockText.setGravity(Gravity.CENTER);


        RelativeLayout.LayoutParams relativeLayoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        relativeLayoutParams.topMargin =
                context.getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        clockText.setLayoutParams(relativeLayoutParams);

        return clockText;
    }

    private static View createContentLayout(Context context, StatusBarNotification n) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View rootLayout = inflater.inflate(R.layout.content_layout, null);

        // Contact ImageView.
        ImageView contactImageView = (ImageView) rootLayout.findViewById(R.id.contact_image);
        contactImageView.setImageDrawable(getNotificationContactImage(context, n));

        // Content TextView.
        TextView contentTextView = (TextView) rootLayout.findViewById(R.id.content_text);
        TextView contentTitleView = (TextView) rootLayout.findViewById(R.id.title_text);

        String content = NotificationHelper.getNotificationContent(n);
        String title = NotificationHelper.getNotificationTitle(n);
        if (!content.startsWith(NotificationHelper.getNotificationTitle(n))) {
            contentTitleView.setText(title);
        }
        contentTextView.setText(content);

        if (contactImageView.getDrawable() == null) {
            // No avatar image available, we need to adjust TextView's paddings.
            int padding =
                    context.getResources().getDimensionPixelSize(R.dimen.content_text_paddings);
            contentTextView.setPadding(
                    padding,
                    contentTextView.getPaddingTop(),
                    padding,
                    contentTextView.getPaddingBottom());
            contentTitleView.setPadding(
                    padding,
                    contentTextView.getPaddingTop(),
                    padding,
                    contentTextView.getPaddingBottom());
            contactImageView.setVisibility(View.GONE);

        }

        return rootLayout;
    }

    private static Drawable getNotificationContactImage(Context context, StatusBarNotification n) {
        Bitmap avatarBitmap =
                ContactHelper.getContactPhoto(context, NotificationHelper.getNotificationTitle(n));
        if (avatarBitmap != null && avatarBitmap.getWidth() > MIN_IMAGE_WIDTH) {
            // Got large contact image, the best case.
            return NotificationPeekViewUtils.getRoundedShape(context.getResources(), avatarBitmap);
        }

        avatarBitmap = n.getNotification().largeIcon;
        if (avatarBitmap != null && avatarBitmap.getWidth() > MIN_IMAGE_WIDTH) {
            // Got large icon in notification that is large enough, not bad.
            return NotificationPeekViewUtils.getRoundedShape(context.getResources(), avatarBitmap);
        }

        return null;
    }

}
