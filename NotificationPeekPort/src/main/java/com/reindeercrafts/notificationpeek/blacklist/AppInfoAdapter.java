package com.reindeercrafts.notificationpeek.blacklist;

import android.content.pm.PackageManager;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.doomonafireball.betterpickers.timepicker.TimePickerBuilder;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment;
import com.reindeercrafts.notificationpeek.R;

import java.util.List;

/**
 * Array adapter used for populating black list.
 * <p/>
 * Created by zhelu on 5/21/14.
 */
public class AppInfoAdapter extends ArrayAdapter<AppInfo> {

    private static final int TYPE_EVERYTHING = 1;
    private static final int TYPE_APP = 2;

    private PackageManager mPackageManager;
    private AppList mAppList;
    private int mLayoutRes;

    private FragmentActivity mActivity;

    private LayoutInflater mLayoutInflater;

    private EverythingCard mEverythingCard;

    public AppInfoAdapter(FragmentActivity activity, AppList appList, int resource,
                          List<AppInfo> objects) {
        super(activity, resource, objects);
        mActivity = activity;
        mAppList = appList;
        mPackageManager = activity.getPackageManager();
        mLayoutRes = resource;
        mLayoutInflater = LayoutInflater.from(activity);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == TYPE_EVERYTHING) {
            convertView =
                    mLayoutInflater.inflate(R.layout.everything_card_item_layout, parent, false);
            bindEverythingView(convertView);
        } else {
            convertView = mLayoutInflater.inflate(mLayoutRes, parent, false);
            bindAppView(position, convertView);
        }

        return convertView;
    }

    /**
     * Setup EverythingCard layout with click listeners.
     *
     * @param convertView   Root View.
     */
    private void bindEverythingView(View convertView) {
        mEverythingCard = (EverythingCard) convertView.findViewById(R.id.everything_card);
        mEverythingCard.setOnPanelButtonClickedListener(new EverythingCard.OnPanelButtonClickedListener() {
            @Override
            public void onPanelButtonClicked(EverythingCard.PanelButtons button) {
                if (button.equals(EverythingCard.PanelButtons.BTN_DISABLE)) {
                    mAppList.removeOthersFromBlackList();
                } else if (button.equals(EverythingCard.PanelButtons.BTN_QUIET_HOUR_FROM)) {
                    TimePickerBuilder builder = new TimePickerBuilder()
                            .setFragmentManager(mActivity.getSupportFragmentManager())
                            .setStyleResId(R.style.BetterPickersDialogFragment)
                            .addTimePickerDialogHandler(mFromTimePickerHandler);
                    builder.show();
                } else {
                    TimePickerBuilder builder = new TimePickerBuilder()
                            .setFragmentManager(mActivity.getSupportFragmentManager())
                            .setStyleResId(R.style.BetterPickersDialogFragment)
                            .addTimePickerDialogHandler(mToTimePickerHandler);
                    builder.show();
                }
            }
        });
    }

    /**
     * Bind normal black list item layout.
     *
     * @param position      Item position.
     * @param convertView   Root View.
     */
    private void bindAppView(int position, View convertView) {

        AppInfo appInfo = getItem(position);

        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.icon_image_view);
        TextView appNameTextView = (TextView) convertView.findViewById(R.id.app_name_text);

        try {
            iconImageView.setImageDrawable(mPackageManager.getApplicationIcon(appInfo.getPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
        }

        appNameTextView.setText(appInfo.getAppName());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && getItem(position).getPackageName().equals(AppInfo.EVERYTHING_PKG)) {
            return TYPE_EVERYTHING;
        }
        return TYPE_APP;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }


    private TimePickerDialogFragment.TimePickerDialogHandler mFromTimePickerHandler =
            new TimePickerDialogFragment.TimePickerDialogHandler() {
                @Override
                public void onDialogTimeSet(int reference, int hr, int min) {
                    if (mEverythingCard != null) {
                        mEverythingCard.setFromTime(hr, min);
                    }
                }
            };

    private TimePickerDialogFragment.TimePickerDialogHandler mToTimePickerHandler =
            new TimePickerDialogFragment.TimePickerDialogHandler() {
                @Override
                public void onDialogTimeSet(int reference, int hr, int min) {
                    if (mEverythingCard != null) {
                        mEverythingCard.setToTime(hr, min);
                    }
                }
            };
}
