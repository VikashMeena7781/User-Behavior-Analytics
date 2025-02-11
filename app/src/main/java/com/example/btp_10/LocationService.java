package com.example.btp_10;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationService extends IntentService {
    private FusedLocationProviderClient fusedLocationClient;

    public LocationService() {
        super("LocationService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Fetch location every 5 minutes
        getLocation();
//        scheduleNextLocationRequest();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Location Service ", "Permission Not Provided.");
            return;
        }

        // Use the main executor for handling location result on the main thread
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(ContextCompat.getMainExecutor(this), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Log the location data
                            Log.d("LocationService", "Location: Latitude = " + location.getLatitude() +
                                    ", Longitude = " + location.getLongitude() +
                                    ", Altitude = " + location.getAltitude() +
                                    ", Accuracy = " + location.getAccuracy());

                            Log.d("LocationService", "Scheduling next location fetch");
                            scheduleNextLocationRequest();
                        } else {
                            Log.d("LocationService", "Location is null.");
                        }
                    }
                });
    }

    private void scheduleNextLocationRequest() {
        // Schedule next location fetch in 5 minutes
        long delay = 5 * 60 * 1000; // 5 minutes in milliseconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("LocationService","Get Location Scheduled");
                getLocation();
            }
        }, delay);
    }
}
