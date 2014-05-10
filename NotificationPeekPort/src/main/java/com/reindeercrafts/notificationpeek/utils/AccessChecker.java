package com.reindeercrafts.notificationpeek.utils;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

import com.reindeercrafts.notificationpeek.LockscreenDeviceAdminReceiver;
import com.reindeercrafts.notificationpeek.NotificationService;

import java.lang.ref.WeakReference;

/**
 * A helper class that is used to check if the notification access and device administrators access
 * are enabled.
 *
 * Code modified from AcDisplay by AChep@xda <artemchep@gmail.com>:
 * https://github.com/AChep/AcDisplay/blob/master/project/ActiveDisplay
 *
 * Created by zhelu on 5/9/14.
 */
public class AccessChecker {

    private static WeakReference<ComponentName> mAdminComponentName;

    public static boolean isNotificationAccessEnabled(Context context) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDeviceAdminEnabled(Context context) {
        ComponentName admin;
        if (mAdminComponentName == null || mAdminComponentName.get() == null) {
            admin = new ComponentName(context, LockscreenDeviceAdminReceiver.class);
            mAdminComponentName = new WeakReference<ComponentName>(admin);
        } else {
            admin = mAdminComponentName.get();
        }

        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isAdminActive(admin);
    }
}
