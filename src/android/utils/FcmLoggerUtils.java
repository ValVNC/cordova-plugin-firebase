package org.apache.cordova.firebase.utils;

import android.content.Context;
import android.util.Log;

import org.apache.cordova.firebase.actions.LogFCMPluginAction;

import java.util.concurrent.Executors;

public class FcmLoggerUtils {
    public static void logFcmReceived(Context context, String msgId){
        String appId = context.getApplicationContext().getPackageName();
        Log.i("FirebasePlugin.FcmLoggerUtils", "app ID: " + appId);

        if (!appId.equals("biz.vnc.vnctalk")) return;

        Executors.newSingleThreadExecutor().submit(new LogFCMPluginAction(context, msgId));
    }
}