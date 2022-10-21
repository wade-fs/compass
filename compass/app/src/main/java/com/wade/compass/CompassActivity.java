package com.wade.compass;

import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.wade.libs.CPDB;
import com.wade.libs.Proj;
import com.wade.libs.Tools;

import java.util.Locale;


public class CompassActivity extends AppCompatActivity {
    private static final String TAG = "CompassActivity";
    BroadcastReceiver locationBroadcastReceiver;
    private boolean wasLocationPermissionGranted = false;

    private Compass compass;
    private ImageView arrowView;
    private String sotw;

    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;
    private Proj mProj;
    private CPDB cpdb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

		// GPS
		checkPermissions();

		// Compass
        sotwFormatter = new SOTWFormatter(this);

        arrowView = findViewById(R.id.main_image_dial);
        setupCompass();
        mProj = new Proj();
        cpdb = new CPDB(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        compass.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
		// GPS
        unregisterReceiver(locationBroadcastReceiver);
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private void adjustArrow(float azimuth) {
        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    private void adjustSotwLabel(float azimuth) {
        sotw = sotwFormatter.format(azimuth);
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                // UI updates only in UI thread
                // https://stackoverflow.com/q/11140285/444966
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adjustArrow(azimuth);
                        adjustSotwLabel(azimuth);
                    }
                });
            }
        };
    }


	////////////////////////////////////////////////////////////////////
	// GPS
	////////////////////////////////////////////////////////////////////
    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                wasLocationPermissionGranted = isGranted;
                if (isGranted) {
                    startLocationService();
                } else {
                    explainTheNeedForPermission();
                }
            });

    private void startLocationService() {
        subscribeToLocationService();

        Intent locationService = new Intent(this, GoogleLocationService.class);
        startService(locationService);
    }

    private void subscribeToLocationService() {
        locationBroadcastReceiver = new LocationBroadcastReceiver(
                (location) -> displayCurrentLocation(location)
        );
        registerReceiver(locationBroadcastReceiver, new IntentFilter("CURRENT_LOCATION"));
    }

    private void checkPermissions() {
        String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;

        int result = ContextCompat.checkSelfPermission(this, locationPermission);
        wasLocationPermissionGranted = result == PackageManager.PERMISSION_GRANTED;

        if (wasLocationPermissionGranted) {
            startLocationService();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(locationPermission)) {
                explainTheNeedForPermission();
            } else {
                requestPermissionLauncher.launch(locationPermission);
            }
        }
    }

    private void explainTheNeedForPermission() {
        Toast.makeText(this, "We need to talk.", Toast.LENGTH_SHORT).show();
    }

    private void displayCurrentLocation(Location location) {
        // 先讓小綠人指向前進方向
        findViewById(R.id.arrow).setRotation(location.getBearing());
        double[] res = mProj.LL2UTM(location.getLongitude(), location.getLatitude(), 0);
        double[] res2 = mProj.LL2TM2(location.getLongitude(), location.getLatitude());

        String message = (location.getLatitude()) + ", " + location.getLongitude();
        setTextFor(R.id.asa, String.format(Locale.TRADITIONAL_CHINESE,"精確度:%s 橢球高:%.2f公尺 速度:%.2f公尺/秒",
                location.hasAccuracy() ? String.format(Locale.TRADITIONAL_CHINESE, "%.1f公尺",location.getAccuracy()):"未知",
        location.getAltitude(), location.getSpeed()));

        setTextFor(R.id.longitude, "經　度: " + Tools.Deg2DmsStr2(location.getLongitude()));
        setTextFor(R.id.latitude, "緯　度: " + Tools.Deg2DmsStr2(location.getLatitude()));

        setTextFor(R.id.bearing, String.format(Locale.TRADITIONAL_CHINESE, "航　向: %s", Tools.ang2Str(location.getBearing())));
        setTextFor(R.id.utm6, String.format(Locale.TRADITIONAL_CHINESE,    "UTM6 : E%.2f, N%.2f", res[0], res[1]));
        setTextFor(R.id.tm2, String.format(Locale.TRADITIONAL_CHINESE,    "TM2　　: E%.2f, N%.2f", res2[0], res2[1]));
    }

    private void setTextFor(int p, String text) {
        ((TextView) findViewById(p)).setText(text);
    }

}
