package com.tkporter.sendsms;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.net.Uri;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

public class SendSMSModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext reactContext;
    private Callback callback = null;
    private static final int REQUEST_CODE = 5235;

    public SendSMSModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "SendSMS";
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        //System.out.println("in module onActivityResult() request " + requestCode + " result " + resultCode);
        //canceled intent
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            sendCallback(false, true, false);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    public void sendCallback(Boolean completed, Boolean cancelled, Boolean error) {
        if (callback != null) {
            callback.invoke(completed, cancelled, error);
            callback = null;
        }
    }

    @ReactMethod
    public void send(ReadableMap options, final Callback callback) {
        try {
            this.callback = callback;

            String body = options.hasKey("body") ? options.getString("body") : "";
            ReadableArray recipients = options.hasKey("recipients") ? options.getArray("recipients") : null;

            Intent sendIntent;
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(reactContext);
            if (defaultSmsPackageName != null){
                sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setPackage(defaultSmsPackageName);
            }
            else {
                sendIntent = new Intent(Intent.ACTION_VIEW);
            }

            sendIntent.setType("text/plain");
            sendIntent.putExtra("sms_body", body);

            //if recipients specified
            if (recipients != null) {
                String recipientString = "";

                if (recipients.size() == 1) {
                    recipientString = recipients.getString(0);
                    sendIntent.setData(Uri.parse("smsto:"+ recipientString));
                }
                else {
                    //Samsung for some reason uses commas and not semicolons as a delimiter
                    String separator = ";";
                    if(android.os.Build.MANUFACTURER.equalsIgnoreCase("Samsung")){
                        separator = ",";
                    }

                    for (int i = 0; i < recipients.size(); i++) {
                        recipientString += recipients.getString(i);
                        recipientString += separator;
                    }
                    sendIntent.putExtra("address", recipientString);
                }
            }

            reactContext.startActivity(sendIntent);
        } catch (Exception e) {
            //error!
            sendCallback(false, false, true);
            throw e;
        }
    }

}
