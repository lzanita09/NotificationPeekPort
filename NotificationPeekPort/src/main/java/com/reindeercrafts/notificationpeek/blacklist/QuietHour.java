package com.reindeercrafts.notificationpeek.blacklist;

import android.content.Context;

import com.reindeercrafts.notificationpeek.R;

/**
 * QuietHour object used to store and restore quiet hour settings.
 *
 * Created by zhelu on 5/27/14.
 */
public class QuietHour {

    private static final String DELIMITER = "<>";

    private int mFromHour = -1;
    private int mFromMin = -1;
    private int mToHour = -1;
    private int mToMin = -1;

    // Check if user set "From" time.
    private boolean mFromSet;

    // Check if user set "To" time.
    private boolean mToSet;

    public QuietHour(int fromHour, int fromMin, int toHour, int toMin) {
        this.mFromHour = fromHour;
        this.mFromMin = fromMin;
        this.mToHour = toHour;
        this.mToMin = toMin;
    }

    public int getFromHour() {
        return mFromHour;
    }

    public int getFromMin() {
        return mFromMin;
    }

    public int getToHour() {
        return mToHour;
    }

    public int getToMin() {
        return mToMin;
    }

    public void setFromTime(int hr, int min) {
        mFromHour = hr;
        mFromMin = min;
        mFromSet = true;
    }

    public void setToTime(int hr, int min) {
        mToHour = hr;
        mToMin = min;
        mToSet = true;
    }

    /* Construct quiet hour String for display. */
    public String getDisplayTime(Context context) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(context.getString(R.string.from))
                .append(getTimeText(mFromHour, mFromMin))
                .append(" ")
                .append(context.getString(R.string.to))
                .append(getTimeText(mToHour, mToMin));
        return buffer.toString();
    }

    public String getFromTimeText() {
        return getTimeText(mFromHour, mFromMin);
    }

    public String getToTimeText() {
        return getTimeText(mToHour, mToMin);
    }

    public void reset() {
        mToSet = false;
        mFromSet = false;
    }

    private String getTimeText(int hr, int min) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(" ");
        buffer.append(hr);
        buffer.append(" : ");
        if (min < 10) {
            buffer.append("0");
        }
        buffer.append(min);

        return buffer.toString();
    }

    /**
     * Create new QuietHour instance from given string. The String object should have the format
     * of "HH:mm<>HH:mm", which indicates the "From" time and the "To" time.
     *
     * @param quiet     QuietHour String representation.
     * @return          New QuietHour instance.
     */
    public static QuietHour createQuietHour(String quiet) {
        if (!quiet.matches("\\d+:\\d+<>\\d+:\\d+")) {
            return new QuietHour(-1, -1, -1, -1);
        }
        String[] quietHour = quiet.split(DELIMITER);
        String[] from = quietHour[0].split(":");
        int fromHour = Integer.parseInt(from[0]);
        int fromMin = Integer.parseInt(from[1]);

        String[] to = quietHour[1].split(":");
        int toHour = Integer.parseInt(to[0]);
        int toMin = Integer.parseInt(to[1]);

        return new QuietHour(fromHour, fromMin, toHour, toMin);
    }

    public boolean isBothTimeSet() {
        return mFromSet && mToSet;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(mFromHour).append(":").append(mFromMin).append(DELIMITER).append(mToHour).append(":")
                .append(mToMin);
        return buffer.toString();
    }
}
