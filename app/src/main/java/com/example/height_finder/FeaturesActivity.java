package com.example.height_finder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Objects;

public class FeaturesActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private Switch Gps;
    private boolean enabled;
    private LocationManager service;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        service = (LocationManager) getSystemService(LOCATION_SERVICE);
        enabled = Objects.requireNonNull(service).isProviderEnabled(LocationManager.GPS_PROVIDER);
        // Check if enabled and if not send user to the GPS settings
        if (enabled)
        {
            Intent i = new Intent(this,MapsActivity.class);
            startActivity(i);
        }
        else
        {
            setContentView(R.layout.activity_features);
            Gps = findViewById(R.id.switch_feature_GPS);
            Gps.setOnCheckedChangeListener(this);
        }
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
    public void onCheckedChanged(CompoundButton compoundButton, boolean b)
    {
        if(b)
        {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent,1);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1)
        {
            enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (enabled)
            {
                Intent i = new Intent(this,MapsActivity.class);
                startActivity(i);
            }
            else
            {
                Toast.makeText(this,"GPS must be turned on to use this app",Toast.LENGTH_SHORT).show();
                Gps.setChecked(false);
            }
        }
    }
}
