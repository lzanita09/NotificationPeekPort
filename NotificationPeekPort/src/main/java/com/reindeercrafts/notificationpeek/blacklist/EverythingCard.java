package com.reindeercrafts.notificationpeek.blacklist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

/**
 * Subclass of RelativeLayout to display everything black list item's functionality:
 * - Disable Peek.
 * - Setup quiet hour.
 *
 * Created by zhelu on 5/26/14.
 */
public class EverythingCard extends RelativeLayout implements View.OnClickListener {

    private static final long ANIM_DURATION = 300;
    private static final java.lang.String DELIMITER = "|";

    private ViewSwitcher mPanelSwitcher;
    private Button mFromBtn;
    private Button mToBtn;
    private TextView mFromToText;

    private QuietHour mQuietHour;

    // Quiet hour or disable button callback.
    private OnPanelButtonClickedListener mOnPanelButtonClickedListener;

    private boolean mOptionsShowing = true;

    public void setOnPanelButtonClickedListener(OnPanelButtonClickedListener listener) {
        this.mOnPanelButtonClickedListener = listener;
    }

    public EverythingCard(Context context) {
        super(context);
    }

    public EverythingCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EverythingCard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isPeekDisabled = preferences.getBoolean(PreferenceKeys.PREF_DISABLE_PEEK, false);
        String quietHourStr = preferences
                .getString(PreferenceKeys.PREF_QUIET_HOUR, PreferenceKeys.PREF_QUIET_HOUR_DEF);
        boolean isQuietHourSet = !quietHourStr.equals(PreferenceKeys.PREF_QUIET_HOUR_DEF);

        mOptionsShowing = !(isPeekDisabled || isQuietHourSet);
        mQuietHour = QuietHour.createQuietHour(quietHourStr);

        LayoutInflater.from(context).inflate(R.layout.everything_card, this, true);
        mPanelSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        if (!mOptionsShowing) {
            mPanelSwitcher.setVisibility(GONE);
        }

        mFromBtn = (Button) findViewById(R.id.quiet_hour_from_btn);
        mFromBtn.setOnClickListener(this);

        mToBtn = (Button) findViewById(R.id.quiet_hour_to_btn);
        mToBtn.setOnClickListener(this);

        Button mQuietHourBtn = (Button) findViewById(R.id.quiet_hour_btn);
        mQuietHourBtn.setOnClickListener(this);

        Button mDisableBtn = (Button) findViewById(R.id.as_is_btn);
        mDisableBtn.setOnClickListener(this);

        mFromToText = (TextView) findViewById(R.id.from_to_text);
        if (isQuietHourSet) {
            displayTime();
        }
        mFromToText.setOnClickListener(this);
    }

    /* Update instance's current "from" time. */
    public void setFromTime(int hr, int min) {
        mQuietHour.setFromTime(hr, min);

        mFromBtn.setText(mQuietHour.getFromTimeText());

        if (mQuietHour.isBothTimeSet()) {
            displayTime();
            hideOptions();
            storeQuietHour();
        }
    }

    /* Update instance's current "to" time. */
    public void setToTime(int hr, int min) {
        mQuietHour.setToTime(hr, min);

        mToBtn.setText(mQuietHour.getToTimeText());

        if (mQuietHour.isBothTimeSet()) {
            displayTime();
            hideOptions();
            storeQuietHour();
        }
    }


    /* Store instance's QuietHour object. */
    private void storeQuietHour(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.edit().putString(PreferenceKeys.PREF_QUIET_HOUR, mQuietHour.toString()).apply();
    }

    private void displayTime() {
        mFromToText.setVisibility(VISIBLE);
        mFromToText.setText(mQuietHour.getDisplayTime(getContext()));
    }

    private void hideOptions() {
        if (!mOptionsShowing) {
            return;
        }

        mQuietHour.reset();
        if (mPanelSwitcher.getDisplayedChild() == 1) {
            mPanelSwitcher.showPrevious();
        }

        final int originalHeight = mPanelSwitcher.getHeight();
        ValueAnimator animator = ValueAnimator.ofInt(mPanelSwitcher.getHeight(), 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params = mPanelSwitcher.getLayoutParams();
                params.height = (Integer) animation.getAnimatedValue();
                mPanelSwitcher.setLayoutParams(params);
            }
        });

        animator.setDuration(ANIM_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mPanelSwitcher.setVisibility(GONE);
                ViewGroup.LayoutParams params = mPanelSwitcher.getLayoutParams();
                params.height = originalHeight;
                mPanelSwitcher.setLayoutParams(params);
                mFromBtn.setText(getContext().getString(R.string.from));
                mToBtn.setText(getContext().getString(R.string.to));
            }
        });

        animator.start();
        mOptionsShowing = false;
    }

    private void showOptions() {
        if (mOptionsShowing) {
            return;
        }
        ViewTreeObserver observer = mPanelSwitcher.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver observer= mPanelSwitcher.getViewTreeObserver();
                observer.removeOnPreDrawListener(this);
                ValueAnimator animator = ValueAnimator.ofInt(0, mPanelSwitcher.getHeight());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        ViewGroup.LayoutParams params = mPanelSwitcher.getLayoutParams();
                        params.height = (Integer) animation.getAnimatedValue();
                        mPanelSwitcher.setLayoutParams(params);
                    }
                });

                animator.setDuration(ANIM_DURATION);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.start();
                return false;
            }
        });

        mPanelSwitcher.setVisibility(VISIBLE);
        mOptionsShowing = true;


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.quiet_hour_btn:
                mPanelSwitcher.showNext();
                break;

            case R.id.as_is_btn:
                if (mOnPanelButtonClickedListener != null) {
                    mOnPanelButtonClickedListener.onPanelButtonClicked(PanelButtons.BTN_DISABLE);
                }
                SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(getContext());
                preferences.edit().putBoolean(PreferenceKeys.PREF_DISABLE_PEEK, true)
                        .remove(PreferenceKeys.PREF_QUIET_HOUR).apply();

                mFromToText.setVisibility(GONE);
                hideOptions();
                break;

            case R.id.quiet_hour_from_btn:
                if (mOnPanelButtonClickedListener != null) {
                    mOnPanelButtonClickedListener
                            .onPanelButtonClicked(PanelButtons.BTN_QUIET_HOUR_FROM);
                }

                break;

            case R.id.quiet_hour_to_btn:
                if (mOnPanelButtonClickedListener != null) {
                    mOnPanelButtonClickedListener
                            .onPanelButtonClicked(PanelButtons.BTN_QUIET_HOUR_TO);
                }
                break;

            case R.id.from_to_text:
                // Toggle options panel.
                if (mOptionsShowing) {
                    hideOptions();
                } else {
                    showOptions();
                }
                break;
        }

    }

    /**
     * Callback used to associate actions to the button click event.
     */
    public interface OnPanelButtonClickedListener {
        // Called when an action button in EverythingCard is clicked.
        void onPanelButtonClicked(PanelButtons button);
    }

    /**
     * Button action types.
     */
    public enum PanelButtons {
        BTN_QUIET_HOUR_FROM,
        BTN_QUIET_HOUR_TO,
        BTN_DISABLE
    }

}
