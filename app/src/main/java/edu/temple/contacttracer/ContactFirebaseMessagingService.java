package edu.temple.contacttracer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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
        if(remoteMessage.getFrom().equals("TRACKING")){
            try {
                JSONObject payload = new JSONObject(remoteMessage.getData().get("payload"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
        }
        super.onMessageReceived(remoteMessage);
    }
}
