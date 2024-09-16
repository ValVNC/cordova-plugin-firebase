package org.apache.cordova.firebase;


import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigInfo;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import android.service.notification.StatusBarNotification;

import me.leolin.shortcutbadger.ShortcutBadger;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.notification.NotificationManager;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

// import io.sentry.Sentry;
import io.fabric.sdk.android.Fabric;

public class FirebasePlugin extends CordovaPlugin {

    private FirebaseAnalytics mFirebaseAnalytics;
    private static CordovaWebView appView;
    private final String TAG = "FirebasePlugin";
    private final String ERRORINITFIREBASE = "Firebase isn't initialised";
    private final String ERRORINITCRASHLYTICS = "Crashlytics isn't initialised";
    private final String ERRORINITANALYTICS = "Analytics isn't initialised";
    private final String ERRORINITPERFORMANCE = "Performance isn't initialised";
    // private static final String SENTRY_URL = "https://6d65e128f84b474c83c7004445176498@sentry2.vnc.biz/2";
    protected static final String KEY = "badge";

    private static boolean crashlyticsInit = true; // enable by default
    private static boolean analyticsInit = false;
    private static boolean performanceInit = false;
    private static boolean inBackground = true;
    private static ArrayList<Bundle> notificationStack = null;
    private static CallbackContext notificationOpenCallbackContext;
    private static CallbackContext tokenRefreshCallbackContext;
    private static CallbackContext notificationMarkAsReadCallbackContext;
    private static CallbackContext notificationReceivedCallbackContext;

