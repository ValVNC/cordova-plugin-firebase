package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.FirebasePlugin;
import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;

public class MarkAsReadAction extends BaseActionTalk {
    private static final String TAG = "Firebase.MarkAsReadAction";


    public MarkAsReadAction(String sender, int notificationId, Context context) {
        super(null, sender, notificationId, context, "/markConversationsRead");
    }

    @Override
    public void run() {
        super.run();
        
        try {
            JSONObject postData = new JSONObject();
            postData.put(sender, new Date().getTime() / 1000);
            postData.put("send_rtc_signal", "1");

            Log.i(TAG, "postData : " + postData);

            HttpURLConnection urlConnection = createUrlConnection();

            NotificationCreator.setNotificationSmallIcon(context, notificationBuilder);

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }
            int statusCode = urlConnection.getResponseCode();
            Log.i(TAG, "Server response, statusCode: " + statusCode);
            if (statusCode != 200) {
                saveMarkAsReadOnError(context, sender);
            }
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
            Log.i(TAG, e.getLocalizedMessage());

            saveMarkAsReadOnError(context, sender);
            notificationManager.cancel(notificationId);
        } finally {
            // hide all other notifications for this target
            ArrayList<String> nIds = NotificationUtils.removeFromFileAndHideNotificationsForTarget(context, sender);
            if (nIds != null) {
                for (int i = 0; i < nIds.size(); i++) {
                    notificationManager.cancel(Integer.parseInt(nIds.get(i)));
                }
            }
            Bundle data = new Bundle();
            data.putString("target", sender);
            FirebasePlugin.sendNotificationMarkAsRead(data);
        }
    }

    private void saveMarkAsReadOnError(Context context, String target) {
        Log.i(TAG, "saveMarkAsReadOnError, target: " + target);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "markAsReadFailedRequests");
        ArrayList<MarkAsReadEntity> list = new ArrayList();
        if (data != null && !data.isEmpty()) {
            Type type = new TypeToken<ArrayList<MarkAsReadEntity>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new MarkAsReadEntity(target));
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "markAsReadFailedRequests", json);
    }

    private class MarkAsReadEntity {
        String target;
        long timestamp;

        public MarkAsReadEntity(String target) {
            this.target = target;
            this.timestamp = new Date().getTime();
        }
    }

}
