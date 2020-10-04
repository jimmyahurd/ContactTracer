package edu.temple.contacttracer;

import android.location.Location;

import java.io.Serializable;

public class myLocation implements Serializable {
    private double latitude;
    private double longitude;
    private long created;

    public myLocation(Location location){
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        created = System.currentTimeMillis();
    }

    public boolean olderThan14Days(){
        return created < (System.currentTimeMillis() - (14*24*60*60*1000));
    }
}
