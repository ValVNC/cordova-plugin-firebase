package org.apache.cordova.firebase;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;

import org.apache.cordova.firebase.actions.InlineReplyAction;
import org.apache.cordova.firebase.actions.MarkAsReadAction;
import org.apache.cordova.firebase.actions.AcceptCallAction;
import org.apache.cordova.firebase.actions.RejectCallAction;
import org.apache.cordova.firebase.actions.SnoozeAction;
import org.apache.cordova.firebase.actions.MailOptionsAction;
import org.apache.cordova.firebase.actions.MailReplyAction;
import org.apache.cordova.firebase.actions.CalendarOptionsAction;

import org.apache.cordova.firebase.models.MailInfoItem;

import org.apache.cordova.firebase.utils.NotificationUtils;

import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.notification.NotificationManager;

import org.apache.cordova.firebase.OnNotificationOpenReceiver;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "Firebase.NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().contains(NotificationCreator.NOTIFICATION_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String target = actionParts[2];
            CharSequence message = getReplyMessage(intent);

            Log.i(TAG, "NotificationReceiver onReceive NotificationReply, notificationId: " + notificationId + ", target: " + target + ", message: " + message);

            if (message != null && message.length() > 0) {
                Thread thread = new Thread(new InlineReplyAction(message.toString(), target, notificationId, context));
                thread.start();
            }
        } else if (intent.getAction().contains(NotificationCreator.MARK_AS_READ_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String target = actionParts[2];

            Log.i(TAG, "NotificationReceiver onReceive MarkAsReadReply, notificationId: " + notificationId + ", target: " + target);

            Thread thread = new Thread(new MarkAsReadAction(target, notificationId, context));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.SNOOZE_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String taskId = actionParts[2];

            Log.i(TAG, "NotificationReceiver onReceive Snooze, notificationId: " + notificationId + ", taskId: " + taskId);

            Date remindOn = new Date(System.currentTimeMillis() + 3600 * 1000);
            Thread thread = new Thread(new SnoozeAction(taskId, notificationId, remindOn, context));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.MAIL_MARK_AS_READ)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);

            Log.i(TAG, "NotificationReceiver onReceive MarkAsRead, notificationId: " + notificationId + ", msgId: " + msgId);

            Thread thread = new Thread(new MailOptionsAction(context, notificationId, "read", msgId));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.MAIL_DELETE)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);

            Log.i(TAG, "NotificationReceiver onReceive Delete, notificationId: " + notificationId + ", msgId: " + msgId);

            Thread thread = new Thread(new MailOptionsAction(context, notificationId, "trash", msgId));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.MAIL_NOTIFICATION_REPLY)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if(remoteInput == null) {
                Log.w(TAG, "NotificationReceiver onReceive Mail Reply, reply Bundle is NULL");
                return;
            }

            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String msgId = actionParts[2];
            String subject = actionParts[3];
            String fromAddress = actionParts[4];
            String fromDisplay = actionParts[5];
            String replyText = remoteInput.getCharSequence("Reply").toString();

            Log.i(TAG, "NotificationReceiver onReceive Reply, notificationId: " + notificationId + ", msgId: " + msgId);

            MailInfoItem receiver = new MailInfoItem();
            receiver.type = "t";
            receiver.address = fromAddress;
            receiver.displayName = fromDisplay;

            Thread thread = new Thread(new MailReplyAction(context, notificationId, msgId, subject, replyText, receiver));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.TALK_CALL_DECLINE)) {
            String[] actionParts = intent.getAction().split("@@");
            String callId = actionParts[1];
            String callType = actionParts[2];
            String callReceiver = actionParts[3];
            boolean isGroupCall = Boolean.parseBoolean(actionParts[4]);

            Log.i(TAG, "NotificationReceiver onReceive Call REJECT, callId: " + callId);

            LocalBroadcastManager.getInstance(context.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_CALL_DECLINE).putExtra(NotificationUtils.EXTRA_CALL_ID, callId));
            
            Thread thread = new Thread(new RejectCallAction(context, callId, callType, callReceiver, isGroupCall));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.TALK_CALL_ACCEPT)) {
            String[] actionParts = intent.getAction().split("@@");
            String callId = actionParts[1];
            String callType = actionParts[2];
            String callInitiator = actionParts[3];
            String jitsiRoom = actionParts[4];
            String jitsiUrl = actionParts[5];
            String callReceiver = actionParts[6];
            boolean isGroupCall = Boolean.parseBoolean(actionParts[7]);
            int callNotificationId = NotificationUtils.generateCallNotificationId(callId);

            Log.i(TAG, "NotificationReceiver onReceive Call ACCEPT, callId: " + callId);

            Intent launchIntent = new Intent(context.getApplicationContext(), OnNotificationOpenReceiver.class);
            Bundle bundle = new Bundle();
            bundle.putString(NotificationCreator.VNC_PEER_JID, callId);
            bundle.putString(NotificationCreator.VNC_INITIATOR_JID, callInitiator);
            bundle.putString("vncEventType", callType);
            bundle.putInt(NotificationCreator.NOTIFY_ID, callNotificationId);
            bundle.putString(NotificationUtils.EXTRA_CALL_ACTION, NotificationCreator.TALK_CALL_ACCEPT);
            bundle.putString(NotificationUtils.EXTRA_CALL_JITSI_ROOM, jitsiRoom);
            bundle.putString(NotificationUtils.EXTRA_CALL_JITSI_URL, jitsiUrl);
            launchIntent.putExtras(bundle);

            dismissAnotherCalls(context, callId);

            context.sendBroadcast(launchIntent);

            NotificationUtils.getManager(context).cancel(callNotificationId);
            LocalBroadcastManager.getInstance(context.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_CALL_ACCEPT).putExtra(NotificationUtils.EXTRA_CALL_ID, callId));
            Thread thread = new Thread(new AcceptCallAction(context, callId, callType, callReceiver, isGroupCall));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.TALK_DELETE_CALL_NOTIFICATION)) {
            Log.i(TAG, "NotificationReceiver onReceive Delete Call Notification, intent.getAction() = " + intent.getAction());
            String[] actionParts = intent.getAction().split("@@");
            String callId = actionParts[1];
            String name = actionParts[2];
            String groupName = actionParts[3];
            String callType = actionParts[4];
            
            Log.i(TAG, "NotificationReceiver onReceive Delete Call Notification, callId: " + callId);
            Log.i(TAG, "NotificationReceiver onReceive Delete Call Notification, params: " + intent.getExtras());

            LocalBroadcastManager.getInstance(context.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_DELETE_CALL_NOTIFICATION).putExtra(NotificationUtils.EXTRA_CALL_ID, callId));

            NotificationManager.showMissedCallNotification(context.getApplicationContext(), callId, name, groupName, callType);
        } else if (intent.getAction().contains(NotificationCreator.CALENDAR_ACCEPT_ACTION)) {
            Log.i(TAG, "NotificationReceiver onReceive Accept Calendar action, intent.getAction() = " + intent.getAction());
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);
            
            Log.i(TAG, "NotificationReceiver onReceive Accept Calendar action, msgId: " + msgId);

            Thread thread = new Thread(new CalendarOptionsAction(context, notificationId, "ACCEPT", msgId));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.CALENDAR_REJECT_ACTION)) {
            Log.i(TAG, "NotificationReceiver onReceive Decline Calendar action, intent.getAction() = " + intent.getAction());
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);
            
            Log.i(TAG, "NotificationReceiver onReceive Accept Calendar action, msgId: " + msgId);

            Thread thread = new Thread(new CalendarOptionsAction(context, notificationId, "DECLINE", msgId));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.CALENDAR_TENTATIVE_ACTION)) {
            Log.i(TAG, "NotificationReceiver onReceive Tentative Calendar action, intent.getAction() = " + intent.getAction());
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);
            
            Log.i(TAG, "NotificationReceiver onReceive Accept Calendar action, msgId: " + msgId);

            Thread thread = new Thread(new CalendarOptionsAction(context, notificationId, "TENTATIVE", msgId));
            thread.start();
        }
    }

    private CharSequence getReplyMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("Reply");
        }
        return null;
    }

    private void dismissAnotherCalls(Context context, String acceptedCallId){
        StatusBarNotification[] activeNotifications = NotificationUtils.getStatusBarNotifications(context);

        try {
            for (StatusBarNotification sbn : activeNotifications) {
                Notification notification = sbn.getNotification();
                Bundle bundle = notification.extras;
                String callId = bundle.getString(NotificationUtils.EXTRA_CALL_ID);

                if (TextUtils.isEmpty(callId) || acceptedCallId.equals(callId)){
                    continue;
                }
    
                String callType = bundle.getString(NotificationUtils.EXTRA_CALL_TYPE);
                String callReceiver = bundle.getString(NotificationUtils.EXTRA_CALL_RECEIVER);
                boolean isGroupCall = bundle.getBoolean(NotificationUtils.EXTRA_IS_GROUP_CALL);
                
                Thread thread = new Thread(new RejectCallAction(context, callId, callType, callReceiver, isGroupCall));
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
