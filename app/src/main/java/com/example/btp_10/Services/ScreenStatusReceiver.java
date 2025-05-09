package com.example.btp_10.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.btp_10.DataRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ScreenStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "Logs";
    private static final String PREFS_NAME = "ScreenStatePrefs";
    private static final String LAST_STATE_KEY = "lastScreenState";
    private static final String STATE_ON = "ON";
    private static final String STATE_OFF = "OFF";
    private static final String STATE_UNKNOWN = "UNKNOWN"; // Initial state

    // Firebase (Initialize carefully, consider context lifetime)
    // It's generally safer to start a Service/IntentService from the receiver
    // to handle Firebase operations, but this shows the direct approach.
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        // Initialize Firebase Auth and Database reference
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("screenTransitions");

        String action = intent.getAction();
        String currentState;

        // Determine current state based on action
        if (Objects.equals(action, Intent.ACTION_SCREEN_ON)) {
            currentState = STATE_ON;
        } else if (Objects.equals(action, Intent.ACTION_SCREEN_OFF)) {
            currentState = STATE_OFF;
        } else {
            Log.w(TAG, "Received unknown action: " + action);
            return; // Ignore other actions
        }

        // Get SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastState = prefs.getString(LAST_STATE_KEY, STATE_UNKNOWN);

        String transitionCode = null;

        // Determine the transition code
        if (lastState.equals(STATE_OFF) && currentState.equals(STATE_ON)) {
            transitionCode = "01"; // OFF -> ON
        } else if (lastState.equals(STATE_ON) && currentState.equals(STATE_OFF)) {
            transitionCode = "10"; // ON -> OFF
        }

        // Log locally (optional, can be removed if only Firebase is needed)
        String localEntry = "Screen " + currentState + " (Last: " + lastState + ", Transition: " + (transitionCode != null ? transitionCode : "None") + ")";
        Log.d(TAG, localEntry);
        DataRepository.getInstance().addScreenStatus(localEntry); // Keep local log if desired

        // If a valid transition occurred, store it in Firebase
        if (transitionCode != null) {
            storeTransitionInFirebase(transitionCode);
        }

        // Always update the last known state in SharedPreferences,
        // but only if it has actually changed from the stored value.
        if (!lastState.equals(currentState)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LAST_STATE_KEY, currentState);
            editor.apply();
            Log.d(TAG, "Updated last screen state to: " + currentState);
        }
    }

    private void storeTransitionInFirebase(String transitionCode) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            long timestamp = System.currentTimeMillis();

            Map<String, Object> transitionData = new HashMap<>();
            transitionData.put("transition", transitionCode);
            transitionData.put("timestamp", timestamp);

            // Generate a unique key for this transition entry
            String entryKey = databaseRef.child(userId).push().getKey();

            if (entryKey != null) {
                databaseRef.child(userId).child(entryKey)
                        .setValue(transitionData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Screen transition (" + transitionCode + ") stored in Firebase."))
                        .addOnFailureListener(e -> Log.e(TAG, "Error storing screen transition in Firebase: " + e.getMessage()));
            }
        } else {
            Log.w(TAG, "User not logged in, screen transition not stored in Firebase.");
        }
    }
}