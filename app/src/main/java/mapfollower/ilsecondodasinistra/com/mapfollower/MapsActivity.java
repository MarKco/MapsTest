package mapfollower.ilsecondodasinistra.com.mapfollower;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mCurrentLocation;
    LocationRequest locationRequest;
    protected final int REQUEST_CHECK_SETTINGS = 20;
    protected final int MIN_DISTANCE_FOR_LINE_TRACING = 2;
    protected final String REQUESTING_LOCATION_UPDATES_SETTING = "This";
    protected boolean mRequestingLocationUpdates = true;
    protected ArrayList<Location> locationsList = new ArrayList<>();    //Contains the list of locations the user has been in
    protected double totalDistance;
    protected Polyline myPath;
    private boolean isCustomZoom;
    private float previousZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);   //Retrieves the Fused Location Provider

        createLocationRequest();
        updateValuesFromBundle(savedInstanceState);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);    //Creates a builder and adds the location request in order to be able to retrieve updates
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());    //The task checks for the location configuration

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //We can initialize location requests here
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        resolvableApiException.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        //They say we can ignore this error... ok.
                    }
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                updateMapWithLocation(location);
//                Toast.makeText(getApplicationContext(), "Siamo in " + location.getLatitude() + " - " + location.getLongitude(), Toast.LENGTH_SHORT).show();
            }
        });

        if(mRequestingLocationUpdates) //This variable is saved when the user changes the activity state, IE when the device is flipped
            startLocationUpdates();

        Button infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayInfo();
                isCustomZoom = false;
            }
        });

    }

    /**
     * This method shows on sceen - in a toast for instance - some useful informations about the path we've taken
     */
    private void displayInfo() {
        NumberFormat formatter = new DecimalFormat("#0.00");
        String number = formatter.format(totalDistance);
        Toast.makeText(MapsActivity.this, "Total distance: " + number + "m", Toast.LENGTH_SHORT).show();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if(previousZoom != cameraPosition.zoom) {
                    isCustomZoom = true;
                }

                previousZoom = cameraPosition.zoom;
            }
        });
    }

    private void logMeThis(String text) {
        if(BuildConfig.DEBUG)
            Log.d("Maptest", text);
    }

    /**
     * Updates the map with given location, places the marker, puts the location in the locationsList
     * @param location
     */
    public void updateMapWithLocation(Location location) {

        //Creates point, marker and moves map
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
//        logMeThis("created point");

        if(locationsList.size() > 0) {
            //Calculates global distance
            float latestDistance = locationsList.get(locationsList.size() -1).distanceTo(location);
            totalDistance = totalDistance + latestDistance;
            logMeThis("Calculated distance: " + latestDistance);

            if(latestDistance > MIN_DISTANCE_FOR_LINE_TRACING)  //We don't draw polylines for less than 5m
            {
                //Adds location to list
                locationsList.add(location);
                logMeThis("drawing poly in existing list");
                addMarkerAndPolyline(point);
            }
        }
        else {
            logMeThis("Adding point in empty list");
            //Adds location to list
            locationsList.add(location);
            addMarkerAndPolyline(point);    //We add the first marker for sure
        } }

    /**
     * Phisically creates the marker and polyline on the map
     * @param point
     */
    protected void addMarkerAndPolyline(LatLng point) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(point).title("Here you are!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(point));

        if(!isCustomZoom)
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

        if(myPath == null)
        {
            logMeThis("There's no polyline yet");
            myPath = mMap.addPolyline(new PolylineOptions().clickable(false)
                    .add(point));
        }
        else {
            logMeThis("Add to existing polyline");
            ArrayList<LatLng> pathPoints = (ArrayList)myPath.getPoints();
            pathPoints.add(point);
            myPath.setPoints(pathPoints);
            mMap.addPolyline(new PolylineOptions().clickable(false)).setPoints(pathPoints);
        }
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
    }

    /**
     * Stops retrieving locations when the activity goes in background
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_SETTING, mRequestingLocationUpdates);
    }


   protected void updateValuesFromBundle(Bundle onSavedInstanceState) {
        if(onSavedInstanceState != null && onSavedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_SETTING)) {
            mRequestingLocationUpdates = onSavedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_SETTING);
        }
   }







    /**
     * Callback called when new locations are available
     */
    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location: locationResult.getLocations()
                 ) {
                updateMapWithLocation(location);
                //Here is where we will want to log the locations updates and possibly call the method which will trace the markers and polylines
            }

        }
    };

}
