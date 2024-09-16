package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.util.Log;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.apache.cordova.firebase.utils.WidgetNotifier;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StringBuffer;

public class CalendarOptionsAction extends BaseActionCalendar {
    private static final String TAG = "Firebase.CalendarOptionsAction";

    private final String option;
    private final Integer msgId;

    public CalendarOptionsAction(Context context, int notificationId, String option, Integer msgId) {
        super(notificationId, context, "/sendInviteReply");

        this.option = option;
        this.msgId = msgId;
    }

    @Override
    public void run() {
        super.run();

        try {
            JSONObject postData = new JSONObject();
            postData.put("verb", option);
            postData.put("id", msgId);
            postData.put("updateOrganizer", "TRUE");

            Log.i(TAG, "postData : " + postData);

            HttpURLConnection urlConnection = createUrlConnection();

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

                saveOptionOnError(context, option, msgId);
            } else if (HttpURLConnection.HTTP_OK == statusCode) {
                // notify widget data set changed
                WidgetNotifier.updateCalendarWidgets(context);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getLocalizedMessage());
            saveOptionOnError(context, option, msgId);
        } finally {
            cancelNotification();
        }
    }

    private void saveOptionOnError(Context context, String option, Integer msgId) {
        Log.i(TAG, "saveOptionOnError, msgId: " + msgId + ", option: " + option);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "calendarOptionFailedRequests");

        ArrayList<OptionEntity> list;
        if (!TextUtils.isEmpty(data)) {
            Type type = new TypeToken<ArrayList<OptionEntity>>() {}.getType();
            list = gson.fromJson(data, type);
            if (list == null) {
                list = new ArrayList();
            }
        } else {
            list = new ArrayList();
        }
        OptionEntity oe = new OptionEntity(msgId, option);
        list.add(oe);
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "calendarOptionFailedRequests", json);
    }

    private class OptionEntity {
        Integer msgId;
        String option;

        public OptionEntity(Integer msgId, String option) {
            this.msgId = msgId;
            this.option = option;
        }
    }
}
