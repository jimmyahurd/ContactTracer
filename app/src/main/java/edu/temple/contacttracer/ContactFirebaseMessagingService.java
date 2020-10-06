package edu.temple.contacttracer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

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
                    Log.d("FCM", contact.toString());
                    if(application.inForeground()){
                        //Main Activity should alert user immediately as to when and where they
                        //were exposed
                        Log.d("FCM", "Notifying Main Activity");
                        //Create intent with contact information
                        Intent intent = new Intent(getString(R.string.IntentDisplayContact));
                        intent.putExtra(getString(R.string.IntentContactExtra), contact.toString());
                        //broadcast to main activity
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    }else{
                        //User should be sent a notification alerting them they were exposed
                        //If clicked, will show them when and where exposure occurred.
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setAction(getString(R.string.IntentDisplayContact));
                        intent.putExtra(getString(R.string.IntentContactExtra), contact.toString());
                        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT);

                        //Creates notification with pending intent that can be sent to main activity
                        Notification notification = new NotificationCompat.Builder(this, getString(R.string.LocationChannelID))
                                .setSmallIcon(R.drawable.ic_launcher_background)
                                .setContentTitle("Possible COVID Exposure!!")
                                .setContentText("Click to see where and when possible exposure ocurred")
                                .setContentIntent(pi)
                                .build();
                        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
                        manager.notify(002, notification);

                        /*
                        TestThread thread = new TestThread(this, contact);
                        thread.run();
                        thread.join();
                         */
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
/*
    private class TestThread extends Thread{
        Context context;
        JSONObject contact;
        public TestThread(Context context, JSONObject contact){
            this.context = context;
            this.contact = contact;
        }
        @Override
        public void run() {
            Log.d("Thread", "Now running");
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("Thread", "Sending notification now");
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(getString(R.string.IntentDisplayContact));
            intent.putExtra(getString(R.string.IntentContactExtra), contact.toString());
            //User should be sent a notification alerting them they were exposed
            //If clicked, will show them when and where exposure occurred.
            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT);

            //Creates notification with pending intent that can be sent to main activity
            Notification notification = new NotificationCompat.Builder(context, getString(R.string.LocationChannelID))
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Possible COVID Exposure!!")
                    .setContentText("Click to see where and when possible exposure ocurred")
                    .setContentIntent(pi)
                    .build();
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(002, notification);
        }
    }

 */
}
