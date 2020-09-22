package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Notification;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements StartupFragment.StartupFragmentListener {

    private final String UUIDKEY = "UUID";
    private ArrayList<edu.temple.contacttracer.UUID> UUIDs;

    Fragment fragment;

    TracerService.TracerServiceBinder lsBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!checkPermission()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        }
        getUUIDs();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(getString(R.string.LocationChannelID), "All Notifications", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);

        fragment = StartupFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void getUUIDs(){
        File file = new File(getFilesDir(), getString(R.string.UUIDFileName));
        if(file.length() == 0){
            Log.d("Main", "Created first UUID");
            UUIDs = new ArrayList<UUID>();
            UUIDs.add(new UUID());
        }else {
            try {
                if (file.exists()) {
                    String path = file.getPath();
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String response;
                    StringBuilder builder = new StringBuilder();
                    while ((response = reader.readLine()) != null) {
                        builder.append(response);
                    }
                    JSONArray jsonArray = new JSONArray(builder.toString());
                    UUIDs = new ArrayList<UUID>(jsonArray.length() + 1);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        UUID id = new UUID(jsonArray.getJSONObject(i));
                        if(!id.olderThan14Days())
                            UUIDs.add(id);
                    }
                    if(!UUIDs.get(UUIDs.size() - 1).youngerThan1Day())
                        UUIDs.add(new UUID());
                    Log.d("Main", "Grabbed UUIDs from file");
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
                file.delete();
                Log.e("Main", "file deleted");
            }
        }
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
    protected void onStop() {
        super.onStop();

        try {
            Log.d("Main", "Writing to file");
            File file = new File(getFilesDir(), getString(R.string.UUIDFileName));
            if(!file.exists())
                file.createNewFile();
            String path = file.getPath();
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));

            JSONArray jsonArray = new JSONArray();
            for(UUID id : UUIDs){
                jsonArray.put(id.toJSON());
            }
            writer.write(jsonArray.toString());
            writer.flush();
            writer.close();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
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
        if(lsBinder != null)
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
