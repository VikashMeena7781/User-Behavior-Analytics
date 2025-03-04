package com.example.btp_10.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class ScreenStatusReceiver extends BroadcastReceiver {
    private final String TAG = "Logs";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Getting Screen Status");
        if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
            // Log when the screen is turned on
            Log.d(TAG, "Screen ON");
        } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
            // Log when the screen is turned off
            Log.d(TAG, "Screen OFF");
        }else{
            Log.d(TAG, "Something went wrong " + intent.getAction());
        }
    }
}

