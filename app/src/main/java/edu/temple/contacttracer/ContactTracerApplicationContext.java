package edu.temple.contacttracer;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
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

//Used to share data between different components
//Also handles logic dealing with adding and removing from data
//Reads and writes data to file
public class ContactTracerApplicationContext extends Application implements UUIDtracker{
    //List of data that needs to be tracked by the application
    protected Set<UUID> UUIDs;
    protected UUID currentID;
    protected Set<String> ids;
    protected Set<Contact> contacts;
    protected Set<myLocation> myLocations;
    protected float TRACING_DISTANCE;
    protected long SEDENTARY_TIME;

    //Current location of user
    protected Location currentLocation;

    //Used to know if Main Activity is visible
    private boolean inForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        readFromFile();

        //Creates a worker that will periodically clean some of the data list above
        //see cleanData() for more information
        PeriodicWorkRequest dataCleaning =
                new PeriodicWorkRequest.Builder(dataCleaner.class,
                        1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "Data Cleaning",
                ExistingPeriodicWorkPolicy.KEEP,
                dataCleaning);
    }

    //Used to read in data from a file
    //Called whenever application is recreated
    public void readFromFile() {
        File file = new File(getFilesDir(), getString(R.string.DataFileName));
        if(file.length() == 0){
            //Initializes all the data fields as user has never used this application before
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
                contacts = (Set<Contact>)inputStream.readObject();
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

    //Writes all data to the file
    //Called anytime data is changed
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

    //Adds an ID to user's set of their own UUID's
    //Updates current ID
    @Override
    public void addID() {
        currentID = new UUID();
        UUIDs.add(currentID);
        ids.add(currentID.getID());
        writeToFile();
    }

    //Returns set of User's UUID's
    @Override
    public Set<UUID> getIDs() {
        return UUIDs;
    }

    //Returns the User's current UUID
    @Override
    public String getCurrentID(){
        return currentID.getID();
    }

    //Checks contact to see if they are not the user and that they are close enough for exposure
    //Adds contact to set if both criteria are met
    //Returns true if contact was added, false if contact was not added
    @Override
    public boolean addContact(JSONObject contact) throws JSONException {
        if(checkContact(contact)) {
            if(contacts == null)
                contacts = new HashSet<>();
            contacts.add(new Contact(contact));
            writeToFile();
            return true;
        }else{
            return false;
        }
    }

    //Performs check for above method
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

    //Returns the set of contacts the User has had
    @Override
    public Set<JSONObject> getContacts() throws JSONException {
        Set<JSONObject> newContacts = new HashSet<>();
        for(Contact contact : contacts){
            newContacts.add(contact.toJSON());
        }
        return newContacts;
    }

    //Checks if the user has come into contact with any of the passed id's
    //If user has come into contact with one, it will immediately return the corresponding
    //contact JSONObject
    //If user has not come into contact with any or if the id's are from the user, then it will
    //return a null object
    @Override
    public JSONObject checkContacts(JSONArray ids) throws JSONException {
        if(this.ids.contains(ids.getString(0))){
            Log.d("Application Context", "Message ignored as sent by me");
        }
        for(Contact contact : contacts){
            String contactID = contact.toJSON().getString(getString(R.string.PayloadUUID));
            for(int i = 0; i < ids.length(); i++){
                if(contactID.equals(ids.getString(i))){
                    Log.d("Application Context", "Possible Exposure!");
                    return contact.toJSON();
                }
            }
        }
        Log.d("Application Context", "Never came in contact with this person");
        return null;
    }

    //Changes Tracing Distance
    @Override
    public void setTracingDistance(float distance) {
        TRACING_DISTANCE = distance;
        writeToFile();
    }

    //Returns Tracing Distance
    @Override
    public float getTracingDistance() {
        return TRACING_DISTANCE;
    }

    //Changes Sedentary Time
    @Override
    public void setSedentaryTime(long sedentaryTime) {
        SEDENTARY_TIME = sedentaryTime;
        writeToFile();
    }

    //Returns Sedentary Time
    @Override
    public long getSedentaryTime() {
        return SEDENTARY_TIME;
    }

    //Updates user's current location
    @Override
    public void setCurrentLocation(Location location) {
        currentLocation = location;
    }

    //Returns User's current location
    @Override
    public Location getCurrentLocation() {
        return currentLocation;
    }

    //Adds location where user was at rest for a while
    //Only called when user has not moved for SEDENTARY_TIME
    @Override
    public void addLocation(Location location) {
        if(myLocations == null)
            myLocations = new HashSet<>();
        myLocations.add(new myLocation(location, currentID.getID()));
        writeToFile();
    }

    //Returns set of locations where user was at rest for a while
    @Override
    public Set<myLocation> getMyLocations(){
        return myLocations;
    }

    //Cleans the data by removing old UUID's and old myLocation's (defined as ones older than 14 days)
    //Also generates a new UUID
    //Code runs ~24 hours
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

    //Updates application that main activity is visible
    @Override
    public void nowInForeground() {
        inForeground = true;
    }

    //Updates application that main activity is not visible
    @Override
    public void outOfForeground() {
        inForeground = false;
    }

    //Returns if main activity is visible
    @Override
    public boolean inForeground() {
        return inForeground;
    }
}
