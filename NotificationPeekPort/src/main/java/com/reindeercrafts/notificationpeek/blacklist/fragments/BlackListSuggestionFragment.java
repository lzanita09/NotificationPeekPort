package com.reindeercrafts.notificationpeek.blacklist.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.blacklist.AppInfo;
import com.reindeercrafts.notificationpeek.blacklist.AppList;
import com.reindeercrafts.notificationpeek.blacklist.utils.AppInfoFilter;

/**
 * Suggestion list fragment, used to display suggestions based on the input.
 *
 * Created by zhelu on 5/21/14.
 */
public class BlackListSuggestionFragment extends Fragment
        implements AdapterView.OnItemClickListener {

    private AppList mAppList;

    // List items filter for displaying proper results based on input.
    private AppInfoFilter mAppInfoFilter;

    private SuggestionCallback mCallback;

    public void setSuggestionCallback(SuggestionCallback mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ListView suggestionList =
                (ListView) inflater.inflate(R.layout.suggestion_listview, container, false);

        mAppList = AppList.getInstance(getActivity());
        mAppInfoFilter = new AppInfoFilter(getActivity(), mAppList, suggestionList);
        suggestionList.setOnItemClickListener(this);

        return suggestionList;
    }

    /**
     * Perform filtering with current input.
     *
     * @param newText   Input text in EditText.
     */
    public void suggest(CharSequence newText) {
        mAppInfoFilter.filter(newText);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // User has clicked one item, add it to current black list.
        AppInfo appInfo = (AppInfo) parent.getAdapter().getItem(position);
        if(mAppList.addToBlackList(appInfo)) {
            if (mCallback != null) {
                mCallback.onSuggestionSelected();
            }
        } else {
            Toast.makeText(getActivity(), R.string.peek_disabled, Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Callback used by {@link com.reindeercrafts.notificationpeek.blacklist.BlackListActivity}
     * to display the updated black list after user's selection.
     */
    public interface SuggestionCallback {
        public void onSuggestionSelected();
    }


}
