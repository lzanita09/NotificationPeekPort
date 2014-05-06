package com.reindeercrafts.notificationpeek.diagnosis;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.MainActivity;
import com.reindeercrafts.notificationpeek.R;

/**
 * Diagnosis activity for checking sensors and sending test notification.
 * <p/>
 * Created by zhelu on 5/3/14.
 */
public class PeekDiagnosisActivity extends Activity implements View.OnClickListener {

    private static final String RESULT_OK = "OK";
    private static final String RESULT_FAILED = "Missing";
    private static final long LOCK_SCREEN_DELAY = 1000;
    private static final long SEND_NOTIFICATION_DELAY = 5000;
    private static final int TEST_ID = 10592;

    private DevicePolicyManager mDevicePolicyManager;
    private Handler mHandler;

    private LinearLayout mDiagnosisLinear;
    private Button mSendNotificationBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDevicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mHandler = new Handler(getMainLooper());

        setContentView(R.layout.diagnosis_layout);

        mDiagnosisLinear = (LinearLayout) findViewById(R.id.states_container);
        mSendNotificationBtn = (Button) findViewById(R.id.send_notification_btn);
        mSendNotificationBtn.setOnClickListener(this);

        diagnoseSensors();

    }

    private void diagnoseSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Check if there is proximity sensor.
        Sensor proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mDiagnosisLinear.addView(generateResultItem("Proximity Sensor", proxSensor != null));

        // Check if there is gyroscope sensor.
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mDiagnosisLinear.addView(generateResultItem("Gyroscope Sensor", gyroSensor != null));

    }

    private void diagnoseNotification() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getString(R.string.diagnosis_notification_title))
                .setContentTitle(getString(R.string.diagnosis_notification_title))
                .setContentText(getString(R.string.diagnosis_notification_content))
                .setContentIntent(pendingIntent);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDevicePolicyManager.lockNow();
            }
        }, LOCK_SCREEN_DELAY);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManager manager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.notify(TEST_ID, builder.build());
                finish();
            }
        }, SEND_NOTIFICATION_DELAY);
    }

    private View generateResultItem(String title, boolean success) {
        LayoutInflater inflater = LayoutInflater.from(this);

        View root = inflater.inflate(R.layout.diagnosis_item_layout, null);
        TextView titleText = (TextView) root.findViewById(R.id.diagnosis_title_text);
        TextView resultText = (TextView) root.findViewById(R.id.diagnosis_result_text);

        titleText.setText(title);

        SpannableString spannedResult;
        if (success) {
            spannedResult = new SpannableString(RESULT_OK);
            spannedResult.setSpan(new ForegroundColorSpan(Color.GREEN), 0, RESULT_OK.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannedResult = new SpannableString(RESULT_FAILED);
            spannedResult.setSpan(new ForegroundColorSpan(Color.RED), 0, RESULT_FAILED.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        resultText.setText(spannedResult);

        return root;
    }

    @Override
    public void onClick(View v) {
        diagnoseNotification();
    }
}
