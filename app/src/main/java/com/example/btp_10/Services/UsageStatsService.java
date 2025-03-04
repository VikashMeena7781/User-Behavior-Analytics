package com.example.btp_10.Services;

import android.app.IntentService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UsageStatsService extends IntentService {

    private static final String TAG = "Logs";
    private static final String LAST_RUN_DATE_KEY = "last_run_date";

    public UsageStatsService() {
        super("UsageStatsService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "Checking if usage stats should be collected...");

        // Check if the service has already run today
        if (hasServiceRunToday()) {
            Log.d(TAG, "Usage stats collection already done for today. Skipping...");
            return;
        }

        // Collect usage stats and save today's date
        Log.d(TAG, "Collecting App Usage Stats...");
        collectUsageStats();
        saveLastRunDate();
    }

    private void collectUsageStats() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // Get the start of today (midnight)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 22);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDayMillis = calendar.getTimeInMillis();

        long currentTime = System.currentTimeMillis();

        // Query usage stats for today
        List<UsageStats> statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDayMillis, currentTime);

        if (statsList != null && !statsList.isEmpty()) {
            for (UsageStats usageStats : statsList) {
                if (usageStats.getTotalTimeInForeground() > 0) { // Ignore unused apps
                    Log.d(TAG, "App: " + usageStats.getPackageName() +
                            ", Last Used: " + new Date(usageStats.getLastTimeUsed()) +
                            ", Time in Foreground: " + (usageStats.getTotalTimeInForeground() / 1000) + " sec");
                }
            }
        } else {
            Log.d(TAG, "No usage stats available for today.");
        }
    }

    private boolean hasServiceRunToday() {
        SharedPreferences prefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE);
        String lastRunDate = prefs.getString(LAST_RUN_DATE_KEY, "");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return todayDate.equals(lastRunDate);
    }

    private void saveLastRunDate() {
        SharedPreferences prefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString(LAST_RUN_DATE_KEY, todayDate).apply();
    }
}
