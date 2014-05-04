package com.reindeercrafts.notificationpeek.settings;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by zhelu on 5/3/14.
 */
public class Settings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GeneralSettingsFragment()).commit();
    }
}
