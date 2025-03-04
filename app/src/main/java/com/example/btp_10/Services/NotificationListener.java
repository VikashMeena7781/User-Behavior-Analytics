package com.example.btp_10.Services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.app.Notification;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "Logs";

    /**
     * Called when the notification listener is connected and ready.
     * Here you can safely call getActiveNotifications().
     */
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "Notification Listener connected. Fetching active notifications...");

        // Fetch all currently active notifications
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications != null && activeNotifications.length > 0) {
            for (StatusBarNotification sbn : activeNotifications) {
                logNotificationDetails(sbn, "Active");
            }
        } else {
            Log.d(TAG, "No active notifications found.");
        }
    }

    /**
     * Called when a new notification is posted.
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        logNotificationDetails(sbn, "Posted");
    }

    /**
     * Called when a notification is removed (cleared, dismissed, or canceled).
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }

    /**
     * Helper method to log the notification details.
     */
    private void logNotificationDetails(StatusBarNotification sbn, String eventType) {
        String packageName = sbn.getPackageName();

        // Extract title and text from the notification
        Notification notification = sbn.getNotification();
        String title = notification.extras.getString(Notification.EXTRA_TITLE);
        String text  = notification.extras.getString(Notification.EXTRA_TEXT);

        Log.d(TAG,
                eventType + " Notification --> " +
                        "Package: " + packageName +
                        ", ID: " + sbn.getId() +
                        ", Title: " + title +
                        ", Text: " + text
        );
    }
}
