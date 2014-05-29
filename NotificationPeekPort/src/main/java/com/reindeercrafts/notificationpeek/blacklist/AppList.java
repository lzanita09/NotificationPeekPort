package com.reindeercrafts.notificationpeek.blacklist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;

import com.reindeercrafts.notificationpeek.NotificationService;
import com.reindeercrafts.notificationpeek.peek.NotificationHelper;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by zhelu on 5/21/14.
 */
public class AppList {

    private static final String BLACK_LIST_PREF = "BlackList";
    private static final String KEYWOR_EVERYTHING = "everything";

    private static AppList INSTANCE;

    private PackageManager mPackageManager;

    private ArrayList<AppInfo> mCurrentBlackList;
    private List<ApplicationInfo> mApplicationInfos;
    private boolean mBlackListChanged = false;
    private boolean mQuietHourChanged = false;

    private Context mContext;

    private QuietHour mQuietHour;
    private long mFromTime;
    private long mToTime;

    public static synchronized AppList getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AppList(context);
        }

        return INSTANCE;
    }

    private AppList(Context context) {
        this.mContext = context;
        mPackageManager = context.getPackageManager();
        mApplicationInfos = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        mCurrentBlackList = restoreBlackList();
    }

    public ArrayList<AppInfo> getCurrentBlackList() {
        return mCurrentBlackList;
    }

    public boolean addToBlackList(AppInfo appInfo) {
        if (NotificationHelper.isPeekDisabled(mContext)) {
            return false;
        }
        if (!mCurrentBlackList.contains(appInfo)) {
            if (appInfo.getPackageName().equals(AppInfo.EVERYTHING_PKG)) {
                // If the added item is "Everything", add it to the first place.
                mCurrentBlackList.add(0, appInfo);
                mQuietHourChanged = true;
            } else {
                mCurrentBlackList.add(appInfo);
            }

            mBlackListChanged = true;
        }

        return true;
    }

    public void removeFromBlackList(AppInfo appInfo) {
        mCurrentBlackList.remove(appInfo);

        if (appInfo.getPackageName().equals(AppInfo.EVERYTHING_PKG)) {
            // If the removed item is "Everything", we also remove all related preferences.
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            preferences.edit().remove(PreferenceKeys.PREF_QUIET_HOUR)
                    .remove(PreferenceKeys.PREF_DISABLE_PEEK).apply();
            mQuietHourChanged = true;
        }

        mBlackListChanged = true;
    }

    public void removeOthersFromBlackList() {
        Iterator<AppInfo> it = mCurrentBlackList.iterator();
        while (it.hasNext()) {
            AppInfo appInfo = it.next();
            if (!appInfo.getPackageName().equals(AppInfo.EVERYTHING_PKG)) {
                it.remove();
            }
        }
    }

    public ArrayList<AppInfo> getAppList(CharSequence constraint) {

        if (constraint == null || constraint.length() == 0) {
            return new ArrayList<AppInfo>();
        }

        ArrayList<AppInfo> appInfos = new ArrayList<AppInfo>();

        String keyWord = constraint.toString().toLowerCase();

        if (KEYWOR_EVERYTHING.contains(keyWord)) {
            appInfos.add(AppInfo.createEverythingInfo());
        }

        for (ApplicationInfo info : mApplicationInfos) {
            String appName = mPackageManager.getApplicationLabel(info).toString();

            if (appName.toLowerCase().contains(keyWord)) {
                appInfos.add(new AppInfo(info.packageName, appName));
            }
        }

        return appInfos;
    }

    /**
     * Save the current black list into SharedPreferences as String set.
     */
    public void storeBlackList() {
        if (!mBlackListChanged) {
            // Black list hasn't changed since last restore.
            return;
        }
        SharedPreferences blackListPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        HashSet<String> blackList = new HashSet<String>();

        for (AppInfo appInfo : mCurrentBlackList) {
            blackList.add(appInfo.getString());
        }
        blackListPref.edit().putStringSet(BLACK_LIST_PREF, blackList).apply();

        if (mQuietHourChanged) {
            // Notify NotificationService to update quiet hour.
            Intent intent = new Intent(NotificationService.ACTION_QUIET_HOUR_CHANGE);
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * Restore black list from SharedPreferences.
     *
     * @return List of {@link com.reindeercrafts.notificationpeek.blacklist.AppInfo}.
     */
    private ArrayList<AppInfo> restoreBlackList() {
        ArrayList<AppInfo> blackList = new ArrayList<AppInfo>();

        SharedPreferences blackListPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        Set<String> blackListSet =
                blackListPref.getStringSet(BLACK_LIST_PREF, new HashSet<String>());


        for (String blockedAppStr : blackListSet) {
            AppInfo appInfo = AppInfo.fromString(blockedAppStr);
            if (appInfo.getPackageName().equals(AppInfo.EVERYTHING_PKG)) {
                blackList.add(0, appInfo);
            } else {
                blackList.add(AppInfo.fromString(blockedAppStr));
            }
        }

        return blackList;
    }

    /**
     * Update quiet hour period.
     */
    public void updateQuietHour() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String quietHourStr = preferences
                .getString(PreferenceKeys.PREF_QUIET_HOUR, PreferenceKeys.PREF_QUIET_HOUR_DEF);
        mQuietHour = QuietHour.createQuietHour(quietHourStr);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, mQuietHour.getFromHour());
        calendar.set(Calendar.MINUTE, mQuietHour.getFromMin());
        mFromTime = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, mQuietHour.getToHour());
        calendar.set(Calendar.MINUTE, mQuietHour.getToMin());
        if (calendar.getTimeInMillis() < mFromTime) {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
        }
        mToTime = calendar.getTimeInMillis();
    }

    /**
     * Check if the package name is in black list, or Peek is disabled, or it is in quiet hour now.
     *
     * @param sbn StatusBarNotification object to be checked.
     * @return True if it is in black list, False otherwise.
     */
    public boolean shouldPeekWakeUp(StatusBarNotification sbn) {
        if (NotificationHelper.isPeekDisabled(mContext)) {
            return true;
        }
        for (AppInfo appInfo : mCurrentBlackList) {
            if (sbn.getPackageName().equals(appInfo.getPackageName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the posted time of the notification is in quiet hour.
     *
     * @param postedTime    Timestamp in ms to check.
     * @return              True if it is in quiet hour, false otherwise.
     */
    public boolean isInQuietHour(long postedTime) {
        if (mQuietHour == null) {
            updateQuietHour();
        }

        if (postedTime >= mFromTime && postedTime <= mToTime) {
            return true;
        }

        return false;
    }


}
