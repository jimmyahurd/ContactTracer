package edu.temple.contacttracer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactFirebaseMessagingService extends FirebaseMessagingService {
    public ContactFirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "Received message from server");
        if(remoteMessage.getFrom().equals("/topics/TRACKING")) {
            try {
                JSONObject payload = new JSONObject(remoteMessage.getData().get("payload"));
                Log.d("FCM", "Received " + payload + " from server");
                UUIDtracker applicationContext = (UUIDtracker) getApplicationContext();
                if (applicationContext.addContact(payload)) {
                    Log.d("FCM", "Successfully added new contact");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if(remoteMessage.getFrom().equals("/topics/TRACING")){
            try {
                JSONObject payload = new JSONObject(remoteMessage.getData().get("payload"));
                Log.d("FCM", "Received " + payload + " from server");
                JSONArray ids = payload.getJSONArray("uuids");
                UUIDtracker application = (UUIDtracker) getApplicationContext();
                JSONObject contact;
                if((contact = application.checkContacts(ids)) != null){
                    Log.d("FCM", "Found Possible contact");
                    if(application.inForeground()){
                        Log.d("FCM", "Notifying Main Activity");
                        //broadcast to main activity
                        Intent intent = new Intent(getString(R.string.IntentDisplayContact));
                        intent.putExtra(getString(R.string.IntentContactExtra), contact.toString());
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    }else{
                        //notification
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        super.onMessageReceived(remoteMessage);
    }
}
