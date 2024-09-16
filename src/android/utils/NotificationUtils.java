package org.apache.cordova.firebase.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class NotificationUtils {

    private static final String FILE_NAME = "notificationMapping.json";
    private static final String CALL_PREFIX = "call_";

    public static final String EXTRA_CALL_ID = "extra_call_id";
    public static final String EXTRA_CALL_TYPE = "extra_call_type";
    public static final String EXTRA_CALL_INITIATOR = "extra_call_initiator";
    public static final String EXTRA_CALL_NAME = "extra_call_name";
    public static final String EXTRA_CALL_GROUP_NAME = "extra_call_group_name";
    public static final String EXTRA_CALL_RECEIVER = "extra_call_receiver";
    public static final String EXTRA_IS_GROUP_CALL = "extra_is_group_call";
    public static final String EXTRA_CALL_ACTION = "extra_call_action";
    public static final String EXTRA_CALL_JITSI_ROOM = "extra_jitsi_room";
    public static final String EXTRA_CALL_JITSI_URL = "extra_jitsi_url";

    public static void saveNotificationsIdInFile(Context activityOrServiceContext, String target, Integer nId) {
        File file = new File(activityOrServiceContext.getFilesDir(), FILE_NAME);

        // create file if does not exist
        if (!file.exists()) {
            FileUtils.createNewFile(file);
        }

        try {
            // read file into string
            String response = FileUtils.readFile(file);
            if (response == null) {
                return;
            }

            String nIdString = String.valueOf(nId);

            // parse file content
            JSONObject messageDetails = new JSONObject(response);

            // put notification id
            Boolean isConversationExisting = messageDetails.has(target);
            if (isConversationExisting) {
                JSONArray userMessages = (JSONArray) messageDetails.get(target);

                boolean idAlreadyExists = false;
                for (int i = 0; i < userMessages.length(); i++) {
                    if (userMessages.get(i).toString().equals(nIdString)) {
                        idAlreadyExists = true;
                        break;
                    }
                }
                if (!idAlreadyExists) {
                    userMessages.put(nIdString);
                }
            } else {
                JSONArray newUserMessages = new JSONArray();
                newUserMessages.put(nIdString);
                messageDetails.put(target, newUserMessages);
            }
            // save file
            FileUtils.saveJsonToFile(file, messageDetails);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> removeFromFileAndHideNotificationsForTarget(Context activityOrServiceContext, String target) {
        File file = new File(activityOrServiceContext.getFilesDir(), FILE_NAME);

        // returne if file does not exist
        if (!file.exists()) {
            return null;
        }

        ArrayList<String> nIds = null;

        try {
            // read file into string
            String response = FileUtils.readFile(file);
            if (response == null) {
                return null;
            }

            // parse file content
            JSONObject messageDetails = new JSONObject(response);

            // remove notification ids
            Boolean isConversationExisting = messageDetails.has(target);
            if (isConversationExisting) {
                nIds = new ArrayList<String>();

                // collect notifications ids
                JSONArray userMessages = (JSONArray) messageDetails.get(target);
                for (int i = 0; i < userMessages.length(); i++) {
                    nIds.add(userMessages.getString(i));
                }

                // remove
                messageDetails.remove(target);

                // save file

                FileUtils.saveJsonToFile(file, messageDetails);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return nIds;
    }

    public static StatusBarNotification[] getStatusBarNotifications(Context context) {
        StatusBarNotification[] statusBarNotifications = new StatusBarNotification[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            statusBarNotifications = ((android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications();
        }
        return statusBarNotifications;
    }

    public static NotificationManager getManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static NotificationCompat.Builder getBuilder(Context context) {
        return (new NotificationCompat.Builder(context));
    }

    public static int generateCallNotificationId(String callId){
        String resultString = CALL_PREFIX + callId;

        return resultString.hashCode();
    }
}
