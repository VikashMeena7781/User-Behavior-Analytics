package com.example.btp_10;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Date;

public class CallLogService extends IntentService {

    public CallLogService() {
        super("CallLogService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("CallLogs", "Collecting Call Logs");
        collectCallLogs();
    }

    private void collectCallLogs() {
        String[] projection = new String[]{
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE
        };

        // Query the call log content provider
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, null);

        if (cursor != null) {
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);

            // Check if all columns exist
            if (dateIndex != -1 && durationIndex != -1 && numberIndex != -1 && typeIndex != -1) {
                // Iterate through the cursor and log the call details
                while (cursor.moveToNext()) {
                    long date = cursor.getLong(dateIndex);
                    int duration = cursor.getInt(durationIndex);
                    String number = cursor.getString(numberIndex);
                    int type = cursor.getInt(typeIndex);

                    String typeStr = getCallType(type);

                    // Log the call log data
                    Log.d("CallLogService", "Call Date: " + new Date(date) +
                            ", Duration: " + duration + " seconds" +
                            ", Number: " + number +
                            ", Type: " + typeStr);
                }
            } else {
                Log.e("CallLogService", "One or more columns are missing in the projection.");
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
}
