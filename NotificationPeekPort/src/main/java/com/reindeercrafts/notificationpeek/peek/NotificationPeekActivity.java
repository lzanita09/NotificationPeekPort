package com.reindeercrafts.notificationpeek.peek;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;
import com.reindeercrafts.notificationpeek.utils.NotificationPeekViewUtils;

import java.util.Date;

/**
 * This class is used to display the Notification Peek layout. The original implementation
 * uses Window and directly added layout to it, but it seems impossible to do so externally.
 * <p/>
 * This class also controls waking up the device and removing Peek layout from its parent.
 */
public class NotificationPeekActivity extends Activity {

    private static final String TAG = NotificationPeekActivity.class.getSimpleName();

    private static final long LOCK_DELAY = 200; // 200 ms
    private TextView mClockTextView;
    private NotificationPeekReceiver mReceiver;

    private RelativeLayout mPeekView;

    private NotificationPeek mPeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        super.onCreate(savedInstanceState);

        mPeekView = NotificationPeek.sPeekView;

        setContentView(mPeekView);

        boolean showClock = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PreferenceKeys.PREF_CLOCK, true);

        if (showClock) {
            mClockTextView = (TextView) PeekLayoutFactory
                    .createPeekLayout(this, PeekLayoutFactory.LAYOUT_TYPE_CLOCK);
            mPeekView.addView(mClockTextView);
            mClockTextView.setText(getCurrentTimeText());
        }

        mPeekView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // Initialize broadcast receiver.
        initReceiver();

        // Retrieve NotificationPeek object.
        NotificationLayout notificationView = (NotificationLayout) mPeekView
                .findViewById(NotificationPeek.NOTIFICATION_LAYOUT_ID);
        mPeek = notificationView.getNotificationPeek();
    }

    private void initReceiver() {

        mReceiver = new NotificationPeekReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationPeekReceiver.ACTION_DISMISS);
        filter.addAction(NotificationPeekReceiver.ACTION_UPDATE_NOTIFICATION);
        // Add time tick intent only when the clock is shown.
        if (mClockTextView != null) {
            filter.addAction(Intent.ACTION_TIME_TICK);
        }

        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove mPeekView from its parent so that it can be reused.
        ViewGroup parent = (ViewGroup) mPeekView.getParent();
        parent.removeView(mPeekView);

        try {
            // Remove Clock only when it is displayed.
            if (mClockTextView != null) {
                mPeekView.removeView(mClockTextView);
            }
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPeekView = null;
    }

    /**
     * Get formatted time String (Follows system setting).
     *
     * @return Formatted time String.
     */
    private String getCurrentTimeText() {
        return android.text.format.DateFormat.getTimeFormat(this).format(new Date());
    }

    /**
     * Update notification views upon each swipe, moving the next latest notification to 'Current
     * Notification' spot, and remove it from the small icon container.
     *
     * @param description Description of the StatusBarNotification we just swiped.
     * @return True if the update is successful, i.e there is more than one unread notification.
     * False if the notification we just swipe away is the last unread notification.
     */
    private boolean updateNotification(String description) {
        GridLayout notificationContainer =
                (GridLayout) mPeekView.findViewById(NotificationPeek.NOTIFICATION_CONTAINER_ID);
        ImageView notificationIcon =
                (ImageView) mPeekView.findViewById(NotificationPeek.NOTIFICATION_ICON_ID);
        TextView notificationTextView =
                (TextView) mPeekView.findViewById(NotificationPeek.NOTIFICATION_TEXT_ID);
        View notificationView = mPeekView.findViewById(NotificationPeek.NOTIFICATION_VIEW_ID);


        int currentNotificationIdex =
                getCurrentNotificationIndex(notificationContainer, description);

        // Remove the current notification from container.
        notificationContainer.removeViewAt(currentNotificationIdex);

        int nextNotificationIndex = notificationContainer.getChildCount() - 1;

        // We have more than one unread notification.
        if (nextNotificationIndex >= 0) {
            StatusBarNotification nextNotification =
                    (StatusBarNotification) notificationContainer.getChildAt(nextNotificationIndex)
                            .getTag();

            if (nextNotification.getNotification().largeIcon != null) {
                notificationIcon.setImageBitmap(NotificationPeekViewUtils
                        .getRoundedShape(nextNotification.getNotification().largeIcon));
            } else {
                notificationIcon.setImageDrawable(
                        NotificationPeekViewUtils.getIconFromResource(this, nextNotification));
            }

            notificationTextView.setText(
                    NotificationPeekViewUtils.getNotificationDisplayText(this, nextNotification));

            // Animate back icon and text.
            notificationView.setTranslationX(0);
            notificationView.animate().alpha(1f).start();

            // Set new tag.
            notificationView.setTag(nextNotification);

            if (nextNotification.getNotification().contentIntent != null) {
                final View.OnClickListener listener =
                        new NotificationClicker(nextNotification, mPeek);
                notificationIcon.setOnClickListener(listener);
            }

            if (nextNotificationIndex == 0) {
                // As we already moved the next notification to 'Current Notification' spot, we need
                // to hide it too if there is only one unread notification left.
                notificationContainer.getChildAt(nextNotificationIndex).setVisibility(View.GONE);
            } else {
                // Otherwise, highlight that icon.
                notificationContainer.getChildAt(nextNotificationIndex).setAlpha(1);
            }

            return true;
        }

        // The only unread notification is swiped away.
        return false;
    }


    private int getCurrentNotificationIndex(ViewGroup container, String description) {
        for (int i = 0; i < container.getChildCount(); i++) {
            StatusBarNotification n = (StatusBarNotification) container.getChildAt(i).getTag();
            if (NotificationHelper.getContentDescription(n).equals(description)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Release partial wake lock from NotificationPeek object and
     * lock screen.
     */
    private void lockScreen() {

        // Lock screen by a fixed delay, otherwise sometimes the screen will turn on again
        // showing the lock screen UI.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Release partial wake lock.
                mPeek.dismissNotification();

                DevicePolicyManager devicePolicyManager =
                        (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                devicePolicyManager.lockNow();
            }
        }, LOCK_DELAY);


    }


    public class NotificationPeekReceiver extends BroadcastReceiver {

        // Action for updating notification peek view.
        public static final String ACTION_UPDATE_NOTIFICATION =
                "NotificationPeek.update_notification";

        // Action for finishing this activity.
        public static final String ACTION_DISMISS = "NotificationPeek.dismiss_notification";
        public static final String EXTRA_NOTIFICATION_DESCRIPTION =
                "NotificationPeek.extra_status_bar_notification_description";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DISMISS)) {
                finish();
            } else if (intent.getAction().equals(ACTION_UPDATE_NOTIFICATION)) {
                String description = intent.getStringExtra(EXTRA_NOTIFICATION_DESCRIPTION);
                if (!updateNotification(description)) {
                    lockScreen();
                }
            } else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                mClockTextView.setText(getCurrentTimeText());
            }
        }
    }


}
