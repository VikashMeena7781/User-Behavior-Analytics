package com.example.btp_10;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    private static final DataRepository instance = new DataRepository();
    private final List<String> callLogs = new ArrayList<>();
    private final List<String> locations = new ArrayList<>();
    private final List<String> microphoneData = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();
    private final List<String> screenStatus = new ArrayList<>();
    private final List<String> sensorData = new ArrayList<>();
    private final List<String> usageStats = new ArrayList<>();

    private DataRepository() { }

    public static DataRepository getInstance() {
        return instance;
    }

    public synchronized void addCallLog(String entry) {
        callLogs.clear();
        callLogs.add(entry);
    }

    public synchronized List<String> getCallLogs() {
        return new ArrayList<>(callLogs);
    }

    public synchronized void addLocation(String entry) {
        locations.clear();
        locations.add(entry);
    }

    public synchronized List<String> getLocations() {
        return new ArrayList<>(locations);
    }

    public synchronized void addMicrophoneData(String entry) {
        microphoneData.clear();
        microphoneData.add(entry);
    }

    public synchronized List<String> getMicrophoneData() {
        return new ArrayList<>(microphoneData);
    }

    public synchronized void addNotification(String entry) {
        notifications.clear();
        notifications.add(entry);
    }

    public synchronized List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public synchronized void addScreenStatus(String entry) {
        screenStatus.clear();
        screenStatus.add(entry);
    }

    public synchronized List<String> getScreenStatus() {
        return new ArrayList<>(screenStatus);
    }

    public synchronized void addSensorData(String entry) {
        sensorData.clear();
        sensorData.add(entry);
    }

    public synchronized List<String> getSensorData() {
        return new ArrayList<>(sensorData);
    }

    public synchronized void addUsageStats(String entry) {
        usageStats.clear();
        usageStats.add(entry);
    }

    public synchronized List<String> getUsageStats() {
        return new ArrayList<>(usageStats);
    }
}
