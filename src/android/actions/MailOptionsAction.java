package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.util.Log;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.FirebasePlugin;
import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.apache.cordova.firebase.utils.WidgetNotifier;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StringBuffer;

public class MailOptionsAction extends BaseActionMail {
    private static final String TAG = "Firebase.MailOptionsAction";

    private final String option;
    private final Integer[] msgIds;

    public MailOptionsAction(Context context, int notificationId, String option, Integer... msgIds) {
        super(notificationId, context, "/msgAction");

        this.option = option;
        this.msgIds = msgIds;
    }

    @Override
    public void run() {
        super.run();
        if (!isInternetAvailable()) {
            showToast("No internet connection", false);
        } else {

            try {
                JSONObject postData = new JSONObject();
                postData.put("op", option);
                postData.put("id", new JSONArray(msgIds));

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
                if (statusCode > 400) {
                    Log.i(TAG, "Server response: " + urlConnection.getResponseMessage());

                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Log.i(TAG, "Server error response: " + response.toString());

                    saveOptionOnError(context, option, msgIds);
                } else if (HttpURLConnection.HTTP_OK == statusCode) {
                    // notify widget data set changed
                    WidgetNotifier.notifyMessagesListUpdated(context);
                }
                cancelNotification();
            } catch (Exception e) {
                Log.i(TAG, e.getLocalizedMessage());

                saveOptionOnError(context, option, msgIds);
                cancelNotification();
            } finally {

            }
        }
    }

    private void saveOptionOnError(Context context, String option, Integer[] msgIds) {
        Log.i(TAG, "saveOptionOnError, msgIds: " + TextUtils.join(",", msgIds));

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "mailOptionFailedRequests");

        ArrayList<OptionEntity> list;
        if (data != null) {
            Type type = new TypeToken<ArrayList<OptionEntity>>() {
            }.getType();
            list = gson.fromJson(data, type);
            if (list == null) {
                list = new ArrayList();
            }
        } else {
            list = new ArrayList();
        }
        OptionEntity oe = new OptionEntity(msgIds, option);
        list.add(oe);
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "mailOptionFailedRequests", json);
    }

    private class OptionEntity {
        Integer[] msgIds;
        String option;

        public OptionEntity(Integer[] msgIds, String option) {
            this.msgIds = msgIds;
            this.option = option;
        }
    }
}
