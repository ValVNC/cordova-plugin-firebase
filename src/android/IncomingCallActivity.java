package org.apache.cordova.firebase;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.apache.cordova.firebase.utils.StringUtils;
import org.apache.cordova.firebase.utils.ImagesUtils;

import java.lang.ref.WeakReference;
import java.net.URL;


public class IncomingCallActivity extends Activity {

    private static final String EXTRA_CALL_ID = "extra_call_id";
    private static final String EXTRA_CALL_TYPE = "extra_call_type";
    private static final String EXTRA_CALL_INITIATOR = "extra_call_initiator";
    private static final String EXTRA_CALL_RECEIVER = "extra_call_receiver";
    private static final String EXTRA_CALL_TITLE = "extra_call_title";
    private static final String EXTRA_CALL_SUBTITLE = "extra_call_subtitle";
    private static final String EXTRA_IS_GROUP_CALL = "extra_is_group_call";
    private static final String EXTRA_JITSI_ROOM = "extra_jitsi_room";
    private static final String EXTRA_JITSI_URL = "extra_jitsi_url";

    private static final String HIN_APP_ID = "biz.vnc.vnctalk.hintalk";
    private static final String EKBO_APP_ID = "biz.vnc.vnctalk.ekbodialog";
    private static final String TALK_APP_ID = "biz.vnc.vnctalk";

    private BroadcastReceiver callStateReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private String callId;
    private String callType;
    private String callInitiator;
    private String callReceiver;
    private String callTitle;
    private String callSubTitle;
    private boolean isGroupCall;
    private String jitsiRoom;
    private String jitsiURL;

    public static Intent createStartIntent(Context context, String callId, String callType, String callInitiator,
                                           String callReceiver, String title, String subTitle, boolean isGroupCall,
                                           String jitsiRoom, String jitsiURL){
        Intent intent = new Intent(context, IncomingCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CALL_ID, callId);
        intent.putExtra(EXTRA_CALL_TYPE, callType);
        intent.putExtra(EXTRA_CALL_INITIATOR, callInitiator);
        intent.putExtra(EXTRA_CALL_RECEIVER, callReceiver);
        intent.putExtra(EXTRA_CALL_TITLE, title);
        intent.putExtra(EXTRA_CALL_SUBTITLE, subTitle);
        intent.putExtra(EXTRA_IS_GROUP_CALL, isGroupCall);
        intent.putExtra(EXTRA_JITSI_ROOM, jitsiRoom);
        intent.putExtra(EXTRA_JITSI_URL, jitsiURL);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(getResources().getIdentifier("activity_incoming_call", "layout", getPackageName()));

        processIncomingData(getIntent());
        initUi();
        initCallStateReceiver();
        registerCallStateReceiver();

        Log.d("IncomingCallActivity", "onCreate(), callId = " + callId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("IncomingCallActivity", "onNewIntent, extras = " + intent.getExtras());
    }

    private void initCallStateReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        callStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || TextUtils.isEmpty(intent.getAction())) return;

                String action = intent.getAction();
                Log.d("IncomingCallActivity", "onReceive(), action  = " + action);
                if (!NotificationCreator.TALK_CALL_DECLINE.equals(action)
                        && !NotificationCreator.TALK_CALL_ACCEPT.equals(action)
                        && !NotificationCreator.TALK_DELETE_CALL_NOTIFICATION.equals(action))
                {
                    return;
                }

                String callIdToProcess = intent.getStringExtra(EXTRA_CALL_ID);
                Log.d("IncomingCallActivity", "onReceive(), callId = " + callIdToProcess);
                if (TextUtils.isEmpty(callIdToProcess) || !callIdToProcess.equals(callId)) {
                    Log.d("IncomingCallActivity", "ignore action for call " + callIdToProcess);
                    return;
                }

