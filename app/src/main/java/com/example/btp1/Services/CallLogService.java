package com.example.btp1.Services;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.btp1.DataRepository;

import java.util.Calendar;
import java.util.Date;

public class CallLogService extends IntentService {

    private static final String TAG = "Logs";

    public CallLogService() {
        super("CallLogService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "Collecting Call Logs...");
        collectCallLogs();
    }

    private void collectCallLogs() {
        String[] projection = new String[]{
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE
        };

//        Get the start of the current day (midnight)
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.HOUR_OF_DAY, 0);
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.SECOND, 0);
//        calendar.set(Calendar.MILLISECOND, 0);
//        long startOfDayMillis = calendar.getTimeInMillis();
//
//        // Filter calls from today only
//        String selection = CallLog.Calls.DATE + " >= ?";
//        String[] selectionArgs = new String[]{String.valueOf(startOfDayMillis)};
        // Calculate the timestamp for 2 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -2);
        long twoDaysAgoMillis = calendar.getTimeInMillis();

        // Build a selection to retrieve calls from the last 2 days
        String selection = CallLog.Calls.DATE + " >= ?";
        String[] selectionArgs = new String[]{String.valueOf(twoDaysAgoMillis)};


        // Query the call log content provider
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, null);

        String entry = "";

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
                    entry = entry +  "Call Date: " + new Date(date) +
                            ", Duration: " + duration + " sec" +
                            ", Number: " + number +
                            ", Type: " + typeStr + "\n";
                    Log.d(TAG, entry);
                }
            } else {
                Log.e(TAG, "One or more columns are missing in the projection.");
            }
            cursor.close();
            DataRepository.getInstance().addCallLog(entry);
        } else {
            Log.e(TAG, "Failed to query call logs. Cursor is null.");
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
}