package com.reindeercrafts.notificationpeek.settings.appearance;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

/**
 * Helper class used for generating background bitmap based on user preferences and check current
 * Peek background preference.
 *
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

    public WallpaperFactory(Context context) {
        this.mContext = context;
        mWallpaperManager = WallpaperManager.getInstance(context);
    }

    /**
     * Create a bitmap that is blurred and dimmed with the amount that user has selected.
     *
     * @return          Background bitmap.
     */
    public Bitmap getPrefSystemWallpaper() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        float radius = preferences
                .getFloat(PreferenceKeys.PREF_RADIUS, ImageBlurrer.MAX_SUPPORTED_BLUR_PIXELS);
        int dim = preferences.getInt(PreferenceKeys.PREF_DIM, DEFAULT_MAX_DIM);

        // Blur
        ImageBlurrer imageBlurrer = new ImageBlurrer(mContext);
        Bitmap blurred = imageBlurrer
                .blurBitmap(drawableToBitmap(mWallpaperManager.getFastDrawable()), radius);
        // Dim
        Canvas c = new Canvas(blurred);
        c.drawColor(Color.argb(255 - dim, 0, 0, 0));

        return blurred;
    }

    /**
     * Convert drawable to bitmap.
     *
     * @param drawable      Drawable object to be converted.
     * @return              converted bitmap.
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap =
                Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Check if user selected system wallpaper as Peek background.
     *
     * @return          True if system wallpaper is selected, false otherwise.
     */
    public  boolean isWallpaperThemeSelected() {
        return PreferenceManager.getDefaultSharedPreferences(mContext)
                .getInt(PreferenceKeys.PREF_BACKGROUND, BACKGROUND_PURE_BLACK) ==
                BACKGROUND_SYSTEM_WALLPAPER;
    }
}
