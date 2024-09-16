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


public abstract class BaseActionTalk extends BaseAction {
    private static final String TAG = "Firebase.BaseActionTalk";

    private static final String TOKEN_CONSTANT = "auth-token";

    protected String mToken;

    protected String body;
    protected String sender;

    BaseActionTalk(String body, String sender, int notificationId, Context context, String urlSupplement) {
        super(notificationId, context, urlSupplement);

        initToken();

        this.body = body;
        this.sender = sender;
    }

    private void initToken() {
        mToken = SharedPrefsUtils.getString(this.context, TOKEN_CONSTANT);
    }

    @Override
    public void run() {
        Log.i(TAG, "Token : " + mToken);
        Log.i(TAG, "notificationId : " + notificationId);
        Log.i(TAG, "To : " + sender);
        Log.i(TAG, "Message body : " + body);
        Log.i(TAG, "Api Url : " + mApiUrl);

        super.run();
    }

    @Override
    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Authorization", mToken);
    }

    @Override
    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestMethod("POST");
    }
}
