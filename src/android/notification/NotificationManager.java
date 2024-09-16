package org.apache.cordova.firebase.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.cordova.firebase.utils.DateUtils;
import org.apache.cordova.firebase.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Date;


public class NotificationManager {

    private static final String TAG = "NotificationDisplay";

    private static final String MESSAGE_ID = "messageId";
    private static final String CONV_ID = "convId";

    private static final String APPOINTMENT_ID = "appointmentId";

    private static final String CALL_EVENT_INVITE = "invite";
    private static final String CALL_EVENT_LEAVE = "leave";
    private static final String CALL_EVENT_JOIN = "join";
    private static final String CALL_EVENT_REJECT = "reject";
    private static final String CALL_EVENT_JOINED_SELF = "joined-self";
    private static final String CALL_EVENT_REJECTED_SELF = "rejected-self";

    private static final String PREFS_NOTIF_COUNTER = "notificationCounter";
    private static final String PREFS_STRING_SET_KEY = "previousNotifications";

    private static final int MAIL_SUMMARY_NOTIFICATION_ID = 123;
    private static final String MAIL_NOTIFICATIONS_GROUP_ID = "mailNotificationsGroupId";

    private static final int CALENDAR_SUMMARY_NOTIFICATION_ID = 456;
    private static final String CALENDAR_NOTIFICATIONS_GROUP_ID = "calendarNotificationsGroupId";

    private static long timeFromPrevNotify = 0;
    private static final int CHANNEL_SUMMARY_NOTIFICATION_ID = 789;
    private static final String CHANNEL_NOTIFICATIONS_GROUP_ID = "channelNotificationsGroupId";

