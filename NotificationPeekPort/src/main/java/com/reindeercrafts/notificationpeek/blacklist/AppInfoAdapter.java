package com.reindeercrafts.notificationpeek.blacklist;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.R;

import java.util.List;

/**
 * Array adapter used for populating black list.
 *
 * Created by zhelu on 5/21/14.
 */
public class AppInfoAdapter extends ArrayAdapter<AppInfo> {

    private PackageManager mPackageManager;

    private int mLayoutRes;

    private LayoutInflater mLayoutInflater;

    public AppInfoAdapter(Context context, int resource, List<AppInfo> objects) {
        super(context, resource, objects);
        mPackageManager = context.getPackageManager();
        mLayoutRes = resource;
        mLayoutInflater = LayoutInflater.from(context);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mLayoutRes, parent, false);
        }

        AppInfo appInfo = getItem(position);

        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.icon_image_view);
        TextView appNameTextView = (TextView) convertView.findViewById(R.id.app_name_text);

        try {
            iconImageView.setImageDrawable(mPackageManager.getApplicationIcon(appInfo.packageName));
        } catch (PackageManager.NameNotFoundException e) {
        }

        appNameTextView.setText(appInfo.appName);

        return convertView;
    }
}
