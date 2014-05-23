package com.reindeercrafts.notificationpeek.blacklist;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by zhelu on 5/21/14.
 */
public class AppList {

    private static final String BLACK_LIST_PREF = "BlackList";

    private static AppList INSTANCE;

    private PackageManager mPackageManager;

    private ArrayList<AppInfo> mCurrentBlackList;
    private List<ApplicationInfo> mApplicationInfos;

    private Context mContext;

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

    public void addToBlackList(AppInfo appInfo) {
        if (!mCurrentBlackList.contains(appInfo)) {
            mCurrentBlackList.add(appInfo);
        }
    }

    public void removeFromBlackList(AppInfo appInfo) {
        mCurrentBlackList.remove(appInfo);
    }

    public ArrayList<AppInfo> getAppList(CharSequence constraint) {

        if (constraint == null || constraint.length() == 0) {
            return new ArrayList<AppInfo>();
        }

        ArrayList<AppInfo> appInfos = new ArrayList<AppInfo>();

        for (ApplicationInfo info : mApplicationInfos) {
            String appName = mPackageManager.getApplicationLabel(info).toString();
            String keyWord = constraint.toString().toLowerCase();

            if (
//                    (info.flags & ApplicationInfo.FLAG_SYSTEM ) == 0 &&
                    appName.toLowerCase().contains(keyWord)) {
                appInfos.add(new AppInfo(info.packageName, appName));
            }
        }

        return appInfos;
    }

    /**
     * Save the current black list into SharedPreferences as String set.
     */
    public void storeBlackList() {
        SharedPreferences blackListPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        HashSet<String> blackList = new HashSet<String>();

        for (AppInfo appInfo : mCurrentBlackList) {
            blackList.add(appInfo.toString());
        }
        blackListPref.edit().putStringSet(BLACK_LIST_PREF, blackList).apply();
    }

    /**
     * Restore black list from SharedPreferences.
     *
     * @return  List of {@link com.reindeercrafts.notificationpeek.blacklist.AppInfo}.
     */
    private ArrayList<AppInfo> restoreBlackList() {
        ArrayList<AppInfo> blackList = new ArrayList<AppInfo>();

        SharedPreferences blackListPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        Set<String> blackListSet =
                blackListPref.getStringSet(BLACK_LIST_PREF, new HashSet<String>());

        for (String blockedAppStr : blackListSet) {
            blackList.add(AppInfo.fromString(blockedAppStr));
        }

        return blackList;
    }

    /**
     * Check if the package name is in black list.
     *
     * @param packageName   Given package name.
     * @return              True if it is in black list, False otherwise.
     */
    public boolean isInBlackList(String packageName) {
        for (AppInfo appInfo : mCurrentBlackList) {
            if (packageName.equals(appInfo.packageName)) {
                return true;
            }
        }

        return false;
    }


}
