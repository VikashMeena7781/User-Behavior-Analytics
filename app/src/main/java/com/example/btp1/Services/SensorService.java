package com.example.btp1.Services;

import android.app.Service;
import android.content.Intent;import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.btp1.DataRepository;

import java.util.List;

public class SensorService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private final String TAG = "Logs";
    private Sensor lightSensor;
    private Sensor motionSensor;
    private Sensor accelerometerSensor;  // For fallback motion detection

    public SensorService() {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get Light Sensor
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Get Motion Sensor
        motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MOTION_DETECT);

        // Fallback: Use Accelerometer for motion detection if motion sensor is not available
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (lightSensor == null) {
            Log.d(TAG, "Light sensor not available");
        } else {
            Log.d(TAG, "Light sensor initialized");
        }

        if (motionSensor == null) {
            Log.d(TAG, "Motion sensor not available");
        } else {
            Log.d(TAG, "Motion sensor initialized");
        }

        if (accelerometerSensor != null) {
            Log.d(TAG, "Accelerometer sensor initialized (fallback for motion detection)");
        } else {
            Log.d(TAG, "Accelerometer sensor not available");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Register light and motion sensors
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }

        if (motionSensor != null) {
            sensorManager.registerListener(this, motionSensor, SensorManager.SENSOR_DELAY_UI);
        } else if (accelerometerSensor != null) {
            // Fallback: Use accelerometer if motion sensor is unavailable
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }

        return START_STICKY; // Keep service running until explicitly stopped
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the sensor listeners to avoid memory leaks
        if (lightSensor != null) {
            sensorManager.unregisterListener(this, lightSensor);
        }

        if (motionSensor != null) {
            sensorManager.unregisterListener(this, motionSensor);
        }

        if (accelerometerSensor != null) {
            sensorManager.unregisterListener(this, accelerometerSensor);
        }

        Log.d(TAG, "Sensors unregistered and service destroyed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Handle changes in sensor data
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float currentLightLevel = event.values[0];
            if (currentLightLevel >= 10) {
                String entry = "The phone is in a light environment";
                Log.d(TAG, entry);
                DataRepository.getInstance().addSensorData(entry);
//                Log.d(TAG, "The phone is in a light environment");
            } else{
                String entry = "The phone is in a dark environment";
                Log.d(TAG, entry);
                DataRepository.getInstance().addSensorData(entry);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MOTION_DETECT) {
            boolean isMotionDetected = event.values[0] == 1.0f;
            if (isMotionDetected) {
                String entry = "Motion Detected: The phone is moving";
                Log.d(TAG, entry);
                DataRepository.getInstance().addSensorData(entry);
//                Log.d(TAG, "Motion Detected: The phone is moving");
            }else{
                String entry = "No Motion Detected: The phone is not moving";
                Log.d(TAG, entry);
                DataRepository.getInstance().addSensorData(entry);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Handle motion detection using accelerometer
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float magnitude = (float) Math.sqrt(x* x + y * y + z * z);
            if (magnitude > 10) {
                String entry = "Motion Detected: The phone is moving";
                Log.d(TAG, entry);
                DataRepository.getInstance().addSensorData(entry);
            }else{
                String entry = "No Motion Detected: The phone is not moving";
                Log.d(TAG, entry);
                DataRepository.getInstance().addSensorData(entry);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes if necessary
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return null since this is a started service, not a bound service
        return null;
    }
}