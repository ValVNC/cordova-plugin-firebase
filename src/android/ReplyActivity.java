package org.apache.cordova.firebase;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.cordova.firebase.actions.InlineReplyAction;

public class ReplyActivity extends Activity implements View.OnClickListener {
    private EditText mMessageTextField;
    private Button mSendButton;
    private Button mCancelButton;
    private String mReplyMessage;
    private static final String VNC_PEER_JID = "vncPeerJid";
    private static final String NOTIFY_ID = "id";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getResources().getIdentifier("reply_activity", "layout", getPackageName()));
        mMessageTextField = (EditText) findViewById(getResources().getIdentifier("reply_message", "id", getPackageName()));
        mSendButton = (Button) findViewById(getResources().getIdentifier("send_message", "id", getPackageName()));
        mCancelButton = (Button) findViewById(getResources().getIdentifier("cancel_message", "id", getPackageName()));
        mSendButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mSendButton) {
            Log.i("VNC", "Send");
            mReplyMessage = mMessageTextField.getText().toString();
            if (mReplyMessage.equals("")) {
                Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show();
                return;
            }
            String sender = getIntent().getExtras().getString(VNC_PEER_JID);
            int notificationId = Integer.parseInt(getIntent().getExtras().getString(NOTIFY_ID));
            Log.i("VNC", "inside receive");

            Thread thread = new Thread(new InlineReplyAction(mReplyMessage, sender, notificationId, getApplicationContext()));
            thread.start();
        }
        finishAndRemoveTask();
    }
}

