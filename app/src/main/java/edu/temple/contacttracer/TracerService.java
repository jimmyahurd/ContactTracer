package edu.temple.contacttracer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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

public class TracerService extends Service {
    public TracerService() {
    }

    private float TRACING_DISTANCE; //distance in meters
    private long SEDENTARY_TIME; //time in seconds user must be at rest for to get infected
    private LocationListener listener;
    private LocationManager lm;
    private long timeLastMoved;

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

            startForeground(000, notification);
        }

        public void changeTracingDistance(float tracingDistance) {
            Log.d("Location Service", "Changing Tracing distance from " + TRACING_DISTANCE
            + " to " + tracingDistance);
            TRACING_DISTANCE = tracingDistance;
        }

        public void changeSedentaryTime(long sedentaryTime) {
            Log.d("Location Service", "Changing sedentary time from " + SEDENTARY_TIME
                    + " to " + sedentaryTime);
            SEDENTARY_TIME = sedentaryTime;
        }

        public void stop(){
            Log.d("Location Service", "Service stopped");
            stopForeground(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        TRACING_DISTANCE = intent.getFloatExtra(getString(R.string.TracingDistancePreference), 2);
        SEDENTARY_TIME = intent.getLongExtra(getString(R.string.SedentaryTimePreference), 300);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Log.v("Location Service", "Location changed");
                long currentTime = System.currentTimeMillis();
                if(timeLastMoved < (currentTime - SEDENTARY_TIME*1000)) {
                    Log.v("Location Tracking", "User has been in location long " +
                            "enough to get infected");
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
        if(checkPermission())
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, TRACING_DISTANCE, listener);
        timeLastMoved = System.currentTimeMillis();
        return new TracerServiceBinder();
    }

    private boolean checkPermission(){
        return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }
}
