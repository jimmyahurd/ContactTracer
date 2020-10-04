package edu.temple.contacttracer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class ContactFirebaseMessagingService extends FirebaseMessagingService {
    public ContactFirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "Received message from server");
        try {
            JSONObject payload = new JSONObject(remoteMessage.getData().get("payload"));
            Log.d("FCM", "Received " + payload + " from server");
            UUIDtracker applicationContext = (UUIDtracker) getApplicationContext();
            if(applicationContext.addContact(payload)) {
                Log.d("FCM", "Successfully added new contact");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onMessageReceived(remoteMessage);
    }
}
