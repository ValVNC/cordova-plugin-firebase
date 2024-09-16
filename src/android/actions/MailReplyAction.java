package org.apache.cordova.firebase.actions;

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.FirebasePlugin;
import org.apache.cordova.firebase.models.MailInfoItem;
import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.notification.NotificationManager;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.apache.cordova.firebase.utils.WidgetNotifier;

import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.lang.StringBuffer;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.ArrayList;


import org.json.JSONArray;
import org.json.JSONObject;

public class MailReplyAction extends BaseActionMail {
    private static final String TAG = "Firebase.MailReplyAction";

    private final String originalMsgId;
    private final String subject;
    private final String body;
    private final MailInfoItem[] mailInfos;

    public MailReplyAction(Context context, int notificationId, String originalMsgId, String subject, String body, MailInfoItem... mailInfos) {
        super(notificationId, context, "/sendEmail");

        this.originalMsgId = originalMsgId;
        this.subject = subject;
        this.body = body;
        this.mailInfos = mailInfos;
    }

    @Override
    public void run() {
        super.run();

        if (!isInternetAvailable()) {
            showToast("No internet connection", false);

            //Need update notification for hiding 'reply' field and progressbar on notification
            android.app.NotificationManager manager = NotificationUtils.getManager(context);
            StatusBarNotification[] activeNotifications = NotificationUtils.getStatusBarNotifications(context);
            if (activeNotifications.length > 0) {
                try {
                    for (StatusBarNotification sbn : activeNotifications) {
                        Notification notification = sbn.getNotification();
                        if (sbn.getId() == notificationId) {
                            manager.notify(notificationId, notification);
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                // Reply
                JsonObject postData = new JsonObject();
                postData.addProperty("origid", originalMsgId);
                postData.addProperty("subject", String.format("Re: %s", subject));
                postData.addProperty("rt", "r");
                postData.addProperty("content", prepareContent(body));
                postData.add("emailInfo", getEmailInfo(mailInfos));

                // postData.addProperty("id",""); // ???
                // postData.addProperty("idnt",""); // ???
                // postData.addProperty("f",""); // ??
                // postData.addProperty("l",""); // ???
                // postData.addProperty("did","");// ???
                // postData.add("attach", new JsonObject());
                // postData.add("related", new JsonArray());

                Log.i(TAG, "postData : " + postData);

                HttpURLConnection urlConnection = createUrlConnection();

                NotificationCreator.setNotificationSmallIcon(context, notificationBuilder);

                if (postData != null) {
                    OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                    writer.write(postData.toString());
                    writer.flush();
                }
                int statusCode = urlConnection.getResponseCode();
                Log.i(TAG, "Server response, statusCode: " + statusCode);
                if (statusCode > 400) {
                    Log.i(TAG, "Server response: " + urlConnection.getResponseMessage());

                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Log.i(TAG, "Server error response: " + response.toString());

                    saveReplyOnError(context, originalMsgId, body);
                } else {
                    // mark as read as well
                    markAsRead(originalMsgId);
                    // notify widget data set changed
                    WidgetNotifier.notifyMessagesListUpdated(context);
                }
            } catch (Exception e) {
                Log.i(TAG, e.getLocalizedMessage());
                saveReplyOnError(context, originalMsgId, body);
            } finally {
                cancelNotification();
            }
        }
    }

    private JsonArray getEmailInfo(MailInfoItem... mailInfos) {
        ArrayList<MailInfoItem> mailInfosList = new ArrayList<MailInfoItem>(Arrays.asList(mailInfos));
        mailInfosList.add(getCurrentUserInfo());

        JsonArray mailInfo = new JsonArray();

        Gson gson = new GsonBuilder().create();
        for (MailInfoItem item : mailInfosList) {
            JsonElement element = gson.toJsonTree(item, MailInfoItem.class);
            mailInfo.add(element);
        }

        return mailInfo;
    }

    private String prepareContent(String body) {
        // Now just return body
        return body;
    }

    private MailInfoItem getCurrentUserInfo() {
        // STUB code to test
        MailInfoItem sender = new MailInfoItem();
        sender.type = "f";
        sender.address = SharedPrefsUtils.getString(context, "mailCurrentUserAddress");
        sender.displayName = SharedPrefsUtils.getString(context, "mailCurrentUserDisplayName");

        return sender;
    }

    private void saveReplyOnError(Context context, String originalMsgId, String replyText) {
        Log.i(TAG, "saveOptionOnError, msgId: " + originalMsgId + ", reply text: " + replyText);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "mailReplyFailedRequests");

        ArrayList<ReplyErrorEntity> list;
        if (data != null) {
            Type type = new TypeToken<ArrayList<ReplyErrorEntity>>() {
            }.getType();
            list = gson.fromJson(data, type);
            if (list == null) {
                list = new ArrayList();
            }
        } else {
            list = new ArrayList();
        }
        ReplyErrorEntity ree = new ReplyErrorEntity(originalMsgId, replyText);
        list.add(ree);
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "mailReplyFailedRequests", json);
    }

    private class ReplyErrorEntity {
        String msgId;
        String replyText;

        public ReplyErrorEntity(String msgId, String replyText) {
            this.msgId = msgId;
            this.replyText = replyText;
        }
    }

    private void markAsRead(String msgId) throws Exception {
        mApiUrl = baseApiUrl() + "/msgAction";

        JSONObject postData = new JSONObject();
        postData.put("op", "read");
        postData.put("id", new JSONArray(new String[]{msgId}));
        //
        HttpURLConnection urlConnection = createUrlConnection();
        //
        if (postData != null) {
            OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
            writer.write(postData.toString());
            writer.flush();
        }
        int statusCode = urlConnection.getResponseCode();
        Log.i(TAG, "Server response (markAsRead), statusCode: " + statusCode);
        Log.i(TAG, "Server response (markAsRead): " + urlConnection.getResponseMessage());
    }
}
