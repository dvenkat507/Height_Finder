package com.example.height_finder;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

class SatParams
{
    private Integer Prn;
    private float Elevation;
    private float Azimuth;
    private float SNRatio;

    public SatParams(Integer Prn, float Elevation, float Azimuth, float SNRatio)
    {
        this.Prn = Prn;
        this.Elevation = Elevation;
        this.Azimuth = Azimuth;
        this.SNRatio = SNRatio;
    }

    public Integer getPrn()
    {
        return this.Prn;
    }

    public float getElevation()
    {
        return this.Elevation;
    }

    public float getAzimuth()
    {
        return this.Azimuth;
    }

    public float getSNRatio()
    {
        return this.SNRatio;
    }
}

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, View.OnClickListener
{
    private static final String TAG ="MapsActivity";
    private Button position_marker,clear_marker;

    protected LocationManager locationManager;
    private GoogleMap mMap;

    private static int marker_value = 0;
    private static Marker marker1,marker2;
    private static boolean ready = false;
    private Polyline pline;

    private static final double Radius = 6372.8;
    private double minViewAngle = 0.0f;
    private double distance = 0.0f, bearing = 0.0f;

//    private static DecimalFormat df2 = new DecimalFormat("#.##");

    private boolean first_readings = true;

    private ArrayList<SatParams> currentReading = new ArrayList<>();
    private ArrayList<SatParams> previousReading = new ArrayList<>();

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
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        position_marker = findViewById(R.id.btn_position_marker);
        position_marker.setOnClickListener(this);
        clear_marker = findViewById(R.id.btn_clear_marker);
        clear_marker.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        if(view.equals(position_marker))
        {
            LatLng currentlocation = getLocation();
            if (currentlocation != null)
            {
                if (marker_value == 0)
                {
                    BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.marker1);
                    Bitmap b = bitmapdraw.getBitmap();
                    marker1 = mMap.addMarker(new MarkerOptions().position(currentlocation).title("Marker 1").icon(BitmapDescriptorFactory.fromBitmap(b)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlocation, 16.0f));
                    Resources res = getResources();
                    position_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.marker2_xml, null), null, null, null);
                    marker_value++;
                    ready = false;
                }
                else if (marker_value == 1)
                {
                    //LatLng pos = new LatLng(13.0723449, 77.5758208);
                    BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.marker2);
                    Bitmap b = bitmapdraw.getBitmap();
                    marker2 = mMap.addMarker(new MarkerOptions().position(currentlocation).title("Marker in Current location").icon(BitmapDescriptorFactory.fromBitmap(b)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlocation, 16.0f));
                    pline = mMap.addPolyline(new PolylineOptions()
                            .add(marker1.getPosition())
                            .add(marker2.getPosition()));
                    distance = findDistance(marker1, marker2);
                    bearing = findBearing(marker2, marker1);
                    Resources res = getResources();
                    position_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.cancelmarkerxml, null), null, null, null);
                    position_marker.setEnabled(false);
                    marker_value++;
                    if((distance <= 15) && (distance > 0))
                    {
                        minViewAngle = Math.toDegrees(Math.atan(7.5 / distance));
                        ready = true;
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Your are to far from building come close", Toast.LENGTH_LONG ).show();
                    }
                    ready = true;
                }
            }
        }
        else if(view.equals(clear_marker))
        {
            if(marker_value == 2)
            {
                marker2.remove();
                marker_value--;
                position_marker.setEnabled(true);
                Resources res = getResources();
                position_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.marker2_xml, null), null, null, null);
                pline.remove();
            }
            else if(marker_value == 1)
            {
                marker1.remove();
                Resources res = getResources();
                position_marker.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(res, R.drawable.marker1_xml, null), null, null, null);
                marker_value--;
            }
            ready = false;
        }
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
        Log.i(TAG, "findDistance: Distance between markers = "+(Radius*c*1000.2f));
        return Radius*c*1000.2f;
    }

    private double findBearing(Marker m1,Marker m2)
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
        Log.i(TAG, "findBearing: Bearing of polyline = "+b);
        return (b);
    }

    @SuppressLint("MissingPermission")
    private LatLng getLocation()
    {
        LatLng currentLocation = null;

        Location location = null , location1 = null , location2 = null;
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
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = Objects.requireNonNull(service).isProviderEnabled(LocationManager.GPS_PROVIDER);
        // Check if enabled and if not send user to the GPS settings
        if (!enabled)
        {
            Intent i = new Intent(this, FeaturesActivity.class);
            startActivity(i);
        }
    }

    @SuppressLint("MissingPermission")
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
            Log.e("Exception: ", Objects.requireNonNull(e.getMessage()));
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
            currentReading.clear();
            previousReading.clear();
            int length = status.getSatelliteCount();
            int mSvCount = 0;
            while (mSvCount < length)
            {
                if ((status.getAzimuthDegrees(mSvCount) >= (bearing - minViewAngle)) && (status.getAzimuthDegrees(mSvCount) <= (bearing + minViewAngle)))
                {
                    SatParams element = new SatParams(status.getSvid(mSvCount), status.getElevationDegrees(mSvCount) ,status.getAzimuthDegrees(mSvCount), status.getCn0DbHz(mSvCount));
                    currentReading.add(element);
                    if(first_readings)
                    {
                        previousReading.add(element);
                    }
                }
                mSvCount++;
            }
            parseSatelliteData();
        }
    }

    private void parseSatelliteData()
    {
        for (SatParams curSat : currentReading)
        {
            Log.i("ParseSatelliteData ","Satellite pRN = "+curSat.getPrn()+"\t\t Azimuth angle = "+curSat.getAzimuth());
        }
        if(!first_readings)
        {
            ArrayList<Integer> intersection = Intersection();
            findHeight(intersection);
            previousReading.clear();
            previousReading.addAll(currentReading);
        }
        else
        {
            first_readings = false;
        }
    }

    private ArrayList<Integer> Intersection()
    {
        Integer[] cPrns = new Integer[currentReading.size()],pPrns = new Integer[previousReading.size()];
        for(int i=0; i<currentReading.size(); i++)
        {
            cPrns[i] = currentReading.get(i).getPrn();
        }
        for(int i=0; i<previousReading.size(); i++)
        {
            pPrns[i] = previousReading.get(i).getPrn();
        }
        Set<Integer> s1 = new HashSet<>(Arrays.asList(cPrns));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(pPrns));
        s1.retainAll(s2);

        return new ArrayList<>(s1);
    }

    private void findHeight(ArrayList<Integer> mPrns)
    {
        float Threshold = 20.0f;
        float Elevation ;
        for(Integer ele : mPrns)
        {
            Elevation = 0.0f;
            SatParams cSat = null ,pSat = null;
            for(SatParams temp: currentReading)
            {
                if(temp.getPrn().equals(ele))
                {
                    cSat = temp;
                    break;
                }
            }
            for(SatParams temp: previousReading)
            {
                if(temp.getPrn().equals(ele))
                {
                    pSat = temp;
                    break;
                }
            }
            if(Objects.requireNonNull(pSat).getSNRatio() > Threshold)
            {
                if(Objects.requireNonNull(cSat).getSNRatio() <= Threshold)
                {
                    Elevation = cSat.getElevation();
                }
            }
            else if(pSat.getSNRatio() < Threshold)
            {
                if(Objects.requireNonNull(cSat).getSNRatio() >= Threshold)
                {
                    Elevation = cSat.getElevation();
                }
            }
            if(Elevation!=0.0f)
            {
                Toast.makeText(getApplicationContext(), "Elevation found for satellite PRN = "+cSat.getPrn(), Toast.LENGTH_LONG).show();
                double height = Math.tan(Math.PI / 180 * Elevation) * distance;
                Log.i(TAG, "findHeight: Result found -\n\tBuilding Height = "+height+"\n\tElevation (in degrees) = "+Elevation+"\n\tSatellite PRN = "+cSat.getPrn()+"\n\tDifference in SNR = "+(cSat.getSNRatio()-pSat.getSNRatio()));
            }
        }
    }
}
