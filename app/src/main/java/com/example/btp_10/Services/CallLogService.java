package com.example.btp_10.Services;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CallLogService extends IntentService {

    private static final String TAG = "Logs";
    private static final String LAST_RUN_DATE_KEY = "last_run_date";

    public CallLogService() {
        super("CallLogService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "Checking if call logs should be collected...");

        // Check if the service has already run today
        if (hasServiceRunToday()) {
            Log.d(TAG, "Call log collection already done for today. Skipping...");
            return;
        }

        // Collect call logs and save today's date
        Log.d(TAG, "Collecting Call Logs...");
        collectCallLogs();
        saveLastRunDate();
    }

    private void collectCallLogs() {
        String[] projection = new String[]{
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE
        };

        // Get the start of the current day in milliseconds
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 28);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDayMillis = calendar.getTimeInMillis();

        // Filter calls from today only
        String selection = CallLog.Calls.DATE + " >= ?";
        String[] selectionArgs = new String[]{String.valueOf(startOfDayMillis)};

        // Query the call log
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, null);

        if (cursor != null) {
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);

            if (dateIndex != -1 && durationIndex != -1 && numberIndex != -1 && typeIndex != -1) {
                while (cursor.moveToNext()) {
                    long date = cursor.getLong(dateIndex);
                    int duration = cursor.getInt(durationIndex);
                    String number = cursor.getString(numberIndex);
                    int type = cursor.getInt(typeIndex);

                    String typeStr = getCallType(type);

                    Log.d(TAG, "Call Date: " + new Date(date) +
                            ", Duration: " + duration + " sec" +
                            ", Number: " + number +
                            ", Type: " + typeStr);
                }
            } else {
                Log.e(TAG, "One or more columns are missing.");
            }
            cursor.close();
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
            default:
                return "Unknown";
        }
    }

    private boolean hasServiceRunToday() {
        // Load last run date from SharedPreferences
        String lastRunDate = getSharedPreferences("CallLogPrefs", MODE_PRIVATE)
                .getString(LAST_RUN_DATE_KEY, "");

        // Get today's date
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        return todayDate.equals(lastRunDate);
    }

    private void saveLastRunDate() {
        // Save today's date in SharedPreferences
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        getSharedPreferences("CallLogPrefs", MODE_PRIVATE)
                .edit()
                .putString(LAST_RUN_DATE_KEY, todayDate)
                .apply();
    }
}
