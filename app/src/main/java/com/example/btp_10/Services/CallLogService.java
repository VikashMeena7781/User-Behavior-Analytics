package com.example.btp_10.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CallLog;
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

public class CallLogService extends IntentService {

    private static final String TAG = "Logs";
    private static final String PREFS_NAME = "CallLogPrefs";
    private static final String LAST_COLLECTION_KEY = "lastCollectionTime";
    private static final long TWO_DAYS_MILLIS = 2 * 24 * 60 * 60 * 1000; // 2 days in milliseconds

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    public CallLogService() {
        super("CallLogService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("callLogs");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Check if it's time to collect call logs
        if (shouldCollectCallLogs()) {
            Log.d(TAG, "Collecting Call Logs for the past 2 days...");
            collectCallLogs();
            // Update the last collection time
            updateLastCollectionTime();
        } else {
            Log.d(TAG, "Skipping call log collection, not yet time");
        }
    }

    /**
     * Determines if call logs should be collected based on the last collection time
     */
    private boolean shouldCollectCallLogs() {
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

        Log.d(TAG, "Updated last collection time: " + new Date(System.currentTimeMillis()));
    }

    private void collectCallLogs() {
        String[] projection = new String[]{
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE
        };

        // Calculate the timestamp for 2 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -2);
        long twoDaysAgoMillis = calendar.getTimeInMillis();

        // Build a selection to retrieve calls from the last 2 days
        String selection = CallLog.Calls.DATE + " >= ?";
        String[] selectionArgs = new String[]{String.valueOf(twoDaysAgoMillis)};

        // Query the call log content provider
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs,
                CallLog.Calls.DATE + " DESC"); // Sort by date descending

        StringBuilder callLogEntries = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        int callCount = 0;

        // List to store individual call log entries for Firebase
        List<Map<String, Object>> callLogsList = new ArrayList<>();

        if (cursor != null) {
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);

            if (dateIndex != -1 && durationIndex != -1 && numberIndex != -1 && typeIndex != -1) {
                callLogEntries.append("Call Logs for ").append(dateFormat.format(new Date())).append("\n\n");

                while (cursor.moveToNext()) {
                    callCount++;
                    long date = cursor.getLong(dateIndex);
                    int duration = cursor.getInt(durationIndex);
                    String number = cursor.getString(numberIndex);
                    int type = cursor.getInt(typeIndex);

                    String typeStr = getCallType(type);

                    // Add to the text summary
                    callLogEntries.append("Call ").append(callCount).append(":\n")
                            .append("Date: ").append(dateFormat.format(new Date(date))).append("\n")
                            .append("Duration: ").append(formatDuration(duration)).append("\n")
//                            .append("Number: ").append(formatPhoneNumber(number)).append("\n")
                            .append("Type: ").append(typeStr).append("\n\n");

                    // Create a map for Firebase storage
                    Map<String, Object> callEntry = new HashMap<>();
                    callEntry.put("date", dateFormat.format(new Date(date)));
                    callEntry.put("timestamp", date);
                    callEntry.put("duration", duration);
                    callEntry.put("formattedDuration", formatDuration(duration));
//                    callEntry.put("phoneNumber", formatPhoneNumber(number));
                    callEntry.put("type", typeStr);

                    callLogsList.add(callEntry);
                }

                if (callCount == 0) {
                    callLogEntries.append("No calls in the past 2 days\n");
                } else {
                    callLogEntries.append("Total calls: ").append(callCount).append("\n");
                }
            } else {
                Log.e(TAG, "One or more columns are missing in the projection.");
                callLogEntries.append("Error: Could not access all required call log columns\n");
            }
            cursor.close();

            // Store the call log data in local repository
            DataRepository.getInstance().addCallLog(callLogEntries.toString());
            Log.d(TAG, "Stored " + callCount + " call log entries locally");

            // Store the call log data in Firebase
            storeCallLogsInFirebase(callLogsList);

        } else {
            Log.e(TAG, "Failed to query call logs. Cursor is null.");
            DataRepository.getInstance().addCallLog("Error: Failed to query call logs");
        }
    }

    /**
     * Stores the call logs in Firebase Realtime Database
     */
    private void storeCallLogsInFirebase(List<Map<String, Object>> callLogs) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Create summary data
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("collectionDate", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date()));
            summaryData.put("timestamp", System.currentTimeMillis());
            summaryData.put("callCount", callLogs.size());
            summaryData.put("calls", callLogs);

            // Generate a unique key for this collection
            String collectionKey = databaseRef.child(userId).push().getKey();

            if (collectionKey != null) {
                databaseRef.child(userId).child(collectionKey)
                        .setValue(summaryData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Call logs successfully stored in Firebase");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Error storing call logs in Firebase: " + e.getMessage());
                            }
                        });
            }
        } else {
            Log.w(TAG, "User not logged in, call logs stored only locally");
        }
    }

    private String getCallType(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "Outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "Missed";
            case CallLog.Calls.REJECTED_TYPE:
                return "Rejected";
            case CallLog.Calls.BLOCKED_TYPE:
                return "Blocked";
            case CallLog.Calls.ANSWERED_EXTERNALLY_TYPE:
                return "Answered Externally";
            default:
                return "Unknown";
        }
    }

    /**
     * Formats the call duration from seconds to a more readable format
     */
    private String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + " min " + remainingSeconds + " sec";
        }
    }

    /**
     * Simple formatter to add privacy to phone numbers
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return phoneNumber;
        }

        // Mask middle digits for privacy
        return phoneNumber.substring(0, 2) + "****" +
                phoneNumber.substring(phoneNumber.length() - 2);
    }
}