package org.apache.cordova.firebase.models;
import com.google.gson.JsonObject;

public class PayloadChannel {

    public String body;
    public String title;
    public JsonObject notification;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonObject getNotification() {
        return notification;
    }

    public void setNotification(JsonObject notification) {
        this.notification = notification;
    }
}