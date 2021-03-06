package com.reindeercrafts.notificationpeek.settings.appearance;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.NotificationService;
import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

/**
 * Appearance setting fragment used to contain Peek view background preferences, including
 * pure black/system wallpaper option, blur radius and dim option.
 * <p/>
 * Created by zhelu on 5/23/14.
 */
public class AppearanceSettingsFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener {

    public static final int TRANSITION_ANIM_DURATION = 1000; // 1s

    private Handler mHandler;

    private WallpaperFactory mWallpaperFactory;

    private Spinner mBackgroundSpinner;
    private boolean mNothingSelected = true;

    private ImageView mPreviewImageView;
    private TransitionDrawable mPreviewImageDrawable;
    private Drawable[] mChangeDrawables;
    private Drawable[] mAdjustDrawables;
    private int mBlackDrawableIndex;
    private boolean mAdjusted;

    private SeekBar mRadiusSeek;
    private SeekBar mDimSeek;
    private TextView mRadiusText;
    private TextView mDimText;

    private boolean mUseLiveWallpaper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mHandler = new Handler(Looper.getMainLooper());
        mWallpaperFactory = new WallpaperFactory(getActivity());

        mUseLiveWallpaper = WallpaperFactory.isLiveWallpaperUsed(getActivity());
        mPreviewImageDrawable = initPreviewBackgroundDrawable();
        mPreviewImageDrawable.setCrossFadeEnabled(true);

        View rootView = inflater.inflate(R.layout.appearance_fragment_layout, container, false);

        // Background selection spinner.
        mBackgroundSpinner = (Spinner) rootView.findViewById(R.id.spinner);
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<String>(getActivity(), R.layout.spinner_item_layout,
                        R.id.text_view,
                        getResources().getStringArray(R.array.background_pref_array));
        mBackgroundSpinner.setAdapter(spinnerAdapter);
        mBackgroundSpinner.setOnItemSelectedListener(this);

        // Background preview image view.
        mPreviewImageView = (ImageView) rootView.findViewById(R.id.preview_image_view);
        mPreviewImageView.setImageDrawable(mPreviewImageDrawable);

        // Seek bars and headers.
        mRadiusSeek = (SeekBar) rootView.findViewById(R.id.radius_seek);
        mDimSeek = (SeekBar) rootView.findViewById(R.id.dim_seek);
        mRadiusText = (TextView) rootView.findViewById(R.id.radius_text);
        mDimText = (TextView) rootView.findViewById(R.id.dim_text);
        initSeekBars();

        updateLayouts(false);


