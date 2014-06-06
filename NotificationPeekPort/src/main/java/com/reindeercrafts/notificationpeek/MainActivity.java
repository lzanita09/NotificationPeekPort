package com.reindeercrafts.notificationpeek;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.reindeercrafts.notificationpeek.blacklist.BlackListActivity;
import com.reindeercrafts.notificationpeek.dialogs.DialogHelper;
import com.reindeercrafts.notificationpeek.peek.NotificationHelper;
import com.reindeercrafts.notificationpeek.settings.Settings;
import com.reindeercrafts.notificationpeek.utils.AccessChecker;
import com.reindeercrafts.notificationpeek.utils.SensorHelper;


public class MainActivity extends Activity implements View.OnClickListener {

    public static final String ACTION_NOTIFICATION_LISTENER_SETTINGS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";


    private static final long LOCK_SCREEN_DELAY = 1000;
    private static final long SEND_NOTIFICATION_DELAY = 5000;
    private static final int TEST_ID = 10592;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        // Device Admin access button.
        View deviceAccessBtn = findViewById(R.id.device_access);
        deviceAccessBtn.setOnClickListener(this);

        // Notification access button.
        View notificationAccessBtn = findViewById(R.id.notification_access);
        notificationAccessBtn.setOnClickListener(this);

        // Status text and icons
        TextView instructionText = (TextView) findViewById(R.id.instruction_text);
        ImageView notificationAccessIcon =
                (ImageView) findViewById(R.id.notification_access_checkbox);
        ImageView deviceAdminAcessIcon = (ImageView) findViewById(R.id.device_access_checkbox);

        boolean isAccessEnabled = true;
        if (AccessChecker.isNotificationAccessEnabled(this)) {
            notificationAccessIcon.setImageResource(R.drawable.ic_checkmark);
        } else {
            notificationAccessIcon.setImageResource(R.drawable.ic_cancel);
            isAccessEnabled = false;
        }

        if (AccessChecker.isDeviceAdminEnabled(this)) {
            deviceAdminAcessIcon.setImageResource(R.drawable.ic_checkmark);
        } else {
            deviceAdminAcessIcon.setImageResource(R.drawable.ic_cancel);
            isAccessEnabled = false;
        }

        if (!isAccessEnabled || NotificationHelper.isPeekDisabled(this)) {
            SpannableString spannableString =
                    new SpannableString(getString(R.string.instruction_start));
            spannableString.setSpan(
                    new ForegroundColorSpan(getResources().getColor(android.R.color.holo_red_dark)),
                    0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            instructionText.setText(spannableString);

        } else {
            SpannableString spannableString =
                    new SpannableString(getString(R.string.instruction_ok));
            spannableString.setSpan(new ForegroundColorSpan(
                            getResources().getColor(android.R.color.holo_green_dark)), 0,
                    spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            instructionText.setText(spannableString);
        }

        // Sensor status.
        ImageView proxLightSensorImg = (ImageView) findViewById(R.id.prox_light_check);
        ImageView gyroSensorImg = (ImageView) findViewById(R.id.gyro_check);

        if (SensorHelper.checkSensorStatus(this, SensorHelper.SENSOR_PROXIMITY_LIGHT, true)) {
            proxLightSensorImg.setImageResource(R.drawable.ic_checkmark);
        } else {
            proxLightSensorImg.setImageResource(R.drawable.ic_cancel);
        }

        if (SensorHelper.checkSensorStatus(this, SensorHelper.SENSOR_GYRO, true)) {
            gyroSensorImg.setImageResource(R.drawable.ic_checkmark);
        } else {
            gyroSensorImg.setImageResource(R.drawable.ic_cancel);
        }

        View proxLightSensorRoot = findViewById(R.id.prox_light_sensor);
        proxLightSensorRoot.setOnClickListener(this);

        View gyroSensorRoot = findViewById(R.id.gyro_sensor);
        gyroSensorRoot.setOnClickListener(this);

        // Send test notification button.
        Button sendNotificationButton = (Button) findViewById(R.id.send_notification_btn);
        sendNotificationButton.setOnClickListener(this);

    }


    /**
     * Lock device screen and send test notification.
     */
    private void sendTestNotification() {
        final DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        Handler handler = new Handler(getMainLooper());

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getString(R.string.diagnosis_notification_title))
                .setContentTitle(getString(R.string.diagnosis_notification_title_content))
                .setContentText(getString(R.string.diagnosis_notification_content))
                .setLights(Color.GREEN, 1000, 5000)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                devicePolicyManager.lockNow();
            }
        }, LOCK_SCREEN_DELAY);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManager manager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.notify(TEST_ID, builder.build());
                finish();
            }
        }, SEND_NOTIFICATION_DELAY);
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
        } else if (id == R.id.action_about) {
            DialogHelper.showAboutDialog(this);
        } else if (id == R.id.action_help) {
            DialogHelper.showHelpDialog(this);
        } else if (id == R.id.action_blacklist) {
            Intent intent = new Intent(this, BlackListActivity.class);
            startActivity(intent);
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
            case R.id.device_access:
                intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        new ComponentName(this, LockscreenDeviceAdminReceiver.class));
                startActivity(intent);
                break;

            case R.id.notification_access:
                intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);

                break;

            case R.id.send_notification_btn:
                if (AccessChecker.isDeviceAdminEnabled(this)) {
                    sendTestNotification();
                } else {
                    Toast.makeText(this, R.string.device_admin_missing, Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.prox_light_sensor:
            case R.id.gyro_sensor:
                // Clicking either sensor goes to the Settings page.
                intent = new Intent(this, Settings.class);
                startActivity(intent);
                break;
        }
    }
}
