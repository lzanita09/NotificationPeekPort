package com.reindeercrafts.notificationpeek.blacklist.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.blacklist.AppInfo;
import com.reindeercrafts.notificationpeek.blacklist.AppInfoAdapter;
import com.reindeercrafts.notificationpeek.blacklist.AppList;
import com.reindeercrafts.notificationpeek.blacklist.utils.SwipeDismissListViewTouchListener;

/**
 * Main black list fragment used to display user preference black list.
 * <p/>
 * Created by zhelu on 5/21/14.
 */
public class BlackListFragment extends Fragment {

    private ListView mBlackListView;
    private AppInfoAdapter mAppInfoAdapter;

    private AppList mAppList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mAppList = AppList.getInstance(getActivity());

        View rootView = inflater.inflate(R.layout.blacklist_list_view, container, false);
        mBlackListView = (ListView) rootView.findViewById(R.id.list_view);

        updateBlackList();

        // Setup swipe to dismiss listener.
        SwipeDismissListViewTouchListener listViewTouchListener =
                new SwipeDismissListViewTouchListener(mBlackListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                AppInfoAdapter adapter = (AppInfoAdapter) listView.getAdapter();
                                for (int position : reverseSortedPositions) {
                                    AppInfo appInfoToRemove = adapter.getItem(position);
                                    adapter.remove(appInfoToRemove);
                                    mAppList.removeFromBlackList(appInfoToRemove);
                                }
                            }
                        }
                );

        mBlackListView.setOnTouchListener(listViewTouchListener);
        mBlackListView.setOnScrollListener(listViewTouchListener.makeScrollListener());

        return rootView;
    }

    public void storeBlackList() {
        mAppList.storeBlackList();
    }

    /**
     * Reload black list and display.
     */
    public void updateBlackList() {
        mAppInfoAdapter =
                new AppInfoAdapter((android.support.v4.app.FragmentActivity) getActivity(),
                        mAppList, R.layout.black_list_item_laout, mAppList.getCurrentBlackList());
        mBlackListView.setAdapter(mAppInfoAdapter);
    }
}
