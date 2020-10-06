package edu.temple.contacttracer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TracerService extends Service {
    public TracerService() {
    }

    private LocationListener listener;
    private LocationManager lm;
    private long timeLastMoved;
    private Location currentLocation;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Location Service", "Service running");
        return super.onStartCommand(intent, flags, startId);
    }

    class TracerServiceBinder extends Binder {
        public void showNotification() {

            Intent intent = new Intent(TracerService.this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(TracerService.this, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(TracerService.this, getString(R.string.LocationChannelID))
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Location Currently Tracked")
                    .setContentText("This is to alert you that a service that tracks your current location is running")
                    .setContentIntent(pi)
                    .build();

            startForeground(001, notification);
        }

        public void stop(){
            Log.d("Location Service", "Service stopped");
            stopForeground(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                UUIDtracker application = (UUIDtracker)getApplicationContext();
                //Log.d("Location Service", application.getSedentaryTime() + " " + application.getTracingDistance());
                if(application.getCurrentLocation() == null){
                    application.setCurrentLocation(location);
                    return;
                }
                if(application.getCurrentLocation().distanceTo(location) < application.getTracingDistance()){
                    return;
                }
                application.setCurrentLocation(location);
                long currentTime = System.currentTimeMillis();
                if(timeLastMoved < (currentTime - application.getSedentaryTime()*1000)) {
                    application.addLocation(location);
                    sendToServer();
                    //Log.d("Location Tracking", "User has been in location long " +
                            //"enough to get infected");
                }
                timeLastMoved = currentTime;
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };
        lm = getSystemService(LocationManager.class);
        if(checkPermission()) {
            UUIDtracker application = (UUIDtracker)getApplicationContext();
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, listener);
            application.setCurrentLocation(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
        timeLastMoved = System.currentTimeMillis();
        return new TracerServiceBinder();
    }

    private void sendToServer(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.FCM_Tracking_URL);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Location Service", response);
                    }
                    }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Location Service", error.toString());
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                UUIDtracker applicationContext = (UUIDtracker) getApplicationContext();
                String id = applicationContext.getCurrentID();
                String latitude = "" + applicationContext.getCurrentLocation().getLatitude();
                String longitude = "" + applicationContext.getCurrentLocation().getLongitude();
                String timeBegin = "" + timeLastMoved;
                String timeEnd = "" + System.currentTimeMillis();

                params.put(getString(R.string.PayloadUUID), id);
                params.put(getString(R.string.PayloadLatitude), latitude);
                params.put(getString(R.string.PayloadLongitude), longitude);
                params.put(getString(R.string.PayloadTimeBegin), timeBegin);
                params.put(getString(R.string.PayloadTimeEnd), timeEnd);

                return params;
            }
        };
        queue.add(request);
    }

    private boolean checkPermission(){
        return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }
}
