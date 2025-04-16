package com.example.btp1;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.btp1.Services.CallLogService;
import com.example.btp1.Services.LocationService;
import com.example.btp1.Services.MicrophoneService;
import com.example.btp1.Services.NotificationListener;
import com.example.btp1.Services.ScreenStatusReceiver;
import com.example.btp1.Services.SensorService;
import com.example.btp1.Services.UsageStatsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int NOTIFICATION_LISTENER_REQUEST_CODE = 101;
    private ScreenStatusReceiver screenStatusReceiver;
    private final String TAG = "Logs";
    private TextView infoTextView, tvWelcomeUser;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "Just set the view");
        // Check and request permissions at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "Getting Permission.");
            checkPermissions();
            // set broadcast
            callScreenStatus();
            infoTextView = findViewById(R.id.infoTextView);
            tvWelcomeUser = findViewById(R.id.tvWelcomeUser);

            // Set welcome message with user's name
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                String displayName = user.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    tvWelcomeUser.setText("Welcome, " + displayName);
                } else {
                    tvWelcomeUser.setText("Welcome");
                }
                tvWelcomeUser.setVisibility(View.VISIBLE);
            } else {
                tvWelcomeUser.setVisibility(View.GONE);
            }

            Button btnCallLogs = findViewById(R.id.btnCallLogs);
            btnCallLogs.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getCallLogs(), "Call Logs")));

            Button btnLocation = findViewById(R.id.btnLocation);
            btnLocation.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getLocations(), "Location")));

            Button btnMicrophone = findViewById(R.id.btnMicrophone);
            btnMicrophone.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getMicrophoneData(), "Microphone")));

            Button btnNotifications = findViewById(R.id.btnNotifications);
            btnNotifications.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getNotifications(), "Notifications")));

            Button btnScreenStatus = findViewById(R.id.btnScreenStatus);
            btnScreenStatus.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getScreenStatus(), "Screen Status")));

            Button btnSensors = findViewById(R.id.btnSensors);
            btnSensors.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getSensorData(), "Sensor Data")));

            Button btnUsageStats = findViewById(R.id.btnUsageStats);
            btnUsageStats.setOnClickListener(v ->
                    infoTextView.setText(getData(DataRepository.getInstance().getUsageStats(), "Usage Stats")));

            // Privacy Policy button logic
            Button btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);
            btnPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());

            Button fillsurvery = findViewById(R.id.btnfillsurvery);
            fillsurvery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, SurveyActivity.class));
                }
            });

            // Logout button
            Button btnLogout = findViewById(R.id.btnLogout);
            btnLogout.setOnClickListener(v -> logoutUser());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Redirect to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getData(List<String> dataList, String header) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append(":\n");
        for (String entry : dataList) {
            sb.append(entry).append("\n");
        }
        return sb.toString();
    }

    // Display the privacy policy in an AlertDialog with a scrollable view.
    private void showPrivacyPolicy() {
        String privacyPolicyText = "Privacy Policy\n" +
                "Effective Date: April 2, 2025\n\n" +
                "1. Introduction\n" +
                "We are committed to protecting your privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our mobile application.\n\n" +
                "2. Information We Collect\n" +
                "We collect the following types of data to conduct our research on the correlation between device activity and mental health:\n" +
                "- Microphone and Light Sensors Data: Data from the device's microphone(Only detecting microphone activity not the actual data) and light sensors, collected for research purposes.\n" +
                "- Screen Usage: Information about screen time and interactions with the device.\n" +
                "- Location Details: Approximate or precise location data, depending on user permissions.\n" +
                "- Call Logs: Metadata about incoming and outgoing calls, excluding call content.\n" +
                "- App and Browser Usage: Time spent on different applications and websites.\n" +
                "- Notifications Details: Metadata about received notifications, excluding content.\n" +
                "- Alarm Details: Information about alarms set and used on the device.\n" +
                "- Device Metadata: Information such as device model, operating system version, and anonymized device identifiers.\n" +
                "- Voluntary Information: Any data you choose to provide related to your mental health and well-being.\n" +
                "- No Personal Identifiable Information (PII): We do not collect names, email addresses, or any other personally identifiable details.\n\n" +
                "3. How We Use Your Information\n" +
                "We use the collected data solely for research purposes, including:\n" +
                "- Understanding patterns in device usage and mental health.\n" +
                "- Improving research methodologies.\n" +
                "- Publishing anonymized, aggregated findings in academic research.\n\n" +
                "4. Data Protection & Security\n" +
                "We take data security seriously and implement the following measures:\n" +
                "- Encryption of data both in transit and at rest.\n" +
                "- Strict access controls to ensure only authorized researchers can access anonymized data.\n" +
                "- No sharing or selling of data to third parties.\n\n" +
                "5. Data Sharing & Disclosure\n" +
                "Your data is never sold or shared with third parties, except in the following cases:\n" +
                "- Research Publications: Aggregated, anonymized insights may be shared in research papers.\n" +
                "- Legal Compliance: If required by law, we may disclose data to authorities.\n\n" +
                "6. Your Rights & Choices\n" +
                "Opt-Out: You can uninstall the App at any time to stop data collection.\n" +
                "Data Deletion: You may request the deletion of your data by contacting us at cs1210115@iitd.ac.in\n\n" +
                "7. Changes to This Privacy Policy\n" +
                "We may update this Privacy Policy from time to time. Any changes will be posted in the App, and continued use of the App constitutes acceptance of these changes.\n\n" +
                "8. Contact Us\n" +
                "If you have any questions about this Privacy Policy, please contact us at: cs1210115@iitd.ac.in";

        TextView policyTextView = new TextView(this);
        policyTextView.setText(privacyPolicyText);
        policyTextView.setPadding(32, 32, 32, 32);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(policyTextView);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setView(scrollView)
                .setPositiveButton("OK", null);

        builder.show();
    }


    private void callScreenStatus() {
        screenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenStatusReceiver, filter);
        Log.d(TAG, "ScreenStatusReceiver registered!");
    }



    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkPermissions() {
        // Log checking permissions for debugging
        Log.d(TAG, "Checking permissions...");

        // Check if we have all the necessary permissions
        boolean isFineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isBackgroundLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isCallLogPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean isUsagePermissionGranted = isUsagePermissionGranted();
        boolean isMicrophonePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean isNotificationListenerGranted = isNotificationListenerGranted();
        boolean isSetAlarmPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED;

        // Log the results of the checks
        Log.d(TAG, "Location Fine Permission: " + isFineLocationGranted);
        Log.d(TAG, "Background Location Permission: " + isBackgroundLocationGranted);
        Log.d(TAG, "Call Log Permission: " + isCallLogPermissionGranted);
        Log.d(TAG, "Usage Stats Permission: " + isUsagePermissionGranted);
        Log.d(TAG, "Microphone Permission: " + isMicrophonePermissionGranted);
        Log.d(TAG, "Notification Listener Permission: " + isNotificationListenerGranted);
        Log.d(TAG, "Set Alarm Permission: " + isSetAlarmPermissionGranted);

        // Check if all required permissions are granted
        if (!isFineLocationGranted || !isBackgroundLocationGranted || !isCallLogPermissionGranted || !isUsagePermissionGranted || !isMicrophonePermissionGranted || !isNotificationListenerGranted || !isSetAlarmPermissionGranted) {
            Log.d(TAG, "Permissions were not set, asking for permissions");

            // Request the necessary permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.PACKAGE_USAGE_STATS,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.SET_ALARM
                    },
                    PERMISSIONS_REQUEST_CODE);

            if (!isUsagePermissionGranted) {
                requestUsagePermission();
            }
            if (!isNotificationListenerGranted) {
                requestNotificationListenerPermission();
            }
        } else {
            // If permissions are granted, start the background services
            Log.d(TAG, "Permission Granted");
            startBackgroundServices();
            restartNotificationService();

        }
    }

    private void restartNotificationService() {
        ComponentName componentName = new ComponentName(this, NotificationListener.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }


    private boolean isUsagePermissionGranted() {
        // Check if usage stats permission is granted
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isNotificationListenerGranted() {
        // Check if NotificationListener permission is granted
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    private void requestUsagePermission() {
        if (!isUsagePermissionGranted()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private void requestNotificationListenerPermission() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivity(intent);
    }

    private void startBackgroundServices() {
        // Start the services
        // This service will automatically called when notification posted !, No need to call it from here .

//        startService(new Intent(this, NotificationListener.class)); // Start the Notification Listener service
        // Working Good !
        startService(new Intent(this, MicrophoneService.class)); // Start the Microphone service
//        scheduleDailyCallLogService();
        // Location is Good to Go !
        startService(new Intent(this, LocationService.class));
//        scheduleDailyUsageStatsService();
        startService(new Intent(this, UsageStatsService.class));
        startService(new Intent(this,CallLogService.class));
        // Working Fine
        startService(new Intent(this, SensorService.class));

//        startService(new Intent(this,ScreenStatusReceiver.class));
        Log.d(TAG, "Background services started");
    }

    private void scheduleDailyCallLogService() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, CallLogService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set execution time (e.g., every day at 12:00 AM)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 28);
        calendar.set(Calendar.SECOND, 0);

        // Schedule the service
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

        Log.d(TAG, "CallLogService scheduled to run daily at midnight.");
    }

    private void scheduleDailyUsageStatsService() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, UsageStatsService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set execution time (e.g., every day at 1 AM)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 22);
        calendar.set(Calendar.SECOND, 0);

        // Schedule the service
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

        Log.d(TAG, "UsageStatsService scheduled to run daily at 1 AM.");
    }

    // If permissions are permanently denied (with 'Don't ask again'), ask user to go to settings
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                startBackgroundServices();
            } else {
                Log.d(TAG, "Permissions are not set");

                // If permissions are permanently denied, show a message and open app settings
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        // Check if the permission is permanently denied
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                            openAppSettings();
                            break; // Open settings once
                        }
                    }
                }
                Toast.makeText(this, "Permissions are required to run this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    // Handle result from Notification Listener settings (onActivityResult for notification listener permission)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NOTIFICATION_LISTENER_REQUEST_CODE) {
            if (isNotificationListenerGranted()) {
                startBackgroundServices();
            } else {
                Toast.makeText(this, "Notification Listener Permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenStatusReceiver != null) {
            unregisterReceiver(screenStatusReceiver);
            Log.d(TAG, "ScreenStatusReceiver unregistered!");
        }
    }
}