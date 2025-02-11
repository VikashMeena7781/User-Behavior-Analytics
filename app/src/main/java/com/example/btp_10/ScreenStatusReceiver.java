package com.example.btp_10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class ScreenStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ScreenStatusReceiver", "Getting Screen Status");
        if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
            // Log when the screen is turned on
            Log.d("ScreenStatusReceiver", "Screen ON");
        } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
            // Log when the screen is turned off
            Log.d("ScreenStatusReceiver", "Screen OFF");
        }else{
            Log.d("ScreenStatusReceiver", "Something went wrong " + intent.getAction());
        }
    }
}

