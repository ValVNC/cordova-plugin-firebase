package org.apache.cordova.firebase.actions;

import android.content.Context;

import android.util.Log;

import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import org.json.JSONObject;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;


public class LogFCMPluginAction extends BaseAction {
    private static final String TAG = "Firebase.LogFCMPluginAction";

    private static final String TOKEN_CONSTANT = "auth-token";

    protected String authToken;
    protected String fcmToken;

    protected String msgId;

    public LogFCMPluginAction(Context context, String msgId) {
        super(0, context, "/receivedfcm");

        this.msgId = msgId;

        initTokens();
    }

    private void initTokens() {
        authToken = SharedPrefsUtils.getString(this.context, TOKEN_CONSTANT);
        fcmToken = FirebaseInstanceId.getInstance().getToken();
    }

    @Override
    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Authorization", authToken);
    }

    @Override
    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestMethod("POST");
    }

    @Override
    public void run() {
        Log.i(TAG, "Auth Token : " + authToken);
        Log.i(TAG, "FCM Token : " + fcmToken);
        Log.i(TAG, "Api Url : " + mApiUrl);

        super.run();

        try {
            JSONObject postData = new JSONObject();
            postData.put("msgid", msgId);
            postData.put("fcmtoken", fcmToken);

            Log.i(TAG, "postData : " + postData);

            HttpURLConnection urlConnection = createUrlConnection();

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }

            int statusCode = urlConnection.getResponseCode();
            Log.i(TAG, "Server response, statusCode: " + statusCode);
            if (statusCode != 200) {
                saveLogFcmError(context, msgId);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getLocalizedMessage());

            saveLogFcmError(context, msgId);
        }
    }

    private void saveLogFcmError(Context context, String msgId) {
        Log.i(TAG, "saveLogFcmError, msgId: " + msgId);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "logFcmFailedRequests");
        ArrayList<LogFcmErrorEntity> list = new ArrayList();
        if (data != null && !data.isEmpty()) {
            Type type = new TypeToken<ArrayList<LogFcmErrorEntity>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new LogFcmErrorEntity(msgId));
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "logFcmFailedRequests", json);
    }


    private class LogFcmErrorEntity {
        String msgId;
        long timestamp;

        public LogFcmErrorEntity(String msgId) {
            this.msgId = msgId;
            this.timestamp = new Date().getTime();
        }
    }
}

