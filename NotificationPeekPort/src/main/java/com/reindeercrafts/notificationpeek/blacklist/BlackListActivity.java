package com.reindeercrafts.notificationpeek.blacklist;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.reindeercrafts.notificationpeek.R;
import com.reindeercrafts.notificationpeek.blacklist.fragments.BlackListFragment;
import com.reindeercrafts.notificationpeek.blacklist.fragments.BlackListSuggestionFragment;
import com.reindeercrafts.notificationpeek.settings.PreferenceKeys;

/**
 * Black list main activity with an EditText to search by app name and a list to display
 * current black list.
 *
 * Created by zhelu on 5/21/14.
 */
public class BlackListActivity extends FragmentActivity
        implements TextWatcher, BlackListSuggestionFragment.SuggestionCallback,
        View.OnFocusChangeListener, CompoundButton.OnCheckedChangeListener {

    private EditText mSearchEditText;
    private CheckBox mSystemAppCheckBox;

    private BlackListFragment mBlackListFragment;

    // Search suggestion list fragment.
    private BlackListSuggestionFragment mSuggestionFragment;

    private boolean mSuggestionShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.black_list_layout);

        mSearchEditText = (EditText) findViewById(R.id.search_edit_text);
        mSearchEditText.setOnFocusChangeListener(this);

        mSystemAppCheckBox = (CheckBox) findViewById(R.id.show_system_app_check);
        mSystemAppCheckBox.setChecked(AppList.shouldExcludeSystemApps(this));
        mSystemAppCheckBox.setOnCheckedChangeListener(this);

        initActionBar();
        initFragments();

    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.black_list);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Initialize both fragments, but only add black list fragment.
     * The suggestion list fragment will be added on demand.
     */
    private void initFragments() {
        mBlackListFragment = new BlackListFragment();
        mSuggestionFragment = new BlackListSuggestionFragment();
        mSuggestionFragment.setSuggestionCallback(this);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mBlackListFragment)
                .commit();
    }

    /**
     * Replace black list fragment with suggestion list fragment, get ready to show suggestions.
     */
    private void showSuggestionFragment() {
        if (mSuggestionFragment == null || mSuggestionShowing) {
            return;
        }

        mSuggestionShowing = true;

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fragment_in, R.animator.fragment_out,
                R.animator.fragment_in, R.animator.fragment_out);

        ft.replace(R.id.fragment_container, mSuggestionFragment)
                .addToBackStack(null).commit();
    }

    /**
     * Remove suggestion fragment and show black list again. This method is also used to handle
     * EditText behavior, i.e remove its focus, clear listener and clear text.
     *
     * @return  False if the suggestion fragment is hidden already. True otherwise.
     */
    private boolean hideSuggestionFragment() {
        if (!mSuggestionShowing) {
            return false;
        }
        mSuggestionShowing = false;
        getFragmentManager().popBackStack();

        mSearchEditText.clearFocus();
        mSearchEditText.removeTextChangedListener(this);
        mSearchEditText.setText("");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Intercept back button press action when user presses back button to go back to
        // the black list.
        if (!hideSuggestionFragment()) {
            // Time to leave the activity, store the up-to-date black list.
            mBlackListFragment.storeBlackList();
            super.onBackPressed();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mSuggestionFragment.suggest(s);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }


    @Override
    public void onSuggestionSelected() {
        mBlackListFragment.updateBlackList();
        hideSuggestionFragment();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            showSuggestionFragment();
            mSystemAppCheckBox.setVisibility(View.GONE);
            mSearchEditText.addTextChangedListener(this);
        } else {
            mSystemAppCheckBox.setVisibility(View.VISIBLE);
            hideSuggestionFragment();
            mSearchEditText.removeTextChangedListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(PreferenceKeys.PREF_SYSTEM_APP, isChecked).apply();
    }
}
