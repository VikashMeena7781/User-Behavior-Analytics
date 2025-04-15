package com.example.btp1.Services;

import android.app.IntentService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.btp1.DataRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class UsageStatsService extends IntentService {

    private static final String TAG = "Logs";

    public UsageStatsService() {
        super("UsageStatsService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "Collecting App Usage Stats for the last 2 days...");
        collectUsageStats();
    }

    private void collectUsageStats() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // Calculate the start time for 2 days ago (from midnight 2 days ago)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -2); // Go back 2 days
        calendar.set(Calendar.HOUR_OF_DAY, 0);    // Set to midnight
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTimeMillis = calendar.getTimeInMillis();

        long currentTime = System.currentTimeMillis();

        // Query usage stats for the period from two days ago until now
        List<UsageStats> statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTimeMillis, currentTime);

        if (statsList != null && !statsList.isEmpty()) {
            StringBuilder entry = new StringBuilder();
            for (UsageStats usageStats : statsList) {
                // Log only apps that have been used (foreground time greater than zero)
                if (usageStats.getTotalTimeInForeground() > 0) {
                    entry.append("App: ").append(usageStats.getPackageName()).append(", Last Used: ").append(new Date(usageStats.getLastTimeUsed())).append(", Total Time Used: ").append(usageStats.getTotalTimeInForeground() / 1000).append(" sec\n");
                }
            }
            Log.d(TAG, entry.toString());
            DataRepository.getInstance().addUsageStats(entry.toString());
        } else {
            Log.d(TAG, "No usage stats available for the last 2 days.");
        }
    }
}