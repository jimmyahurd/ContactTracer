package edu.temple.contacttracer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.sql.Time;
import java.util.Date;
import java.util.GregorianCalendar;

public class LocationService extends Service {
    public LocationService() {
    }

    private int TRACING_DISTANCE;
    private int SEDENTARY_TIME;
    private LocationListener listener;
    private LocationManager lm;
    private Location previousLocation;
    private long timeLastMoved;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    class LocationServiceBinder extends Binder {
        public void showNotification() {
            Intent intent = new Intent(LocationService.this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(LocationService.this, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(LocationService.this, "LocationChannel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Location Currently Tracked")
                    .setContentText("This is to alert you that a service that tracks your current location is running")
                    .setContentIntent(pi)
                    .build();

            startForeground(000, notification);
        }

        public void stopNotificaiton() {
            stopThisService();
        }

        public void changeTracingDistance(int tracingDistance) {
            TRACING_DISTANCE = tracingDistance;
        }

        public void changeSedentaryTime(int sedentaryTime) {
            SEDENTARY_TIME = sedentaryTime;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        TRACING_DISTANCE = 2;
        SEDENTARY_TIME = 300;

        lm = getSystemService(LocationManager.class);
        if(checkPermission()) {
            previousLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        timeLastMoved = System.currentTimeMillis();

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //check if user has moved far enough to trigger reaction
                if((int)previousLocation.distanceTo(location) >= TRACING_DISTANCE){
                    //get current time
                    Long currentTime = System.currentTimeMillis();
                    if(currentTime > (timeLastMoved + SEDENTARY_TIME*1000)){
                        //user has been at rest too long and may have been infected
                        Log.v("Location Tracking", "User has been in location long " +
                                "enough to get infected");
                        timeLastMoved = currentTime;
                    }
                    //update previous location as user has moved far enough to reset location
                    previousLocation = location;
                }else{
                    //do nothing as user has not moved far enough yet
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };

        return new LocationServiceBinder();
    }

    public void stopThisService(){
        stopForeground(true);
    }

    private boolean checkPermission(){
        return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }
}
