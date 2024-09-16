package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.util.Log;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.apache.cordova.firebase.utils.NotificationUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;

public class AcceptCallAction extends BaseActionTalk {
    private static final String TAG = "Firebase.AcceptCallAction";

    private String callId;
    private String callType;
    private String callReceiver;
    private boolean isGroupCall;

    public AcceptCallAction(Context context, String callId, String callType, String callReceiver, boolean isGroupCall) {
        super(null, null, callId.hashCode(), context, "/xmpp-rest");

        this.callId = callId;
        this.callType = callType;
        this.callReceiver = callReceiver;
        this.isGroupCall = isGroupCall;
    }

    @Override
    public void run() {
        super.run();

        notificationManager.cancel(NotificationUtils.generateCallNotificationId(callId));

        try {
            JSONObject postData = new JSONObject();
            postData.put("messagetext", "JOIN_CALL");
            postData.put("join", callType);
            postData.put("confid", prepareConfId(isGroupCall));
            postData.put("target", callId);

            Log.i(TAG, "'join' postData : " + postData);

            HttpURLConnection urlConnection = createUrlConnection();

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }

            int statusCode = urlConnection.getResponseCode();
            Log.i(TAG, "Server response on 'join', statusCode: " + statusCode);

            if (statusCode != 200) {
                Log.w(TAG, "Server response  on 'join', message: " + urlConnection.getResponseMessage());
                
                StringBuilder sb = new StringBuilder();
                InputStreamReader in = new InputStreamReader(urlConnection.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader != null) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
                in.close();
                Log.w(TAG, "Server response  on 'join', Error: " + sb.toString());

                saveAcceptCallOnError(context, callId);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getLocalizedMessage());

            saveAcceptCallOnError(context, callId);
        }
        
        try {
            JSONObject postData = new JSONObject();
            postData.put("messagetext", "JOIN_CALL");
            postData.put("join", callType);
            postData.put("self", 1);
            postData.put("confid", prepareConfId(isGroupCall));
            postData.put("target", callReceiver);

            Log.i(TAG, "'join-self' postData : " + postData);

            HttpURLConnection urlConnection = createUrlConnection();

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }

            int statusCode = urlConnection.getResponseCode();
            Log.i(TAG, "Server response on 'join-self', statusCode: " + statusCode);

            if (statusCode != 200) {
                Log.w(TAG, "Server response on 'join-self', message: " + urlConnection.getResponseMessage());
                
                StringBuilder sb = new StringBuilder();
                InputStreamReader in = new InputStreamReader(urlConnection.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader != null) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
                in.close();
                Log.w(TAG, "Server response on 'join-self', Error: " + sb.toString());

                saveAcceptCallOnError(context, callId);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getLocalizedMessage());

            saveAcceptCallOnError(context, callId);
        }
    }

    private String prepareConfId(boolean isGroupCall){
        if (isGroupCall) return callId;

        StringBuilder sb = new StringBuilder();
        sb.append(callReceiver.replace("@", "#")).append(",");
        sb.append(callId.replace("@", "#"));

        return sb.toString();
    }

    private void saveAcceptCallOnError(Context context, String callId) {
        Log.i(TAG, "saveAcceptCallOnError, callId: " + callId);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "acceptCallFailedRequests");
        ArrayList<AcceptCallErrorEntity> list = new ArrayList();
        if (!TextUtils.isEmpty(data)) {
            Type type = new TypeToken<ArrayList<AcceptCallErrorEntity>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new AcceptCallErrorEntity(callId));
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "acceptCallFailedRequests", json);
    }

    private class AcceptCallErrorEntity {
        String callId;
        long timestamp;

        public AcceptCallErrorEntity(String callId) {
            this.callId = callId;
            this.timestamp = new Date().getTime();
        }
    }
}
