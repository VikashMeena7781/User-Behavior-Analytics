package com.example.btp1;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRepository {
    private static final DataRepository instance = new DataRepository();
    private final List<String> callLogs = new ArrayList<>();
    private final List<String> locations = new ArrayList<>();
    private final List<String> microphoneData = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();
    private final List<String> screenStatus = new ArrayList<>();
    private final List<String> sensorData = new ArrayList<>();
    private final List<String> usageStats = new ArrayList<>();
    private static final String TAG = "DataRepository";

    // Firebase components
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DataRepository() { }

    public static DataRepository getInstance() {
        return instance;
    }

    // Get current user ID
    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "anonymous";
    }

    // Save data to Firebase
    private void saveToFirebase(String dataType, String data) {
        String userId = getCurrentUserId();

        // If user is not logged in, don't try to save to Firestore
        if (userId.equals("anonymous")) {
            Log.d(TAG, "User not logged in, data not saved to Firebase");
            return;
        }

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", data);
        dataMap.put("timestamp", System.currentTimeMillis());

        DocumentReference userDataRef = db.collection("users")
                .document(userId)
                .collection("sensorData")
                .document(dataType);

        userDataRef.set(dataMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, dataType + " data saved to Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving " + dataType + " data", e));
    }

    public synchronized void addCallLog(String entry) {
        callLogs.clear();
        callLogs.add(entry);
        saveToFirebase("callLogs", entry);
    }

    public synchronized List<String> getCallLogs() {
        return new ArrayList<>(callLogs);
    }

    public synchronized void addLocation(String entry) {
        locations.clear();
        locations.add(entry);
        saveToFirebase("locations", entry);
    }

    public synchronized List<String> getLocations() {
        return new ArrayList<>(locations);
    }

    public synchronized void addMicrophoneData(String entry) {
        microphoneData.clear();
        microphoneData.add(entry);
        saveToFirebase("microphoneData", entry);
    }

    public synchronized List<String> getMicrophoneData() {
        return new ArrayList<>(microphoneData);
    }

    public synchronized void addNotification(String entry) {
        notifications.clear();
        notifications.add(entry);
        saveToFirebase("notifications", entry);
    }

    public synchronized List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public synchronized void addScreenStatus(String entry) {
        screenStatus.clear();
        screenStatus.add(entry);
        saveToFirebase("screenStatus", entry);
    }

    public synchronized List<String> getScreenStatus() {
        return new ArrayList<>(screenStatus);
    }

    public synchronized void addSensorData(String entry) {
        sensorData.clear();
        sensorData.add(entry);
        saveToFirebase("sensorData", entry);
    }

    public synchronized List<String> getSensorData() {
        return new ArrayList<>(sensorData);
    }

    public synchronized void addUsageStats(String entry) {
        usageStats.clear();
        usageStats.add(entry);
        saveToFirebase("usageStats", entry);
    }

    public synchronized List<String> getUsageStats() {
        return new ArrayList<>(usageStats);
    }

    // Method to save survey responses to Firebase
    public void saveSurveyData(Map<String, Object> surveyData) {
        String userId = getCurrentUserId();

        // If user is not logged in, don't try to save to Firestore
        if (userId.equals("anonymous")) {
            Log.d(TAG, "User not logged in, survey data not saved to Firebase");
            return;
        }

        // Add timestamp if not already present
        if (!surveyData.containsKey("timestamp")) {
            surveyData.put("timestamp", System.currentTimeMillis());
        }

        // Generate a unique document ID for each survey submission
        db.collection("users")
                .document(userId)
                .collection("surveys")
                .add(surveyData)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "Survey saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving survey", e));
    }