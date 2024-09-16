package org.apache.cordova.firebase.actions;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class BaseActionTask extends BaseAction {
    private static final String TAG = "Firebase.BaseActionTask";

    private static final String API_KEY_CONSTANT = "redmine-api-key";

    protected String mApiKey;

    protected String taskId;

    BaseActionTask(String taskId, int notificationId, Context context, String urlSupplement) {
        super(notificationId, context, urlSupplement);

        initApiKey();

        this.taskId = taskId;
    }

    private void initApiKey() {
        mApiKey = SharedPrefsUtils.getString(this.context, API_KEY_CONSTANT);
    }

    @Override
    public void run() {
        Log.i(TAG, "apiKey : " + mApiKey);
        Log.i(TAG, "notificationId : " + notificationId);
        Log.i(TAG, "taskId : " + taskId);
        Log.i(TAG, "Api Url : " + mApiUrl);

        super.run();
    }

    @Override
    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {
        super.setRequestHeaders(urlConnection);

        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestProperty("Authorization", mApiKey);

        Log.i(TAG, "setRequestHeaders");
    }

    @Override
    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {
        super.setRequestMethod(urlConnection);

        urlConnection.setRequestMethod("PUT");
    }
}