    @Override
    protected void pluginInitialize() {
        final Context context = this.cordova.getActivity().getApplicationContext();
        final Bundle extras = this.cordova.getActivity().getIntent().getExtras();
        // try {
        //     Sentry.init(SENTRY_URL);
        //     Sentry.capture("Init Sentry");
        // } catch (Exception e) {
        //     Log.d(TAG, "Init sentry exception" + e.getMessage());
        // }

        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Log.d(TAG, "Starting Firebase plugin");
                // Sentry.capture("Starting Firebase plugin");
                FirebaseApp.initializeApp(context);
                try {
                    Fabric.with(context, new Crashlytics());
                    FirebasePlugin.crashlyticsInit = true;
                    Crashlytics.logException(new Exception("init Fabric when app is closed"));
                } catch(Exception e) {
                    Log.d(TAG, "Init Fabric exception" + e.getMessage());
                }
                if (extras != null && extras.size() > 1) {
                    if (FirebasePlugin.notificationStack == null) {
                        FirebasePlugin.notificationStack = new ArrayList<Bundle>();
                    }
                    extras.putBoolean("tap", true);
                    notificationStack.add(extras);
                }
            }
        });

        if (extras != null && extras.containsKey(NotificationUtils.EXTRA_CALL_ACTION)){
            String callAction = extras.getString(NotificationUtils.EXTRA_CALL_ACTION);
            if (!TextUtils.isEmpty(callAction) && NotificationCreator.TALK_CALL_ACCEPT.equals(callAction)){
                this.cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initCrashlytics")) {
            this.initCrashlytics(callbackContext);
            return true;
        } else if (action.equals("initAnalytics")) {
            this.initAnalytics(callbackContext);
            return true;
        } else if (action.equals("initPerformance")) {
            this.initPerformance(callbackContext);
            return true;
        } else if (action.equals("getInstanceId")) {
            this.getInstanceId(callbackContext);
            return true;
        } else if (action.equals("getId")) {
            this.getId(callbackContext);
            return true;
        } else if (action.equals("getToken")) {
            this.getToken(callbackContext);
            return true;
        } else if (action.equals("hasPermission")) {
            this.hasPermission(callbackContext);
            return true;
        } else if (action.equals("setBadgeNumber")) {
            this.setBadgeNumber(callbackContext, args.getInt(0));
            return true;
        } else if (action.equals("getBadgeNumber")) {
            this.getBadgeNumber(callbackContext);
            return true;
        } else if (action.equals("subscribe")) {
            this.subscribe(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("unsubscribe")) {
            this.unsubscribe(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("unregister")) {
            this.unregister(callbackContext);
            return true;
        } else if (action.equals("onNotificationOpen")) {
            this.onNotificationOpen(callbackContext);
            return true;
        } else if (action.equals("onNotificationReceived")) {
            this.onNotificationReceived(callbackContext);
            return true;
        } else if (action.equals("onNotificationMarkAsRead")) {
            this.onNotificationMarkAsRead(callbackContext);
            return true;
        } else if (action.equals("onTokenRefresh")) {
            this.onTokenRefresh(callbackContext);
            return true;
        } else if (action.equals("logEvent")) {
            this.logEvent(callbackContext, args.getString(0), args.getJSONObject(1));
            return true;
        } else if (action.equals("logError")) {
            this.logError(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setScreenName")) {
            this.setScreenName(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setUserId")) {
            this.setUserId(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setUserProperty")) {
            this.setUserProperty(callbackContext, args.getString(0), args.getString(1));
            return true;
        } else if (action.equals("activateFetched")) {
            this.activateFetched(callbackContext);
            return true;
        } else if (action.equals("fetch")) {
            if (args.length() > 0) {
                this.fetch(callbackContext, args.getLong(0));
            } else {
                this.fetch(callbackContext);
            }
            return true;
        // } else if (action.equals("getByteArray")) {
        //     if (args.length() > 1) {
        //         this.getByteArray(callbackContext, args.getString(0), args.getString(1));
        //     } else {
        //         this.getByteArray(callbackContext, args.getString(0), null);
        //     }
        //     return true;
        // } else if (action.equals("getValue")) {
        //     if (args.length() > 1) {
        //         this.getValue(callbackContext, args.getString(0), args.getString(1));
        //     } else {
        //         this.getValue(callbackContext, args.getString(0), null);
        //     }
        //     return true;
        } else if (action.equals("getInfo")) {
            this.getInfo(callbackContext);
            return true;
        } else if (action.equals("setConfigSettings")) {
            this.setConfigSettings(callbackContext, args.getJSONObject(0));
            return true;
        // } else if (action.equals("setDefaults")) {
        //     if (args.length() > 1) {
        //         this.setDefaults(callbackContext, args.getJSONObject(0), args.getString(1));
        //     } else {
        //         this.setDefaults(callbackContext, args.getJSONObject(0), null);
        //     }
        //     return true;
        } else if (action.equals("verifyPhoneNumber")) {
            this.verifyPhoneNumber(callbackContext, args.getString(0), args.getInt(1));
            return true;
        } else if (action.equals("startTrace")) {
            this.startTrace(callbackContext, args.getString(0));
            return true;
        // } else if (action.equals("incrementCounter")) {
        //     this.incrementCounter(callbackContext, args.getString(0), args.getString(1));
        //     return true;
        } else if (action.equals("stopTrace")) {
            this.stopTrace(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setAnalyticsCollectionEnabled")) {
            this.setAnalyticsCollectionEnabled(callbackContext, args.getBoolean(0));
            return true;
        } else if (action.equals("setPerformanceCollectionEnabled")) {
            this.setPerformanceCollectionEnabled(callbackContext, args.getBoolean(0));
            return true;
        } else if (action.equals("clearAllNotifications")) {
            this.clearAllNotifications(callbackContext);
            return true;
        } else if (action.equals("clearMailNotification")) {
            this.clearMailNotification(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clearAllMailNotificationsForConv")) {
            this.clearAllMailNotificationsForConv(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clearMailNotificationsExceptMids")) {
            this.clearMailNotificationsExceptMids(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clearMailNotificationsExceptCids")) {
            this.clearMailNotificationsExceptCids(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clear")) {
            this.clear(callbackContext, args.getInt(0));
            return true;
        } else if (action.equals("scheduleLocalNotification")) {
            this.scheduleLocalNotification(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("getActiveIdsByTarget")) {
            this.getActiveIdsByTarget(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clearNotificationsByTarget")) {
            this.clearNotificationsByTarget(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clearTalkNotificationsExceptTargets")) {
            this.clearTalkNotificationsExceptTargets(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("clearTalkNotificationsExceptTargetsAndMissedCalls")) {
            this.clearTalkNotificationsExceptTargetsAndMissedCalls(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("scheduleLocalMailNotification")) {
            this.scheduleLocalMailNotification(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("scheduleCallNotification")) {
            this.scheduleCallNotification(callbackContext, args.getJSONObject(0));
            return true;
        }  else if (action.equals("enableLockScreenVisibility")) {
            this.enableLockScreenVisibility(callbackContext, args.getBoolean(0));
            return true;
        }  else if (action.equals("hideIncomingCallNotification")) {
            this.hideIncomingCallNotification(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("displayMissedCallNotification")) {
            this.displayMissedCallNotification(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("scheduleCalendarNotification")) {
            this.scheduleCalendarNotification(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("clearNotificationByAppointmentId")) {
            this.clearNotificationByAppointmentId(callbackContext, args.getString(0));
            return true;
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        FirebasePlugin.inBackground = true;
    }

    @Override
    public void onResume(boolean multitasking) {
        FirebasePlugin.inBackground = false;
    }

    @Override
    public void onReset() {
        FirebasePlugin.notificationOpenCallbackContext = null;
        FirebasePlugin.tokenRefreshCallbackContext = null;
        FirebasePlugin.notificationMarkAsReadCallbackContext = null;
        FirebasePlugin.notificationReceivedCallbackContext = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (this.appView != null) {
            appView.handleDestroy();
        }
    }

    private void initCrashlytics(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Crashlytics");
        try {
            Fabric.with(context, new Crashlytics());
            FirebasePlugin.crashlyticsInit = true;
            callbackContext.success();
        } catch (Exception e) {
            callbackContext.error(ERRORINITCRASHLYTICS);
        }
    }

    private void initAnalytics(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Analytics");
        try {
          mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
          mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
          FirebasePlugin.analyticsInit = true;
          callbackContext.success();
        } catch(Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(ERRORINITANALYTICS);
        }
    }

    private android.app.NotificationManager getNotMgr() {
        final Context context = this.cordova.getActivity().getApplicationContext();
        return (android.app.NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public void getActiveIdsByTarget(final CallbackContext callbackContext, final String target) {
        Log.d(TAG, "getActiveIdsByTarget: " + target);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    StatusBarNotification[] activeToasts = getActiveNotifications();
                    List<Integer> activeIds              = new ArrayList<Integer>();
                    for (StatusBarNotification toast : activeToasts) {
                        Notification curNotif = toast.getNotification();
                        Bundle bundle = curNotif.extras;
                        String currentTarget = bundle.getString("messageTarget");
                        if (currentTarget != null && currentTarget.equals(target)) {
                            activeIds.add(toast.getId());
                        }
                    }
                    JSONObject info = new JSONObject();
                    info.put("ids", activeIds);
                    callbackContext.success(info);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearNotificationsByTarget(final CallbackContext callbackContext, final String target) {
        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearNotificationsByTarget: " + target);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideNotificationsForTarget(context, target);
                    callbackContext.success(target);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearTalkNotificationsExceptTargets(final CallbackContext callbackContext, final String targets) {
        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearTalkNotificationsExceptTargets: " + targets);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideNotificationsExceptTargets(context, Arrays.asList(targets.split(",")));
                    callbackContext.success(targets);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearTalkNotificationsExceptTargetsAndMissedCalls(final CallbackContext callbackContext, final String targets) {
        Log.d(TAG, "clearTalkNotificationsExceptTargetsAndMissedCalls: " + targets);
        
        final Context context = this.cordova.getActivity().getApplicationContext();
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideNotificationsExceptTargetsAndMissedCalls(context, Arrays.asList(targets.split(",")));
                    callbackContext.success(targets);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    StatusBarNotification[] getActiveNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return getNotMgr().getActiveNotifications();
        } else {
            return new StatusBarNotification[0];
        }
    }


    private void initPerformance(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Performance");
        try {
          FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
          FirebasePlugin.performanceInit = true;
          callbackContext.success();
        } catch(Exception e) {
          if (FirebasePlugin.isCrashlyticsEnabled()) {
            Crashlytics.logException(e);
          }
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    private void onNotificationOpen(final CallbackContext callbackContext) {
        FirebasePlugin.notificationOpenCallbackContext = callbackContext;
        if (FirebasePlugin.notificationStack != null) {
            for (Bundle bundle : FirebasePlugin.notificationStack) {
                FirebasePlugin.sendNotification(bundle, this.cordova.getActivity().getApplicationContext());
            }
            FirebasePlugin.notificationStack.clear();
        }
    }

    private void onNotificationMarkAsRead(final CallbackContext callbackContext) {
        FirebasePlugin.notificationMarkAsReadCallbackContext = callbackContext;
    }

    private void onNotificationReceived(final CallbackContext callbackContext) {
        FirebasePlugin.notificationReceivedCallbackContext = callbackContext;
    }

    private void onTokenRefresh(final CallbackContext callbackContext) {
        FirebasePlugin.tokenRefreshCallbackContext = callbackContext;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Log.d(TAG, "onTokenRefresh");
                try {
                    String currentToken = FirebaseInstanceId.getInstance().getToken();
                    if (currentToken != null) {
                        Log.d(TAG, "onTokenRefresh, current token = " + currentToken);
                        FirebasePlugin.sendToken(currentToken);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "onTokenRefresh, error " + e.getMessage());
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public static void sendNotification(Bundle bundle, Context context) {
        if (!FirebasePlugin.hasNotificationsOpenCallback()) {
            String packageName = context.getPackageName();
            if (FirebasePlugin.notificationStack == null) {
                FirebasePlugin.notificationStack = new ArrayList<Bundle>();
            }
            notificationStack.add(bundle);

            return;
        }
        final CallbackContext callbackContext = FirebasePlugin.notificationOpenCallbackContext;
        if (callbackContext != null && bundle != null) {
            JSONObject json = new JSONObject();
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                try {
                    json.put(key, bundle.get(key));
                } catch (JSONException e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                    return;
                }
            }

            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, json);
            pluginresult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginresult);
        }
    }

    public static void sendNotificationMarkAsRead(Bundle bundle) {
        final CallbackContext callbackContext = FirebasePlugin.notificationMarkAsReadCallbackContext;

        if (callbackContext == null || bundle == null) {
            return;
        }

        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                json.put(key, bundle.get(key));
            } catch (JSONException e) {
                if (FirebasePlugin.isCrashlyticsEnabled()) {
                    Crashlytics.logException(e);
                }
                callbackContext.error(e.getMessage());
                return;
            }
        }

        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, json);
        pluginresult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginresult);
    }

    public static void sendNotificationReceived(Bundle bundle) {
        final CallbackContext callbackContext = FirebasePlugin.notificationReceivedCallbackContext;

        Log.i("FirebasePlugin", "sendNotificationReceived: " + bundle + ", " + callbackContext);

        if(callbackContext == null || bundle == null){
            return;
        }

        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                json.put(key, bundle.get(key));
            } catch (JSONException e) {
                if (FirebasePlugin.isCrashlyticsEnabled()) {
                  Crashlytics.logException(e);
                }
                callbackContext.error(e.getMessage());
                return;
            }
        }

        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, json);
        pluginresult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginresult);
    }

    public static void sendToken(String token) {
        Log.d("FirebasePlugin", "sendToken");
        if (FirebasePlugin.tokenRefreshCallbackContext == null) {
            return;
        }

        final CallbackContext callbackContext = FirebasePlugin.tokenRefreshCallbackContext;
        if (callbackContext != null && token != null) {
            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, token);
            pluginresult.setKeepCallback(true);
            Log.d("FirebasePlugin", "sendToken to app"); 
            callbackContext.sendPluginResult(pluginresult);
        }
    }

    public static boolean inBackground() {
        return FirebasePlugin.inBackground;
    }

    public static boolean isCrashlyticsEnabled() {
        return FirebasePlugin.crashlyticsInit;
    }

    public static boolean analyticsInit() {
        return FirebasePlugin.analyticsInit;
    }

    public static boolean performanceInit() {
        return FirebasePlugin.performanceInit;
    }

    public static boolean hasNotificationsOpenCallback() {
        return FirebasePlugin.notificationOpenCallbackContext != null;
    }

    public static boolean hasNotificationsMarkAsReadCallback() {
        return FirebasePlugin.notificationMarkAsReadCallbackContext != null;
    }

    public static boolean hasNotificationsReceivedCallback() {
        return FirebasePlugin.notificationReceivedCallbackContext != null;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final Bundle data = intent.getExtras();
        if (data != null && data.containsKey("google.message_id")) {
            data.putBoolean("tap", true);
            FirebasePlugin.sendNotification(data, this.cordova.getActivity().getApplicationContext());
        }
    }

    // DEPRECTED - alias of getToken
    private void getInstanceId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    String token = FirebaseInstanceId.getInstance().getToken();
                    callbackContext.success(token);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    String id = FirebaseInstanceId.getInstance().getId();
                    callbackContext.success(id);
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getToken(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    String token = FirebaseInstanceId.getInstance().getToken();
                    callbackContext.success(token);
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void hasPermission(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                    boolean areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
                    JSONObject object = new JSONObject();
                    object.put("isEnabled", areNotificationsEnabled);
                    callbackContext.success(object);
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                       Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setBadgeNumber(final CallbackContext callbackContext, final int number) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    SharedPrefsUtils.putInt(context, KEY, KEY, number);
                    ShortcutBadger.applyCount(context, number);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getBadgeNumber(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    int number = SharedPrefsUtils.getInt(cordova.getActivity(), KEY, KEY);
                    callbackContext.success(number);
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void subscribe(final CallbackContext callbackContext, final String topic) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic(topic);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void unsubscribe(final CallbackContext callbackContext, final String topic) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void unregister(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseInstanceId.getInstance().deleteInstanceId();
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void logEvent(final CallbackContext callbackContext, final String name, final JSONObject params)
            throws JSONException {
        final Bundle bundle = new Bundle();
        Iterator iter = params.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Object value = params.get(key);

            if (value instanceof Integer || value instanceof Double) {
                bundle.putFloat(key, ((Number) value).floatValue());
            } else {
                bundle.putString(key, value.toString());
            }
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if (FirebasePlugin.analyticsInit()) {
                        mFirebaseAnalytics.logEvent(name, bundle);
                        callbackContext.success();
                    } else {
                        callbackContext.error(ERRORINITANALYTICS);
                    }
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void logError(final CallbackContext callbackContext, final String message) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(new Exception(message));
                      callbackContext.success(1);
                    } else {
                        callbackContext.error(ERRORINITCRASHLYTICS);
                    }
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.log(e.getMessage());
                    }
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setScreenName(final CallbackContext callbackContext, final String name) {
        // This must be called on the main thread
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (FirebasePlugin.analyticsInit()) {
                        mFirebaseAnalytics.setCurrentScreen(cordova.getActivity(), name, null);
                        callbackContext.success();
                    } else {
                        callbackContext.error(ERRORINITANALYTICS);
                    }
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setUserId(final CallbackContext callbackContext, final String id) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if (FirebasePlugin.analyticsInit()) {
                        mFirebaseAnalytics.setUserId(id);
                        callbackContext.success();
                    } else {
                        callbackContext.error(ERRORINITANALYTICS);
                    }
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setUserProperty(final CallbackContext callbackContext, final String name, final String value) {
      if(FirebasePlugin.analyticsInit()){
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    mFirebaseAnalytics.setUserProperty(name, value);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                }
            }
        });
      } else {
            callbackContext.error(ERRORINITANALYTICS);
      }
    }

    private void activateFetched(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    final boolean activated = FirebaseRemoteConfig.getInstance().activateFetched();
                    callbackContext.success(String.valueOf(activated));
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void fetch(CallbackContext callbackContext) {
        fetch(callbackContext, FirebaseRemoteConfig.getInstance().fetch());
    }

    private void fetch(CallbackContext callbackContext, long cacheExpirationSeconds) {
        fetch(callbackContext, FirebaseRemoteConfig.getInstance().fetch(cacheExpirationSeconds));
    }

    private void fetch(final CallbackContext callbackContext, final Task<Void> task) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    task.addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            callbackContext.success();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            if (FirebasePlugin.isCrashlyticsEnabled()) {
                              Crashlytics.logException(e);
                            }
                            callbackContext.error(e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    // private void getByteArray(final CallbackContext callbackContext, final String key, final String namespace) {
    //   cordova.getThreadPool().execute(new Runnable() {
    //     public void run() {
    //         try {
    //             byte[] bytes = namespace == null ? FirebaseRemoteConfig.getInstance().getByteArray(key)
    //                     : FirebaseRemoteConfig.getInstance().getByteArray(key, namespace);
    //             JSONObject object = new JSONObject();
    //             object.put("base64", Base64.encodeToString(bytes, Base64.DEFAULT));
    //             object.put("array", new JSONArray(bytes));
    //             callbackContext.success(object);
    //         } catch (Exception e) {
    //             if (FirebasePlugin.isCrashlyticsEnabled()) {
    //                 Crashlytics.logException(e);
    //             }
    //             callbackContext.error(e.getMessage());
    //         }
    //     }
    //   });
    // }

    // private void getValue(final CallbackContext callbackContext, final String key, final String namespace) {
    //   cordova.getThreadPool().execute(new Runnable() {
    //     public void run() {
    //         try {
    //             FirebaseRemoteConfigValue value = namespace == null
    //                     ? FirebaseRemoteConfig.getInstance().getValue(key)
    //                     : FirebaseRemoteConfig.getInstance().getValue(key, namespace);
    //             callbackContext.success(value.asString());
    //         } catch (Exception e) {
    //             if (FirebasePlugin.isCrashlyticsEnabled()) {
    //                 Crashlytics.logException(e);
    //             }
    //             callbackContext.error(e.getMessage());
    //         }
    //     }
    //   });
    // }

    private void getInfo(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseRemoteConfigInfo remoteConfigInfo = FirebaseRemoteConfig.getInstance().getInfo();
                    JSONObject info = new JSONObject();

                    JSONObject settings = new JSONObject();
                    settings.put("developerModeEnabled", remoteConfigInfo.getConfigSettings().isDeveloperModeEnabled());
                    info.put("configSettings", settings);

                    info.put("fetchTimeMillis", remoteConfigInfo.getFetchTimeMillis());
                    info.put("lastFetchStatus", remoteConfigInfo.getLastFetchStatus());

                    callbackContext.success(info);
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setConfigSettings(final CallbackContext callbackContext, final JSONObject config) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    boolean devMode = config.getBoolean("developerModeEnabled");
                    FirebaseRemoteConfigSettings.Builder settings = new FirebaseRemoteConfigSettings.Builder()
                            .setDeveloperModeEnabled(devMode);
                    FirebaseRemoteConfig.getInstance().setConfigSettings(settings.build());
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    // private void setDefaults(final CallbackContext callbackContext, final JSONObject defaults, final String namespace) {
    //     cordova.getThreadPool().execute(new Runnable() {
    //         public void run() {
    //             try {
    //                 if (namespace == null)
    //                     FirebaseRemoteConfig.getInstance().setDefaults(defaultsToMap(defaults));
    //                 else
    //                     FirebaseRemoteConfig.getInstance().setDefaults(defaultsToMap(defaults), namespace);
    //                 callbackContext.success();
    //             } catch (Exception e) {
    //                 if (FirebasePlugin.isCrashlyticsEnabled()) {
    //                   Crashlytics.logException(e);
    //                 }
    //                 callbackContext.error(e.getMessage());
    //             }
    //         }
    //     });
    // }

    private static Map<String, Object> defaultsToMap(JSONObject object) throws JSONException {
        final Map<String, Object> map = new HashMap<String, Object>();

        for (Iterator<String> keys = object.keys(); keys.hasNext(); ) {
            String key = keys.next();
            Object value = object.get(key);

            if (value instanceof Integer) {
                //setDefaults() should take Longs
                value = new Long((Integer) value);
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                if (array.length() == 1 && array.get(0) instanceof String) {
                    //parse byte[] as Base64 String
                    value = Base64.decode(array.getString(0), Base64.DEFAULT);
                } else {
                    //parse byte[] as numeric array
                    byte[] bytes = new byte[array.length()];
                    for (int i = 0; i < array.length(); i++)
                        bytes[i] = (byte) array.getInt(i);
                    value = bytes;
                }
            }

            map.put(key, value);
        }
        return map;
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    public void verifyPhoneNumber(
            final CallbackContext callbackContext,
            final String number,
            final int timeOutDuration
    ) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential credential) {
                            // This callback will be invoked in two situations:
                            // 1 - Instant verification. In some cases the phone number can be instantly
                            //     verified without needing to send or enter a verification code.
                            // 2 - Auto-retrieval. On some devices Google Play services can automatically
                            //     detect the incoming verification SMS and perform verificaiton without
                            //     user action.
                            Log.d(TAG, "success: verifyPhoneNumber.onVerificationCompleted - callback and create a custom JWT Token on server and sign in with custom token - we cant do anything");

                            JSONObject returnResults = new JSONObject();
                            try {
                                returnResults.put("verificationId", false);
                                returnResults.put("instantVerification", true);
                            } catch (JSONException e) {
                                if (FirebasePlugin.isCrashlyticsEnabled()) {
                                  Crashlytics.logException(e);
                                }
                                callbackContext.error(e.getMessage());
                                return;
                            }
                            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, returnResults);
                            pluginresult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginresult);
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            // This callback is invoked in an invalid request for verification is made,
                            // for instance if the the phone number format is not valid.
                            Log.w(TAG, "failed: verifyPhoneNumber.onVerificationFailed ", e);

                            String errorMsg = "unknown error verifying number";
                            errorMsg += " Error instance: " + e.getClass().getName();

                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                // Invalid request
                                errorMsg = "Invalid phone number";
                            } else if (e instanceof FirebaseTooManyRequestsException) {
                                // The SMS quota for the project has been exceeded
                                errorMsg = "The SMS quota for the project has been exceeded";
                            }

                            if (FirebasePlugin.isCrashlyticsEnabled()) {
                              Crashlytics.logException(e);
                            }
                            callbackContext.error(errorMsg);
                        }

                        @Override
                        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                            // The SMS verification code has been sent to the provided phone number, we
                            // now need to ask the user to enter the code and then construct a credential
                            // by combining the code with a verification ID [(in app)].
                            Log.d(TAG, "success: verifyPhoneNumber.onCodeSent");

                            JSONObject returnResults = new JSONObject();
                            try {
                                returnResults.put("verificationId", verificationId);
                                returnResults.put("instantVerification", false);
                            } catch (JSONException e) {
                                if (FirebasePlugin.isCrashlyticsEnabled()) {
                                  Crashlytics.logException(e);
                                }
                                callbackContext.error(e.getMessage());
                                return;
                            }
                            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, returnResults);
                            pluginresult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginresult);
                        }
                    };

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(number, // Phone number to verify
                            timeOutDuration, // Timeout duration
                            TimeUnit.SECONDS, // Unit of timeout
                            cordova.getActivity(), // Activity (for callback binding)
                            mCallbacks); // OnVerificationStateChangedCallbacks
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    //
    // Firebase Performace
    //

    private HashMap<String, Trace> traces = new HashMap<String, Trace>();

    private void startTrace(final CallbackContext callbackContext, final String name) {
        final FirebasePlugin self = this;
        if(FirebasePlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {

                      Trace myTrace = null;
                      if (self.traces.containsKey(name)) {
                          myTrace = self.traces.get(name);
                      }

                      if (myTrace == null) {
                          myTrace = FirebasePerformance.getInstance().newTrace(name);
                          myTrace.start();
                          self.traces.put(name, myTrace);
                      }

                      callbackContext.success();
                  } catch (Exception e) {
                      if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.logException(e);
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    // private void incrementCounter(final CallbackContext callbackContext, final String name, final String counterNamed) {
    //     final FirebasePlugin self = this;
    //     if(FirebasePlugin.performanceInit()){
    //       cordova.getThreadPool().execute(new Runnable() {
    //           public void run() {
    //               try {
    //
    //                   Trace myTrace = null;
    //                   if (self.traces.containsKey(name)) {
    //                       myTrace = self.traces.get(name);
    //                   }
    //
    //                   if (myTrace != null && myTrace instanceof Trace) {
    //                       myTrace.incrementCounter(counterNamed);
    //                       callbackContext.success();
    //                   } else {
    //                       callbackContext.error("Trace not found");
    //                   }
    //               } catch (Exception e) {
    //                   if (FirebasePlugin.isCrashlyticsEnabled()) {
    //                     Crashlytics.logException(e);
    //                   }
    //                   e.printStackTrace();
    //                   callbackContext.error(e.getMessage());
    //               }
    //           }
    //       });
    //     } else {
    //       callbackContext.error(ERRORINITPERFORMANCE);
    //     }
    // }

    // private void incrementCounter(final CallbackContext callbackContext, final String name, final String counterNamed) {
    //     final FirebasePlugin self = this;
    //     if (FirebasePlugin.performanceInit()) {
    //         cordova.getThreadPool().execute(new Runnable() {
    //             public void run() {
    //                 try {
    //
    //                     Trace myTrace = null;
    //                     if (self.traces.containsKey(name)) {
    //                         myTrace = self.traces.get(name);
    //                     }
    //
    //                     if (myTrace != null && myTrace instanceof Trace) {
    //                         myTrace.incrementCounter(counterNamed);
    //                         callbackContext.success();
    //                     } else {
    //                         callbackContext.error("Trace not found");
    //                     }
    //                 } catch (Exception e) {
    //                     if (FirebasePlugin.crashlyticsInit()) {
    //                         Crashlytics.logException(e);
    //                     }
    //                     e.printStackTrace();
    //                     callbackContext.error(e.getMessage());
    //                 }
    //             }
    //         });
    //     } else {
    //         callbackContext.error(ERRORINITPERFORMANCE);
    //     }
    // }

    private void stopTrace(final CallbackContext callbackContext, final String name) {
        final FirebasePlugin self = this;
        if(FirebasePlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {

                      Trace myTrace = null;
                      if (self.traces.containsKey(name)) {
                          myTrace = self.traces.get(name);
                      }

                      if (myTrace != null && myTrace instanceof Trace) { //
                          myTrace.stop();
                          self.traces.remove(name);
                          callbackContext.success();
                      } else {
                          callbackContext.error("Trace not found");
                      }
                  } catch (Exception e) {
                      if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.logException(e);
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
            callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    private void setAnalyticsCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
        final FirebasePlugin self = this;
        if(FirebasePlugin.analyticsInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {
                      mFirebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
                      callbackContext.success();
                  } catch (Exception e) {
                      if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
            callbackContext.error(ERRORINITANALYTICS);
        }
    }

    private void setPerformanceCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
        final FirebasePlugin self = this;
        if(FirebasePlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {
                      FirebasePerformance.getInstance().setPerformanceCollectionEnabled(enabled);
                      callbackContext.success();
                  } catch (Exception e) {
                      if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
            callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    public void clearAllNotifications(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.cancelAll();
                    callbackContext.success();
                } catch (Exception e) {
                  if (FirebasePlugin.isCrashlyticsEnabled()) {
                    Crashlytics.log(e.getMessage());
                  }
                }
            }
        });
    }

    public void clearMailNotification(final CallbackContext callbackContext, final String mid) {
        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearMailNotification: " + mid);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideMailNotificationsForMid(context, mid);
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearMailNotificationsExceptMids(final CallbackContext callbackContext, final String mids) {
        // if (mids.isEmpty()) {
        //     Log.d(TAG, "clearMailNotificationsExceptMids  return, empty data");
        //     return;
        // }

        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearMailNotificationsExceptMids: " + mids);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideMailNotificationsExceptMids(context, Arrays.asList(mids.split(",")));
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearAllMailNotificationsForConv(final CallbackContext callbackContext, final String cid) {
        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearAllMailNotificationsForConv: " + cid);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideMailNotificationsForCid(context, cid);
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearMailNotificationsExceptCids(final CallbackContext callbackContext, final String cids) {
        // if (cids.isEmpty()) {
        //     Log.d(TAG, "clearMailNotificationsExceptCids return, empty data");
        //     return;
        // }

        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearMailNotificationsExceptCids: " + cids);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideMailNotificationsExceptCids(context, Arrays.asList(cids.split(",")));
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clear(final CallbackContext callbackContext, final int id) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.cancel(id);
                    NotificationManager.hideMailSummaryNotificationIfNeed(context, nm);
                    NotificationManager.hideCalendarSummaryNotificationIfNeed(context, nm);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                }
            }
        });
    }

    private ExecutorService notificationPool = Executors.newFixedThreadPool(1);

    public void scheduleLocalNotification(final CallbackContext callbackContext, final JSONObject params) {
        notificationPool.execute(new Runnable() {
            public void run() {
                try {
                    Context activityContext = cordova.getActivity();
                    Context appContext = activityContext.getApplicationContext();

                    String id = params.getString("id");
                    String msgid = params.getString("msgid");
                    String target = params.getString("target");
                    String username = params.getString("username");
                    String groupName = params.getString("groupName");
                    String message = params.getString("message");
                    String eventType = params.getString("eventType");
                    String nsound = params.getString("nsound");
                    String sound = params.getString("sound");
                    String lights = params.getString("lights");
                    String replaceId = params.getString("replaceid");

                    String mentionString = null;
                    if (params.has("mention")) {
                        mentionString = params.getString("mention");
                    }

                    ArrayList<String> mention = new ArrayList<>();

                    if (!TextUtils.isEmpty(mentionString)) {
                        mention = new ArrayList<String>(Arrays.asList(mentionString.split(",")));
                    }

                    NotificationManager.displayTalkNotification(activityContext, appContext, id, msgid, target, username, groupName, message, mention, eventType, nsound, sound, lights, replaceId);

                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void scheduleLocalMailNotification(final CallbackContext callbackContext, final JSONObject params) {
        notificationPool.execute(new Runnable() {
            public void run() {
                try {
                    Context activityContext = cordova.getActivity();
                    Context appContext = activityContext.getApplicationContext();

                    String subject = params.getString("subject");
                    String body = params.getString("body");
                    String fromDisplay = params.getString("fromDisplay");
                    String folderId = params.getString("folderId");
                    String mid = params.getString("mid");
                    String type = params.getString("type");
                    String fromAddress = params.getString("fromAddress");
                    String cid = params.getString("cid");
                    Log.d(TAG, "scheduleLocalMailNotification");
                    Log.d(TAG, "subject=" + subject);
                    Log.d(TAG, "body=" + body);
                    Log.d(TAG, "fromDisplay=" + fromDisplay);
                    Log.d(TAG, "mid=" + mid);
                    Log.d(TAG, "type=" + type);
                    Log.d(TAG, "folderId=" + folderId);
                    Log.d(TAG, "cid=" + cid);
                    NotificationManager.displayMailNotification(activityContext, appContext, subject, body, fromDisplay, mid, type, folderId, "", fromAddress, cid);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void scheduleCallNotification(CallbackContext callbackContext, JSONObject params) {
        notificationPool.execute(new Runnable() {
            public void run() {
                try {
                    Context activityContext = cordova.getActivity();
                    Context appContext = activityContext.getApplicationContext();

                    String msgid = params.getString("msgid");
                    String target = params.getString("target");
                    String initiator = params.getString("vncInitiatorJid");
                    String receiver = params.getString("receiver");
                    String username = params.getString("username");
                    String groupName = params.getString("groupName");
                    String message = params.getString("message");
                    String eventType = params.getString("eventType");
                    String jitsiRoom = params.getString("jitsiRoom");
                    String jitsiURL = params.getString("jitsiURL");

                    Log.d(TAG, "scheduleCallNotification: \n" +
                    "msgid= " + msgid + "\n" +
                    "target= " + target + "\n" +
                    "receiver= " + receiver + "\n" +
                    "username= " + username + "\n" +
                    "groupName= " + groupName + "\n" +
                    "message= " + message + "\n" +
                    "jitsiRoom= " + jitsiRoom + "\n" +
                    "jitsiURL= " + jitsiURL + "\n" +
                    "eventType= " + eventType);

                    String nsound = "nomute";
                    if (params.has("nsound")) {
                        nsound = params.getString("nsound");
                    }

                    NotificationManager.displayTalkCallNotification(activityContext, appContext, msgid, eventType,
                                target, username, groupName, message, nsound, initiator, receiver, 0l, jitsiRoom, jitsiURL);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void scheduleCalendarNotification(CallbackContext callbackContext, JSONObject params) {
        notificationPool.execute(new Runnable() {
            public void run() {
                try {
                    Context activityContext = cordova.getActivity();
                    Context appContext = activityContext.getApplicationContext();

                    String subject = params.getString("subject");
                    String body = params.getString("body");
                    String title = params.getString("title");
                    String fromDisplay = params.getString("fromDisplay");
                    String folderId = params.getString("folderId");
                    String mid = params.getString("mid");
                    String appointmentId = params.getString("appointmentId");
                    String type = params.getString("type");
                    String ntype = params.getString("ntype");
                    String notificationType = params.getString("notificationType");
                    String fromAddress = params.getString("fromAddress");
                    String cid = params.getString("cid");
                    
                    
                    Log.d(TAG, "scheduleCalendarNotification: \n" +
                    "subject = " + subject + "\n" +
                    "body = " + body + "\n" +
                    "title = " + title + "\n" +
                    "fromDisplay = " + fromDisplay + "\n" +
                    "folderId = " + folderId + "\n" +
                    "appointmentId = " + appointmentId + "\n" +
                    "mid = " + mid + "\n" +
                    "type = " + type + "\n" +
                    "ntype = " + ntype + "\n" +
                    "notificationType = " + notificationType + "\n" +
                    "fromAddress = " + fromAddress + "\n" +
                    "cid = " + cid);

                    NotificationManager.displayCalendarNotification(appContext, appointmentId, mid, cid, subject, title, body,
                            fromDisplay, fromAddress, type, ntype, notificationType, folderId);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void clearNotificationByAppointmentId(final CallbackContext callbackContext, final String appointmentId) {
        final Context context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "clearNotificationByAppointmentId: " + appointmentId);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManager.hideNotificationByAppointmentId(context, appointmentId);
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void enableLockScreenVisibility(CallbackContext callbackContext, boolean enable) {
        try {
            if (enable) {
                this.cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            } else {
                this.cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }

            callbackContext.success();
        } catch (Exception e) {
            if (FirebasePlugin.isCrashlyticsEnabled()) {
                Crashlytics.log(e.getMessage());
            }

            callbackContext.error(e.getMessage());
        }
    }

    public void hideIncomingCallNotification(CallbackContext callbackContext, JSONObject params) {
        notificationPool.execute(new Runnable() {
            public void run() {
                try {
                    Context activityContext = cordova.getActivity();
                    Context appContext = activityContext.getApplicationContext();

                    String target = params.getString("target");

                    Log.d(TAG, "hideIncomingCallNotification: target= " + target);

                    NotificationManager.cancelCallNotification(appContext, target);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void displayMissedCallNotification(CallbackContext callbackContext, JSONObject params) {
        notificationPool.execute(new Runnable() {
            public void run() {
                try {
                    Context activityContext = cordova.getActivity();
                    Context appContext = activityContext.getApplicationContext();

                    String callId = params.getString("target");
                    String name = params.getString("name");
                    String groupName = params.getString("groupName");
                    String callType = params.getString("callType");

                    Log.d(TAG, "displayMissedCallNotification: \n" + 
                    "callId= " + callId + "\n" +
                    "name= " + name + "\n" +
                    "groupName= " + groupName + "\n" +
                    "callType= " + callType);

                    NotificationManager.showMissedCallNotification(appContext, callId, name, groupName, callType);
                    callbackContext.success();
                } catch (Exception e) {
                    if (FirebasePlugin.isCrashlyticsEnabled()) {
                        Crashlytics.log(e.getMessage());
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }
}
