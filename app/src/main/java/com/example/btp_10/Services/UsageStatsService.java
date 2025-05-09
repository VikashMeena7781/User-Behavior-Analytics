package com.example.btp_10.Services;

import android.app.IntentService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.btp_10.DataRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsageStatsService extends IntentService {

    private static final String TAG = "Logs";
    private static final String PREFS_NAME = "UsageStatsPrefs";
    private static final String LAST_COLLECTION_KEY = "lastCollectionTime";
    private static final long TWO_DAYS_MILLIS = 2 * 24 * 60 * 60 * 1000; // 2 days in milliseconds

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    public UsageStatsService() {
        super("UsageStatsService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("appUsage");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Check if it's time to collect usage stats
        if (shouldCollectUsageStats()) {
            Log.d(TAG, "Collecting App Usage Stats for the last 2 days...");
            collectUsageStats();
            // Update the last collection time
            updateLastCollectionTime();
        } else {
            Log.d(TAG, "Skipping usage stats collection, not yet time");
        }
    }

    /**
     * Determines if usage stats should be collected based on the last collection time
     */
    private boolean shouldCollectUsageStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCollectionTime = prefs.getLong(LAST_COLLECTION_KEY, 0);
        long currentTime = System.currentTimeMillis();

        // If this is the first run (lastCollectionTime = 0) or it's been 2 days since last collection
        return lastCollectionTime == 0 || (currentTime - lastCollectionTime) >= TWO_DAYS_MILLIS;
    }

    /**
     * Updates the timestamp of the last collection time
     */
    private void updateLastCollectionTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_COLLECTION_KEY, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Updated last usage stats collection time");
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
            List<Map<String, Object>> usageDataList = new ArrayList<>();

            for (UsageStats usageStats : statsList) {
                // Process only apps that have been used (foreground time greater than zero)
                if (usageStats.getTotalTimeInForeground() > 0) {
                    String appName = usageStats.getPackageName();
                    long durationInSeconds = usageStats.getTotalTimeInForeground() / 1000;

                    // Add to local StringBuilder for DataRepository
                    entry.append("App: ").append(appName)
                            .append(", Total Time: ").append(durationInSeconds).append(" sec\n");

                    // Create a map for Firebase
                    Map<String, Object> appData = new HashMap<>();
                    appData.put("appName", appName);
                    appData.put("durationSeconds", durationInSeconds);
                    usageDataList.add(appData);
                }
            }

            // Store locally
            Log.d(TAG, entry.toString());
            DataRepository.getInstance().addUsageStats(entry.toString());

            // Store in Firebase
            storeUsageStatsInFirebase(usageDataList);

        } else {
            Log.d(TAG, "No usage stats available for the last 2 days.");
        }
    }

    private void storeUsageStatsInFirebase(List<Map<String, Object>> usageDataList) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Create summary data
            Map<String, Object> summaryData = new HashMap<>();
            // Add timestamps
            long timestamp = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(timestamp));

            summaryData.put("timestamp", timestamp);
            summaryData.put("formattedTime", formattedDate);
            summaryData.put("appCount", usageDataList.size());
            summaryData.put("apps", usageDataList);

            // Generate a unique key for this collection
            String collectionKey = databaseRef.child(userId).push().getKey();

            if (collectionKey != null) {
                databaseRef.child(userId).child(collectionKey)
                        .setValue(summaryData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "App usage stats successfully stored in Firebase");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Error storing app usage stats in Firebase: " + e.getMessage());
                            }
                        });
            }
        } else {
            Log.w(TAG, "User not logged in, app usage stats stored only locally");
        }
    }
}