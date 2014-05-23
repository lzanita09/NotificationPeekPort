package com.reindeercrafts.notificationpeek.blacklist.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.blacklist.AppInfo;
import com.reindeercrafts.notificationpeek.blacklist.AppList;

import java.util.List;

/**
 * Filter class for selecting corresponding {@link com.reindeercrafts.notificationpeek.blacklist.AppInfo}
 * objects to display based on user input.
 *
 * Created by zhelu on 5/21/14.
 */
public class AppInfoFilter extends Filter {

    private Context mContext;
    private AppList mAppList;

    // ListView object for reloading list.
    private ListView mAppListView;

    public AppInfoFilter(Context context, AppList mAppList, ListView listView) {
        this.mContext = context;
        this.mAppList = mAppList;
        mAppListView = listView;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        if (constraint == null || constraint.length() == 0) {
            // No input, no output.
            List appList = mAppList.getAppList(null);
            results.values = appList;
            results.count = appList.size();
        } else {
            // Output suggestion list.
            List appList = mAppList.getAppList(constraint);
            results.values = appList;
            results.count = appList.size();
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        // New suggestion has arrived, update the list.
        ArrayAdapter<AppInfo> adapter =
                new ArrayAdapter<AppInfo>(mContext,
                                          R.layout.blacklist_suggest_item,
                                          R.id.text, (List<AppInfo>) results.values);
        mAppListView.setAdapter(adapter);
    }
}
