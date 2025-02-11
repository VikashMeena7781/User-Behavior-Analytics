package com.example.btp_10;

import android.app.IntentService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class UsageStatsService extends IntentService {

    public UsageStatsService() {
        super("UsageStatsService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // Define the time range for stats (last hour in this case)
        long currentTime = System.currentTimeMillis();
        long oneHourAgo = currentTime - 1000 * 60 * 60;  // 1 hour ago

        // Query usage stats
        List<UsageStats> statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, oneHourAgo, currentTime);

        if (statsList != null && !statsList.isEmpty()) {
            // Iterate through the list of UsageStats
            for (UsageStats usageStats : statsList) {
                // Log the package name, last used time, and total time in the foreground
                Log.d("UsageStatsService", "Package Name: " + usageStats.getPackageName() +
                        ", Last Used: " + new Date(usageStats.getLastTimeUsed()) +
                        ", Total Time In Foreground: " + usageStats.getTotalTimeInForeground());
            }
        } else {
            Log.d("UsageStatsService", "No usage stats available.");
        }
    }
}