                switch (action){
                    case NotificationCreator.TALK_DELETE_CALL_NOTIFICATION:
                    case NotificationCreator.TALK_CALL_DECLINE:
                        Log.d("IncomingCallActivity", "finishAndRemoveTask()");
                        finishAndRemoveTask();

                        break;
                    case NotificationCreator.TALK_CALL_ACCEPT:
                        finishDelayed();

                        break;
                }
            }
        };
    }

    private void finishDelayed() {
        Log.d("IncomingCallActivity", "finishDelayed()");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("IncomingCallActivity", "run finishAndRemoveTask()");
                finishAndRemoveTask();
            }
        }, 1000);
    }

    private void registerCallStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NotificationCreator.TALK_DELETE_CALL_NOTIFICATION);
        intentFilter.addAction(NotificationCreator.TALK_CALL_DECLINE);
        intentFilter.addAction(NotificationCreator.TALK_CALL_ACCEPT);
        localBroadcastManager.registerReceiver(callStateReceiver, intentFilter);
    }

    private void unRegisterCallStateReceiver() {
        localBroadcastManager.unregisterReceiver(callStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("IncomingCallActivity", "onDestroy");
        unRegisterCallStateReceiver();
    }

    private void processIncomingData(Intent intent) {
        callId = intent.getStringExtra(EXTRA_CALL_ID);
        callType = intent.getStringExtra(EXTRA_CALL_TYPE);
        callInitiator = intent.getStringExtra(EXTRA_CALL_INITIATOR);
        callReceiver = intent.getStringExtra(EXTRA_CALL_RECEIVER);
        callTitle = intent.getStringExtra(EXTRA_CALL_TITLE);
        callSubTitle = intent.getStringExtra(EXTRA_CALL_SUBTITLE);
        isGroupCall = intent.getBooleanExtra(EXTRA_IS_GROUP_CALL, false);
        jitsiRoom = intent.getStringExtra(EXTRA_JITSI_ROOM);
        jitsiURL = intent.getStringExtra(EXTRA_JITSI_URL);
    }

    private void initUi() {
        TextView callTitleTxt = findViewById(getResources().getIdentifier("user_name_txt", "id", getPackageName()));
        callTitleTxt.setText(callTitle);

        TextView callSubTitleTxt = findViewById(getResources().getIdentifier("call_type_txt", "id", getPackageName()));
        callSubTitleTxt.setText(callSubTitle);

        setBackground();
        setAvatarPlaceholder();

        loadAvatar(callId);
    }

    private void setBackground(){
        ImageView backgroundImg = findViewById(getResources().getIdentifier("background_img", "id", getPackageName()));

        String backgroundResourceName;
        if (getPackageName().equals(HIN_APP_ID)){
            backgroundResourceName = "call_background_hin";
        } else if (getPackageName().equals(EKBO_APP_ID)){
            backgroundResourceName = "call_background_ekbo";
        } else {
            backgroundResourceName = "call_background";
        }

        int backgroundId = getResources().getIdentifier(backgroundResourceName, "drawable", getPackageName());

        backgroundImg.setImageResource(backgroundId);
    }

    private void setAvatarPlaceholder(){
        ImageView avatarImg = findViewById(getResources().getIdentifier("avatar_img", "id", getPackageName()));

        String avatarResourceName;
        if (getPackageName().equals(HIN_APP_ID)){
            avatarResourceName = "hin_icon_round";
        } else {
            avatarResourceName = "vnc_icon_circle";
        }

        int placeholderId = getResources().getIdentifier(avatarResourceName, "drawable", getPackageName());

        avatarImg.setImageResource(placeholderId);
    }

    private void loadAvatar(String callId) {
        new LoadAvatarTask(callId, getAvatarServiceUrl(), (ImageView) findViewById(getResources().getIdentifier("avatar_img", "id", getPackageName()))).execute();
    }

    public void onEndCall(View view) {
        String callDeclineActionName = NotificationCreator.TALK_CALL_DECLINE
                + "@@" + callId
                + "@@" + callType
                + "@@" + callReceiver
                + "@@" + isGroupCall;

        Intent endCallIntent = new Intent(this, NotificationReceiver.class);
        endCallIntent.setAction(callDeclineActionName);

        getApplicationContext().sendBroadcast(endCallIntent);
    }

    public void onStartCall(View view) {
        String callAcceptActionName = NotificationCreator.TALK_CALL_ACCEPT
                + "@@" + callId
                + "@@" + callType
                + "@@" + callInitiator
                + "@@" + jitsiRoom
                + "@@" + jitsiURL
                + "@@" + callReceiver
                + "@@" + isGroupCall;

        Intent startCallIntent = new Intent(this, NotificationReceiver.class);
        startCallIntent.setAction(callAcceptActionName);

        getApplicationContext().sendBroadcast(startCallIntent);
    }

    private String getAvatarServiceUrl() {
        String avatarServiceUrl = SharedPrefsUtils.getString(this, "avatarServiceUrl");
        if (TextUtils.isEmpty(avatarServiceUrl)){
            avatarServiceUrl = "https://avatar.vnc.biz";
        }

        return avatarServiceUrl;
    }

    private static class LoadAvatarTask extends AsyncTask<Void, Integer, Bitmap> {

        private final String callId;
        private final String avatarServiceUrl;
        private final WeakReference<ImageView> imageView;
        
        private LoadAvatarTask(String callId, String avatarServiceUrl, ImageView imageView) {
            this.callId = callId;
            this.avatarServiceUrl = avatarServiceUrl;
            this.imageView = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            String callIdInMD5 = StringUtils.getMD5forString(callId);
            if (TextUtils.isEmpty(callIdInMD5)) return null;

            Bitmap result = null;

            try {
                URL avatarUrl = new URL(avatarServiceUrl + "/" + callIdInMD5 + ".jpg");

                result = BitmapFactory.decodeStream(avatarUrl.openStream());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null || imageView.get() == null) return;

            try {
                imageView.get().setImageDrawable(ImagesUtils.getCircleDrawable(imageView.get().getContext(), bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}