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

import android.animation.LayoutTransition;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.NotificationHub;
import com.reindeercrafts.notificationpeek.NotificationService;
import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.settings.appearance.WallpaperFactory;
import com.reindeercrafts.notificationpeek.utils.NotificationPeekViewUtils;
import com.reindeercrafts.notificationpeek.utils.UnlockGesture;

import java.util.ArrayList;
import java.util.List;

public class NotificationPeek implements SensorActivityHandler.SensorChangedCallback {

    private final static String TAG = "NotificationPeek";
    public final static boolean DEBUG = true;

    public static final float ICON_LOW_OPACITY = 0.3f;

    private static final int NOTIFICATION_PEEK_TIME = 5000; // 5 secs
    private static final int PARTIAL_WAKELOCK_TIME = 10000; // 10 secs
    private static final long SCREEN_ON_START_DELAY = 300; // 300 ms
    private static final long REMOVE_VIEW_DELAY = 300; // 300 ms
    private static final int COL_NUM = 10;
    private static final long SCREEN_WAKELOCK_TIMEOUT = 2000; // 1 sec
    private static final int SENSOR_TIME_OUT_INFINITE = -1;

    private SensorActivityHandler mSensorHandler;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private DevicePolicyManager mDevicePolicyManager;
    private WallpaperFactory mWallpaperFactory;

    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager.WakeLock mScreenWakeLock;
    private Runnable mPartialWakeLockRunnable;
    private Runnable mLockScreenRunnable;
    private Handler mWakeLockHandler;
    private Handler mHandler;

    private static RelativeLayout sPeekView;

    private List<StatusBarNotification> mShownNotifications =
            new ArrayList<StatusBarNotification>();
    private StatusBarNotification mNextNotification;
    private LinearLayout mNotificationView;
    private GridLayout mNotificationsContainer;
    private ImageView mNotificationIcon;
    private TextView mNotificationText;
    private ImageView mPeekBackgroundImageView;

    private Context mContext;

    private boolean mShowing;
    private boolean mEnabled = true;
    private boolean mAnimating;

    private boolean mEventsRegistered;

    private NotificationHub mNotificationHub;

    // Peek timeout multiplier, the final peek timeout period is 5000 * mPeekTimeoutMultiplier.
    private int mPeekTimeoutMultiplier;

    // Sensor listener timeout, the final sensor timeout period is 10000 * mSensorTimeoutMultiplier.
    private int mSensorTimeoutMultiplier;

    // Display notification content regardless lock screen methods.
    private boolean mShowContent;

    private NotificationHelper mNotificationHelper;

    public NotificationPeek(NotificationHub notificationHub, Context context) {
        mNotificationHub = notificationHub;
        mContext = context;

        mNotificationHelper = new NotificationHelper(context);
        mWallpaperFactory = new WallpaperFactory(context);

        mSensorHandler = new SensorActivityHandler(context, this);
        mHandler = new Handler(Looper.getMainLooper());
        mWakeLockHandler = new Handler();

        mSensorHandler.registerScreenReceiver();

        mPartialWakeLockRunnable = new Runnable() {
            @Override
            public void run() {
                // After PARTIAL_WAKELOCK_TIME with no user interaction, release CPU wakelock
                // and unregister event listeners.
                if (mPartialWakeLock.isHeld()) {
                    if (mEventsRegistered) {
                        if (DEBUG) {
                            Log.d(TAG, "Removing event listeners");
                        }

                        tryUnregisterEventListeners();
                        mEventsRegistered = false;

                    }
                    mPartialWakeLock.release();
                }
            }
        };

        mLockScreenRunnable = new Runnable() {
            @Override
            public void run() {
                if (mShowing) {
                    if (DEBUG) {
                        Log.d(TAG, "Turning screen off");
                    }
                    mDevicePolicyManager.lockNow();

                    if (mScreenWakeLock.isHeld()) {
                        mScreenWakeLock.release();
                    }
                }
            }
        };


        mDevicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mPartialWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getSimpleName() + "_partial");