        return rootView;
    }

    /**
     * Create a new {@link android.graphics.drawable.TransitionDrawable} object with correct order
     * of Drawables based on user selection.
     *
     * @return TransitionDrawable object created.
     */
    private TransitionDrawable initPreviewBackgroundDrawable() {

        boolean isWallpaperSelected = WallpaperFactory.isWallpaperThemeSelected(getActivity());
        Drawable black = new ColorDrawable(Color.BLACK);
        Drawable wallpaper =
                mUseLiveWallpaper ? new ColorDrawable(Color.TRANSPARENT) : new BitmapDrawable(
                        getResources(), mWallpaperFactory.getPrefSystemWallpaper());

        mChangeDrawables =
                !isWallpaperSelected ? new Drawable[]{black, wallpaper} : new Drawable[]{wallpaper, black};
        mBlackDrawableIndex = !isWallpaperSelected ? 0 : 1;
        return new TransitionDrawable(mChangeDrawables);
    }

    private void initSeekBars() {
        mRadiusSeek.setMax(ImageBlurrer.MAX_SUPPORTED_BLUR_PIXELS * 10);
        mDimSeek.setMax(WallpaperFactory.DEFAULT_MAX_DIM);

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        int radius = (int) (preferences
                .getFloat(PreferenceKeys.PREF_RADIUS, ImageBlurrer.MAX_SUPPORTED_BLUR_PIXELS) * 10);
        int dim = preferences.getInt(PreferenceKeys.PREF_DIM, WallpaperFactory.DEFAULT_MAX_DIM);

        mRadiusSeek.setProgress(radius);
        mDimSeek.setProgress(255 - dim);

        mRadiusSeek.setOnSeekBarChangeListener(this);
        mDimSeek.setOnSeekBarChangeListener(this);
    }

    /**
     * Seek Drawable index within TransitionDrawable object. We need to find the index of the
     * drawable that is the starting drawable of the TransitionDrawable.
     *
     * @return Index of the source drawable.
     */
    private int getSourceDrawableIndex() {
        if (mPreviewImageDrawable.getDrawable(0).getClass()
                .equals(mPreviewImageDrawable.getDrawable(1).getClass())) {
            // At this point we know that user is using the SeekBar to adjust background, so that
            // we have two Drawables having the same type. Therefore, we return the last index of
            // the TransitionDrawable, which points to the "Current" drawable that user chooses.
            return 1;
        }

        // One of the Drawables is a ColorDrawable (Black), the other is a BitmapDrawable, choose the one
        // that is different from the Black ColorDrawable.
        return (mBlackDrawableIndex + 1) % 2;
    }

    /**
     * Update layout components (SeekBar, TextView, ImageView) according to the new preference.
     *
     * @param manual If the update is from user or from the initialization.
     */
    private void updateLayouts(boolean manual) {
        if (!WallpaperFactory.isWallpaperThemeSelected(getActivity())) {
            // Using pure black.
            if (!manual) {
                mBackgroundSpinner.setSelection(0);
            } else {
                mPreviewImageDrawable.reverseTransition(TRANSITION_ANIM_DURATION);
            }
            mRadiusSeek.setEnabled(false);
            mDimSeek.setEnabled(false);
            mRadiusText.setEnabled(false);
            mDimText.setEnabled(false);


        } else {
            // using system wallpaper.
            if (!manual) {
                mBackgroundSpinner.setSelection(1);
            } else {
                mPreviewImageDrawable.reverseTransition(TRANSITION_ANIM_DURATION);
            }
            mRadiusSeek.setEnabled(true && !mUseLiveWallpaper);
            mDimSeek.setEnabled(true && !mUseLiveWallpaper);
            mRadiusText.setEnabled(true && !mUseLiveWallpaper);
            mDimText.setEnabled(true && !mUseLiveWallpaper);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Inform Notification Peek to update Peek view background.
        Intent intent = new Intent(NotificationService.ACTION_PREFERENCE_CHANGED);
        intent.putExtra(PreferenceKeys.INTENT_ACTION_KEY, PreferenceKeys.PREF_BACKGROUND);
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        // onItemSelected() is called the first time the SeekBar is populated but nothing has been
        // selected, so we use this boolean value to work it around.
        if (mNothingSelected) {
            mNothingSelected = false;
            return;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (position == 0) {
            pref.edit()
                    .putInt(PreferenceKeys.PREF_BACKGROUND, WallpaperFactory.BACKGROUND_PURE_BLACK)
                    .apply();
        } else {
            pref.edit().putInt(PreferenceKeys.PREF_BACKGROUND,
                    WallpaperFactory.BACKGROUND_SYSTEM_WALLPAPER).apply();
        }

        mHandler.postDelayed(mChangePreviewRunnable, 300);


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        switch (seekBar.getId()) {
            case R.id.radius_seek:
                float newRadius = progress / 10f;
                editor.putFloat(PreferenceKeys.PREF_RADIUS, newRadius).apply();
                break;

            case R.id.dim_seek:
                editor.putInt(PreferenceKeys.PREF_DIM, 255 - progress).apply();
                break;
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Change preview background.
        mAdjusted = true;
        mHandler.postDelayed(mReloadPreviewRunnable, 300);
    }

    private Runnable mReloadPreviewRunnable = new Runnable() {
        @Override
        public void run() {
            mAdjustDrawables = new Drawable[]{mPreviewImageDrawable
                    .getDrawable(getSourceDrawableIndex()), new BitmapDrawable(
                    getResources(), mWallpaperFactory.getPrefSystemWallpaper())};
            mPreviewImageDrawable = new TransitionDrawable(mAdjustDrawables);
            mPreviewImageView.setImageDrawable(mPreviewImageDrawable);
            mPreviewImageDrawable.startTransition(TRANSITION_ANIM_DURATION);
        }
    };

    private Runnable mChangePreviewRunnable = new Runnable() {
        @Override
        public void run() {
            // Build a new TransitionDrawable that presents the change of preview images.
            if (mAdjusted) {
                mAdjusted = false;
                mChangeDrawables[1] = mChangeDrawables[mBlackDrawableIndex];
                mChangeDrawables[0] = mAdjustDrawables[1];
                mPreviewImageDrawable = new TransitionDrawable(mChangeDrawables);
                mPreviewImageView.setImageDrawable(mPreviewImageDrawable);
            }

            updateLayouts(true);
        }
    };


}
