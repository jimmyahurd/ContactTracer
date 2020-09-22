package edu.temple.contacttracer;

import android.os.Parcel;
import android.os.Parcelable;

public class UUID implements Parcelable {
    private String id;
    private long created;

    public UUID(){
        id = java.util.UUID.randomUUID().toString();
        created = System.currentTimeMillis();
    }

    protected UUID(Parcel in) {
        id = in.readString();
        created = in.readLong();
    }

    public static final Creator<UUID> CREATOR = new Creator<UUID>() {
        @Override
        public UUID createFromParcel(Parcel in) {
            return new UUID(in);
        }

        @Override
        public UUID[] newArray(int size) {
            return new UUID[size];
        }
    };

    public boolean olderThan14Days(){
        return created < System.currentTimeMillis()*14*24*60*60*1000;
    }

    public boolean youngerThan1Day(){
        return created > System.currentTimeMillis()*14*24*60*60*1000;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(created);
    }
}
