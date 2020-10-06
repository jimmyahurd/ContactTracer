package edu.temple.contacttracer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

//Container for JSONObject data fields that can be printed to file
public class Contact implements Serializable {
    private String id;
    private double latitude;
    private double longitude;
    private long timeBegin;
    private long timeEnd;

    public Contact(JSONObject contact) throws JSONException {
        id = contact.getString("uuid");
        latitude = contact.getDouble("latitude");
        longitude = contact.getDouble("longitude");
        timeBegin = contact.getLong("sedentary_begin");
        timeEnd = contact.getLong("sedentary_end");
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", id);
        jsonObject.put("latitude", latitude);
        jsonObject.put("longitude", longitude);
        jsonObject.put("sedentary_begin", timeBegin);
        jsonObject.put("sedentary_end", timeEnd);
        return jsonObject;
    }
}
