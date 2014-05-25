package com.reindeercrafts.notificationpeek.blacklist;

/**
 * Application information class used to store app name and package name.
 * <p/>
 * Created by zhelu on 5/21/14.
 */
public class AppInfo {

    private static final String DELIMITER = "<>";

    public String appName;
    public String packageName;

    public AppInfo(String mPackageName, String mAppName) {
        this.appName = mAppName;
        this.packageName = mPackageName;
    }

    /**
     * The default {@link android.widget.ArrayAdapter} displays text using toString() method,
     * so we feed it the app name so that it displays it.
     *
     * @return App name String.
     */
    @Override
    public String toString() {
        return appName;
    }

    /**
     * String representation of the object, used to save into SharedPreferences.
     *
     * @return 'package name <> app name' formatted String.
     */
    public String getString() {
        return packageName + DELIMITER + appName;
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

    @Override
    public boolean equals(Object o) {
        return ((AppInfo) o).packageName.equals(packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }
}