package org.apache.cordova.firebase.models;

import com.google.gson.annotations.SerializedName;

public class MailInfoItem {

    //Known values "t" - to, "f" - from, "c" - copy, "b" - blind copy
    @SerializedName("t")
    public String type;

    @SerializedName("a")
    public String address;

    @SerializedName("d")
    public String displayName;
}
