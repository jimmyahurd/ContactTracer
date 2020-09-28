package edu.temple.contacttracer;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class UUID{
    private final String IDKEY = "ID";
    private final String CREATEDKEY = "Created";
    private String id;
    private long created;

    public UUID(){
        id = java.util.UUID.randomUUID().toString();
        created = System.currentTimeMillis();
    }

    public UUID(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString(IDKEY);
        created = jsonObject.getLong(CREATEDKEY);
    }

    public boolean olderThan14Days(){
        return created < System.currentTimeMillis()*14*24*60*60*1000;
    }

    public boolean youngerThan1Day(){
        return created > System.currentTimeMillis()*24*60*60*1000;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject toReturn = new JSONObject();
        toReturn.put(IDKEY, id);
        toReturn.put(CREATEDKEY, created);
        return toReturn;
    }
}
