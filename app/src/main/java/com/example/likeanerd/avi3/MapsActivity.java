package com.example.likeanerd.avi3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

    public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener{


        private GoogleMap mMap;
        private GoogleApiClient client;
        private LocationRequest locationRequest;
        private FusedLocationProviderClient mFusedLocationClient;
        private Location mlastlocation;
        private FirebaseAuth mAuth;
        private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
        private DatabaseReference mRef;
        private Marker currentLocationmMarker ;
        private  Intent myService;
        double latitude,longitude;
        private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
        private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
        private Boolean mLocationPermissionsGranted = false;
        private LatLng destinationLocation;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_maps);
            getLocationPermission();

            if(mLocationPermissionsGranted)
            {
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);


            }
            //adding toolbar to the activity
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

        }


        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                }else{
                    getLocationPermission();                }
            }
            bulidGoogleApiClient();
            mMap.setMyLocationEnabled(true);


        }
        //connecting to google maps server
        protected synchronized void bulidGoogleApiClient() {
            client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
            //onConnected method will be invoked asynchronously when the connect request has successfully completed.
            client.connect();

        }

        @Override
        public void onLocationChanged(Location location) {

            Toast.makeText(this, "1 sec",Toast.LENGTH_SHORT ).show();
            //Log.d("Location","location.getLatitude()");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            mlastlocation = location;
            if(currentLocationmMarker != null)
            {
                currentLocationmMarker.remove();

            }
            LatLng latLng = new LatLng(location.getLatitude() , location.getLongitude());

            currentLocationmMarker=mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            updateLocation();


        }

        //updating location to the firebase database using geofire API
        private void updateLocation() {

            Toast.makeText(this, "db ",Toast.LENGTH_LONG ).show();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            //gets the reference
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users Available").child(userId);
            GeoFire geofire = new GeoFire(ref);
            geofire.setLocation(userId, new GeoLocation(mlastlocation.getLatitude(), mlastlocation.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if (error != null) {
                        Toast.makeText(getApplicationContext(), "Error : "+error,Toast.LENGTH_LONG ).show();

                    } else {
                       Toast.makeText(getApplicationContext(), "Location saved on server successfully!",Toast.LENGTH_SHORT ).show();
                    }


                }
            });




        }

        //invoked asynchronously when client.connect is called
        @Override
        public void onConnected(@Nullable Bundle bundle) {

            Toast.makeText(this, "Inside connected",Toast.LENGTH_SHORT ).show();

            locationRequest = new LocationRequest();
            locationRequest.setInterval(100);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                }else{
                    getLocationPermission();
                }
            }

            //to refresh location after every 1 sec
            //calls onLocationChanges
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);

        }




       // @Override
       //protected void onStop() {
          // super.onStop();
           //}

        @Override
        public void onConnectionSuspended(int i) {
            startService(myService);


        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {



        }




        //checking permissions

        private void getLocationPermission() {

            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION};

            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                        COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionsGranted = true;

                } else {
                    ActivityCompat.requestPermissions(this,
                            permissions,
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            mLocationPermissionsGranted = false;

            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE: {
                    if (grantResults.length > 0) {
                        for (int i = 0; i < grantResults.length; i++) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                mLocationPermissionsGranted = false;
                                Toast.makeText(MapsActivity.this, "Please Turn on GPS", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        mLocationPermissionsGranted = true;

                    }
                }

            }
        }



    //overflow menu options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menuHelp:
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("User Requests");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(mlastlocation.getLatitude(), mlastlocation.getLongitude()));


                break;

            case R.id.menuSettings:


                break;
            case R.id.menuLogout:

                FirebaseAuth.getInstance().signOut();
                client.disconnect();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
        }



        return true;
    }
}
