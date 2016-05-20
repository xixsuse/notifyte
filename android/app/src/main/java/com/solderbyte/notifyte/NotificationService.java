package com.solderbyte.notifyte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationService extends NotificationListenerService {
    private static final String LOG_TAG = "Notifyte:Notification";

    private ArrayList<String> ListPackageNames = new ArrayList<String>();
    private ArrayList<NotifyteNotification> listNotifyte = new ArrayList<NotifyteNotification>();

    // view data
    private String NOTIFICATION_TITLE = null;
    private String NOTIFICATION_TEXT = null;
    private String NOTIFICATION_BIG_TEXT = null;

    // global
    private PackageManager packageManager = null;
    private Context context = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Created NotificationService");
        this.registerReceiver(serviceStopReceiver, new IntentFilter(Intents.INTENT_SERVICE_STOP));
        this.registerReceiver(applicationsReceiver, new IntentFilter(Intents.INTENT_APPLICATION));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Intents.INTENT_BLUETOOTH));

        context = NotificationService.this.getApplicationContext();
        packageManager = this.getPackageManager();

        Intent msg = new Intent(Intents.INTENT_NOTIFICATION_START);
        context.sendBroadcast(msg);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        unregisterReceiver(serviceStopReceiver);
        unregisterReceiver(applicationsReceiver);
        unregisterReceiver(bluetoothLeReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(LOG_TAG, "onNotificationPosted");
        String packageName = sbn.getPackageName();
        String appName = this.getAppName(packageName);

        /*if(!ListPackageNames.contains(packageName)) {
            return;
        }*/

        // API v19
        Notification notification = sbn.getNotification();
        this.getViewNotification(notification, packageName);
        Bundle extras = notification.extras;

        if((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) {
            return;
        }

        String ticker = null;
        String message = null;
        String submessage = null;
        String summary = null;
        String info = null;
        String title = null;
        try {
            ticker = (String) sbn.getNotification().tickerText;
        }
        catch(Exception e) {
            Log.d(LOG_TAG, "Notification does not have tickerText");
        }
        String tag = sbn.getTag();
        long time = sbn.getPostTime();
        int id = sbn.getId();

        if(extras.getCharSequence("android.title") != null) {
            title = extras.getString("android.title");
        }
        if(extras.getCharSequence("android.text") != null) {
            message = extras.getCharSequence("android.text").toString();
        }
        if(extras.getCharSequence("android.subText") != null) {
            submessage = extras.getCharSequence("android.subText").toString();
        }
        if(extras.getCharSequence("android.summaryText") != null) {
            summary = extras.getCharSequence("android.summaryText").toString();
        }
        if(extras.getCharSequence("android.infoText") != null) {
            info = extras.getCharSequence("android.infoText").toString();
        }

        Log.d(LOG_TAG, "Captured notification message: " + message + " from source:" + packageName);
        Log.d(LOG_TAG, "app name: " + appName);
        Log.d(LOG_TAG, "ticker: " + ticker);
        Log.d(LOG_TAG, "title: " + title);
        Log.d(LOG_TAG, "message: " + message);
        Log.d(LOG_TAG, "tag: " + tag);
        Log.d(LOG_TAG, "time: " + time);
        Log.d(LOG_TAG, "id: " + id);
        Log.d(LOG_TAG, "submessage: " + submessage);
        Log.d(LOG_TAG, "summary: " + summary);
        Log.d(LOG_TAG, "info: " + info);
        Log.d(LOG_TAG, "view title: " + NOTIFICATION_TITLE);
        Log.d(LOG_TAG, "view big text: " + NOTIFICATION_BIG_TEXT);
        Log.d(LOG_TAG, "view text: " + NOTIFICATION_TEXT);


        String APP_NAME = appName;
        String PACKAGE_NAME = packageName;
        String NAME = null;
        String CONTACT = null;
        String GROUP = null;
        String MESSAGE = null;
        int ID = id;
        long CREATED = time;

        if(packageName.equals("com.facebook.orca")) {
            // facebook messenger
            // appName: Messenger
            // packageName: com.facebook.orca
            // name: title or view title
            // contact: n/a
            // message: message or view big text

            if(title != null) {
                NAME = title;
            }
            else {
                NAME = NOTIFICATION_TITLE;
            }
            if(message != null) {
                MESSAGE = message;
            }
            else if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else {
                MESSAGE = NOTIFICATION_TEXT;
            }
        }
        else if(packageName.equals("com.google.android.talk")) {
            // google hangouts
            // appName: Hangouts
            // packageName: com.google.android.talk
            // name: title or view title (name)
            // contact: summary or view text (email)
            // message: message or view big text

            if(title != null) {
                NAME = title;
            }
            else {
                NAME = NOTIFICATION_TITLE;
            }
            if(summary != null) {
                CONTACT = summary;
            }
            else {
                CONTACT = NOTIFICATION_TEXT;
            }
            if(message != null) {
                MESSAGE = message;
            }
            else {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
        }
        else if(packageName.equals("com.whatsapp")) {
            // whatsapp
            // appName: WhatsApp
            // packageName: com.whatsapp
            // name: title or view title (group: name @ group)
            // contact: n/a
            // message: message or view text big text

            String[] split;
            if(title != null) {
                // group message
                if(title.contains("@")) {
                    split = title.split("(.+)@(.+)");
                    NAME = split[0];
                    GROUP = split[1];
                }
                else {
                    NAME = title;
                }
            }
            else if(NOTIFICATION_TITLE != null) {
                if(title.contains("@")) {
                    split = NOTIFICATION_TITLE.split("(.+)@(.+)");
                    NAME = split[0];
                    GROUP = split[1];
                }
                else {
                    NAME = NOTIFICATION_TITLE;
                }
            }
            else {
                NAME = title;
            }
            if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else if(message != null) {
                MESSAGE = message;
            }
            else {
                MESSAGE = NOTIFICATION_TEXT;
            }
        }
        else {
            if(title != null) {
                NAME = title;
            }
            else {
                NAME = NOTIFICATION_TITLE;
            }
            if(NOTIFICATION_BIG_TEXT != null) {
                MESSAGE = NOTIFICATION_BIG_TEXT;
            }
            else if(message != null) {
                MESSAGE = message;
            }
            else {
                MESSAGE = NOTIFICATION_TEXT;
            }
        }



        RemoteInput[] remoteInputs = this.getRemoteInputs(notification);

        if(remoteInputs != null) {
            Log.d(LOG_TAG, "creating notifyte");
            NotifyteNotification notifyte = new NotifyteNotification();
            notifyte.appName = APP_NAME;
            notifyte.packageName = PACKAGE_NAME;
            notifyte.name = NAME;
            notifyte.contact = CONTACT;
            notifyte.group = GROUP;
            notifyte.message = MESSAGE;
            notifyte.created = CREATED;
            notifyte.id = ID;
            notifyte.tag = tag;
            notifyte.bundle = extras;
            notifyte.pendingIntent = notification.contentIntent;
            notifyte.remoteInputs.addAll(Arrays.asList(remoteInputs));
            listNotifyte.add(notifyte);
        }



        Log.d(LOG_TAG, "created");

        Intent msg = new Intent(Intents.INTENT_NOTIFICATION);
        msg.putExtra(Intents.INTENT_NOTIFICATION_APP_NAME, APP_NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_PACKAGE_NAME, PACKAGE_NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_NAME, NAME);
        msg.putExtra(Intents.INTENT_NOTIFICATION_CONTACT, CONTACT);
        msg.putExtra(Intents.INTENT_NOTIFICATION_GROUP, GROUP);
        msg.putExtra(Intents.INTENT_NOTIFICATION_MESSAGE, MESSAGE);
        msg.putExtra(Intents.INTENT_NOTIFICATION_CREATED, CREATED);
        msg.putExtra(Intents.INTENT_NOTIFICATION_ID, ID);

        context.sendBroadcast(msg);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(LOG_TAG, "onNotificationRemoved");
        String packageName = sbn.getPackageName();
        String shortMsg = "";
        try {
            shortMsg = (String) sbn.getNotification().tickerText;
        }
        catch(Exception e) {

        }
        Log.d(LOG_TAG, "Removed notification message: " + shortMsg + " from source:" + packageName);
    }

    public String getAppName(String packageName) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(LOG_TAG, "Cannot get application info");
        }
        String appName = (String) packageManager.getApplicationLabel(appInfo);
        return appName;
    }


    public boolean getViewNotification(Notification n, String packageName) {
        Resources resources = null;
        try {
            resources = packageManager.getResourcesForApplication(packageName);
        }
        catch(Exception e){
            Log.e(LOG_TAG, "Failed to get PackageManager: " + e.getMessage());
        }
        if(resources == null) {
            Log.e(LOG_TAG, "No PackageManager resources");
            return false;
        }

        int TITLE = resources.getIdentifier("android:id/title", null, null);
        int BIG_TEXT = resources.getIdentifier("android:id/big_text", null, null);
        int TEXT = resources.getIdentifier("android:id/text", null, null);

        RemoteViews views = n.bigContentView;
        if(views == null) {
            views = n.contentView;
        }
        if(views == null) {
            return false;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
        views.reapply(getApplicationContext(), localView);

        TextView title = (TextView) localView.findViewById(TITLE);
        if(title != null) {
            NOTIFICATION_TITLE = title.getText().toString();
        }
        else {
            NOTIFICATION_TITLE = null;
        }
        TextView big = (TextView) localView.findViewById(BIG_TEXT);
        if(big != null) {
            NOTIFICATION_BIG_TEXT = big.getText().toString();
        }
        else {
            NOTIFICATION_BIG_TEXT = null;
        }
        TextView text = (TextView) localView.findViewById(TEXT);
        if(text != null) {
            NOTIFICATION_TEXT = text.getText().toString();
        }
        else {
            NOTIFICATION_TEXT = null;
        }

        return true;
    }

    public RemoteInput[] getRemoteInputs(Notification notification) {
        Log.d(LOG_TAG, "getRemoteInputs");
        RemoteInput[] remoteInputs = null;
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        Log.d(LOG_TAG, "wearableExtender: " + wearableExtender);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        Log.d(LOG_TAG, "actions: " + actions);
        for(NotificationCompat.Action act : actions) {
            Log.d(LOG_TAG, "act: " + act);
            if(act != null && act.getRemoteInputs() != null) {
                remoteInputs = act.getRemoteInputs();
            }
        }
        Log.d(LOG_TAG, "returning");
        return remoteInputs;
    }

    public android.app.RemoteInput[] getRemoteInputsFromBundle(Bundle bundle) {
        Log.d(LOG_TAG, "getRemoteInputsFromBundle");
        android.app.RemoteInput[] remoteInputs = null;

        for(String key : bundle.keySet()) {
            Object value = bundle.get(key);

            if("android.wearable.EXTENSIONS".equals(key)) {
                Log.d(LOG_TAG, "android.wearable.EXTENSIONS!");
                Bundle wearBundle = ((Bundle) value);
                for(String keyInner : wearBundle.keySet()) {
                    Object valueInner = wearBundle.get(keyInner);

                    if(keyInner != null && valueInner != null) {
                        if("actions".equals(keyInner) && valueInner instanceof ArrayList) {
                            ArrayList<Notification.Action> actions = new ArrayList<>();
                            actions.addAll((ArrayList) valueInner);
                            for(Notification.Action act : actions) {
                                Log.d(LOG_TAG, "act: " + act);
                                if(Build.VERSION.SDK_INT >= 20) {
                                    if(act.getRemoteInputs() != null) {
                                        remoteInputs = act.getRemoteInputs();
                                    }
                                }
                                else {
                                    Log.e(LOG_TAG, "Failed to get remoteInput. API 20 required");
                                }
                            }
                        }
                    }
                }
            }
        }

        return remoteInputs;
    }

    public NotifyteNotification getNotifyte() {
        for(int i = 0; i < listNotifyte.size(); i++) {
            return listNotifyte.get(i);
        }
        return null;
    }

    public void replyToNotification(JSONObject json) {
        Log.d(LOG_TAG, "replyToNotification");
        Log.d(LOG_TAG, json.toString());
        NotifyteNotification notifyte = getNotifyte();
        RemoteInput[] remoteInputs = null;
        try {
            remoteInputs = new RemoteInput[notifyte.remoteInputs.size()];
        }
        catch(NullPointerException e) {
            Log.e(LOG_TAG, "Error: no remoteInputs");
            return;
        }


        Log.d(LOG_TAG, "notifyte: " + notifyte);

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = notifyte.bundle;
        int i = 0;
        for(RemoteInput remoteInput : notifyte.remoteInputs){
            this.getRemoteInputInfo(remoteInput);
            remoteInputs[i] = remoteInput;
            //This work, apart from Hangouts as probably they need additional parameter (notification_tag?)
            Log.d(LOG_TAG, "remoteInput: " + remoteInput);
            bundle.putCharSequence(remoteInputs[i].getResultKey(), notifyte.message);
            i++;
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle);
        try {
            Log.d(LOG_TAG, "trying to send");
            notifyte.pendingIntent.send(this, 0, intent);
        }
        catch (PendingIntent.CanceledException e) {
            Log.e(LOG_TAG, "Error: replyToNotification: " + e);
            e.printStackTrace();
        }
    }

    private void getRemoteInputInfo(RemoteInput remoteInput) {
        String resultKey = remoteInput.getResultKey();
        String label = remoteInput.getLabel().toString();
        Boolean canFreeForm = remoteInput.getAllowFreeFormInput();
        if(remoteInput.getChoices() != null && remoteInput.getChoices().length > 0) {
            String[] possibleChoices = new String[remoteInput.getChoices().length];
            for(int i = 0; i < remoteInput.getChoices().length; i++){
                possibleChoices[i] = remoteInput.getChoices()[i].toString();
            }
        }
    }

    private BroadcastReceiver serviceStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "serviceStopReceiver");
            unregisterReceiver(applicationsReceiver);
            unregisterReceiver(serviceStopReceiver);
            unregisterReceiver(bluetoothLeReceiver);
            NotificationService.this.stopSelf();
        }
    };

    private BroadcastReceiver applicationsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> applications = intent.getStringArrayListExtra(Intents.INTENT_EXTRA_DATA);
            ListPackageNames = applications;
            Log.d(LOG_TAG, "Received listeningApps: " + applications.size());
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "bluetoothLeReceiver");

            String message = intent.getStringExtra(Intents.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, message);

            if(message.equals(Intents.INTENT_BLUETOOTH_NOTIFICATION)) {
                Log.d(LOG_TAG, "Bluetooth notification");
                String data = intent.getStringExtra(Intents.INTENT_EXTRA_DATA);
                JSONObject json = null;
                try {
                    json = new JSONObject(data);
                    NotificationService.this.replyToNotification(json);
                }
                catch(JSONException e) {
                    Log.e(LOG_TAG, "Error: converting string to json" + e);
                    e.printStackTrace();
                }
            }
        }
    };
}
