package edu.temple.contacttracer;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

public interface UUIDtracker {
    void cleanData();

    void addID();
    Set<UUID> getIDs();
    String getCurrentID();

    boolean addContact(JSONObject contact) throws JSONException;
    Set<JSONObject> getContacts();

    void setTracingDistance(float distance);
    float getTracingDistance();

    void setSedentaryTime(long sedentaryTime);
    long getSedentaryTime();

    void setCurrentLocation(Location location);
    Location getCurrentLocation();

    void addLocation(Location location);
    Set<myLocation> getMyLocations();
}
