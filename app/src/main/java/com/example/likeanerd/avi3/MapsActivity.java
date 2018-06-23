package com.example.likeanerd.avi3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;

    public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener{


        private GoogleMap mMap;
        private GoogleApiClient client;
        private LocationRequest locationRequest;
        private Location mlastlocation;
        private FirebaseAuth mAuth;
        private DatabaseReference mRef;
        private Marker currentLocationmMarker ;
        private  Intent myService;
        public static final int REQUEST_LOCATION_CODE = 99;
        double latitude,longitude;
        private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
        private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
        private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

        private Boolean mLocationPermissionsGranted = false;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_maps);

            getLocationPermission();

            if(mLocationPermissionsGranted==true)
            {
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);


            }

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

        }



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
                                Toast.makeText(this, "Allow permissions!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        mLocationPermissionsGranted = true;

                    }
                }

            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

                          bulidGoogleApiClient();
                mMap.setMyLocationEnabled(true);

        }


        protected synchronized void bulidGoogleApiClient() {
            client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
            client.connect();

        }

        @Override
        public void onLocationChanged(Location location) {

            Toast.makeText(this, "1 sec",Toast.LENGTH_LONG ).show();

            Log.d("Location","location.getLatitude()");

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            mlastlocation = location;
            if(currentLocationmMarker != null)
            {
                currentLocationmMarker.remove();

            }
            Log.d("lat = ",""+latitude);
            LatLng latLng = new LatLng(location.getLatitude() , location.getLongitude());


            currentLocationmMarker=mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            updateLocation();


        }

        private void updateLocation() {

            Toast.makeText(this, "db ",Toast.LENGTH_LONG ).show();

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users Available");
            GeoFire geofire = new GeoFire(ref);
            geofire.setLocation(userId, new GeoLocation(mlastlocation.getLatitude(), mlastlocation.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if (error != null) {
                        Toast.makeText(getApplicationContext(), "Error : "+error,Toast.LENGTH_LONG ).show();

                    } else {
                       Toast.makeText(getApplicationContext(), "Location saved on server successfully!",Toast.LENGTH_LONG ).show();
                    }


                }
            });




        }


        @SuppressLint("MissingPermission")
        @Override
        public void onConnected(@Nullable Bundle bundle) {

            Toast.makeText(this, "Please Turn on GPS if its off ",Toast.LENGTH_LONG ).show();



            locationRequest = new LocationRequest();
            locationRequest.setInterval(100);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);

        }


        public boolean checkLocationPermission()
        {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED )
            {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
                {
                    ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
                }
                else
                {
                    ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
                }
                return false;

            }
            else
                return true;
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
