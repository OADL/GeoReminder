package cse.oadl.geo_alarm;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String TAG = "MapActivity";
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));

    //widgets
    private AutoCompleteTextView searchText;
    private Button infobutton , DoneButton , CancelButton;
    private FloatingActionButton listbutton;
    private LinearLayout addmarkerlout;
    private SupportMapFragment mapFragment;
    private EditText ReminderName , ReminderAddress;
    private View view;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlaceAutocompleteAdapter autocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private Marker mMarker;
    private Circle mCircle;
    private Reminder mreminder;
    private ArrayList<Reminder> reminders;
    private Boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        searchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        infobutton = (Button) findViewById(R.id.infobutt);
        infobutton.setEnabled(false);
        reminders = new ArrayList<>();
        listbutton = /*(FloatingActionButton)*/ findViewById(R.id.listbutton);
        addmarkerlout = (LinearLayout) findViewById(R.id.addmarker);
        ReminderName = (EditText) findViewById(R.id.ReminderName);
        ReminderAddress = (EditText) findViewById(R.id.AddressText);
        DoneButton = (Button) findViewById(R.id.DoneButton);
        CancelButton = (Button) findViewById(R.id.CancelButton);
        view = findViewById(R.id.view);
        getLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(started){
            mMap.clear();
            try {
                getReminders();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        123);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    123);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)  {
        mMap = googleMap;
        if (mLocationPermissionsGranted) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            init();
        }
    }

    private void init() throws SecurityException {

        mMap.setPadding(0,180,0,0);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.setMyLocationEnabled(true);
        moveToDeviceLocation();

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        autocompleteAdapter = new PlaceAutocompleteAdapter(this,mGoogleApiClient,LAT_LNG_BOUNDS,null);

        searchText.setAdapter(autocompleteAdapter);
        searchText.setOnItemClickListener(mAutocompleteClickListener);
        searchText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchText.setCursorVisible(true);
            }
        });
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_GO
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER
                        && keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                    geoLocate();
                }

                return false;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(!infobutton.isEnabled()){
                    infobutton.setEnabled(true);
                }
                moveCamera(marker.getPosition(),DEFAULT_ZOOM,"no");
                mMarker = marker;
                mMarker.showInfoWindow();
                return true;
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                // TODO Auto-generated method stub
                geoLocate(point);
            }
        });

        infobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        mMarker.showInfoWindow();
                    }
                }catch (NullPointerException e){

                }
            }
        });

        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideaddmarkerlayout();
                mMarker.remove();
                mCircle.remove();
                mreminder = null;
                ReminderName.setText(null);
                ReminderName.clearFocus();
                ReminderName.setError(null);
            }
        });

        DoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ReminderName.getText().toString().isEmpty()){
                    ReminderName.setError("Name is empty.");
                }else{
                    ReminderName.setError(null);
                    String name = ReminderName.getText().toString();
                    mreminder.setName(name);
                    mMarker.setTitle(name);
                    mMarker.hideInfoWindow();
                    mMarker.showInfoWindow();
                    reminders.add(mreminder);
                    try {
                        saveReminder(reminders.size() - 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ReminderName.setText(null);
                    ReminderName.clearFocus();
                    hideaddmarkerlayout();
                }
            }
        });

        listbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                started = true;
                Intent intent = new Intent(MapsActivity.this,RemindersActivity.class);
                startActivity(intent);
            }
        });

        hideSoftKeyboard();
        try {
            getReminders();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void geoLocate(){
        String searchString = searchText.getText().toString();
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList = new ArrayList<>();
        try{
            addressList = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            System.err.println("IOEXCEPTIONNN");
        }

        if(addressList.size() > 0){
            Address address = addressList.get(0);
            Log.d(TAG , "geolocat: found a location: "+address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,"");
            mreminder.setAddress(address.getAddressLine(0));
            mMarker.setSnippet(address.getAddressLine(0));
            mMarker.hideInfoWindow();
            mMarker.showInfoWindow();
        }
        showaddmarkerlayout();
    }

    private void geoLocate(LatLng latLng){
        moveCamera(latLng, DEFAULT_ZOOM, "");
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList = new ArrayList<>();
        try{
            addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude , 1);
        }catch (IOException e){
            System.err.println("IOEXCEPTIONNN");
        }

        if(addressList.size() > 0){
            Address address = addressList.get(0);
            Log.d(TAG , "geolocat: found a location: "+address.toString());
            mreminder.setAddress(address.getAddressLine(0));
            mMarker.setSnippet(address.getAddressLine(0));
            mMarker.hideInfoWindow();
            mMarker.showInfoWindow();
        }
        showaddmarkerlayout();
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom),800,null);
        //mMap.clear();
        //mMarker.hideInfoWindow();
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
        if(!title.equals("no")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("Location")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker1));
            mMarker = mMap.addMarker(options);
            mMarker.showInfoWindow();

            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .radius(350)
                    .strokeColor(0xffadd8e6)
                    .fillColor(0x70add8e6)
                    .strokeWidth(2);
            mCircle = mMap.addCircle(circleOptions);

            mreminder = new Reminder(mMarker.getPosition().latitude,mMarker.getPosition().longitude);

            if(!infobutton.isEnabled())
                infobutton.setEnabled(true);

            searchText.setText("");
            hideSoftKeyboard();
        }
    }

    private void moveToDeviceLocation(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            Task location = fusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Location currentLocation = (Location) task.getResult();
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude()
                                ,currentLocation.getLongitude())));
                    }
                }
            });
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 123: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }

        }
    }

    private void hideSoftKeyboard(){
        View view = this.getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            searchText.setCursorVisible(false);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void showaddmarkerlayout(){
        if(mMarker == null){
            return;
        }
        ReminderAddress.setText(mMarker.getSnippet());
        addmarkerlout.setTranslationY(addmarkerlout.getHeight());
        addmarkerlout.setVisibility(View.VISIBLE);
        addmarkerlout.animate().setDuration(500).translationYBy(-addmarkerlout.getHeight()).start();
        view.setVisibility(View.VISIBLE);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }

    private void hideaddmarkerlayout(){
        addmarkerlout.animate().setDuration(500).translationYBy(addmarkerlout.getHeight()).start();
        view.setVisibility(View.GONE);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
    }

    private void getReminders() throws FileNotFoundException,IOException,ClassNotFoundException{
        FileInputStream fis = this.openFileInput("reminders.txt");
        ObjectInputStream is = new ObjectInputStream(fis);
        int i = -1;
        if(fis != null){
            i = is.readInt();
            is.close();
            fis.close();
            for(int l = 0 ; l < i ; l++){
                fis = this.openFileInput("reminder"+l+".txt");
                is = new ObjectInputStream(fis);
                Reminder r;
                if((r = (Reminder) is.readObject()) != null){
                    reminders.add(r);
                    MarkerOptions options = new MarkerOptions()
                            .position(new LatLng(r.getLatitude(),r.getLongitude()))
                            .title(r.getName())
                            .snippet(r.getAddress())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker1));
                    mMap.addMarker(options);

                    CircleOptions circleOptions = new CircleOptions()
                            .center(new LatLng(r.getLatitude(),r.getLongitude()))
                            .radius(350)
                            .strokeColor(0xffadd8e6)
                            .fillColor(0x70add8e6)
                            .strokeWidth(2);
                    mMap.addCircle(circleOptions);
                }
                is.close();
                fis.close();
            }
        }else{
            is.close();
            fis.close();
        }
    }

    public void saveReminder(int i) throws IOException{
        FileOutputStream fos = openFileOutput("reminder"+i+".txt", Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(mreminder);
        os.close();
        fos.close();

        fos = openFileOutput("reminders.txt", Context.MODE_PRIVATE);
        os = new ObjectOutputStream(fos);
        os.writeInt(i+1);
        os.close();
        fos.close();
    }
    //--------------------------- google places API autocomplete suggestions -----------------//

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = autocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, "");
            mreminder.setAddress(place.getAddress().toString());
            mMarker.setSnippet(place.getAddress().toString());
            mMarker.hideInfoWindow();
            mMarker.showInfoWindow();
            showaddmarkerlayout();
            places.release();
        }
    };
}
