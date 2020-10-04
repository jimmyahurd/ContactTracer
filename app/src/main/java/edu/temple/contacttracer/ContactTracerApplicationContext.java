package edu.temple.contacttracer;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ContactTracerApplicationContext extends Application implements UUIDtracker{
    protected Set<UUID> UUIDs;
    protected UUID currentID;
    protected Set<String> ids;
    protected Set<JSONObject> contacts;
    protected Set<myLocation> myLocations;
    protected float TRACING_DISTANCE;
    protected long SEDENTARY_TIME;
    protected Location currentLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        readFromFile();
        PeriodicWorkRequest dataCleaning =
                new PeriodicWorkRequest.Builder(dataCleaner.class,
                        1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "Data Cleaning",
                ExistingPeriodicWorkPolicy.KEEP,
                dataCleaning);
    }

    public void readFromFile() {
        File file = new File(getFilesDir(), getString(R.string.DataFileName));
        if(file.length() == 0){
            Log.d("Application Context", "Created first UUID");
            currentID = new UUID();
            UUIDs = new HashSet<>();
            UUIDs.add(currentID);
            ids = new HashSet<>();
            ids.add(currentID.getID());
            myLocations = new HashSet<>();
            contacts = new HashSet<>();
            TRACING_DISTANCE = 2;
            SEDENTARY_TIME = 300;
        }else {
            try {
                String path = file.getPath();
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path));
                UUIDs = (Set<UUID>)inputStream.readObject();
                ids = new HashSet<>();
                for(UUID id: UUIDs)
                    ids.add(id.getID());
                currentID = (UUID)inputStream.readObject();
                contacts = (Set<JSONObject>)inputStream.readObject();
                myLocations = (Set<myLocation>)inputStream.readObject();
                TRACING_DISTANCE = inputStream.readFloat();
                Log.d("Application Context", "" + TRACING_DISTANCE);
                SEDENTARY_TIME = inputStream.readLong();
                Log.d("Application Context", "" + SEDENTARY_TIME);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                file.delete();
                Log.e("Application Context", "file deleted");
            }
        }
        Log.d("Application Context", "Have " + UUIDs.size() + " UUIDs");
    }

    public void writeToFile(){
        File file = new File(getFilesDir(), getString(R.string.DataFileName));
        try {
            String path = file.getPath();
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(path));
            outputStream.writeObject(UUIDs);
            outputStream.writeObject(currentID);
            outputStream.writeObject(contacts);
            outputStream.writeObject(myLocations);
            outputStream.writeFloat(TRACING_DISTANCE);
            outputStream.writeLong(SEDENTARY_TIME);
            outputStream.close();
            Log.d("Application Context", "Wrote to file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addID() {
        currentID = new UUID();
        UUIDs.add(currentID);
        ids.add(currentID.getID());
        writeToFile();
    }

    @Override
    public Set<UUID> getIDs() {
        return UUIDs;
    }

    @Override
    public String getCurrentID(){
        return currentID.getID();
    }

    @Override
    public boolean addContact(JSONObject contact) throws JSONException {
        if(checkContact(contact)) {
            if(contacts == null)
                contacts = new HashSet<>();
            contacts.add(contact);
            writeToFile();
            return true;
        }else{
            return false;
        }
    }

    private boolean checkContact(JSONObject location) throws JSONException {
        if(currentLocation == null) {
            Log.d("Application Context", "Unable to track current location");
            return false;
        }
        if(ids.contains(location.getString(getString(R.string.PayloadUUID)))){
            Log.d("Application Context" ,"Contact ignored as message was sent by me");
            return false;
        }
        try {
            double latitude = location.getDouble( getString(R.string.PayloadLatitude));
            double longitude = location.getDouble(getString(R.string.PayloadLongitude));
            float distance[] = new float[3];
            Location.distanceBetween(latitude, longitude, currentLocation.getLatitude(), currentLocation.getLongitude(), distance);
            if(distance[0] > TRACING_DISTANCE) {
                Log.d("Application Context", "Contact ignored as distance is too far");
                return false;
            }else{
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Set<JSONObject> getContacts() {
        return contacts;
    }

    @Override
    public void setTracingDistance(float distance) {
        TRACING_DISTANCE = distance;
        writeToFile();
    }

    @Override
    public float getTracingDistance() {
        return TRACING_DISTANCE;
    }

    @Override
    public void setSedentaryTime(long sedentaryTime) {
        SEDENTARY_TIME = sedentaryTime;
        writeToFile();
    }

    @Override
    public long getSedentaryTime() {
        return SEDENTARY_TIME;
    }

    @Override
    public void setCurrentLocation(Location location) {
        currentLocation = location;
    }

    @Override
    public Location getCurrentLocation() {
        return currentLocation;
    }

    @Override
    public void addLocation(Location location) {
        if(myLocations == null)
            myLocations = new HashSet<>();
        myLocations.add(new myLocation(location));
        writeToFile();
    }

    @Override
    public Set<myLocation> getMyLocations(){
        return myLocations;
    }

    @Override
    public void cleanData(){
        readFromFile();
        for(UUID id : UUIDs){
            if(id.olderThan14Days())
                UUIDs.remove(id);
        }
        for(myLocation location : myLocations){
            if(location.olderThan14Days())
                myLocations.remove(location);
        }
        addID(); //method will write all changes to file
    }
}
