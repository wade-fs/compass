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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


public class CompassActivity extends AppCompatActivity {
    private static final String TAG = "CompassActivity";
    BroadcastReceiver locationBroadcastReceiver;
    private boolean wasLocationPermissionGranted = false;

    private Compass compass;
    private ImageView arrowView;

    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;
    private Proj mProj;
    private CPDB cpdb;
    private List<CPDB.CP> cps;

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
        double[] res2 = mProj.LL2TM2(location.getLongitude(), location.getLatitude());

        setTextFor(R.id.asa, String.format(Locale.TRADITIONAL_CHINESE,"精確度:%s 橢球高:%.2f公尺 速度:%.2f公尺/秒",
                location.hasAccuracy() ? String.format(Locale.TRADITIONAL_CHINESE, "%.1f公尺",location.getAccuracy()):"未知",
        location.getAltitude(), location.getSpeed()));

        setTextFor(R.id.longitude, "經　度: " + Tools.Deg2DmsStr2(location.getLongitude()));
        setTextFor(R.id.latitude, "緯　度: " + Tools.Deg2DmsStr2(location.getLatitude()));

        float bearing = location.getBearing();
        setTextFor(R.id.bearing, String.format(Locale.TRADITIONAL_CHINESE, "航　向: %s", sotwFormatter.format(bearing)+" "+Tools.ang2Str(bearing)));
        setTextFor(R.id.utm6, String.format(Locale.TRADITIONAL_CHINESE,    "UTM6 : E%.2f, N%.2f", res[0], res[1]));
        setTextFor(R.id.tm2, String.format(Locale.TRADITIONAL_CHINESE,    "TM2　　: E%.2f, N%.2f", res2[0], res2[1]));

        double dx = res2[0] - lastX;
        double dy = res2[1]  - lastY;
        if (Math.sqrt(dx*dx + dy*dy) > 1) {
            lastX = res2[0];
            lastY = res2[1];
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
                String cpMsg = "";
                for (CPDB.CP cp : cps) {
                    double dx1 = cp.x - res2[0], dy1 = cp.y-res2[1];
                    double[] resCP = Tools.POLd(dy1, dx1);
//                    cpMsg += String.format(Locale.CHINESE, "[%s]%s\n@%d(%s)\n%.0fE,%.0fN\n距離=%.2f公尺\n方位=%s\n",
//                            cp.number, (cp.name.length()>0?cp.name:""),
//                            cp.t, cpName(cp.t),
//                            cp.x, cp.y,
//                            resCP[0],
//                            Tools.Deg2DmsStr2(resCP[1])
//                    );
                    cpMsg += String.format(Locale.CHINESE, "[%s]%s#%d　E%.0f,N%.0f　%.2f公尺\n",
                            cp.number, (cp.name.length()>0?cp.name:""), cp.t,
                            cp.x, cp.y,
                            resCP[0]
                    );
                }
                setTextFor(R.id.cp, cpMsg);
            }
        }
    }

    private void setTextFor(int p, String text) {
        ((TextView) findViewById(p)).setText(text);
    }

}
