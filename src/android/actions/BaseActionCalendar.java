package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.util.Log;

import org.apache.cordova.firebase.notification.NotificationManager;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.io.IOException;
import java.net.HttpURLConnection;


public abstract class BaseActionCalendar extends BaseAction {
    private static final String TAG = "Firebase.BaseActionCalendar";

    private static final String TOKEN_CONSTANT = "auth-token";

    protected String mToken;

    BaseActionCalendar(int notificationId, Context context, String urlSupplement) {
        super(notificationId, context, urlSupplement);

        initToken();
    }

    private void initToken() {
        mToken = SharedPrefsUtils.getString(this.context, TOKEN_CONSTANT);
    }

    @Override
    public void run() {
        Log.i(TAG, "Token : " + mToken);
        Log.i(TAG, "NotificationId : " + notificationId);
        Log.i(TAG, "Api Url : " + mApiUrl);

        super.run();
    }

    @Override
    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestProperty("Authorization", mToken);
    }

    @Override
    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestMethod("POST");
    }

    protected void cancelNotification(){
        notificationManager.cancel(notificationId);
        NotificationManager.hideCalendarSummaryNotificationIfNeed(context, notificationManager);
    }
}
