package org.apache.cordova.firebase.models;

import android.text.TextUtils;

import java.util.List;

public class PayloadTalk {
    public String msgid;
    public String replaceid;
    public String jid;
    public String nto;
    public String nfrom;
    public String name;
    public String eType;
    public String callSignal;
    public String body;
    public String gt;
    public String nType;
    public String nsound;
    public List<String> mention;
    public long t;
    public String jitsiURL;
    public String jitsiRoom;
    public boolean syncpush;

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getNTo() {
        return nto;
    }

    public void setNTo(String to) {
        this.nto = to;
    }

    public String getNFrom() {
        return nfrom;
    }

    public void setNFrom(String from) {
        this.nfrom = from;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String geteType() {
        return eType;
    }

    public void seteType(String eType) {
        this.eType = eType;
    }

    public String getCallSignal() {
        return callSignal;
    }

    public void setcallSignal(String callSignal) {
        this.callSignal = callSignal;
    }

    public boolean isCallNotification(){
        return !TextUtils.isEmpty(callSignal) && callSignal.equals("1");
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getGt() {
        return gt;
    }

    public void setGt(String gt) {
        this.gt = gt;
    }

    public String getnType() {
        return nType;
    }

    public void setnType(String nType) {
        this.nType = nType;
    }

    public String getNsound() {
        return nsound;
    }

    public void setNsound(String nsound) {
        this.nsound = nsound;
    }

    public long getTimeStamp() {
        return t;
    }

    public void setTimeStamp(long timeStamp) {
        this.t = timeStamp;
    }

    public String getJitsiURL() {
        return jitsiURL;
    }

    public void setJitsiURL(String jitsiURL) {
        this.jitsiURL = jitsiURL;
    }

    public String getJitsiRoom() {
        return jitsiRoom;
    }

    public void setJitsiRoom(String jitsiRoom) {
        this.jitsiRoom = jitsiRoom;
    }
}
