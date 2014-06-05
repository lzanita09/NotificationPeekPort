package com.reindeercrafts.notificationpeek.peek;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.NotificationHub;
import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;
import com.reindeercrafts.notificationpeek.settings.appearance.WallpaperFactory;
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
    private NotificationLayout mNotificationLayout;
    private GridLayout mNotificationsContainer;
    private ViewGroup mNotificationView;
    private ImageView mNotificationIcon;
    private TextView mNotificationText;

    private NotificationPeek mPeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        if (WallpaperFactory.isLiveWallpaperUsed(this) &&
                WallpaperFactory.isWallpaperThemeSelected(this)) {
            setTheme(R.style.AppTheme_Peek_Wallpaper);
        } else {
            setTheme(R.style.AppTheme_Peek);
        }

        super.onCreate(savedInstanceState);

        mPeekView = NotificationPeek.getPeekView();

        mNotificationsContainer = (GridLayout) mPeekView.findViewById(R.id.notification_container);
        mNotificationView = (ViewGroup) mPeekView.findViewById(R.id.notification_view);

        setContentView(mPeekView);

        boolean showClock = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PreferenceKeys.PREF_CLOCK, true);

        if (showClock) {
            mClockTextView = (TextView) PeekLayoutFactory
                    .createPeekLayout(this, PeekLayoutFactory.LAYOUT_TYPE_CLOCK, null);
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
        mNotificationLayout = (NotificationLayout) mPeekView.findViewById(R.id.notification_layout);
        mPeek = mNotificationLayout.getNotificationPeek();

        // Setup OnClickListener.
        mNotificationIcon = (ImageView) mPeekView.findViewById(R.id.notification_icon);

        // Notification snippet TextView.
        mNotificationText = (TextView) mPeekView.findViewById(R.id.notification_text);

    }

    private void initReceiver() {

        mReceiver = new NotificationPeekReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationPeekReceiver.ACTION_FINISH_PEEK);
        filter.addAction(NotificationPeekReceiver.ACTION_DIMISS_NOTIFICATION);
        filter.addAction(NotificationPeekReceiver.ACTION_UPDATE_NOTIFICATION_ICONS);
        filter.addAction(NotificationPeekReceiver.ACTION_SHOW_CONTENT);
        filter.addAction(NotificationPeekReceiver.ACTION_HIDE_CONTENT);
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

        int currentNotificationIdex =
                getCurrentNotificationIndex(mNotificationsContainer, description);

        // Remove the current notification from container.
        mNotificationsContainer.removeViewAt(currentNotificationIdex);

        int nextNotificationIndex = mNotificationsContainer.getChildCount() - 1;

        // We have more than one unread notification.
        if (nextNotificationIndex >= 0) {
            StatusBarNotification nextNotification = (StatusBarNotification) mNotificationsContainer
                    .getChildAt(nextNotificationIndex).getTag();

            if (nextNotification.getNotification().largeIcon != null) {
                mNotificationIcon.setImageDrawable(NotificationPeekViewUtils
                        .getRoundedShape(getResources(),
                                nextNotification.getNotification().largeIcon));
            } else {
                mNotificationIcon.setImageDrawable(
                        NotificationPeekViewUtils.getIconFromResource(this, nextNotification));
            }

            mNotificationText.setText(
                    NotificationPeekViewUtils.getNotificationDisplayText(this, nextNotification));

            // Animate back icon and text.
            mNotificationView.setTranslationX(0);
            mNotificationView.animate().alpha(1f).start();

            // Set new tag.
            mNotificationView.setTag(nextNotification);
            mPeek.getNotificationHub().setCurrentNotification(nextNotification);

            if (nextNotification.getNotification().contentIntent != null) {
                NotificationClicker mNotificationClicker =
                        new NotificationClicker(nextNotification, mPeek);
                mNotificationIcon.setOnClickListener(mNotificationClicker);
            }

            if (nextNotificationIndex == 0) {
                // As we already moved the next notification to 'Current Notification' spot, we need
                // to hide it too if there is only one unread notification left.
                mNotificationsContainer.getChildAt(nextNotificationIndex).setVisibility(View.GONE);
            } else {
                // Otherwise, highlight that icon.
                mNotificationsContainer.getChildAt(nextNotificationIndex).setAlpha(1);
            }
            // Recover TextView alpha, because we will still have notification(s) to show.
            mPeek.updateNotificationTextAlpha(1);

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

    private void restoreFirstIconVisibility() {
        View firstIcon = mNotificationsContainer.getChildAt(0);
        if (firstIcon.getVisibility() == View.GONE) {
            // The first icon is hidden before in response to the swipe gesture,
            firstIcon.setVisibility(View.VISIBLE);
        }

        // Highlight the first notification if it is currently selected.
        StatusBarNotification largeIconNotification =
                (StatusBarNotification) mNotificationView.getTag();
        StatusBarNotification firstIconNotification = (StatusBarNotification) firstIcon.getTag();
        if (largeIconNotification.getPackageName().equals(firstIconNotification.getPackageName())) {
            firstIcon.setAlpha(1f);
        }

    }

    /**
     * Check whether a notification with the same package name as the new notification is
     * shown in the icon container.
     *
     * @param hub NotificationHub instance.
     * @return Index of the icon ImageView in its parent. -1 if not found.
     */
    private int getOldIconViewIndex(NotificationHub hub) {
        for (int i = 0; i < mNotificationsContainer.getChildCount(); i++) {
            View child = mNotificationsContainer.getChildAt(i);

            if (child.getTag() == null) {
                continue;
            }

            StatusBarNotification n = (StatusBarNotification) child.getTag();

            if (n.getPackageName().equals(hub.getCurrentNotification().getPackageName())) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Update small notification icons when there is new notification coming, and the
     * Activity is in foreground.
     */
    private void updateNotificationIcons() {

        if (mNotificationsContainer.getVisibility() != View.VISIBLE) {
            mNotificationsContainer.setVisibility(View.VISIBLE);
        }

        NotificationHub notificationHub = NotificationHub.getInstance();

        int iconSize = getResources().getDimensionPixelSize(R.dimen.small_notification_icon_size);
        int padding = getResources().getDimensionPixelSize(R.dimen.small_notification_icon_padding);

        final StatusBarNotification n = notificationHub.getCurrentNotification();
        ImageView icon = new ImageView(this);
        icon.setAlpha(NotificationPeek.ICON_LOW_OPACITY);

        icon.setPadding(padding, 0, padding, 0);
        icon.setImageDrawable(NotificationPeekViewUtils.getIconFromResource(this, n));
        icon.setTag(n);

        restoreFirstIconVisibility();

        int oldIndex = getOldIconViewIndex(notificationHub);

        if (oldIndex >= 0) {
            mNotificationsContainer.removeViewAt(oldIndex);
        }

        mNotificationsContainer.addView(icon);
        LinearLayout.LayoutParams linearLayoutParams =
                new LinearLayout.LayoutParams(iconSize, iconSize);

        // Wrap LayoutParams to GridLayout.LayoutParams.
        GridLayout.LayoutParams gridLayoutParams = new GridLayout.LayoutParams(linearLayoutParams);
        icon.setLayoutParams(gridLayoutParams);
    }

    /**
     * Animate the small TextView below the icon and the clock TextView and hide them when
     * the notification content is displayed.
     */
    private void hidePeekComponents() {
        mNotificationText.animate().alpha(0f).setInterpolator(new DecelerateInterpolator()).start();
        if (mClockTextView != null) {
            mClockTextView.animate().alpha(0f).setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    /**
     * Bring the small TextView and the clock TextView back when the notification content
     * is removed.
     */
    private void showPeekComponents() {
        mNotificationText.animate().alpha(1f).setInterpolator(new AccelerateInterpolator()).start();
        if (mClockTextView != null) {
            mClockTextView.animate().alpha(1f).setInterpolator(new AccelerateInterpolator())
                    .start();
        }
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

        // Action for dismissing notification peek view.
        public static final String ACTION_DIMISS_NOTIFICATION =
                "NotificationPeek.dismiss_notification";

        // Action for updating notification icons.
        public static final String ACTION_UPDATE_NOTIFICATION_ICONS =
                "NotificationPeek.update_notification";

        // Action for finishing this activity.
        public static final String ACTION_FINISH_PEEK = "NotificationPeek.finish_peek";

        public static final String ACTION_SHOW_CONTENT = "NotificationPeek.show_content";

        public static final String ACTION_HIDE_CONTENT = "NotificationPeek.hide_content";

        public static final String EXTRA_NOTIFICATION_DESCRIPTION =
                "NotificationPeek.extra_status_bar_notification_description";


        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_FINISH_PEEK)) {
                finish();
            } else if (intent.getAction().equals(ACTION_DIMISS_NOTIFICATION)) {
                String description = intent.getStringExtra(EXTRA_NOTIFICATION_DESCRIPTION);
                if (!updateNotification(description)) {
                    lockScreen();
                }
            } else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                mClockTextView.setText(getCurrentTimeText());
            } else if (intent.getAction().equals(ACTION_UPDATE_NOTIFICATION_ICONS)) {
                // Update notification icons according to actions.
                updateNotificationIcons();
            } else if (intent.getAction().equals(ACTION_SHOW_CONTENT)) {
                // Display current notification's content.
                hidePeekComponents();
            } else if (intent.getAction().equals(ACTION_HIDE_CONTENT)) {
                // Hide notification content view.
                showPeekComponents();
            }
        }
    }


}
