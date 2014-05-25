package com.reindeercrafts.notificationpeek.settings.appearance;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.reindeercrafts.notificationpeek.R;

/**
 * Appearance settings main Activity, used to display {@link com.reindeercrafts.notificationpeek.settings.appearance.AppearanceSettingsFragment}
 *
 * Created by zhelu on 5/23/14.
 */
public class AppearanceSettings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (WallpaperFactory.isLiveWallpaperUsed(this)) {
            setTheme(R.style.AppTheme_Wallpaper);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.appearance_settings_layout);

        initActionBar();
        initFragments();

    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.appearance_settings);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initFragments() {
        AppearanceSettingsFragment settingsFragment = new AppearanceSettingsFragment();

        getFragmentManager().beginTransaction()
                .replace(R.id.appearance_settings_frag_container, settingsFragment).commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
