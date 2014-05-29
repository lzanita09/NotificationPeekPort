package com.reindeercrafts.notificationpeek.blacklist;

/**
 * Application information class used to store app name and package name.
 * <p/>
 * Created by zhelu on 5/21/14.
 */
public class AppInfo {

    public static final String EVERYTHING_PKG = AppInfo.class.getName() + ".everything";
    public static final String EVERYTHING_APP_NAME = "Everything";

    private static final String DELIMITER = "<>";

    private String mAppName;
    private String mPackageName;

    public AppInfo(String mPackageName, String mAppName) {
        this.mAppName = mAppName;
        this.mPackageName = mPackageName;
    }

    public String getAppName() {
        return mAppName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    /**
     * The default {@link android.widget.ArrayAdapter} displays text using toString() method,
     * so we feed it the app name so that it displays it.
     *
     * @return App name String.
     */
    @Override
    public String toString() {
        return mAppName;
    }

    /**
     * String representation of the object, used to save into SharedPreferences.
     *
     * @return 'package name <> app name' formatted String.
     */
    public String getString() {
        return mPackageName + DELIMITER + mAppName;
    }

    /**
     * Restore object from String.
     *
     * @param str String generated from {@link com.reindeercrafts.notificationpeek.blacklist.AppInfo#getString()}
     *            method.
     * @return AppInfo object.
     */
    public static AppInfo fromString(String str) {
        if (!str.matches(".*<>.*")) {
            return new AppInfo("", "");
        }
        String[] infos = str.split(DELIMITER);
        return new AppInfo(infos[0], infos[1]);
    }

    public static AppInfo createEverythingInfo() {
        return new AppInfo(EVERYTHING_PKG, EVERYTHING_APP_NAME);
    }

    @Override
    public boolean equals(Object o) {
        return ((AppInfo) o).mPackageName.equals(mPackageName);
    }

    @Override
    public int hashCode() {
        return mPackageName.hashCode();
    }
}