package com.example.btp_10;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d("NotificationListener", "Getting Notif");
        // Log the notification details
        String notificationDetails = "Notification posted: "
                + sbn.getPackageName()
                + " | ID: " + sbn.getId()
                + " | Title: " + sbn.getNotification().extras.getString("android.title")
                + " | Text: " + sbn.getNotification().extras.getString("android.text");

        Log.d("NotificationListener", notificationDetails);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Log when a notification is removed
        Log.d("NotificationListener", "Notification removed: " + sbn.getPackageName());
    }
}
