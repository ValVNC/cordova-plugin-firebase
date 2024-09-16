package org.apache.cordova.firebase.actions;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

public abstract class BaseAction implements Runnable {
    protected Context context;
    int notificationId;
    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;

    String mApiUrl;
    private static final String API_URL = "apiUrl";

    BaseAction(int notificationId, Context context, String urlSupplement) {
        this.context = context;
        this.notificationId = notificationId;

        chooseAndSetApiUrl(urlSupplement);
        initNotificationObjects();
    }

    private void chooseAndSetApiUrl(String urlSupplement) {
        mApiUrl = baseApiUrl() + urlSupplement;
    }

    private void initNotificationObjects() {
        notificationManager = NotificationUtils.getManager(context);
        notificationBuilder = NotificationUtils.getBuilder(context);
    }

    String baseApiUrl() {
        return SharedPrefsUtils.getString(this.context, API_URL);
    }

    @Override
    public void run() {

    }

    HttpURLConnection createUrlConnection() throws IOException {
        URL url = new URL(mApiUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        
        setRequestHeaders(urlConnection);
        setRequestMethod(urlConnection);

        return urlConnection;
    }

    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {

    }

    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {

    }

    protected boolean isInternetAvailable() {
        try {
            InetAddress ipAddress = InetAddress.getByName("google.com");
            Log.i("BaseAction", "[isInternetAvailable] ipAddress = " + ipAddress == null ? "null" : ipAddress.toString());
            return ipAddress != null && !ipAddress.equals("");
        } catch (Exception e) {
            Log.e("BaseAction", "[isInternetAvailable] Exception " + e);
            return false;
        }
    }

    protected void showToast(CharSequence message, boolean isDurationShort) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, isDurationShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
    }
}
