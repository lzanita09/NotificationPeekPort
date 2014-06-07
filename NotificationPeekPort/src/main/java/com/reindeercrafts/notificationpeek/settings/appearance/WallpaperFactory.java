package com.reindeercrafts.notificationpeek.settings.appearance;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

/**
 * Helper class used for generating background bitmap based on user preferences and check current
 * Peek background preference.
 * <p/>
 * Created by zhelu on 5/24/14.
 */
public class WallpaperFactory {

    public static final int DEFAULT_MAX_DIM = 128; // technical max 255

    // SharedPreferences value for pure black background.
    public static final int BACKGROUND_PURE_BLACK = 1;

    // SharedPreferenes value for system wallpaper.
    public static final int BACKGROUND_SYSTEM_WALLPAPER = 2;

    private WallpaperManager mWallpaperManager;
    private Context mContext;

    /**
     * Check if live wallpaper is used.
     *
     * @param context   Context object.
     * @return          True if live wallpaper is used, false otherwise.
     */
    public static boolean isLiveWallpaperUsed(Context context) {
        return WallpaperManager.getInstance(context).getWallpaperInfo() != null;
    }

    /**
     * Check if user selected system wallpaper as Peek background.
     *
     * @return True if system wallpaper is selected, false otherwise.
     */
    public static boolean isWallpaperThemeSelected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PreferenceKeys.PREF_BACKGROUND, BACKGROUND_PURE_BLACK) ==
                BACKGROUND_SYSTEM_WALLPAPER;
    }

    public WallpaperFactory(Context context) {
        this.mContext = context;
        mWallpaperManager = WallpaperManager.getInstance(context);
    }

    /**
     * Create a bitmap that is blurred and dimmed with the amount that user has selected.
     *
     * @return Background bitmap.
     */
    public Bitmap getPrefSystemWallpaper() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        float radius = preferences
                .getFloat(PreferenceKeys.PREF_RADIUS, ImageBlurrer.MAX_SUPPORTED_BLUR_PIXELS);
        int dim = preferences.getInt(PreferenceKeys.PREF_DIM, DEFAULT_MAX_DIM);

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();

        // Blur
        ImageBlurrer imageBlurrer = new ImageBlurrer(mContext);
        Bitmap blurred = imageBlurrer.blurBitmap(drawableToBitmap(mWallpaperManager.getFastDrawable(),
                displayMetrics.widthPixels), radius);
        // Dim
        Canvas c = new Canvas(blurred);
        c.drawColor(Color.argb(255 - dim, 0, 0, 0));

        return blurred;
    }

    /**
     * Convert drawable to bitmap.
     *
     * @param drawable Drawable object to be converted.
     * @return converted bitmap.
     */
    private Bitmap drawableToBitmap(Drawable drawable, int width) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        bitmap = cropBitmap(bitmap, width);

        return bitmap;
    }

    /**
     * Crop wallpaper to fit the Peek view.
     *
     * @param original  Original Wallpaper bitmap.
     * @param width     Desired width.
     * @return          Cropped bitmap.
     */
    private Bitmap cropBitmap(Bitmap original, int width) {
        if (width > original.getWidth()) {
            // If the wallpaper doesn't even have the width of the screen, don't crop.
            return original;
        }
        Bitmap cropped = Bitmap.createBitmap(original, 0, 0, width, original.getHeight());
        return cropped;
    }


}
