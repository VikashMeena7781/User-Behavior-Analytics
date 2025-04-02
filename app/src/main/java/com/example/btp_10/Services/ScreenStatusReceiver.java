package com.example.btp_10.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.btp_10.DataRepository;

import java.util.Objects;

public class ScreenStatusReceiver extends BroadcastReceiver {
    private final String TAG = "Logs";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Getting Screen Status");
        if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
            // Log when the screen is turned on
            String entry = "Screen ON";
            DataRepository.getInstance().addScreenStatus(entry);
            Log.d(TAG, entry);
        } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
            // Log when the screen is turned off
            Log.d(TAG, "Screen OFF");
            String entry = "Screen OFF";
            DataRepository.getInstance().addScreenStatus(entry);
        }else{
            Log.d(TAG, "Something went wrong " + intent.getAction());
        }
    }
}

