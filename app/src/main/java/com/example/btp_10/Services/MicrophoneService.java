package com.example.btp_10.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.btp_10.DataRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MicrophoneService extends Service {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;
    private static final int THRESHOLD = 500; // Amplitude threshold for detection
    private static final long DETECTION_DURATION = 30 * 1000; // 30 seconds
    private static final long COLLECTION_INTERVAL = 15 * 60 * 1000; // 15 minutes

    private static final String TAG = "Logs";
    private static final String PREFS_NAME = "MicrophonePrefs";
    private static final String LAST_COLLECTION_KEY = "lastCollectionTime";

    private AudioRecord audioRecord;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean activityDetected = new AtomicBoolean(false);
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("microphoneActivity");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MicrophoneService started");
        // Check if it's time to collect microphone data
        if (shouldCollectMicrophoneData()) {
            Log.d(TAG, "Time to check microphone activity");
            startMicrophoneDetection();
        } else {
            Log.d(TAG, "Skipping microphone check, not yet time");
            stopSelf(); // Stop the service if it's not time
        }
        // Use START_NOT_STICKY as we rely on external triggers and SharedPreferences
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMicrophoneDetection();
        Log.d(TAG, "MicrophoneService destroyed");
    }

    /**
     * Determines if microphone data should be collected based on the last collection time
     */
    private boolean shouldCollectMicrophoneData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCollectionTime = prefs.getLong(LAST_COLLECTION_KEY, 0);
        long currentTime = System.currentTimeMillis();

        // If this is the first run or it's been 15 minutes since last collection
        return lastCollectionTime == 0 || (currentTime - lastCollectionTime) >= COLLECTION_INTERVAL;
    }

    /**
     * Updates the timestamp of the last collection time
     */
    private void updateLastCollectionTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_COLLECTION_KEY, System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Updated last microphone collection time");
    }

    private void startMicrophoneDetection() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Failed to get minimum buffer size");
            storeResult(false); // Store 0 if setup fails
            updateLastCollectionTime();
            stopSelf();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "RECORD_AUDIO Permission not provided");
            storeResult(false); // Store 0 if permission missing
            updateLastCollectionTime();
            stopSelf();
            return;
        }

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.");
                storeResult(false); // Store 0
                updateLastCollectionTime();
                stopSelf();
                return;
            }

            isRecording.set(true);
            activityDetected.set(false); // Reset detection flag

            audioRecord.startRecording();
            Log.d(TAG, "Microphone detection started for " + (DETECTION_DURATION / 1000) + " seconds.");

            // Start reading data in a background thread
            new Thread(this::readAudioData).start();

            // Schedule task to stop detection and store result after DETECTION_DURATION
            handler.postDelayed(() -> {
                stopMicrophoneDetection();
                boolean detected = activityDetected.get();
                Log.d(TAG, "Microphone detection finished. Activity detected: " + detected);
                storeResult(detected); // Store 1 if true, 0 if false
                updateLastCollectionTime();
                stopSelf(); // Stop the service after completion
            }, DETECTION_DURATION);

        } catch (Exception e) {
            Log.e(TAG, "Error starting AudioRecord: " + e.getMessage());
            storeResult(false); // Store 0
            updateLastCollectionTime();
            stopSelf();
        }
    }

    private void stopMicrophoneDetection() {
        if (isRecording.compareAndSet(true, false)) { // Ensure stop is called only once
            if (audioRecord != null) {
                try {
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop();
                    }
                    audioRecord.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping AudioRecord: " + e.getMessage());
                } finally {
                    audioRecord = null;
                    Log.d(TAG, "Microphone resources released.");
                }
            }
        }
    }

    private void readAudioData() {
        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            return;
        }

        short[] audioBuffer = new short[BUFFER_SIZE];

        // Read data only while isRecording is true
        while (isRecording.get()) {
            int numberOfShorts = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (numberOfShorts > 0) {
                double amplitude = calculateAmplitude(audioBuffer);
                if (amplitude > THRESHOLD) {
                    activityDetected.set(true);
                }
            } else if (numberOfShorts == AudioRecord.ERROR_INVALID_OPERATION || numberOfShorts == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Error reading audio data: " + numberOfShorts);
                break; // Stop reading on error
            }
        }
        Log.d(TAG, "Audio reading thread finished.");
    }

    private double calculateAmplitude(short[] audioBuffer) {
        double sum = 0;
        for (short sample : audioBuffer) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / audioBuffer.length);
        return rms;
    }

    private void storeResult(boolean detected) {
        // Convert boolean to integer (1 for true, 0 for false)
        int detectedValue = detected ? 1 : 0;
        String entry = detected ? "Microphone activity detected" : "Microphone activity not detected";
        Log.d(TAG, "Storing result: " + entry + " (Value: " + detectedValue + ")");

        // Store locally (still as string)
        DataRepository.getInstance().addMicrophoneData(entry);

        // Store in Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Map<String, Object> data = new HashMap<>();
            // Store the integer value instead of boolean
            data.put("activityDetected", detectedValue);
            data.put("timestamp", System.currentTimeMillis());

            String entryKey = databaseRef.child(userId).push().getKey();
            if (entryKey != null) {
                databaseRef.child(userId).child(entryKey)
                        .setValue(data)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Microphone status (0/1) stored in Firebase"))
                        .addOnFailureListener(e -> Log.e(TAG, "Error storing microphone status (0/1) in Firebase: " + e.getMessage()));
            }
        } else {
            Log.w(TAG, "User not logged in, microphone status stored only locally");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}