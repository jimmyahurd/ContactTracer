package edu.temple.contacttracer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

//Container for a UUID String and time of creation
//Has method to tell if UUID is too old, and should thus be discarded
public class UUID implements Serializable {
    private final String IDKEY = "ID";
    private final String CREATEDKEY = "Created";
    private String id;
    private long created;

    public UUID(){
        id = java.util.UUID.randomUUID().toString();
        created = System.currentTimeMillis();
    }

    public boolean olderThan14Days(){
        return created < (System.currentTimeMillis() - (14*24*60*60*1000));
    }

    public String getID(){
        return id;
    }
}
