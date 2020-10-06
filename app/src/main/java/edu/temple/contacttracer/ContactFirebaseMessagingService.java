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

//Recieves Messages from FCM Server
public class ContactFirebaseMessagingService extends FirebaseMessagingService {
    public ContactFirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "Received message from server");
        if(remoteMessage.getFrom().equals("/topics/TRACKING")) {
            //Message relates to someone stopping for too long
            try {
                //Grabs payload from message, which contains contact info
                JSONObject payload = new JSONObject(remoteMessage.getData().get("payload"));
                Log.d("FCM", "Received " + payload + " from server");

                //Sends contact info to application context, where it is analyzed
                UUIDtracker applicationContext = (UUIDtracker) getApplicationContext();
                if (applicationContext.addContact(payload)) {
                    //User made contact with this person
                    Log.d("FCM", "Successfully added new contact");
                }else{
                    //User did not make contact with them, as message could've been sent by user
                    //or was sent by someone who was too far away to count
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if(remoteMessage.getFrom().equals("/topics/TRACING")){
            //Messages relate to someone testing positive for COVID
            try {
                //Grabs payload from message which contains time of positive test and UUID's
                JSONObject payload = new JSONObject(remoteMessage.getData().get("payload"));
                Log.d("FCM", "Received " + payload + " from server");
                JSONArray ids = payload.getJSONArray("uuids");

                //Sends UUIDs to application context to be analyzed
                UUIDtracker application = (UUIDtracker) getApplicationContext();
                JSONObject contact;
                if((contact = application.checkContacts(ids)) != null){
                    //User was exposed to COVID and now must be alerted
                    Log.d("FCM", "Found Possible contact");
                    if(application.inForeground()){
                        //Main Activity should alert user immediately as to when and where they
                        //were exposed
                        Log.d("FCM", "Notifying Main Activity");
                        //broadcast to main activity
                        Intent intent = new Intent(getString(R.string.IntentDisplayContact));
                        intent.putExtra(getString(R.string.IntentContactExtra), contact.toString());
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    }else{
                        //User should be sent a notification alerting them they were exposed
                        //If clicked, will show them when and where exposure occurred.
                    }
                }else{
                    //User was not exposed to COVID, as message was from them or from someone
                    //they never had contact with
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        super.onMessageReceived(remoteMessage);
    }
}