    synchronized public static void displayMailNotification(Context activityOrServiceContext, Context appContext, String subject,
        String body, String fromDisplay, String msgId,  String type, String folderId, String sound, String fromAddress, String cId) {

        if (checkIfNotificationExist(appContext, msgId)) {
            Log.i(TAG, "Notification EXIST = " + msgId + ", so ignore it");
            return;
        }

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Integer notificationId = msgId.hashCode();
        Log.i(TAG, "displayMailNotification: subject:" + subject + ", body: " + body + ", fromDisplay: " + fromDisplay + ", msgId: " + msgId
            + ", type: " + type + ", notificationId: " + notificationId + ", cId: " + cId + ", sound: " + sound);

        // defineChannelData
        String nsound = sound.equals("false") ? "mute" : "";
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        //prepare group's root notification
        PendingIntent summaryNotificationPendingIntent = NotificationCreator.createNotifPendingIntentMail(activityOrServiceContext, null, MAIL_SUMMARY_NOTIFICATION_ID, null, null, null);
        NotificationCompat.Builder summaryNotification = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
        null, null, null, summaryNotificationPendingIntent, defaultSoundUri);
        summaryNotification.setGroup(MAIL_NOTIFICATIONS_GROUP_ID);
        summaryNotification.setGroupSummary(true);
        summaryNotification.setCategory(Notification.CATEGORY_EMAIL);
        summaryNotification.setOnlyAlertOnce(true);

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, summaryNotification);
        NotificationCreator.setNotificationColor(activityOrServiceContext, summaryNotification);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentMail(activityOrServiceContext, msgId, notificationId, type, folderId, cId);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(fromDisplay);
        bigTextStyle.bigText(subject + "\n" + body);

        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
        fromDisplay, subject, bigTextStyle, pendingIntent, defaultSoundUri);
        notificationBuilder.setGroup(MAIL_NOTIFICATIONS_GROUP_ID);
        notificationBuilder.setCategory(Notification.CATEGORY_EMAIL);
        notificationBuilder.setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY);

        NotificationCreator.addReplyMailAction(activityOrServiceContext, appContext, notificationId, notificationBuilder, msgId, subject, fromAddress, fromDisplay);
        NotificationCreator.addMarkMailAsReadAction(activityOrServiceContext, appContext, notificationId, notificationBuilder, msgId);
        NotificationCreator.addDeleteMailAction(activityOrServiceContext, appContext, notificationId, notificationBuilder, msgId);

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        notification.extras.putString(MESSAGE_ID, msgId);
        notification.extras.putString(CONV_ID, cId);

        Log.i(TAG, "displayMailNotification: channelId: " + channelId + ", channelName: " + channelName + ", defaultSoundUri: " + defaultSoundUri);
        Log.i(TAG, "displayMailNotification: display notificationId: " + notificationId);

        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        notificationManager.notify(MAIL_SUMMARY_NOTIFICATION_ID, summaryNotification.build());
        notificationManager.notify(notificationId, notification);
    }

    synchronized public static void displayCalendarNotification(Context context, String appointmentId, String msgId, String cId, String subject,
        String title, String body, String fromDisplay, String fromAddress, String type, String nType, String notificationType, String folderId) {

        if (checkIfNotificationExist(context, appointmentId)) {
            Log.i(TAG, "Notification EXIST = " + appointmentId + ", so ignore it");
            return;
        }

        android.app.NotificationManager notificationManager = NotificationUtils.getManager(context);

        Integer notificationId = appointmentId.hashCode();

        Log.i(TAG, "displayCalendarNotification: \n" +
            "notificationId: "  + notificationId    + "\n" +
            "appointmentId: "   + appointmentId     + "\n" +
            "msgId: "           + msgId             + "\n" +
            "subject: "         + subject           + "\n" +
            "title: "           + title             + "\n" +
            "body: "            + body              + "\n" +
            "fromDisplay: "     + fromDisplay       + "\n" +
            "fromAddress: "     + fromAddress       + "\n" +
            "type: "            + type              + "\n" +
            "nType: "           + nType             + "\n" +
            "notificationType: " + notificationType + "\n" +
            "folderId: "        + folderId          + "\n" +
            "cId: "             + cId);

        // defineChannelData
        String nsound = "";
        String channelId = NotificationCreator.defineChannelId(context, nsound);
        String channelName = NotificationCreator.defineChannelName(context, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        //prepare group's root notification
        PendingIntent summaryNotificationPendingIntent = NotificationCreator.createNotifPendingIntentCalendar(context, null, null, CALENDAR_SUMMARY_NOTIFICATION_ID, null, null, null, null, null);
        NotificationCompat.Builder summaryNotification = NotificationCreator.createNotification(context, channelId, nsound,
        null, null, null, summaryNotificationPendingIntent, defaultSoundUri);
        summaryNotification.setGroup(CALENDAR_NOTIFICATIONS_GROUP_ID);
        summaryNotification.setGroupSummary(true);
        summaryNotification.setCategory(Notification.CATEGORY_EVENT);
        summaryNotification.setOnlyAlertOnce(true);

        NotificationCreator.setNotificationSmallIcon(context, summaryNotification);
        NotificationCreator.setNotificationColor(context, summaryNotification);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentCalendar(context, appointmentId, msgId, notificationId, type, nType, notificationType, folderId, cId);

        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(context, channelId, nsound,
        title, body, null, pendingIntent, defaultSoundUri);
        notificationBuilder.setCategory(Notification.CATEGORY_EVENT);
        notificationBuilder.setGroup(CALENDAR_NOTIFICATIONS_GROUP_ID);
        notificationBuilder.setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY);

        if (TextUtils.isEmpty(notificationType) || !notificationType.equals("appointment_invite_reply")){
            NotificationCreator.addAcceptCalendarAction(context, notificationId, notificationBuilder, msgId);
            NotificationCreator.addRejectCalendarAction(context, notificationId, notificationBuilder, msgId);
            NotificationCreator.addTentativeCalendarAction(context, notificationId, notificationBuilder, msgId);
        }

        NotificationCreator.setNotificationSmallIcon(context, notificationBuilder);
        NotificationCreator.setNotificationColor(context, notificationBuilder);

        Notification notification = notificationBuilder.build();

        notification.extras.putString(APPOINTMENT_ID, appointmentId);

        NotificationCreator.setNotificationImageRes(context, notification);
        
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        notificationManager.notify(CALENDAR_SUMMARY_NOTIFICATION_ID, summaryNotification.build());
        notificationManager.notify(notificationId, notification);
    }

    public static void hideNotificationByAppointmentId(Context context, String appointmentId) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(context);
            android.app.NotificationManager notificationManager = NotificationUtils.getManager(context);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentAppointmentId = bundle.getString(APPOINTMENT_ID);
                if (currentAppointmentId != null && currentAppointmentId.equals(appointmentId)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
            hideCalendarSummaryNotificationIfNeed(context, notificationManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public static void displayTaskNotification(Context activityOrServiceContext, Context appContext,
                                                          String body, String username, String taskId, String taskUpdatedOn,
                                                          String type, String sound, String open_in_browser, String language) {
        Log.i(TAG, "displayTaskNotification: body: " + body + ", username: " + username + ", taskId: " + taskId + ", taskUpdatedOn: " + taskUpdatedOn + ", type: " + type);

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Integer notificationId = (taskId + body).hashCode();

        // defineChannelData
        String nsound = sound.equals("false") ? "mute" : "";
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentTask(activityOrServiceContext,
                taskId, notificationId, "vncTaskEventType", taskUpdatedOn, type, open_in_browser);

        String title;
        if (language.equals("en")) {
            if (type.equals("assignment")) {
                title = "Task assignment";
            } else if (type.equals("task_update")) {
                title = "Task updated";
            } else if (type.equals("reminder")) {
                title = "Task reminder";
            } else if (type.equals("mentioning")) {
                title = "Task mention";
            } else if (type.equals("overdue_tasks")) {
                title = "Task overdue";
            } else if (type.equals("deletion")) {
                title = "Task deleted";
            } else if (type.equals("assignment_removed")) {
                title = "Task removed";
            } else if (type.equals("due_tasks")) {
                title = "Task due today";
            } else if (type.equals("watcher_added")) {
                title = "Task watcher";
            } else {
                title = "Task notification";
            }
        } else {
            if (type.equals("assignment")) {
                title = "Aufgabenzuweisung";
            } else if (type.equals("task_update")) {
                title = "Aufgabenaktualisierung";
            } else if (type.equals("reminder")) {
                title = "Aufgabenerinnerung";
            } else if (type.equals("mentioning")) {
                title = "Erwähnung in Aufgaben";
            } else if (type.equals("overdue_tasks")) {
                title = "Überfällige Aufgaben";
            } else if (type.equals("deletion")) {
                title = "Aufgabe(n) gelöscht";
            } else if (type.equals("assignment_removed")) {
                title = "Aufgabe(n) entfernt";
            } else if (type.equals("due_tasks")) {
                title = "Heute fällige Aufgaben";
            } else if (type.equals("watcher_added")) {
                title = "Aufgabe(n) Beobachter";
            } else {
                title = "Aufgabenbenachrichtigung";
            }
        }

        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
                title, body, null, pendingIntent, defaultSoundUri);

        if (type.equals("reminder")) {
            NotificationCreator.addSnoozeAction(activityOrServiceContext, appContext,
                  notificationId, notificationBuilder, taskId);
        }

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        // NotificationCreator.setNotificationSound(activityOrServiceContext, notificationBuilder, nsound, sound);
        // NotificationCreator.setNotificationLights(notificationBuilder, lights);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        Log.i(TAG, "displayTaskNotification: channelId: " + channelId + ", channelName: " + channelName + ", defaultSoundUri: " + defaultSoundUri);
        Log.i(TAG, "displayTaskNotification: display notificationId: " + notificationId);

        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        notificationManager.notify(notificationId, notification);
    }

    synchronized public static void displayTalkNotification(Context activityOrServiceContext, Context appContext,
                                                        String id, String msgid, String target, String name, String groupName,
                                                        String message, List<String> mention, String eventType, String nsound,
                                                        String sound, String lights, String replaceId) {
        Log.i(TAG, "displayNotification: msgid: " + msgid);
        Log.i(TAG, "displayNotification: Target: " + target);
        Log.i(TAG, "displayNotification: username: " + name);
        Log.i(TAG, "displayNotification: groupName: " + groupName);
        Log.i(TAG, "displayNotification: message: " + message);
        Log.i(TAG, "displayNotification: mention: " + mention);
        Log.i(TAG, "displayNotification: eventType: " + eventType);
        Log.i(TAG, "displayNotification: nsound: " + nsound);
        Log.i(TAG, "displayNotification: sound: " + sound);
        Log.i(TAG, "displayNotification: lights: " + lights);
        Log.i(TAG, "displayNotification: replaceId: " + replaceId);

        if (checkIfNotificationExist(appContext, msgid)) {
            Log.i(TAG, "Notification EXIST = " + msgid);
            return;
        }

        Integer notificationId = NotificationCreator.createNotifIdIfNecessary(id, target);

        // defineChannelData
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        // defineTitleAndText()
        String title = NotificationCreator.defineNotificationTitle(eventType, target, name, groupName);
        Log.d(TAG, "Notification title: " + title);
        CharSequence text = NotificationCreator.defineNotificationText(eventType, name, message, mention);

        //
        if (timeFromPrevNotify > 0) {
            long difference = System.currentTimeMillis() - timeFromPrevNotify;
            if (difference <= 25) {
                try {
                    Thread.sleep(35);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // find previous messages and update notification id (if necessary)
        StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(appContext);
        LinkedHashMap<String, CharSequence> existMsgs = new LinkedHashMap<>();
        ArrayList<String> msgsIds = new ArrayList<>();
        Integer existingNotificationId = NotificationCreator.findNotificationIdForTargetAndUpdateContent(target, statusBarNotifications, existMsgs, msgsIds);
        if (existingNotificationId != -1) {
            notificationId = existingNotificationId;
            if ("CORRECTION".equals(eventType)) {
                String previousTitle = NotificationCreator.getNotificationTitle(statusBarNotifications, notificationId);
                if (!TextUtils.isEmpty(previousTitle)) {
                    title = previousTitle;
                }

                if ("  ".equals(message)) {
                    existMsgs.remove(replaceId);
                    msgsIds.remove(replaceId);
                } else {
                    existMsgs.replace(replaceId, message);
                }
            }
        }

        if (!"CORRECTION".equals(eventType)) {
            existMsgs.put(msgid, text);
            msgsIds.add(msgid);
        }

        if (existMsgs.isEmpty()) {
            notificationManager.cancel(notificationId);
            return;
        };

        List<CharSequence> msgs = new ArrayList(existMsgs.values());

        // fill messaging style object
        NotificationCompat.MessagingStyle messagingStyle = NotificationCreator.defineMessagingStyle(title, msgs);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentTalk(activityOrServiceContext,
                target, notificationId, "vncEventType", "chat", null);

        // createNotification
        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
                title, text, messagingStyle, pendingIntent, defaultSoundUri);

        // Add actions
        NotificationCreator.addReplyAndMarkAsReadActions(activityOrServiceContext, appContext,
                notificationId, notificationBuilder, target);

        //
        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        NotificationCreator.setNotificationSound(activityOrServiceContext, notificationBuilder, nsound, sound);
        NotificationCreator.setNotificationLights(notificationBuilder, lights);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        //saveDataInNotification
        notification.extras.putSerializable(NotificationCreator.PREVIOUS_MESSAGES, existMsgs);
        notification.extras.putStringArrayList(NotificationCreator.PREVIOUS_MESSAGES_IDS, msgsIds);
        notification.extras.putString(NotificationCreator.NOTIFICATION_TITLE, title);
        notification.extras.putInt(NotificationCreator.NOTIFY_ID_FOR_UPDATING, notificationId);
        notification.extras.putString(NotificationCreator.MESSAGE_TARGET, target);

        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        //
        notificationManager.notify(notificationId, notification);

        //
        timeFromPrevNotify = System.currentTimeMillis();

        //
        NotificationUtils.saveNotificationsIdInFile(activityOrServiceContext, target, notificationId);
    }

    synchronized public static void displayTalkCallNotification(Context activityOrServiceContext, Context appContext, String msgId,
                                                String callEventType, String callId, String name, String groupName, String callType, String nsound,
                                                String callInitiator, String callReceiver, long timeStamp, String jitsiRoom, String jitsiURL) {
        Log.i(TAG, "displayCallNotification: \n" +
            "msgId: "         + msgId         + "\n" +
            "callId: "        + callId        + "\n" +
            "username: "      + name          + "\n" +
            "groupName: "     + groupName     + "\n" +
            "callInitiator: " + callInitiator + "\n" +
            "callReceiver: "  + callReceiver  + "\n" +
            "timeStamp: "     + timeStamp     + "\n" +
            "nsound: "        + nsound        + "\n" +
            "jitsiRoom: "     + jitsiRoom     + "\n" +
            "jitsiURL: "      + jitsiURL      + "\n" +
            "callType: "      + callType);

        if(CALL_EVENT_JOINED_SELF.equals(callEventType) || CALL_EVENT_REJECTED_SELF.equals(callEventType)){
            cancelCallNotification(appContext, callId);
            return;
        }

        if (checkIfNotificationExist(appContext, msgId)) {
            Log.i(TAG, "Notification EXIST = " + msgId + ", so ignore it");
            return;
        }

        if (cancelExistCall(appContext, callId, callId + "_" + name, callEventType)) {
            Log.i(TAG, "Cancel EXIST call " + callId);
            LocalBroadcastManager.getInstance(activityOrServiceContext.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_CALL_DECLINE).putExtra("extra_call_id", callId));
            return;
        }

        if(!CALL_EVENT_INVITE.equals(callEventType)) {
            Log.i(TAG, "NOT INVITE push reseive, call event type = " + callEventType);
            return;
        }

        // if (timeStamp > 0){
        //     long currentTime = DateUtils.getCorrectedTime(appContext);
        //     if(currentTime > timeStamp + 60){
        //         showMissedCallNotification(appContext, callId, name, groupName, callType);
        //         return;
        //     }
        // }

        Integer notificationId = NotificationUtils.generateCallNotificationId(callId);
        boolean isGroupCall = !TextUtils.isEmpty(groupName);

        // defineChannelData
        String channelId = NotificationCreator.defineCallChannelId(activityOrServiceContext);
        String channelName = NotificationCreator.defineCallChannelName(activityOrServiceContext);
        Uri soundUri = NotificationCreator.defineCallSoundUri(activityOrServiceContext, nsound);

        // defineTitleAndText()
        String title = NotificationCreator.defineCallNotificationTitle(callId, name, groupName);
        String text = NotificationCreator.defineCallNotificationText(activityOrServiceContext, callType);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentTalk(activityOrServiceContext,
                callId, notificationId, "vncEventType", "chat", callInitiator);

        // createNotification
        NotificationCompat.Builder notificationBuilder = NotificationCreator.createCallNotification(activityOrServiceContext, channelId,
                title, text, pendingIntent, soundUri);

        // Add actions
        NotificationCreator.addCallDeclineAction(activityOrServiceContext, appContext, notificationBuilder, callId, callType, callReceiver, isGroupCall);
        NotificationCreator.addCallAcceptAction(activityOrServiceContext, appContext, notificationBuilder, callId, callType, callInitiator, callReceiver, isGroupCall, jitsiRoom, jitsiURL);

        // Add full screen intent (to show on lock screen)
        NotificationCreator.addCallFullScreenIntent(appContext, notificationBuilder, callId, callType, callInitiator, callReceiver, title, text, isGroupCall, jitsiRoom, jitsiURL);

        // Add action when delete call notification
        NotificationCreator.addDeleteCallNotificationIntent(appContext, notificationBuilder, callId, name, groupName, callType);

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();
        notification.flags = notification.flags | Notification.FLAG_INSISTENT; // repeat notification sound
        notification.extras.putString(NotificationUtils.EXTRA_CALL_ID, callId);
        notification.extras.putString(NotificationUtils.EXTRA_CALL_TYPE, callType);
        notification.extras.putString(NotificationUtils.EXTRA_CALL_INITIATOR, callId + "_" + name);
        notification.extras.putString(NotificationUtils.EXTRA_CALL_RECEIVER, callReceiver);
        notification.extras.putString(NotificationUtils.EXTRA_CALL_NAME, name);
        notification.extras.putString(NotificationUtils.EXTRA_CALL_GROUP_NAME, groupName);
        notification.extras.putBoolean(NotificationUtils.EXTRA_IS_GROUP_CALL, isGroupCall);
        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);

        android.app.NotificationManager notificationManager = NotificationUtils.getManager(appContext);

        NotificationCreator.createCallNotificationChannel(appContext, notificationManager, channelId, channelName, soundUri);
        notificationManager.notify(notificationId, notification);
    }

    public static void showMissedCallNotification(Context context, String callId, String name, String groupName, String callType){
        Log.i(TAG, "showMissedCallNotification: \n" +
            "callId: "        + callId        + "\n" +
            "username: "      + name          + "\n" +
            "groupName: "     + groupName     + "\n" +
            "callType: "      + callType);

        Integer notificationId = ("missed-call-"+callId).hashCode();

        String channelId = NotificationCreator.defineChannelId(context, "");
        String channelName = NotificationCreator.defineChannelName(context, "");
        Uri defaultSoundUri = NotificationCreator.defineSoundUri("");

        String title = NotificationCreator.defineCallNotificationTitle(
            callId,
            name == null || name.equals("null") ? null : name,
            groupName == null || groupName.equals("null") ? null : groupName);
        String text = NotificationCreator.defineMissedCallNotificationText(context, callType);

        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentTalk(context,
                callId, notificationId, "vncEventType", "chat", null);

        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(context, channelId, "",
                title, text, null, pendingIntent, defaultSoundUri);

        NotificationCreator.setNotificationSmallIcon(context, notificationBuilder);
        NotificationCreator.setNotificationColor(context, notificationBuilder);

        Notification notification = notificationBuilder.build();
        notification.extras.putString(NotificationCreator.MESSAGE_TARGET, callId);
        notification.extras.putString(NotificationCreator.MISSED_CALL_ID, callId);

        NotificationCreator.setNotificationImageRes(context, notification);

        android.app.NotificationManager notificationManager = NotificationUtils.getManager(context);

        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, "");

        notificationManager.notify(notificationId, notification);

        NotificationUtils.saveNotificationsIdInFile(context, callId, notificationId);
    }

    private static boolean checkIfNotificationExist(Context appContext, String msgid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> previousNotificationsInPrefs = prefs.getStringSet(PREFS_STRING_SET_KEY, null);
        // here we copy to a new set bacause of https://stackoverflow.com/a/35625262/574475
        Set<String> previousNotifications;
        if (previousNotificationsInPrefs != null) {
            previousNotifications = new HashSet<String>(previousNotificationsInPrefs);
        } else {
            previousNotifications = new HashSet<String>();
        }

        int counter = prefs.getInt(PREFS_NOTIF_COUNTER, 0);
        String stringNotificationId = msgid;
        long currentTime = System.currentTimeMillis();

        if (previousNotifications != null && previousNotifications.size() > 0) {
            //Checking notifications on time to expire
            long hour = 1000 * 60 * 60;

            // Log.i(TAG, "checkIfNotificationExist, counter: " + counter);

            if (counter > 100) {
                editor.putInt(PREFS_NOTIF_COUNTER, 0);
                Set<String> curNotif = new HashSet<String>();
                Iterator<String> iter = previousNotifications.iterator();
                Log.i(TAG, "checkIfNotificationExist, cleanup prev ids");
                while (iter.hasNext()) {
                    String prevNotif = iter.next();
                    long timeNotif = prefs.getLong(prevNotif, 0);
                    if (timeNotif != 0 && currentTime - timeNotif > hour) {
                        //remove timeStamp for given notificationId
                        editor.remove(prevNotif).apply();
                        //removed notificationId from Set

                        iter.remove();
                    } else {
                        curNotif.add(prevNotif);
                    }
                }
                previousNotifications = curNotif;
            } else {
                editor.putInt(PREFS_NOTIF_COUNTER, ++counter);
            }
            //Check if notificationId already exist in set
            if (previousNotifications.contains(stringNotificationId)) {
                return true;
            } else {
                //add to set, and create record in prefs with timestamp
                writeNotificationToPrefs(stringNotificationId, currentTime, previousNotifications, editor);
            }
        } else {
            //add to set, and create record in prefs with timestamp
            writeNotificationToPrefs(stringNotificationId, currentTime, new HashSet<String>(), editor);
        }
        return false;
    }

    private static void writeNotificationToPrefs(String notificationId, long currentTime, Set<String> existedSet,
                                                 SharedPreferences.Editor editor) {
        existedSet.add(notificationId);
        editor.putStringSet(PREFS_STRING_SET_KEY, existedSet).apply();
        editor.putLong(notificationId, currentTime).apply();
    }

    public static void hideMailSummaryNotificationIfNeed(Context context, android.app.NotificationManager notificationManager) {
        Log.d(TAG, "hideMailSummaryNotificationIfNeed");
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(context);
            Log.d(TAG, "statusBarNotifications.length = " + statusBarNotifications.length);

            int notificationsInGroup = 0;
            for (StatusBarNotification statusBarNotification : statusBarNotifications){
                String groupId = statusBarNotification.getNotification().getGroup();
                if (MAIL_NOTIFICATIONS_GROUP_ID.equals(groupId)) {
                    notificationsInGroup++;
                }

                if (notificationsInGroup > 1) return;
            }

            notificationManager.cancel(MAIL_SUMMARY_NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideCalendarSummaryNotificationIfNeed(Context context, android.app.NotificationManager notificationManager) {
        Log.d(TAG, "hideCalendarSummaryNotificationIfNeed");
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(context);
            Log.d(TAG, "statusBarNotifications.length = " + statusBarNotifications.length);

            int notificationsInGroup = 0;
            for (StatusBarNotification statusBarNotification : statusBarNotifications){
                String groupId = statusBarNotification.getNotification().getGroup();
                if (CALENDAR_NOTIFICATIONS_GROUP_ID.equals(groupId)) {
                    notificationsInGroup++;
                }

                if (notificationsInGroup > 1) return;
            }

            notificationManager.cancel(CALENDAR_SUMMARY_NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsForMid(Context activityOrServiceContext, String mid) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentMessageId = bundle.getString(MESSAGE_ID);
                if (currentMessageId != null && currentMessageId.equals(mid)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
            hideMailSummaryNotificationIfNeed(activityOrServiceContext, notificationManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsExceptMids(Context activityOrServiceContext, List<String> mids) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentMessageId = bundle.getString(MESSAGE_ID);
                if (currentMessageId != null && !mids.contains(currentMessageId)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
            hideMailSummaryNotificationIfNeed(activityOrServiceContext, notificationManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsForCid(Context activityOrServiceContext, String cid) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentCId = bundle.getString(CONV_ID);
                if (currentCId != null && currentCId.equals(cid)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
            hideMailSummaryNotificationIfNeed(activityOrServiceContext, notificationManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsExceptCids(Context activityOrServiceContext, List<String> cids) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentCId = bundle.getString(CONV_ID);
                if (currentCId != null && !cids.contains(currentCId)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
            hideMailSummaryNotificationIfNeed(activityOrServiceContext, notificationManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideNotificationsForTarget(Context activityOrServiceContext, String target) {
        try {
            StatusBarNotification[] activeToasts = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : activeToasts) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentTarget = bundle.getString(NotificationCreator.MESSAGE_TARGET);
                if (currentTarget != null && currentTarget.equals(target)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideNotificationsExceptTargets(Context activityOrServiceContext, List<String> targets) {
        try {
            StatusBarNotification[] activeToasts = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : activeToasts) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentTarget = bundle.getString(NotificationCreator.MESSAGE_TARGET);
                if (currentTarget != null && !targets.contains(currentTarget)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideNotificationsExceptTargetsAndMissedCalls(Context activityOrServiceContext, List<String> targets) {
        try {
            StatusBarNotification[] activeToasts = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = NotificationUtils.getManager(activityOrServiceContext);
            for (StatusBarNotification sbn : activeToasts) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentTarget = bundle.getString(NotificationCreator.MESSAGE_TARGET);
                String currentMissedCallId = bundle.getString(NotificationCreator.MISSED_CALL_ID);
                if (currentTarget != null && !targets.contains(currentTarget) && TextUtils.isEmpty(currentMissedCallId)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean cancelExistCall(Context context, String pushCallId, String pushCallInitiator, String callEventType) {
        if (!CALL_EVENT_LEAVE.equals(callEventType)){
            return false;
        }

        for (StatusBarNotification sbNotification : NotificationUtils.getStatusBarNotifications(context)){
            Notification notification = sbNotification.getNotification();

            Bundle bundle = notification.extras;
            String callId = bundle.getString(NotificationUtils.EXTRA_CALL_ID);

            if (TextUtils.isEmpty(callId) || !callId.equals(pushCallId)){
                continue;
            }

            String callInitiator = bundle.getString(NotificationUtils.EXTRA_CALL_INITIATOR);
            String callType = bundle.getString(NotificationUtils.EXTRA_CALL_TYPE);
            String name = bundle.getString(NotificationUtils.EXTRA_CALL_NAME);
            String groupName = bundle.getString(NotificationUtils.EXTRA_CALL_GROUP_NAME);

            if (pushCallInitiator.equals(callInitiator)){
                int callNotificationId = NotificationUtils.generateCallNotificationId(pushCallId);

                NotificationUtils.getManager(context).cancel(callNotificationId);
                NotificationManager.showMissedCallNotification(context, callId, name, groupName, callType);

                return true;
            }
        }

        return false;
    }

    // TODO VT change to canceling call notifications for separated calls
    public static void cancelCallNotification(Context context, String pushCallId) {
        for (StatusBarNotification sbNotification : NotificationUtils.getStatusBarNotifications(context)) {
            Notification notification = sbNotification.getNotification();

            Bundle bundle = notification.extras;
            String callId = bundle.getString(NotificationUtils.EXTRA_CALL_ID);

            if (TextUtils.isEmpty(callId)) {
                continue;
            } else {
                int callNotificationId = NotificationUtils.generateCallNotificationId(callId);
                NotificationUtils.getManager(context).cancel(callNotificationId);

                LocalBroadcastManager.getInstance(context.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_DELETE_CALL_NOTIFICATION).putExtra(NotificationUtils.EXTRA_CALL_ID, callId));
            }
        }
    }

     synchronized public static void displayChannelNotification(Context activityOrServiceContext, Context appContext,
                                                          String body, String title, String notification) {
        Log.i(TAG, "displayChannelNotification: body: " + body + ", title: " + title + ", notification: " + notification );

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Date d = new Date();

        Integer notificationId = String.valueOf(d.getTime()).hashCode();

         // defineChannelData
        String nsound = "";
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentChannel(activityOrServiceContext,
                title, body, notification, notificationId);


        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
                title, body, null, pendingIntent, defaultSoundUri);

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notificationObj = notificationBuilder.build();

        Log.i(TAG, "displayChannelNotification: channelId: " + channelId + ", channelName: " + channelName + ", defaultSoundUri: " + defaultSoundUri);
        Log.i(TAG, "displayChannelNotification: display notificationId: " + notificationId);

        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notificationObj);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        notificationManager.notify(notificationId, notificationObj);
    }

    public static void hideChannelSummaryNotificationIfNeed(Context context, android.app.NotificationManager notificationManager) {
        Log.d(TAG, "hideChannelSummaryNotificationIfNeed");
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(context);
            Log.d(TAG, "statusBarNotifications.length = " + statusBarNotifications.length);

            int notificationsInGroup = 0;
            for (StatusBarNotification statusBarNotification : statusBarNotifications){
                String groupId = statusBarNotification.getNotification().getGroup();
                if (CHANNEL_NOTIFICATIONS_GROUP_ID.equals(groupId)) {
                    notificationsInGroup++;
                }

                if (notificationsInGroup > 1) return;
            }

            notificationManager.cancel(CHANNEL_SUMMARY_NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
