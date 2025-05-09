package com.example.btp_10.Services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
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
import java.util.regex.Pattern; // Import Pattern

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "Logs";

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    // Pattern to match invalid Firebase key characters
    private static final Pattern INVALID_KEY_CHARS_PATTERN = Pattern.compile("[.#$\\[\\]]");
    // Replacement character (use something unlikely to be in original keys)
    private static final String REPLACEMENT_CHAR = "_";

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("notifications");
        Log.d(TAG, "NotificationListener Service Created");
    }

    /**
     * Called when the notification listener is connected and ready.
     */
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "Notification Listener connected.");
    }

    /**
     * Called when a new notification is posted.
     * Stores package name and post time in Firebase.
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (sbn == null) return;

        String packageName = sbn.getPackageName();
        long postTime = sbn.getPostTime();
        String originalNotificationKey = sbn.getKey(); // Get the original key

        // *** Sanitize the key for Firebase ***
        String sanitizedNotificationKey = sanitizeFirebaseKey(originalNotificationKey);

        // Log locally
        String entry = "Posted Notification --> Package: " + packageName + ", OrigKey: " + originalNotificationKey + ", SanitizedKey: " + sanitizedNotificationKey;
        Log.d(TAG, entry);
        DataRepository.getInstance().addNotification(entry); // Store sanitized key if needed locally

        // Store in Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && sanitizedNotificationKey != null) {
            String userId = currentUser.getUid();

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("packageName", packageName);
            notificationData.put("postTime", postTime);
            notificationData.put("status", "posted"); // Initial status
            notificationData.put("originalKey", originalNotificationKey); // Optionally store original key

            // Use the *sanitized* notification key as the key in Firebase
            databaseRef.child(userId).child(sanitizedNotificationKey) // Use sanitized key here
                    .setValue(notificationData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification posted data stored in Firebase for key: " + sanitizedNotificationKey))
                    .addOnFailureListener(e -> Log.e(TAG, "Error storing posted notification in Firebase: " + e.getMessage()));
        } else {
            Log.w(TAG, "User not logged in or notification key is null/invalid, notification post not stored in Firebase.");
        }
    }

    /**
     * Called when a notification is removed (cleared, dismissed, or canceled).
     * Updates the corresponding Firebase entry with the removal time.
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        if (sbn == null) return;

        String packageName = sbn.getPackageName();
        long removeTime = System.currentTimeMillis(); // Time when it was removed
        String originalNotificationKey = sbn.getKey();

        // *** Sanitize the key for Firebase ***
        String sanitizedNotificationKey = sanitizeFirebaseKey(originalNotificationKey);

        // Log locally
        String entry = "Removed Notification --> Package: " + packageName + ", OrigKey: " + originalNotificationKey + ", SanitizedKey: " + sanitizedNotificationKey;
        Log.d(TAG, entry);
        // Optionally add to DataRepository if needed for local logging

        // Update Firebase entry
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && sanitizedNotificationKey != null) {
            String userId = currentUser.getUid();

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("removeTime", removeTime);
            updateData.put("status", "removed"); // Update status

            // Update the existing entry using the *sanitized* notification key
            databaseRef.child(userId).child(sanitizedNotificationKey) // Use sanitized key here
                    .updateChildren(updateData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification removed data updated in Firebase for key: " + sanitizedNotificationKey))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating removed notification in Firebase: " + e.getMessage()));
        } else {
            Log.w(TAG, "User not logged in or notification key is null/invalid, notification removal not updated in Firebase.");
        }
    }

    /**
     * Replaces characters invalid in Firebase Database keys.
     * Replaces '.', '#', '$', '[', ']', and '|' with '_'.
     *
     * @param key The original key string.
     * @return A sanitized string suitable for use as a Firebase key, or null if the input is null.
     */
    public static String sanitizeFirebaseKey(String key) {
        if (key == null) {
            return null;
        }
        // First replace the specific characters Firebase dislikes
        String sanitized = INVALID_KEY_CHARS_PATTERN.matcher(key).replaceAll(REPLACEMENT_CHAR);
        // Also replace the pipe character '|' which was causing the specific error
        sanitized = sanitized.replace('|', '_');
        return sanitized;
    }


    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "Notification Listener disconnected.");
    }
}