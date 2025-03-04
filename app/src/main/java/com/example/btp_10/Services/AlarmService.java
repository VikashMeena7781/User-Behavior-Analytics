package com.example.btp_10.Services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmService extends Service {

    public AlarmService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if AlarmClockInfo is available (Android 5.1 and above)
        Log.d("AlarmService", "Getting Alarms");
        getNextAlarm();

        return START_STICKY; // Keep the service running
    }

    private void getNextAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();


        if (alarmClockInfo != null) {
            long alarmTime = alarmClockInfo.getTriggerTime();
            String alarmPackage = alarmClockInfo.getShowIntent().getCreatorPackage();
            // Create a Date object from the timestamp
            Date date = new Date(alarmTime);

            // Format the date into a readable string
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = sdf.format(date);

            // Print the formatted date
            Log.d("AlarmService", "Next alarm set for: " + formattedDate + " by package: " + alarmPackage);
        } else {
            Log.d("AlarmService", "No next alarm is scheduled.");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This is a started service, not a bound service, so return null
        return null;
    }
}
