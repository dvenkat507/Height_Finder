package com.example.height_finder;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private static final String TAG ="MainActivity";

    private GoogleMap mMap;
    protected LocationManager locationManager;
    public static int marker_value = 0;
    public static Marker marker1,marker2;
    private static  double distance = 0.0, bearing = 0.0;
    private static  boolean ready = false;
    private Button positon_marker,clear_marker;
    private Polyline pline;
    public static final double Raidus = 6372.8;
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private int[] mPrns = null, pPrns = null;
    private float[] mSnrCn0s = null;
    private float[] mElevs = null, pElevs = null;
    private float[] mAzims = null, pAzims = null;
    private int[] mConstellationType = null;
    private boolean[] mHasEphemeris = null;
    private boolean[] mHasAlmanac = null;
    private boolean[] mUsedInFix = null;
    private int mSvCount = 0;
    private float mSnrCn0UsedAvg = 0.0f;
    private float mSnrCn0InViewAvg = 0.0f;
    private double minViewAngle = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setup();
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        positon_marker = (Button)findViewById(R.id.btn_position_marker);
        positon_marker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng currentlocation = getLocation();
                if (currentlocation != null) {
                    if (marker_value == 0) {
                        Log.d(TAG, "run: " + currentlocation.latitude + " " + currentlocation.longitude);
                        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.marker1);
                        Bitmap b = bitmapdraw.getBitmap();
                        marker1 = mMap.addMarker(new MarkerOptions().position(currentlocation).title("Marker in Current location").icon(BitmapDescriptorFactory.fromBitmap(b)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlocation, 16.0f));
                        Resources res = getResources();
                        positon_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.marker2_xml, null), null, null, null);
                        marker_value++;
                        ready = false;
                    } else if (marker_value == 1) {
                        //LatLng pos = new LatLng(13.0723449, 77.5758208);
                        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.marker2);
                        Bitmap b = bitmapdraw.getBitmap();
                        marker2 = mMap.addMarker(new MarkerOptions().position(currentlocation).title("Marker in Current location").icon(BitmapDescriptorFactory.fromBitmap(b)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlocation, 16.0f));
                        pline = mMap.addPolyline(new PolylineOptions()
                                .add(marker1.getPosition())
                                .add(marker2.getPosition()));
                        distance = findDistance(marker1, marker2);
                        bearing = findbearing(marker2, marker1);
                        Resources res = getResources();
                        positon_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.cancelmarkerxml, null), null, null, null);
                        positon_marker.setEnabled(false);
                        marker_value++;
                        if(distance <= 25) {
                            minViewAngle = Math.toDegrees(Math.atan(7.5 / distance));
                            ready = true;
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "Your are to far from building come close", Toast.LENGTH_LONG ).show();
                        }
                    }
                }
            }
        });
        clear_marker = (Button)findViewById(R.id.btn_clear_marker);
        clear_marker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(marker_value == 2)
                {
                    marker2.remove();
                    marker_value--;
                    positon_marker.setEnabled(true);
                    Resources res = getResources();
                    positon_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.marker2_xml, null), null, null, null);
                    pline.remove();
                }
                else if(marker_value == 1)
                {
                    marker1.remove();
                    Resources res = getResources();
                    positon_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.marker1_xml, null), null, null, null);
                    marker_value--;
                }
                ready = false;
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        updateLocationUI();

        final LatLng currentlocation = getLocation();
        if (currentlocation != null)
        {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlocation, 16.0f));
        }
    }

    private double findDistance(Marker marker1,Marker marker2)
    {
        double lat1 = marker1.getPosition().latitude;
        double lon1 = marker1.getPosition().longitude;
        double lat2 = marker2.getPosition().latitude;
        double lon2 = marker2.getPosition().longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat/2),2) + Math.pow(Math.sin(dLon/2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2* Math.asin(Math.sqrt(a));
        return Raidus*c*1000.2f;
    }

    private double findbearing(Marker m1,Marker m2)
    {
        double lat1 = Math.toRadians(m1.getPosition().latitude);
        double lon1 = Math.toRadians(m1.getPosition().longitude);
        double lat2 = Math.toRadians(m2.getPosition().latitude);
        double lon2 = Math.toRadians(m2.getPosition().longitude);
        double X = Math.cos(lat2) * Math.sin(lon2-lon1);
        double Y = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2-lon1);
        double b = Math.atan2(X,Y);
        b = Math.toDegrees(b);
        if(b<0)
        {
            b = 360 + b;
        }
        return (b);
    }

    private LatLng getLocation()
    {
        LatLng currentLocation = null;
        double release=Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)","$1"));
        if(release>=6.0)
        {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == 0 && ContextCompat.checkSelfPermission(this.getApplicationContext(), "android.permission.ACCESS_FINE_LOCATION") == 0)
            {

            }
            else
            {
                Intent i = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(i);
            }
        }

        Location location=null,location1=null,location2=null;
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        location2 = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        if (location != null)
        {
            double latti = location.getLatitude();
            double longi = location.getLongitude();
            currentLocation = new LatLng(latti, longi);
        }
        else  if (location1 != null)
        {
            double latti = location1.getLatitude();
            double longi = location1.getLongitude();
            currentLocation = new LatLng(latti, longi);
        }
        else  if (location2 != null)
        {
            double latti = location2.getLatitude();
            double longi = location2.getLongitude();
            currentLocation = new LatLng(latti, longi);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Location not found", Toast.LENGTH_SHORT).show();
        }
        return currentLocation;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode==KeyEvent.KEYCODE_BACK)
        {
            finish();
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
        return super.onKeyDown(keyCode,event);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        double release=Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)","$1"));
        if(release>=6.0)
        {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == 0 && ContextCompat.checkSelfPermission(this.getApplicationContext(), "android.permission.ACCESS_FINE_LOCATION") == 0)
            {

            }
            else
            {
                Intent i = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(i);
            }
        }
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // Check if enabled and if not send user to the GPS settings
        if (enabled)
        {

        }
        else
        {
            Intent i = new Intent(this, FeaturesActivity.class);
            startActivity(i);
        }
    }

    private void updateLocationUI()
    {
        if (mMap == null)
        {
            return;
        }
        try
        {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        catch (Exception e)
        {
            Log.e("Exception: ", e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle)
    {
    }

    @Override
    public void onProviderEnabled(String s)
    {
    }

    @Override
    public void onProviderDisabled(String s)
    {
    }

    private GnssStatus.Callback mGnssCallback = new GnssStatus.Callback()
    {
        @Override
        public void onStarted()
        {
            super.onStarted();
        }

        @Override
        public void onStopped()
        {
            super.onStopped();
        }

        @Override
        public void onSatelliteStatusChanged(GnssStatus status)
        {
            super.onSatelliteStatusChanged(status);
            setGnssStatus(status);
        }
    };

    private void setup()
    {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            locationManager.registerGnssStatusCallback(mGnssCallback);
            Toast.makeText(this, "Setup Complete", Toast.LENGTH_SHORT).show();
        }
        catch(SecurityException e)
        {
            Toast.makeText(this,"ERROR:LOC-SEC",Toast.LENGTH_LONG).show();
        }
        catch (NullPointerException e)
        {
            Toast.makeText(this,"ERROR:LOC-NUL",Toast.LENGTH_LONG).show();
        }
    }

    public synchronized void setGnssStatus(GnssStatus status)
    {
        if(ready)
        {
            if (mPrns == null)
            {
                final int MAX_LENGTH = 255;
                mPrns = new int[MAX_LENGTH];
                mSnrCn0s = new float[MAX_LENGTH];
                mElevs = new float[MAX_LENGTH];
                mAzims = new float[MAX_LENGTH];
                mConstellationType = new int[MAX_LENGTH];
                mHasEphemeris = new boolean[MAX_LENGTH];
                mHasAlmanac = new boolean[MAX_LENGTH];
                mUsedInFix = new boolean[MAX_LENGTH];
            }

            int length = status.getSatelliteCount();
            mSvCount = 0;
            int svInViewCount = 0;
            int svUsedCount = 0;
            float cn0InViewSum = 0.0f;
            float cn0UsedSum = 0.0f;
            mSnrCn0InViewAvg = 0.0f;
            mSnrCn0UsedAvg = 0.0f;
            while (mSvCount < length)
            {
                if ((status.getAzimuthDegrees(mSvCount) >= (bearing - minViewAngle)) && (status.getAzimuthDegrees(mSvCount) <= (bearing + minViewAngle))) {
                    mSnrCn0s[mSvCount] = status.getCn0DbHz(mSvCount);  // Store C/N0 values (see #65)
                    mElevs[mSvCount] = status.getElevationDegrees(mSvCount);
                    mAzims[mSvCount] = status.getAzimuthDegrees(mSvCount);
                    mPrns[mSvCount] = status.getSvid(mSvCount);
                    mConstellationType[mSvCount] = status.getConstellationType(mSvCount);
                    mHasEphemeris[mSvCount] = status.hasEphemerisData(mSvCount);
                    mHasAlmanac[mSvCount] = status.hasAlmanacData(mSvCount);
                    mUsedInFix[mSvCount] = status.usedInFix(mSvCount);
                    // If satellite is in view, add signal to calculate avg
                    if (status.getCn0DbHz(mSvCount) != 0.0f) {
                        svInViewCount++;
                        cn0InViewSum = cn0InViewSum + status.getCn0DbHz(mSvCount);
                    }
                    if (status.usedInFix(mSvCount)) {
                        svUsedCount++;
                        cn0UsedSum = cn0UsedSum + status.getCn0DbHz(mSvCount);
                    }

                }
                mSvCount++;
            }
            ParseSatelliteData();

            if (svInViewCount > 0) {
                mSnrCn0InViewAvg = cn0InViewSum / svInViewCount;
            }
            if (svUsedCount > 0) {
                mSnrCn0UsedAvg = cn0UsedSum / svUsedCount;
            }
        }

    }

    private void ParseSatelliteData()
    {
        Log.d(TAG, "ParseSatelliteData: Start");
        int lenght = mAzims.length;
        for(int i = 0; i < lenght; i++)
        {
            if (mPrns[i] != 0) {
                Log.d(TAG, "ParseSatelliteData: " + mAzims[i] + " " + mPrns[i] + " " + mElevs[i]);
            }
        }
    }
}
