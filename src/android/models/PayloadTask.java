package org.apache.cordova.firebase.models;

public class PayloadTask {
    public String body;
    public String username;
    public String task_id;
    public String type;
    public String task_updated_on;
    public String sound;
    public String open_in_browser = "false";
    public String language = "en";

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getOpen_in_browser() {
        return this.open_in_browser;
    }

    public void setOpen_in_browser(String open_in_browser) {
        this.open_in_browser = open_in_browser;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTaskId() {
        return task_id;
    }

    public void setTaskId(String taskId) {
        this.task_id = taskId;
    }

    public String getTaskUpdatedOn() {
        return task_updated_on;
    }

    public void setTaskUpdatedOn(String taskUpdatedOn) {
        this.task_updated_on = taskUpdatedOn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }
}
