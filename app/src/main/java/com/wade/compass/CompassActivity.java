package com.wade.compass;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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

import com.google.android.material.snackbar.Snackbar;
import com.wade.libs.CPDB;
import com.wade.libs.Proj;
import com.wade.libs.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


public class CompassActivity extends AppCompatActivity {
    private static final String TAG = "Compass";
    BroadcastReceiver locationBroadcastReceiver;
    private boolean wasLocationPermissionGranted = false;

    private Compass compass;
    private ImageView arrowView;

    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;
    private Proj mProj;
    private CPDB cpdb;
    private ListView lvCps;
    private static CpsAdapter cpsAdapter;
    private List<CPDB.CP> cps = new ArrayList<>();
    private double[] O = new double[]{0,0};
    private int phoneOrientation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupCompass();
        sotwFormatter = new SOTWFormatter(this);
        checkPermissions();
        mProj = new Proj();
        cpdb = new CPDB(this);
        setPhoneOrientation(getResources().getConfiguration().orientation);
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            CPDB.CP cp = cps.get(position);
            double dx1 = cp.x - O[0], dy1 = cp.y-O[1];
            double[] toCp = Tools.POLd(dy1, dx1);

            Snackbar.make(view, "控制點", Snackbar.LENGTH_LONG)
                    .setText(String.format(Locale.CHINESE, "[%s]%s @%d(%s) %.2f公尺\n方位=%s E%.0f,N%.0f",
                            cp.number, (cp.name.length()>0?cp.name:""),
                            cp.t, cpName(cp.t),
                            toCp[0], Tools.Deg2DmsStr2(toCp[1]),
                            cp.x, cp.y
                    ))
                    .show();
        }
    };

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

    private Compass.CompassListener getCompassListener() {
        return azimuth -> {
            // UI updates only in UI thread
            // https://stackoverflow.com/q/11140285/444966
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adjustArrow(azimuth);
                }
            });
        };
    }

    private void setPhoneOrientation(int dir) {
        setContentView(R.layout.activity_compass);
        lvCps = findViewById(R.id.cps);
        lvCps.setOnItemClickListener(onItemClickListener);

        arrowView = findViewById(R.id.main_image_dial);
        compass.setPhoneOrientation(dir);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        phoneOrientation = getDisplay().getRotation();
        setPhoneOrientation(phoneOrientation);
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
    private double lastX = 0, lastY = 0;

    public static String cpName(int t) {
        switch (t) {
            case 1: return "一等連續站";
            case 2: return "一等控制點";
            case 3: return "二等控制點";
            case 4: return "三等控制點";
            case 5: return "一等水準點";
            case 6: return "金門";
            case 7: return "馬祖";
            case 8: return "澎湖";
            case 9: return "永康";
            case 10: return "歸仁";
            case 11: return "關廟";
            default:return "";
        }
    }

    private void displayCurrentLocation(Location location) {
        // 先讓小綠人指向前進方向
        findViewById(R.id.arrow).setRotation(location.getBearing());
        double[] res = mProj.LL2UTM(location.getLongitude(), location.getLatitude(), 0);
        O = mProj.LL2TM2(location.getLongitude(), location.getLatitude());
        // location.hasAccuracy()  location.getAccuracy()

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setTextFor(R.id.altitude, String.format(Locale.TRADITIONAL_CHINESE, "橢球高:%.2f公尺",
                    location.getAltitude()));
            setTextFor(R.id.speed, String.format(Locale.TRADITIONAL_CHINESE, "速度:%.2f公尺/秒",
                    location.getSpeed()));
            setTextFor(R.id.longitude, "經　度: " + Tools.Deg2DmsStr2(location.getLongitude()));
            setTextFor(R.id.latitude, "緯　度: " + Tools.Deg2DmsStr2(location.getLatitude()));

            float bearing = location.getBearing();
            setTextFor(R.id.bearing, String.format(Locale.TRADITIONAL_CHINESE, "航　向: %s", sotwFormatter.format(bearing) + " " + Tools.ang2Str(bearing)));
            setTextFor(R.id.utm6, String.format(Locale.TRADITIONAL_CHINESE, "UTM6 : E%.2f, N%.2f", res[0], res[1]));
            setTextFor(R.id.tm2, String.format(Locale.TRADITIONAL_CHINESE, "TM2  : E%.2f, N%.2f", O[0], O[1]));
        } else {
            setTextFor(R.id.asa, String.format(Locale.TRADITIONAL_CHINESE,"精確度:%s 橢球高:%.2f公尺 >速度:%.2f公尺/秒",
                    location.hasAccuracy() ? String.format(Locale.TRADITIONAL_CHINESE, "%.1f公尺",location.getAccuracy()):"未知",
                    location.getAltitude(), location.getSpeed()));

            setTextFor(R.id.longitude, "經　度: " + Tools.Deg2DmsStr2(location.getLongitude()));
            setTextFor(R.id.latitude, "緯　度: " + Tools.Deg2DmsStr2(location.getLatitude()));

            float bearing = location.getBearing();
            setTextFor(R.id.bearing, String.format(Locale.TRADITIONAL_CHINESE, "航　向: %s", sotwFormatter.format(bearing)+" "+Tools.ang2Str(bearing)));
            setTextFor(R.id.utm6, String.format(Locale.TRADITIONAL_CHINESE,    "UTM6 : E%.2f, N%.2f", res[0], res[1]));
            setTextFor(R.id.tm2, String.format(Locale.TRADITIONAL_CHINESE,     "TM2  : E%.2f, N%.2f", O[0], O[1]));
        }
        double dx = O[0] - lastX;
        double dy = O[1]  - lastY;
        if (Math.sqrt(dx*dx + dy*dy) > 1) {
            lastX = O[0];
            lastY = O[1];
            cps = cpdb.getCp(lastX, lastY, 0);
            if (cps.size() > 0) {
                // 先依距離排序
                Collections.sort(cps, (o1, o2) -> {
                    double d = (Math.pow(o1.x - lastX, 2) + Math.pow(o1.y - lastY, 2)) -
                            (Math.pow(o2.x - lastX, 2) + Math.pow(o2.y - lastY, 2));
                    if (d > 0) return 1;
                    else if (d == 0) return 0;
                    else return -1;
                });
                cpsAdapter = new CpsAdapter((Context) this, (ArrayList)cps);
                lvCps.setAdapter(cpsAdapter);
            }
        }
    }

    private void setTextFor(int p, String text) {
        TextView tv = (TextView) findViewById(p);
        if (tv == null) {
            Log.i(TAG, "setTextFor is null for "+text);
        }
        tv.setText(text);
    }

}
