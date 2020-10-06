package edu.temple.contacttracer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

//Fragment used to show user information relating to when they could have been exposed to COVID
public class TraceFragment extends Fragment {
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";
    private static final String TIME_KEY = "time";

    double latitude, longitude;
    long time;
    MapView mapView;

    //Requires a JSONObject representing a contact to function
    //Latitude and longitude are pulled out to show the location of exposure on a map
    //Time is pulled out to show the day and time that the exposure occurred.
    public static TraceFragment newInstance(JSONObject contact) throws JSONException {
        TraceFragment fragment = new TraceFragment();

        double latitude = contact.getDouble("latitude");
        double longitude = contact.getDouble("longitude");
        long time = contact.getLong("sedentary_begin");

        Bundle arguments = new Bundle();
        arguments.putDouble(LATITUDE_KEY, latitude);
        arguments.putDouble(LONGITUDE_KEY, longitude);
        arguments.putLong(TIME_KEY, time);

        fragment.setArguments(arguments);
        return fragment;
    }

    //When map is created, it adds a marker where there was a possible exposure
    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            LatLng contact = new LatLng(latitude, longitude);
            googleMap.addMarker(new MarkerOptions()
                .position(contact)
                .title("Possible Exposure"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(contact, 15));
            mapView.onResume();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(args != null){
            latitude = args.getDouble(LATITUDE_KEY);
            longitude = args.getDouble(LONGITUDE_KEY);
            time = args.getLong(TIME_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trace, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        //Populates textview with date and time that exposure occurred
        TextView textView = view.findViewById(R.id.date);
        Instant instant = Instant.ofEpochMilli(time);
        LocalDateTime date = LocalDateTime.ofInstant(instant, ZoneId.of("America/New_York"));
        textView.setText(date.toString());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MapView map = view.findViewById(R.id.mapView);
        if(map != null){
            map.getMapAsync(callback);
        }
    }
}