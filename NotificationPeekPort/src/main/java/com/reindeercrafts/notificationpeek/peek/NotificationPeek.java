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
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.NotificationHub;
import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationPeek implements SensorActivityHandler.SensorChangedCallback {

    private final static String TAG = "NotificationPeek";
    public final static boolean DEBUG = true;

    private static final float ICON_LOW_OPACITY = 0.3f;
    private static final int NOTIFICATION_PEEK_TIME = 5000; // 5 secs
    private static final int PARTIAL_WAKELOCK_TIME = 10000; // 10 secs
    private static final long SCREEN_ON_START_DELAY = 300; // 300 ms
    private static final long REMOVE_VIEW_DELAY = 300; // 300 ms
    private static final int COL_NUM = 10;
    private static final long SCREEN_WAKELOCK_TIMEOUT = 1000; // 1 sec

    private SensorActivityHandler mSensorHandler;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private DevicePolicyManager mDevicePolicyManager;

    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager.WakeLock mScreenWakeLock;
    private Runnable mPartialWakeLockRunnable;
    private Runnable mLockScreenRunnable;
    private Handler mWakeLockHandler;
    private Handler mHandler;

    private List<StatusBarNotification> mShownNotifications =
            new ArrayList<StatusBarNotification>();
    private StatusBarNotification mNextNotification;
    private static RelativeLayout mPeekView;
    private LinearLayout mNotificationView;
    private GridLayout mNotificationsContainer;
    private ImageView mNotificationIcon;
    private TextView mNotificationText;

    private Context mContext;

    private boolean mRingingOrConnected;
    private boolean mShowing;
    private boolean mEnabled = true;
    private boolean mAnimating;

    private boolean mEventsRegistered = false;

    private NotificationHub mNotificationHub;

    // Peek timeout multiplier, the final peek timeout period is 5000 * mPeekTimeoutMultiplier.
    private int mPeekTimeoutMultiplier;

    // Experimental feature: not removing mNextNotification, let listeners to listen all the time
    // until user click screen.
    private boolean mListenForever;

    public NotificationPeek(NotificationHub notificationHub, Context context) {
        mNotificationHub = notificationHub;
        mContext = context;

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
        mScreenWakeLock = mPowerManager
                .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        getClass().getSimpleName() + "_screen");

        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new CallStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

        // build the layout
        mPeekView = new RelativeLayout(context) {
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
        mPeekView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    dismissNotification();
                }
                return true;
            }
        });


        // root view
        NotificationLayout rootView = new NotificationLayout(context);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setNotificationPeek(NotificationPeek.this);
        rootView.setId(1);
        mPeekView.addView(rootView);

        RelativeLayout.LayoutParams rootLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        rootLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        rootView.setLayoutParams(rootLayoutParams);


        // notification container
        mNotificationView = new LinearLayout(context);
        mNotificationView.setOrientation(LinearLayout.VERTICAL);
        rootView.addView(mNotificationView);

        // current notification icon
        mNotificationIcon = new ImageView(context);
        mNotificationIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mNotificationIcon.setOnTouchListener(PanelHelper.getHighlightTouchListener(Color.DKGRAY));

        // current notification text
        mNotificationText = new TextView(context);
        Typeface textTypeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        mNotificationText.setTypeface(textTypeface);
        mNotificationText.setGravity(Gravity.CENTER);
        mNotificationText.setEllipsize(TextUtils.TruncateAt.END);
        mNotificationText.setSingleLine(true);
        mNotificationText
                .setPadding(0, mContext.getResources().getDimensionPixelSize(R.dimen.item_padding),
                        0, 0);

        mNotificationView.addView(mNotificationIcon);
        mNotificationView.addView(mNotificationText);

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
        mNotificationsContainer.setColumnCount(COL_NUM);
        mNotificationsContainer.setOrientation(LinearLayout.HORIZONTAL);
        mNotificationsContainer.setPadding(0,
                mContext.getResources().getDimensionPixelSize(R.dimen.item_padding) * 2, 0, 0);
        LayoutTransition transitioner = new LayoutTransition();
        transitioner.enableTransitionType(LayoutTransition.CHANGING);
        transitioner.disableTransitionType(LayoutTransition.DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.APPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        mNotificationsContainer.setLayoutTransition(transitioner);

        mPeekView.addView(mNotificationsContainer);

        RelativeLayout.LayoutParams notificationsLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        notificationsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        notificationsLayoutParams.addRule(RelativeLayout.BELOW, rootView.getId());
        mNotificationsContainer.setLayoutParams(notificationsLayoutParams);


    }

    /* If mListenForever is true, we don't remove listeners, but let them keep listening. */
    private void tryUnregisterEventListeners() {
        if (!mListenForever) {
            mSensorHandler.unregisterEventListeners();
        }
    }

    private boolean isKeyguardSecureShowing() {
        return mKeyguardManager.isKeyguardLocked() && mKeyguardManager.isKeyguardSecure();
    }

    public View getNotificationView() {
        return mNotificationView;
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

    /* Show notification with up-to-date timeout and listen forever configurations. */
    public void showNotification(StatusBarNotification n, boolean update, int peekTimeoutMultiplier,
                                 boolean listenForever) {
        mListenForever = listenForever;
        showNotification(n, update, peekTimeoutMultiplier);
    }

    /* Show notification with up-to-date timeout */
    public void showNotification(StatusBarNotification n, boolean update,
                                 int peekTimeoutMultiplier) {
        mPeekTimeoutMultiplier = peekTimeoutMultiplier;
        showNotification(n, update, false);
    }

    private void showNotification(StatusBarNotification n, boolean update, boolean force) {
        boolean shouldDisplay = shouldDisplayNotification(n) || force;
        addNotification(n);

        if (!mEnabled /* peek is disabled */ || (mPowerManager.isScreenOn() && !mShowing) /* no peek when screen is on */ ||
                !shouldDisplay /* notification has already been displayed */ || !n.isClearable() /* persistent notification */ ||
                mRingingOrConnected /* is phone ringing? */) {
            return;
        }

        if (isNotificationActive(n) && (!update || (update && shouldDisplay))) {
            // update information
            updateNotificationIcons();
            updateSelection(n);

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
                mWakeLockHandler.postDelayed(mPartialWakeLockRunnable, PARTIAL_WAKELOCK_TIME);

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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    public void dismissNotification() {
        if (mShowing) {
            mShowing = false;
            mContext.sendBroadcast(
                    new Intent(NotificationPeekActivity.NotificationPeekReceiver.ACTION_DISMISS));

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
            if (PanelHelper.getContentDescription(n)
                    .equals(PanelHelper.getContentDescription(mShownNotifications.get(i)))) {
                mShownNotifications.set(i, n);
                return;
            }
        }
        mShownNotifications.add(n);
    }

    public void removeNotification(StatusBarNotification n) {
        for (int i = 0; i < mShownNotifications.size(); i++) {
            if (PanelHelper.getContentDescription(n)
                    .equals(PanelHelper.getContentDescription(mShownNotifications.get(i)))) {
                mShownNotifications.remove(i);
                i--;
            }
        }
        updateNotificationIcons();
    }

    public void updateNotificationIcons() {
        if (mShowing) {
            //TODO: Find workaround for updating notificaiton icons when the activity is shown.
            // Because of the thread, we cannot update notification icons here when the acivity
            // is shown.
            return;
        }
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
        for (int i = 0; i < notificationCount; i++) {
            final StatusBarNotification n = mNotificationHub.getNotifications().get(i);
            ImageView icon = new ImageView(mContext);
            if (n.toString().equals(currentNotification)) {
                foundCurrentNotification = true;
            } else {
                icon.setAlpha(ICON_LOW_OPACITY);
            }
            icon.setPadding(padding, 0, padding, 0);
            icon.setImageDrawable(getIconFromResource(n));
            icon.setTag(n);
            mNotificationsContainer.addView(icon);
            LinearLayout.LayoutParams linearLayoutParams =
                    new LinearLayout.LayoutParams(iconSize, iconSize);

            // Wrap LayoutParams to GridLayout.LayoutParams.
            GridLayout.LayoutParams gridLayoutParams =
                    new GridLayout.LayoutParams(linearLayoutParams);
            icon.setLayoutParams(gridLayoutParams);
        }
        if (!foundCurrentNotification) {
            if (notificationCount > 0) {
                updateSelection(mNotificationHub.getNotifications().get(notificationCount - 1));
            } else {
                dismissNotification();
            }
        }
    }

    private void updateSelection(StatusBarNotification n) {

        String oldNotif = PanelHelper
                .getContentDescription((StatusBarNotification) mNotificationView.getTag());
        String newNotif = PanelHelper.getContentDescription(n);
        boolean sameNotification = newNotif.equals(oldNotif);
        if (!mAnimating || sameNotification) {
            // update big icon
            Bitmap b = n.getNotification().largeIcon;
            if (b != null) {
                mNotificationIcon.setImageBitmap(getRoundedShape(b));
            } else {
                mNotificationIcon.setImageDrawable(getIconFromResource(n));
            }
            final PendingIntent contentIntent = n.getNotification().contentIntent;
            if (contentIntent != null) {
                final View.OnClickListener listener = new NotificationClicker(contentIntent, n);
                mNotificationIcon.setOnClickListener(listener);
            } else {
                mNotificationIcon.setOnClickListener(null);
            }
            mNotificationText.setText(getNotificationDisplayText(n));
            mNotificationText.setVisibility(isKeyguardSecureShowing() ? View.GONE : View.VISIBLE);
            mNotificationView.setTag(n);

            if (!sameNotification) {
                mNotificationView.setAlpha(1f);
                mNotificationView.setX(0);
            }
        }

        // update small icons
        for (int i = 0; i < mNotificationsContainer.getChildCount(); i++) {
            ImageView view = (ImageView) mNotificationsContainer.getChildAt(i);
            if ((mAnimating ? oldNotif : newNotif).equals(PanelHelper
                    .getContentDescription((StatusBarNotification) view.getTag()))) {
                view.setAlpha(1f);
            } else {
                view.setAlpha(ICON_LOW_OPACITY);
            }
        }
    }

    private boolean isNotificationActive(StatusBarNotification n) {
        for (int i = 0; i < mNotificationHub.getNotificationCount(); i++) {
            if (PanelHelper.getContentDescription(n).equals(PanelHelper
                            .getContentDescription(mNotificationHub.getNotifications().get(i))
            )) {
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
            if (PanelHelper.getContentDescription(n)
                    .equals(PanelHelper.getContentDescription(shown))) {
                return PanelHelper.shouldDisplayNotification(shown, n);
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

    private String getNotificationDisplayText(StatusBarNotification n) {
        String text = null;
        if (n.getNotification().tickerText != null) {
            text = n.getNotification().tickerText.toString();
        }
        PackageManager pm = mContext.getPackageManager();
        if (n != null) {
            if (text == null) {
                text = PanelHelper.getNotificationTitle(n);
                if (text == null) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(n.getPackageName(), 0);
                        text = (String) pm.getApplicationLabel(ai);
                    } catch (NameNotFoundException e) {
                        // application is uninstalled, run away
                        text = "";
                    }
                }
            }
        }
        return text;
    }

    public Bitmap getRoundedShape(Bitmap scaleBitmapImage) {
        int targetWidth = scaleBitmapImage.getWidth();
        int targetHeight = scaleBitmapImage.getHeight();
        Bitmap targetBitmap =
                Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float) targetWidth - 1) / 2, ((float) targetHeight - 1) / 2,
                (Math.min(((float) targetWidth), ((float) targetHeight)) / 2), Path.Direction.CCW);

        canvas.clipPath(path);
        Bitmap sourceBitmap = scaleBitmapImage;
        canvas.drawBitmap(sourceBitmap,
                new Rect(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight()),
                new Rect(0, 0, targetWidth, targetHeight), null);
        return targetBitmap;
    }

    private Drawable getIconFromResource(StatusBarNotification n) {
        Drawable icon;
        String packageName = n.getPackageName();
        int resource = n.getNotification().icon;
        try {
            Context remotePackageContext = mContext.createPackageContext(packageName, 0);
            icon = remotePackageContext.getResources().getDrawable(resource);
        } catch (NameNotFoundException nnfe) {
            icon = new BitmapDrawable(mContext.getResources(), n.getNotification().largeIcon);
        }
        return icon;
    }

    public void unregisterScreenReceiver() {
        mSensorHandler.unregisterScreenReceiver();
    }

    @Override
    public void onPocketModeChanged(boolean inPocket) {
        // If we set to use always listening, if we detect the device is out of pocket,
        // we restore mNextNotification, and let showNotification to decide whether it is active
        // or not.
        if (inPocket && mListenForever && mNextNotification == null) {
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
        if (!onTable && mListenForever && mNextNotification == null) {
            mNextNotification = mNotificationHub.getCurrentNotification();
        }

        if (!onTable && mNextNotification != null) {
            showNotification(mNextNotification, false, true);
            mNextNotification = null;
        }
    }

    @Override
    public void onScreenStateChaged(boolean screenOn) {
        if (!screenOn) {
            mHandler.removeCallbacksAndMessages(null);
            dismissNotification();
        }
    }


    /**
     * This class is used to display the Notification Peek layout. The original implementation
     * uses Window and directly added layout to it, but it seems impossible to do so externally.
     * <p/>
     * This class also controls waking up the device and removing Peek layout from its parent.
     */
    public static class NotificationPeekActivity extends Activity {

        private TextView mClockTextView;

        private NotificationPeekReceiver mReceiver;

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );

            super.onCreate(savedInstanceState);

            setContentView(mPeekView);

            boolean showClock = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(PreferenceKeys.PREF_CLOCK, true);

            if (showClock) {
                mClockTextView = (TextView) PeekLayoutFactory
                        .createPeekLayout(this, PeekLayoutFactory.LAYOUT_TYPE_CLOCK);
                mPeekView.addView(mClockTextView);
                mClockTextView.setText(getCurrentTimeText());
            }

            mPeekView.setAlpha(1f);
            mPeekView.setVisibility(View.VISIBLE);
            mPeekView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );

            // Initialize broadcast receiver.
            initReceiver();
        }

        private void initReceiver() {

            mReceiver = new NotificationPeekReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(NotificationPeekReceiver.ACTION_DISMISS);
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

        }

        /**
         * Get formatted time String (Follows system setting).
         *
         * @return Formatted time String.
         */
        private String getCurrentTimeText() {
            return android.text.format.DateFormat.getTimeFormat(this).format(new Date());
        }

        private class NotificationPeekReceiver extends BroadcastReceiver {


            public static final String ACTION_DISMISS = "NotificationPeek.dismiss_notification";

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_DISMISS)) {
                    finish();
                } else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                    mClockTextView.setText(getCurrentTimeText());
                }
            }
        }

    }

    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mRingingOrConnected = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    mRingingOrConnected = false;
                    break;
            }
        }
    }

    public class NotificationClicker implements View.OnClickListener {

        private PendingIntent mPendingIntent;

        private StatusBarNotification mNotification;

        public NotificationClicker(PendingIntent mPendingIntent,
                                   StatusBarNotification notification) {
            this.mPendingIntent = mPendingIntent;
            this.mNotification = notification;
        }

        @Override
        public void onClick(View v) {
            try {
                mPendingIntent.send();
                dismissNotification();
                // Reset mOnTable and mInPocket for next notification.
                removeNotification(mNotification);
                mSensorHandler.unregisterEventListeners();
                mNextNotification = null;
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }


}