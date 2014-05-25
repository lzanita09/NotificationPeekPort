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

    private Spinner mBackgroundSpinner;
    private boolean mNothingSelected = true;

    private ImageView mPreviewImageView;
    private TransitionDrawable mPreviewImageDrawable;

    private SeekBar mRadiusSeek;
    private SeekBar mDimSeek;
    private TextView mRadiusText;
    private TextView mDimText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mHandler = new Handler(Looper.getMainLooper());

        mPreviewImageDrawable = initPreviewBackgroundDrawable();

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
     * @return  TransitionDrawable object created.
     */
    private TransitionDrawable initPreviewBackgroundDrawable() {
        Drawable[] drawables;

        boolean isWallpaperSelected = WallpaperFactory.isWallpaperThemeSelected(getActivity());
        Drawable black = new ColorDrawable(Color.BLACK);
        Drawable wallpaper = new BitmapDrawable(getResources(),
                WallpaperFactory.getPrefSystemWallpaper(getActivity()));

        drawables = !isWallpaperSelected ?
                new Drawable[]{black, wallpaper} :
                new Drawable[]{wallpaper, black};

        return new TransitionDrawable(drawables);
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
        mDimSeek.setProgress(dim);

        mRadiusSeek.setOnSeekBarChangeListener(this);
        mDimSeek.setOnSeekBarChangeListener(this);
    }

    /**
     * Seek Drawable index within TransitionDrawable object. We need to find the index of the
     * drawable that is different from the parameter, which is the starting drawable of the
     * TransitionDrawable.
     *
     * @param T target drawable class.
     * @return  Index of the source drawable.
     */
    private int getSourceDrawableIndex(Class T) {
        if (mPreviewImageDrawable.getDrawable(0).getClass().equals(T) &&
                mPreviewImageDrawable.getDrawable(1).getClass().equals(T)) {
            // At this point we know that user is using the SeekBar to adjust background, so that
            // we have two Drawables having the same type. Therefore, we return the last index of
            // the TransitionDrawable, which points to the "Current" drawable that user chooses.
            return 1;
        }

        // One of the Drawables is a ColorDrawable, the other is a BitmapDrawable, choose the one
        // that is different from the given class.
        return mPreviewImageDrawable.getDrawable(0).getClass().equals(T) ? 1 : 0;
    }

    /**
     * Update layout components (SeekBar, TextView, ImageView) according to the new preference.
     *
     * @param manual    If the update is from user or from the initialization.
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
            mRadiusSeek.setEnabled(true);
            mDimSeek.setEnabled(true);
            mRadiusText.setEnabled(true);
            mDimText.setEnabled(true);
        }
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

        Intent intent = new Intent(NotificationService.ACTION_PREFERENCE_CHANGED);
        intent.putExtra(PreferenceKeys.INTENT_ACTION_KEY, PreferenceKeys.PREF_BACKGROUND);
        getActivity().sendBroadcast(intent);
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
        mHandler.postDelayed(mReloadPreviewRunnable, 300);

        // Inform NotificationPeek to update background image.
        Intent intent = new Intent(NotificationService.ACTION_PREFERENCE_CHANGED);
        String intentActionKey = seekBar.getId() ==
                R.id.radius_seek ? PreferenceKeys.PREF_RADIUS : PreferenceKeys.PREF_DIM;
        intent.putExtra(PreferenceKeys.INTENT_ACTION_KEY, intentActionKey);
        getActivity().sendBroadcast(intent);
    }

    private Runnable mReloadPreviewRunnable = new Runnable() {
        @Override
        public void run() {
            mPreviewImageDrawable = new TransitionDrawable(new Drawable[]{mPreviewImageDrawable
                    .getDrawable(getSourceDrawableIndex(ColorDrawable.class)), new BitmapDrawable(
                    getResources(), WallpaperFactory.getPrefSystemWallpaper(getActivity()))}
            );
            mPreviewImageView.setImageDrawable(mPreviewImageDrawable);
            mPreviewImageDrawable.startTransition(TRANSITION_ANIM_DURATION);
        }
    };

    private Runnable mChangePreviewRunnable = new Runnable() {
        @Override
        public void run() {
            // Build a new TransitionDrawable that presents the change of preview images.
            Drawable[] previewDrawables = new Drawable[2];

            previewDrawables[1] =
                    WallpaperFactory.isWallpaperThemeSelected(getActivity()) ? new BitmapDrawable(
                            getResources(), WallpaperFactory
                            .getPrefSystemWallpaper(getActivity())) : new ColorDrawable(
                            Color.BLACK);
            previewDrawables[0] = mPreviewImageDrawable
                    .getDrawable(getSourceDrawableIndex(previewDrawables[1].getClass()));

            mPreviewImageDrawable = new TransitionDrawable(previewDrawables);
            mPreviewImageView.setImageDrawable(mPreviewImageDrawable);
            updateLayouts(true);
        }
    };


}
