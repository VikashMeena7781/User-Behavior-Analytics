package com.example.btp_10.Services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.btp_10.DataRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SensorService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private final String TAG = "Logs";
    private Sensor lightSensor;
    private Sensor motionSensor; // Preferred motion sensor
    private Sensor accelerometerSensor; // Fallback motion sensor

    // State tracking variables
    private static final String STATE_UNKNOWN = "UNKNOWN";
    private static final String STATE_LIGHT = "LIGHT"; // Light level >= threshold
    private static final String STATE_DARK = "DARK";   // Light level < threshold
    private static final String STATE_MOVING = "MOVING";
    private static final String STATE_STATIONARY = "STATIONARY";

    private String lastLightState = STATE_UNKNOWN;
    private String lastMotionState = STATE_UNKNOWN;

    // Thresholds
    private static final float LIGHT_THRESHOLD = 10.0f; // Lux level to differentiate light/dark
    private static final float ACCEL_MOTION_THRESHOLD = 10.5f; // Magnitude threshold for accelerometer motion (adjust as needed)
    private static final float MOTION_DETECT_VALUE = 1.0f; // Value indicating motion detected by TYPE_MOTION_DETECT

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference lightTransitionRef;
    private DatabaseReference motionTransitionRef;

    public SensorService() {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SensorService onCreate");
        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            lightTransitionRef = FirebaseDatabase.getInstance().getReference("lightTransitions").child(userId);
            motionTransitionRef = FirebaseDatabase.getInstance().getReference("motionTransitions").child(userId);
        } else {
            Log.w(TAG, "User not logged in, Firebase references not initialized.");
            // Consider stopping the service or handling this case appropriately
        }

        // Get Sensors
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MOTION_DETECT);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Log sensor availability
        logSensorStatus(lightSensor, "Light");
        logSensorStatus(motionSensor, "Motion Detect");
        logSensorStatus(accelerometerSensor, "Accelerometer (Motion Fallback)");
    }

    private void logSensorStatus(Sensor sensor, String name) {
        if (sensor != null) {
            Log.d(TAG, name + " sensor initialized.");
        } else {
            Log.d(TAG, name + " sensor not available.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SensorService onStartCommand");
        // Register listeners with a slightly less frequent delay
        int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL; // Use NORMAL instead of UI

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, sensorDelay);
        }

        // Prefer TYPE_MOTION_DETECT if available
        if (motionSensor != null) {
            sensorManager.registerListener(this, motionSensor, sensorDelay);
        } else if (accelerometerSensor != null) {
            // Fallback to accelerometer
            Log.d(TAG, "Using Accelerometer for motion detection.");
            sensorManager.registerListener(this, accelerometerSensor, sensorDelay);
        }

        return START_STICKY; // Keep service running
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister listeners
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Sensors unregistered and service destroyed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String currentLightState = null;
        String currentMotionState = null;

        // Determine current state based on sensor type
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                float lightLevel = event.values[0];
                currentLightState = (lightLevel >= LIGHT_THRESHOLD) ? STATE_LIGHT : STATE_DARK;
                handleLightStateChange(currentLightState);
                break;

            case Sensor.TYPE_MOTION_DETECT:
                // This sensor typically triggers once when motion starts.
                // We interpret its trigger as entering the MOVING state.
                // Lack of trigger implies STATIONARY (handled implicitly by state change logic).
                boolean motionDetected = (event.values[0] == MOTION_DETECT_VALUE);
                if (motionDetected) {
                    currentMotionState = STATE_MOVING;
                    handleMotionStateChange(currentMotionState);
                }
                // Note: TYPE_MOTION_DETECT doesn't explicitly signal stopping motion.
                // We might need a timeout or rely on accelerometer if continuous state is needed.
                // For simplicity, we only record the start of motion ("01") with this sensor.
                // To record "10", we'd need the accelerometer fallback or a timeout mechanism.
                break;

            case Sensor.TYPE_ACCELEROMETER:
                // Only use accelerometer if motionSensor is null
                if (motionSensor == null) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];
                    // Calculate magnitude, ignoring gravity (approximate)
                    double magnitude = Math.sqrt(x * x + y * y + z * z);
                    // Simple threshold check (adjust ACCEL_MOTION_THRESHOLD)
                    currentMotionState = (magnitude > ACCEL_MOTION_THRESHOLD) ? STATE_MOVING : STATE_STATIONARY;
                    handleMotionStateChange(currentMotionState);
                }
                break;
        }
    }

    private void handleLightStateChange(String currentState) {
        if (!lastLightState.equals(currentState) && !lastLightState.equals(STATE_UNKNOWN)) {
            String transitionCode = null;
            if (lastLightState.equals(STATE_DARK) && currentState.equals(STATE_LIGHT)) {
                transitionCode = "01"; // Dark -> Light
            } else if (lastLightState.equals(STATE_LIGHT) && currentState.equals(STATE_DARK)) {
                transitionCode = "10"; // Light -> Dark
            }

            if (transitionCode != null) {
                Log.d(TAG, "Light Transition: " + lastLightState + " -> " + currentState + " (" + transitionCode + ")");
                storeTransitionInFirebase(lightTransitionRef, transitionCode, "Light");
                // Log locally if needed
                DataRepository.getInstance().addSensorData("Light Transition: " + transitionCode);
            }
        }
        // Update last state if it changed or was unknown
        if (!lastLightState.equals(currentState)) {
            lastLightState = currentState;
        }
    }

    private void handleMotionStateChange(String currentState) {
        if (!lastMotionState.equals(currentState) && !lastMotionState.equals(STATE_UNKNOWN)) {
            String transitionCode = null;
            if (lastMotionState.equals(STATE_STATIONARY) && currentState.equals(STATE_MOVING)) {
                transitionCode = "01"; // Stationary -> Moving
            } else if (lastMotionState.equals(STATE_MOVING) && currentState.equals(STATE_STATIONARY)) {
                transitionCode = "10"; // Moving -> Stationary
            }

            // Special case for TYPE_MOTION_DETECT: It only signals "01" reliably.
            if (motionSensor != null && "10".equals(transitionCode)) {
                Log.d(TAG, "Ignoring '10' transition for TYPE_MOTION_DETECT sensor.");
                transitionCode = null; // Don't store "10" if using only TYPE_MOTION_DETECT
            }


            if (transitionCode != null) {
                Log.d(TAG, "Motion Transition: " + lastMotionState + " -> " + currentState + " (" + transitionCode + ")");
                storeTransitionInFirebase(motionTransitionRef, transitionCode, "Motion");
                // Log locally if needed
                DataRepository.getInstance().addSensorData("Motion Transition: " + transitionCode);
            }
        }
        // Update last state if it changed or was unknown
        if (!lastMotionState.equals(currentState)) {
            lastMotionState = currentState;
        }
    }

    private void storeTransitionInFirebase(DatabaseReference dbRef, String transitionCode, String type) {
        if (dbRef == null) {
            Log.w(TAG, "Database reference is null. Cannot store " + type + " transition.");
            return; // Don't proceed if user wasn't logged in during onCreate
        }

        long timestamp = System.currentTimeMillis();
        Map<String, Object> transitionData = new HashMap<>();
        transitionData.put("transition", transitionCode);
        transitionData.put("timestamp", timestamp);

        // Generate a unique key
        String entryKey = dbRef.push().getKey();

        if (entryKey != null) {
            dbRef.child(entryKey)
                    .setValue(transitionData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, type + " transition (" + transitionCode + ") stored in Firebase."))
                    .addOnFailureListener(e -> Log.e(TAG, "Error storing " + type + " transition in Firebase: " + e.getMessage()));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Can be ignored for this use case
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}