package com.example.btp_10;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int NOTIFICATION_LISTENER_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MainActivity ", "Just set the view");

        // Check and request permissions at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("MainActivity", "Getting Permission.");
            checkPermissions();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkPermissions() {
        // Log checking permissions for debugging
        Log.d("MainActivity", "Checking permissions...");

        // Check if we have all the necessary permissions
        boolean isFineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isBackgroundLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isCallLogPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean isUsagePermissionGranted = isUsagePermissionGranted();
        boolean isMicrophonePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean isNotificationListenerGranted = isNotificationListenerGranted();
        boolean isSetAlarmPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED;

        // Log the results of the checks
        Log.d("MainActivity", "Location Fine Permission: " + isFineLocationGranted);
        Log.d("MainActivity", "Background Location Permission: " + isBackgroundLocationGranted);
        Log.d("MainActivity", "Call Log Permission: " + isCallLogPermissionGranted);
        Log.d("MainActivity", "Usage Stats Permission: " + isUsagePermissionGranted);
        Log.d("MainActivity", "Microphone Permission: " + isMicrophonePermissionGranted);
        Log.d("MainActivity", "Notification Listener Permission: " + isNotificationListenerGranted);
        Log.d("MainActivity", "Set Alarm Permission: " + isSetAlarmPermissionGranted);

        // Check if all required permissions are granted
        if (!isFineLocationGranted || !isBackgroundLocationGranted || !isCallLogPermissionGranted || !isUsagePermissionGranted || !isMicrophonePermissionGranted || !isNotificationListenerGranted || !isSetAlarmPermissionGranted) {
            Log.d("MainActivity", "Permissions were not set, asking for permissions");

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
            Log.d("MainActivity", "Permission Granted");
            startBackgroundServices();
        }
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
        startActivityForResult(intent, NOTIFICATION_LISTENER_REQUEST_CODE);
    }

    private void startBackgroundServices() {
        // Start the services
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, AlarmService.class)); // Start the Alarm service to get alarm info
        startService(new Intent(this, NotificationListener.class)); // Start the Notification Listener service
//        startService(new Intent(this, MicrophoneService.class)); // Start the Microphone service
        Log.d("MainActivity", "Background services started");
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
                Log.d("MainActivity", "Permissions are not set");

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
}
