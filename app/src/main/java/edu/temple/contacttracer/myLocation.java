package edu.temple.contacttracer;

import android.location.Location;

import java.io.Serializable;

//Data container for location where user was at rest for longer than SEDENTARY_TIME
public class myLocation implements Serializable {
    private double latitude;
    private double longitude;
    private long created;
    private String UUID;

    public myLocation(Location location, String uuid){
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        created = System.currentTimeMillis();
        UUID = uuid;
    }

    public boolean olderThan14Days(){
        return created < (System.currentTimeMillis() - (14*24*60*60*1000));
    }

    public String getUUID(){
        return UUID;
    }
}
