package org.apache.cordova.firebase.utils;

import android.content.Context;
import android.util.Log;

public class DateUtils {
    static final String TAG = "FirebasePlugin.DateUtils";

    public static long getCorrectedTime(Context context) {
        long currentTime = System.currentTimeMillis() / 1000;
        Log.i(TAG, "Device time = " + currentTime);

        try {
            int timeDiff = SharedPrefsUtils.getInt(context, "time-diff-interval");
            Log.i(TAG, "Time diff from shared = " + timeDiff);

            if (timeDiff != -1) {
                currentTime = currentTime - (timeDiff / 1000);
                Log.i(TAG, "Correct time, diff = " + timeDiff + ", correctedTime = " + currentTime);
            }
        } catch (Exception e){
            Log.w(TAG, "Error during parse diff: " + e.getMessage());
        }

        return currentTime;
    }
}