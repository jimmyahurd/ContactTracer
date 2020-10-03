package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements StartupFragment.StartupFragmentListener, SettingsFragment.SettingsListener {
    private ArrayList<edu.temple.contacttracer.UUID> UUIDs;

    Fragment startupFragment;
    SharedPreferences preferences;
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    TracerService.TracerServiceBinder lsBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!checkPermission()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        }

        FirebaseMessaging.getInstance().subscribeToTopic("TRACKING")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Subscribed to topic";
                        if (!task.isSuccessful()) {
                            msg = "Failed to subscribe to topic";
                        }
                        Log.d("Main", msg);
                    }
                });

        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(getString(R.string.LocationChannelID), "All Notifications", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);

        makePreferenceListener();

        startupFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if(!(startupFragment instanceof StartupFragment)){
            startupFragment = StartupFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, startupFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.settings) {
            Fragment settings = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, settings)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void makePreferenceListener(){
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(getString(R.string.TracingDistancePreference))){
                    float tracingDistance = (float)sharedPreferences.getInt(getString(R.string.TracingDistancePreference), 2);
                    if(lsBinder != null)
                        lsBinder.changeTracingDistance(tracingDistance);
                }
                if(key.equals(getString(R.string.SedentaryTimePreference))){
                    long sedentaryTime = (long)sharedPreferences.getInt(getString(R.string.SedentaryTimePreference), 300);
                    if(lsBinder != null)
                        lsBinder.changeSedentaryTime(sedentaryTime);
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
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
                    ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path));
                    UUIDs = (ArrayList<UUID>)inputStream.readObject();
                    while(UUIDs.get(0).olderThan14Days()) UUIDs.remove(0);
                    if(!UUIDs.get(UUIDs.size() - 1).youngerThan1Day()) UUIDs.add(new UUID());
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                file.delete();
                Log.e("Main", "file deleted");
            }
        }
        Log.d("Main", "Have " + UUIDs.size() + " UUIDs");
    }

    private void saveUUIDs(){
        try {
            Log.d("Main", "Writing to file");
            File file = new File(getFilesDir(), getString(R.string.UUIDFileName));
            if(!file.exists())
                file.createNewFile();
            String path = file.getPath();
            ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(path));

            writer.writeObject(UUIDs);

            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
    public void startServiceButtonPressed() {
        //start foreground service
        Log.d("Main", "Start service button pressed");

        getUUIDs();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        float tracingDistance = (float)sharedPreferences.getInt(getString(R.string.TracingDistancePreference), 2);
        long sedentaryTime = (long)sharedPreferences.getInt(getString(R.string.SedentaryTimePreference), 300);

        Intent intent = new Intent(this, TracerService.class);
        intent.putExtra(getString(R.string.TracingDistancePreference), tracingDistance);
        intent.putExtra(getString(R.string.SedentaryTimePreference), sedentaryTime);
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
        saveUUIDs();
        Log.d("Main", "Stop Service Button Pressed");
    }

    @Override
    public void generateUUID() {
        Log.d("Main", "New UUID generated");
        UUIDs.add(new UUID());
    }
}
