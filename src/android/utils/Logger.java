package org.apache.cordova.firebase.utils;

import android.util.Log;

/**
 * Created by deslant on 06.06.2017.
 */

public class Logger {

    public final static String INFO_LOG_TAG = "vncTalk";

    public static void i(String msg) {

        if (msg.length() > 1000) {
            int maxLogSize = 1000;
            for (int i = 0; i <= msg.length() / maxLogSize; i++) {
                int start = i * maxLogSize;
                int end = (i + 1) * maxLogSize;
                end = end > msg.length() ? msg.length() : end;
                Log.i(INFO_LOG_TAG, msg.substring(start, end));
            }
        } else {
            Log.i(INFO_LOG_TAG, msg);
        }

    }

}