        // Screen dim wakelock for waking up screen.
        mScreenWakeLock = mPowerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                getClass().getSimpleName() + "_screen");

        // build the layout
        sPeekView = new RelativeLayout(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }
                if (action == MotionEvent.ACTION_UP) {
                    scheduleTasks();
                }
                return super.onInterceptTouchEvent(event);
            }
        };

        // Setup double-tap gesture detector.
        sPeekView.setOnTouchListener(UnlockGesture
                .createTouchListener(mContext, new UnlockGesture.UnlockGestureCallback() {
                    @Override
                    public void onUnlocked() {
                        dismissNotification();
                    }
                }));


        // root view
        NotificationLayout rootView = new NotificationLayout(context);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setNotificationPeek(NotificationPeek.this);
        rootView.setId(R.id.notification_layout);
        sPeekView.addView(rootView);

        RelativeLayout.LayoutParams rootLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        rootLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        rootView.setLayoutParams(rootLayoutParams);


        // notification container
        mNotificationView = new LinearLayout(context);
        mNotificationView.setOrientation(LinearLayout.VERTICAL);
        mNotificationView.setId(R.id.notification_view);
        rootView.addView(mNotificationView);

        // current notification icon
        mNotificationIcon = new ImageView(context);
        mNotificationIcon.setId(R.id.notification_icon);
        mNotificationIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mNotificationIcon
                .setOnTouchListener(NotificationHelper.getHighlightTouchListener(Color.DKGRAY));

        // current notification text
        mNotificationText = new TextView(context);
        mNotificationText.setId(R.id.notification_text);
        Typeface typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        mNotificationText.setTypeface(typeface);
        mNotificationText.setGravity(Gravity.CENTER);
        mNotificationText.setEllipsize(TextUtils.TruncateAt.END);
        mNotificationText.setSingleLine(true);
        mNotificationText
                .setPadding(0, mContext.getResources().getDimensionPixelSize(R.dimen.item_padding),
                        0, 0);

        mNotificationView.addView(mNotificationIcon);

        // Move NotificationText out of NotificationView, so that it won't be swiped away with
        // the icon.
        rootView.addView(mNotificationText);

        int iconSize =
                mContext.getResources().getDimensionPixelSize(R.dimen.notification_icon_size);
        LinearLayout.LayoutParams linearLayoutParams =
                new LinearLayout.LayoutParams(iconSize, iconSize);
        linearLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        mNotificationIcon.setLayoutParams(linearLayoutParams);

        linearLayoutParams = new LinearLayout.LayoutParams(
                mContext.getResources().getDimensionPixelSize(R.dimen.notification_text_width),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        mNotificationText.setLayoutParams(linearLayoutParams);

        // notification icons
        // Use GridLayout instead of LinearLayout to avoid having too many notifications in the
        // container in just one line. (It's not likely to happen but still possible)
        mNotificationsContainer = new GridLayout(context) {
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                int action = ev.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    StatusBarNotification n = getNotificationFromEvent(ev);
                    if (n != null) {
                        updateSelection(n);
                    }
                }
                return true;
            }
        };
        mNotificationsContainer.setId(R.id.notification_container);
        mNotificationsContainer.setColumnCount(COL_NUM);
        mNotificationsContainer.setOrientation(GridLayout.HORIZONTAL);
        mNotificationsContainer.setPadding(0,
                mContext.getResources().getDimensionPixelSize(R.dimen.item_padding) * 2, 0, 0);
        LayoutTransition transitioner = new LayoutTransition();
        transitioner.enableTransitionType(LayoutTransition.CHANGING);
        transitioner.disableTransitionType(LayoutTransition.DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.APPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        mNotificationsContainer.setLayoutTransition(transitioner);

        sPeekView.addView(mNotificationsContainer);

        RelativeLayout.LayoutParams notificationsLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        notificationsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        notificationsLayoutParams.addRule(RelativeLayout.BELOW, rootView.getId());
        mNotificationsContainer.setLayoutParams(notificationsLayoutParams);

        updateBackgroundImageView();
    }

    public static RelativeLayout getPeekView() {
        return sPeekView;
    }

    public void updateNotificationTextAlpha(float alpha) {
        mNotificationText.setAlpha(alpha);
    }

    /**
     * Update background ImageView to display proper background according to user preference.
     */
    public void updateBackgroundImageView() {

        boolean used = WallpaperFactory.isWallpaperThemeSelected(mContext) &&
                !WallpaperFactory.isLiveWallpaperUsed(mContext);

        if (mPeekBackgroundImageView == null) {
            mPeekBackgroundImageView = new ImageView(mContext);
            ViewGroup.LayoutParams params =
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            mPeekBackgroundImageView.setLayoutParams(params);
            mPeekBackgroundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        if (used) {
            mPeekBackgroundImageView.setImageBitmap(mWallpaperFactory.getPrefSystemWallpaper());
            if (!isBackgroundImageViewAdded()) {
                sPeekView.addView(mPeekBackgroundImageView, 0);
            }
        } else {
            if (isBackgroundImageViewAdded()) {
                sPeekView.removeViewAt(0);
            }
        }

    }

    private boolean isBackgroundImageViewAdded() {
        return sPeekView.getChildAt(0) instanceof ImageView;
    }

    /* If mSensorTimeoutMultiplier is -1, we don't remove listeners, but let them keep listening. */
    private void tryUnregisterEventListeners() {
        if (mSensorTimeoutMultiplier != SENSOR_TIME_OUT_INFINITE) {
            mSensorHandler.unregisterEventListeners();
        }
    }

    private boolean isKeyguardSecureShowing() {
        return !mShowContent &&
                (mKeyguardManager.isKeyguardLocked() && mKeyguardManager.isKeyguardSecure());
    }

    public View getNotificationView() {
        return mNotificationView;
    }

    public NotificationHub getNotificationHub() {
        return mNotificationHub;
    }

    public void setAnimating(boolean animating) {
        mAnimating = animating;
    }

    private void scheduleTasks() {
        mHandler.removeCallbacksAndMessages(null);

        // turn on screen task
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    Log.d(TAG, "Turning screen on");
                }
                mScreenWakeLock.acquire(SCREEN_WAKELOCK_TIMEOUT);
            }
        }, SCREEN_ON_START_DELAY);

        // turn off screen task
        mHandler.postDelayed(mLockScreenRunnable,
                SCREEN_ON_START_DELAY + NOTIFICATION_PEEK_TIME * mPeekTimeoutMultiplier);

        // remove view task (make sure screen is off by delaying a bit)
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismissNotification();
            }
        }, SCREEN_ON_START_DELAY + (NOTIFICATION_PEEK_TIME * mPeekTimeoutMultiplier * (long) 1.3));
    }


    /**
     * Show notification according to user preferences.
     *
     * @param n                     Notificaiton to display.
     * @param update                If the given notification is an update.
     * @param peekTimeoutMultiplier User preference: timeout.
     * @param sensorTimeout         User preference: sensor timeout.
     * @param showContent           User preference: always show content.
     */
    public void showNotification(StatusBarNotification n, boolean update, int peekTimeoutMultiplier,
                                 int sensorTimeout, boolean showContent) {
        mSensorTimeoutMultiplier = sensorTimeout;
        mShowContent = showContent;
        mPeekTimeoutMultiplier = peekTimeoutMultiplier;
        showNotification(n, update, false);

        updateNotificationTextAlpha(1);
    }

    private void showNotification(StatusBarNotification n, boolean update, boolean force) {
        boolean shouldDisplay = shouldDisplayNotification(n) || force;
        addNotification(n);

        if (!mEnabled /* peek is disabled */ || (mPowerManager.isScreenOn() && !mShowing) /* no peek when screen is on */ ||
                !shouldDisplay /* notification has already been displayed */ || !n.isClearable() /* persistent notification */ ||
                mNotificationHelper.isRingingOrConnected() /* is phone ringing? */ ||
                mNotificationHelper.isSimPanelShowing() /* is sim pin lock screen is shown? */) {
            return;
        }

        if (isNotificationActive(n) && (!update || (update && shouldDisplay))) {
            // update information
            if (!mShowing) {
                updateNotificationIcons();
                updateSelection(n);
            } else {
                mContext.sendBroadcast(new Intent(NotificationPeekActivity.
                        NotificationPeekReceiver.ACTION_UPDATE_NOTIFICATION_ICONS
                ));
            }

            // check if phone is in the pocket or lying on a table
            if (mSensorHandler.isInPocket() || mSensorHandler.isOnTable()) {
                if (DEBUG) {
                    Log.d(TAG, "Queueing notification");
                }

                // use partial wakelock to get sensors working
                if (mPartialWakeLock.isHeld()) {
                    if (DEBUG) {
                        Log.d(TAG, "Releasing partial wakelock");
                    }
                    mPartialWakeLock.release();
                }

                if (DEBUG) {
                    Log.d(TAG, "Acquiring partial wakelock");
                }
                mPartialWakeLock.acquire();
                if (!mEventsRegistered) {
                    mSensorHandler.registerEventListeners();
                    mEventsRegistered = true;
                }

                mWakeLockHandler.removeCallbacks(mPartialWakeLockRunnable);

                // If always listening is selected, we still release the wake lock in 10 sec, but
                // we do not unregister sensor listeners.
                int multiplier = Math.max(1, mSensorTimeoutMultiplier);

                mWakeLockHandler
                        .postDelayed(mPartialWakeLockRunnable, PARTIAL_WAKELOCK_TIME * multiplier);

                mNextNotification = n;
                return;
            }

            mWakeLockHandler.removeCallbacks(mPartialWakeLockRunnable);

            addNotificationView(); // add view instantly
            if (!mAnimating) {
                scheduleTasks();
            }
        }
    }

    private void addNotificationView() {
        if (!mShowing) {
            mShowing = true;
            Intent intent = new Intent(mContext, NotificationPeekActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mContext.startActivity(intent);
        }
    }

    public void dismissNotification() {
        if (mShowing) {
            mShowing = false;
            mContext.sendBroadcast(new Intent(
                    NotificationPeekActivity.NotificationPeekReceiver.ACTION_FINISH_PEEK));

            if (mPartialWakeLock.isHeld()) {
                if (DEBUG) {
                    Log.d(TAG, "Releasing partial wakelock");
                }
                mPartialWakeLock.release();
            }
            mHandler.removeCallbacks(mLockScreenRunnable);
        }
    }

    public void addNotification(StatusBarNotification n) {
        for (int i = 0; i < mShownNotifications.size(); i++) {
            if (NotificationHelper.getContentDescription(n)
                    .equals(NotificationHelper.getContentDescription(mShownNotifications.get(i)))) {
                mShownNotifications.set(i, n);
                return;
            }
        }
        mShownNotifications.add(n);
    }

    public void removeNotification(StatusBarNotification n) {
        for (int i = 0; i < mShownNotifications.size(); i++) {
            if (NotificationHelper.getContentDescription(n)
                    .equals(NotificationHelper.getContentDescription(mShownNotifications.get(i)))) {
                mShownNotifications.remove(i);
                i--;
            }
        }
        updateNotificationIcons();
    }

    private void updateNotificationIcons() {
        mNotificationsContainer.removeAllViews();
        int iconSize =
                mContext.getResources().getDimensionPixelSize(R.dimen.small_notification_icon_size);
        int padding = mContext.getResources()
                .getDimensionPixelSize(R.dimen.small_notification_icon_padding);
        Object tag = mNotificationView.getTag();
        String currentNotification = tag != null ? tag.toString() : null;
        boolean foundCurrentNotification = false;
        int notificationCount = mNotificationHub.getNotificationCount();
        mNotificationsContainer.setVisibility(View.VISIBLE);
        if (notificationCount <= 1) {
            mNotificationsContainer.setVisibility(View.GONE);
        }

        StatusBarNotification lastNotification = null;
        for (StatusBarNotification notification : mNotificationHub.getNotifications()) {
            final StatusBarNotification n = notification;
            ImageView icon = new ImageView(mContext);
            if (n.toString().equals(currentNotification)) {
                foundCurrentNotification = true;
            } else {
                icon.setAlpha(ICON_LOW_OPACITY);
            }
            icon.setPadding(padding, 0, padding, 0);
            icon.setImageDrawable(NotificationPeekViewUtils.getIconFromResource(mContext, n));
            icon.setTag(n);
            mNotificationsContainer.addView(icon);
            LinearLayout.LayoutParams linearLayoutParams =
                    new LinearLayout.LayoutParams(iconSize, iconSize);

            // Wrap LayoutParams to GridLayout.LayoutParams.
            GridLayout.LayoutParams gridLayoutParams =
                    new GridLayout.LayoutParams(linearLayoutParams);
            icon.setLayoutParams(gridLayoutParams);

            if (lastNotification == null) {
                lastNotification = n;
            }
        }


        if (!foundCurrentNotification) {
            if (notificationCount > 0) {
                updateSelection(lastNotification);
            } else {
                dismissNotification();
            }
        }
    }

    private void updateSelection(StatusBarNotification n) {

        String oldNotif = NotificationHelper
                .getContentDescription((StatusBarNotification) mNotificationView.getTag());
        String newNotif = NotificationHelper.getContentDescription(n);
        boolean sameNotification = newNotif.equals(oldNotif);
        if (!mAnimating || sameNotification) {
            // update big icon
            Bitmap b = n.getNotification().largeIcon;
            if (b != null) {
                mNotificationIcon.setImageDrawable(
                        NotificationPeekViewUtils.getRoundedShape(mContext.getResources(), b));
            } else {
                mNotificationIcon.setImageDrawable(
                        NotificationPeekViewUtils.getIconFromResource(mContext, n));
            }

            mNotificationText
                    .setText(NotificationPeekViewUtils.getNotificationDisplayText(mContext, n));
            mNotificationText.setVisibility(isKeyguardSecureShowing() ? View.GONE : View.VISIBLE);
            mNotificationView.setTag(n);

            // If the notification view is moved before, we need to restore its position before
            // displaying new notification.
            if (!sameNotification || mNotificationView.getX() != 0) {
                mNotificationView.setAlpha(1f);
                mNotificationView.setX(0);
            }
        }

        // update small icons
        for (int i = 0; i < mNotificationsContainer.getChildCount(); i++) {
            ImageView view = (ImageView) mNotificationsContainer.getChildAt(i);
            if ((mAnimating ? oldNotif : newNotif).equals(NotificationHelper
                    .getContentDescription((StatusBarNotification) view.getTag()))) {
                view.setAlpha(1f);
            } else {
                view.setAlpha(ICON_LOW_OPACITY);
            }
        }
    }

    private boolean isNotificationActive(StatusBarNotification n) {

        for (StatusBarNotification notification : mNotificationHub.getNotifications()) {
            if (NotificationHelper.getContentDescription(n)
                    .equals(NotificationHelper.getContentDescription(notification))) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldDisplayNotification(StatusBarNotification n) {
        if (n.getNotification().priority < Notification.PRIORITY_DEFAULT) {
            return false;
        }
        for (StatusBarNotification shown : mShownNotifications) {
            if (NotificationHelper.getContentDescription(n)
                    .equals(NotificationHelper.getContentDescription(shown))) {
                return NotificationHelper.shouldDisplayNotification(shown, n);
            }
        }
        return true;
    }

    private StatusBarNotification getNotificationFromEvent(MotionEvent event) {
        for (int i = 0; i < mNotificationsContainer.getChildCount(); i++) {
            View view = mNotificationsContainer.getChildAt(i);
            Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            if (rect.contains((int) event.getX(), (int) event.getY())) {
                if (view.getTag() instanceof StatusBarNotification) {
                    return (StatusBarNotification) view.getTag();
                }
            }
        }
        return null;
    }

    public void unregisterScreenReceiver() {
        mSensorHandler.unregisterScreenReceiver();
        sPeekView = null;
    }

    public void unregisterEventListeners() {
        if (mEventsRegistered) {
            mSensorHandler.unregisterEventListeners();
            mEventsRegistered = false;
        }

    }

    @Override
    public void onPocketModeChanged(boolean inPocket) {
        // If we set to use always listening, if we detect the device is out of pocket,
        // we restore mNextNotification, and let showNotification to decide whether it is active
        // or not.
        if (inPocket && mSensorTimeoutMultiplier == SENSOR_TIME_OUT_INFINITE &&
                mNextNotification == null) {
            mNextNotification = mNotificationHub.getCurrentNotification();
        }
        if (!inPocket && mNextNotification != null) {
            showNotification(mNextNotification, false, true);
            mNextNotification = null;
        }
    }

    @Override
    public void onTableModeChanged(boolean onTable) {
        // If we set to use always listening, if we detect the device is held on hand,
        // we restore mNextNotification, and let showNotification to decide whether it is active
        // or not.
        if (!onTable && mSensorTimeoutMultiplier == SENSOR_TIME_OUT_INFINITE &&
                mNextNotification == null) {
            mNextNotification = mNotificationHub.getCurrentNotification();
        }

        if (!onTable && mNextNotification != null) {
            showNotification(mNextNotification, false, true);
            mNextNotification = null;
        }
    }

    @Override
    public void onScreenStateChanged(boolean screenOn) {
        if (!screenOn) {
            mHandler.removeCallbacksAndMessages(null);
            dismissNotification();
        }
    }

    /**
     * Send broadcast to NotificationService, and let it perform the final dismiss action.
     *
     * @param pkg package name associated with the notification to be dismissed.
     * @param tag tag associated with the notification to be dismissed.
     * @param id  notification id associated with the notification to be dismissed.
     */
    public void onChildDismissed(String description, String pkg, String tag, int id) {
        // Send broadcast to NotificationService for dismiss action.
        Intent intent = new Intent(NotificationService.ACTION_DISMISS_NOTIFICATION);
        intent.putExtra(NotificationService.EXTRA_PACKAGE_NAME, pkg);
        intent.putExtra(NotificationService.EXTRA_NOTIFICATION_TAG, tag);
        intent.putExtra(NotificationService.EXTRA_NOTIFICATION_ID, id);
        mContext.sendBroadcast(intent);

        // Send broadcast to NotificationPeekActivity for updating NotificationView.
        Intent updateViewIntent = new Intent(
                NotificationPeekActivity.NotificationPeekReceiver.ACTION_DIMISS_NOTIFICATION);
        updateViewIntent.putExtra(
                NotificationPeekActivity.NotificationPeekReceiver.EXTRA_NOTIFICATION_DESCRIPTION,
                description);
        mContext.sendBroadcast(updateViewIntent);
    }

    /* Called after click event is triggered, used to clean up event listeners and
     * set mNextNotification to null. */
    public void onPostClick() {
        mSensorHandler.unregisterEventListeners();
        mNextNotification = null;
    }


}