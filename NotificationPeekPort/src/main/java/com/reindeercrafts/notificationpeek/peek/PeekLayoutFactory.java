package com.reindeercrafts.notificationpeek.peek;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.R;

/**
 * Factory class for creating components for Notification Peek layout:
 * - Clock
 * <p/>
 * More custom layouts are added here instead of messing up with NotificationPeek class.
 * <p/>
 * Created by zhelu on 5/1/14.
 */
public class PeekLayoutFactory {

    public static final int LAYOUT_TYPE_CLOCK = 0;

    public static View createPeekLayout(Context context, int layoutType) {
        switch (layoutType) {
            case LAYOUT_TYPE_CLOCK:
                return createClockLayout(context);
        }

        return null;
    }

    private static View createClockLayout(Context context) {
        TextView clockText = new TextView(context);
        clockText.setTextAppearance(context, android.R.style.TextAppearance_Holo_Large);
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

}
