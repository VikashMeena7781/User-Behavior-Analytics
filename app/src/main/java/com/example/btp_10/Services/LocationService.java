package com.example.btp_10.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.btp_10.DataRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationService extends IntentService {
    private FusedLocationProviderClient fusedLocationClient;
    private final String TAG = "Logs";
    private static final String PREFS_NAME = "LocationPrefs";
    private static final String LAST_LOCATION_KEY = "lastLocationTime";
    private static final long LOCATION_INTERVAL = 15 * 60 * 1000; // 15 minutes in milliseconds

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    public LocationService() {
        super("LocationService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("locationData");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Check if it's time to collect location
        if (shouldCollectLocation()) {
            Log.d(TAG, "Time to collect location data");
            getLocation();
        } else {
            Log.d(TAG, "Skipping location collection, not yet time");
        }
    }

    /**
     * Determines if location should be collected based on the last collection time
     */
    private boolean shouldCollectLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCollectionTime = prefs.getLong(LAST_LOCATION_KEY, 0);
        long currentTime = System.currentTimeMillis();

        // If this is the first run (lastCollectionTime = 0) or it's been 15 minutes since last collection
        return lastCollectionTime == 0 || (currentTime - lastCollectionTime) >= LOCATION_INTERVAL;
    }

    /**
     * Updates the timestamp of the last location collection time
     */
    private void updateLastCollectionTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_LOCATION_KEY, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Updated last location collection time");
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location Permission Not Provided.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(ContextCompat.getMainExecutor(this), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Log the location data
                            String entry = "Location: Lat=" + location.getLatitude() +
                                    ", Long=" + location.getLongitude() +
                                    ", Altitude=" + location.getAltitude();

                            Log.d(TAG, entry);

                            // Store in local repository
                            DataRepository.getInstance().addLocation(entry);

                            // Store in Firebase
                            storeLocationInFirebase(location);

                            // Update the last collection time
                            updateLastCollectionTime();
                        } else {
                            Log.d(TAG, "Location is null.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error getting location: " + e.getMessage());
                    }
                });
    }

    private void storeLocationInFirebase(Location location) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Create simplified location data object with only requested fields
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("altitude", location.hasAltitude() ? location.getAltitude() : 0.0);
            // Add timestamps
            long timestamp = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(timestamp));

            locationData.put("timestamp", timestamp);
            locationData.put("formattedTime", formattedDate);

            // Generate a unique key for this location entry
            String locationKey = databaseRef.child(userId).push().getKey();

            if (locationKey != null) {
                databaseRef.child(userId).child(locationKey)
                        .setValue(locationData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Location successfully stored in Firebase");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Error storing location in Firebase: " + e.getMessage());
                            }
                        });
            }
        } else {
            Log.w(TAG, "User not logged in, location stored only locally");
        }
    }
}