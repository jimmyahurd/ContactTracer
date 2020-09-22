package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements StartupFragment.StartupFragmentListener {

    private final String UUIDKEY = "UUID";
    private final String TIMEKEY = "Times";
    private ArrayList<String> UUIDs;
    private ArrayList<Long> UUIDtimes;

    private boolean locationServiceRunning;

    Fragment fragment;

    TracerService.TracerServiceBinder lsBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!checkPermission()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        }
        if(savedInstanceState == null){
            UUIDs = new ArrayList<String>();
            UUIDtimes = new ArrayList<Long>();
            UUIDs.add(UUID.randomUUID().toString());
            UUIDtimes.add(System.currentTimeMillis());
        }else{
            String[] ids = savedInstanceState.getStringArray(UUIDKEY);
            long[] times = savedInstanceState.getLongArray(TIMEKEY);
            UUIDs = new ArrayList<String>(ids.length*2);
            UUIDtimes = new ArrayList<Long>(times.length*2);
            for(int i = 0; i < ids.length; i++){
                UUIDs.add(ids[i]);
                UUIDtimes.add(times[i]);
            }
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(getString(R.string.LocationChannelID), "All Notifications", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);

        fragment = StartupFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit();
    }

    private boolean checkPermission(){
        return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "App cannot function without access to location", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        long[] times = new long[UUIDtimes.size()];
        String[] ids = new String[UUIDs.size()];
        UUIDs.toArray(ids);
        for(int i = 0; i < UUIDtimes.size(); i++){
            times[i] = UUIDtimes.get(i);
        }

        outPersistentState.putLongArray(TIMEKEY, times);
        outPersistentState.putStringArray(UUIDKEY, ids);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void startServiceButtonPressed() {
        //start foreground service
        Log.d("Main", "Start service button pressed");
        Intent intent = new Intent(this, TracerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            lsBinder = (TracerService.TracerServiceBinder) service;
            lsBinder.showNotification();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            lsBinder = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void stopServiceButtonPressed() {
        //stop foreground service
        if(lsBinder != null) {
            lsBinder.stop();
            unbindService(serviceConnection);
            lsBinder = null;
        }
        Log.d("Main", "Stop Service Button Pressed");
    }
}
