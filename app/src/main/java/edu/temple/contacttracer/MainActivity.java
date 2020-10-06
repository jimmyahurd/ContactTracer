package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements StartupFragment.StartupFragmentListener, SettingsFragment.SettingsListener, DatePickerFragment.DateSelectedListener {
    Fragment startupFragment;
    SharedPreferences preferences;
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    TracerService.TracerServiceBinder lsBinder;

    BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                JSONObject contact = new JSONObject(intent.getStringExtra(getString(R.string.IntentContactExtra)));
                double latitude = contact.getDouble(getString(R.string.PayloadLatitude));
                double longitude = contact.getDouble(getString(R.string.PayloadLongitude));
                Log.d("Main Activity", "Received Contact at (" + latitude + ", " + longitude + ")");
                //display contact fragment
                TraceFragment traceFragment = TraceFragment.newInstance(contact);
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragmentContainer, traceFragment)
                        .addToBackStack(null)
                        .commit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

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
                        String msg = "Subscribed to tracking topic";
                        if (!task.isSuccessful()) {
                            msg = "Failed to subscribe to tracking topic";
                        }
                        Log.d("Main", msg);
                    }
                });
        FirebaseMessaging.getInstance().subscribeToTopic("TRACING")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Subscribed to tracing topic";
                        if (!task.isSuccessful()) {
                            msg = "Failed to subscribe to tracing topic";
                        }
                        Log.d("Main", msg);
                    }
                });

        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(getString(R.string.LocationChannelID), "All Notifications", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);

        makePreferenceListener();

        /*
        JSONObject contact = new JSONObject();
        try {
            contact.put("latitude", 39.960935);
            contact.put("longitude", -75.390174);
            contact.put("sedentary_begin", System.currentTimeMillis());
            TraceFragment traceFragment = TraceFragment.newInstance(contact);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, traceFragment)
                    .commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
         */

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
            Fragment settings = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if(!(settings instanceof SettingsFragment)) {
                settings = new SettingsFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, settings)
                        .addToBackStack(null)
                        .commit();
            }
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
                    UUIDtracker application = (UUIDtracker)getApplicationContext();
                    application.setTracingDistance(tracingDistance);
                }
                if(key.equals(getString(R.string.SedentaryTimePreference))){
                    long sedentaryTime = (long)sharedPreferences.getInt(getString(R.string.SedentaryTimePreference), 300);
                    UUIDtracker application = (UUIDtracker)getApplicationContext();
                    application.setSedentaryTime(sedentaryTime);
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
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

        Intent intent = new Intent(this, TracerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, TracerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        ((UUIDtracker)getApplicationContext()).inForeground();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.IntentDisplayContact));
        registerReceiver(br, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(lsBinder != null)
            unbindService(serviceConnection);
        ((UUIDtracker)getApplicationContext()).outOfForeground();
        unregisterReceiver(br);
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

    @Override
    public void positiveTestButtonPressed() {
        DialogFragment datePicker = new DatePickerFragment();
        datePicker.show(getSupportFragmentManager(), "datePicker");
    }

    @Override
    public void generateUUID() {
        Log.d("Main", "New UUID generated");
        UUIDtracker application = (UUIDtracker)getApplicationContext();
        application.addID();
    }

    @Override
    public void dateSelected(final long time) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.FCM_Tracing_URL);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Main Activity", response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Main Activity", error.toString());
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("date", "" + time);

                UUIDtracker application = (UUIDtracker) getApplicationContext();
                Set<myLocation> locations = application.getMyLocations();
                JSONArray array = new JSONArray();
                for(myLocation location : locations){
                    array.put(location.getUUID());
                }
                params.put("uuids", array.toString());

                //Log.d("Main Activity", "Sending Message: " + params.toString());

                return params;
            }
        };
        queue.add(request);
    }
}
