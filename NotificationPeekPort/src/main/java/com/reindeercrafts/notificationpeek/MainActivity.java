package com.reindeercrafts.notificationpeek;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.diagnosis.PeekDiagnosisActivity;
import com.reindeercrafts.notificationpeek.settings.Settings;
import com.reindeercrafts.notificationpeek.utils.AccessChecker;


public class MainActivity extends Activity implements View.OnClickListener {

    public static final String ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        // Device Admin access button.
        Button deviceAccessBtn = (Button) findViewById(R.id.device_access_btn);
        deviceAccessBtn.setEnabled(!AccessChecker.isDeviceAdminEnabled(this));
        deviceAccessBtn.setOnClickListener(this);

        // Notification access button.
        Button notificationAccessBtn = (Button) findViewById(R.id.notification_access_btn);
        notificationAccessBtn.setEnabled(!AccessChecker.isNotificationAccessEnabled(this));
        notificationAccessBtn.setOnClickListener(this);

        // Instructions text.
        TextView instructionText = (TextView) findViewById(R.id.instruction_text);
        if (deviceAccessBtn.isEnabled() || notificationAccessBtn.isEnabled()) {
            instructionText.setText(R.string.instruction_start);
        } else {
            instructionText.setText(R.string.instruction_ok);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
        } else if (id == R.id.action_diagnosis) {
            Intent intent = new Intent(this, PeekDiagnosisActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViews();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.device_access_btn:
                intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        new ComponentName(this, LockscreenDeviceAdminReceiver.class));
                startActivity(intent);
                break;

            case R.id.notification_access_btn:
                intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);

                break;
        }
    }
}
