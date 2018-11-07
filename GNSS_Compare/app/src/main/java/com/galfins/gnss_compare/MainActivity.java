/*
 * Copyright 2018 TFI Systems

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.galfins.gnss_compare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.androidplot.util.PixelUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.rd.PageIndicatorView;

import java.util.Observable;
import java.util.Observer;

import com.galfins.gnss_compare.DataViewers.DataViewer;
import com.galfins.gnss_compare.DataViewers.DataViewerAdapter;
import com.galfins.gnss_compare.FileLoggers.RawMeasurementsFileLogger;


public class MainActivity extends AppCompatActivity {

    private static int dismissableNotificationTextColor;
    /**
     * Tag used for logging to logcat
     */
    @SuppressWarnings("unused")
    private final String TAG = "MainActivity";

    /**
     * Tag used to mark module names for savedInstanceStates of the onCreate method.
     */
    private final String MODULE_NAMES_BUNDLE_TAG = "__module_names";

    /**
     * ID for startActivityForResult regarding the preferences screen
     */
    private static int PREFERENCES_REQUEST = 1;

    /**
     * Permission needed for accessing the measurements from the GNSS chip
     */
    private static final String GNSS_REQUIRED_PERMISSIONS = Manifest.permission.ACCESS_FINE_LOCATION;

    /**
     * Permission needed for accessing the measurements from the GNSS chip
     */
    private static final String LOG_REQUIRED_PERMISSIONS = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    /**
     * Request code used for permissions
     */
    private static final int PERMISSION_REQUEST_CODE = 1;

    /**
     * Client for receiving the location from Google Services
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * LocationManager object to receive GNSS measurements
     */
    private LocationManager mLocationManager;

    /**
     * ViewPager object, which allows for scrolling over Fragments
     */
    private ViewPager mPager;

    /**
     * Adapter for the ViewPager
     */
    private DataViewerAdapter mPagerAdapter;

    /**
     * Raw measurements logger
     */
    public static RawMeasurementsFileLogger rawMeasurementsLogger = new RawMeasurementsFileLogger("rawMeasurements");

    /**
     * Locally saved state of created calculation modules
     */
    private static Bundle savedState;

    private static Snackbar rnpFailedSnackbar = null;

    private Menu menu;

    private Observer calculationModuleObserver;

    private GnssCoreService.GnssCoreBinder gnssCoreBinder;

    private boolean mGnssCoreBound = false;

    private class GnssCoreServiceConnector implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            if(!mGnssCoreBound) {
                gnssCoreBinder = (GnssCoreService.GnssCoreBinder) service;
                mGnssCoreBound = true;

                gnssCoreBinder.addObserver(calculationModuleObserver);
            }
        }

        public void resetConnection(){
            if(gnssCoreBinder != null && mGnssCoreBound) {
                gnssCoreBinder.removeObserver(calculationModuleObserver);
                mGnssCoreBound = false;

                gnssCoreBinder = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            resetConnection();
        }

    }

    private ServiceConnection mConnection = new GnssCoreServiceConnector() ;

    /**
     * Callback used for receiving phone's location
     */
    LocationCallback locationCallback;
    private static final Object metaDataMutex = new Object();

    public static Location getLocationFromGoogleServices() {
        synchronized (locationFromGoogleServicesMutex) {
            return locationFromGoogleServices;
        }
    }

    /**
     * Callback object assigned to the GNSS measurement callback
     */
    GnssMeasurementsEvent.Callback gnssCallback;

    static View mainView;

    private static Location locationFromGoogleServices = null;

    public static boolean isLocationFromGoogleServicesInitialized(){
        synchronized (locationFromGoogleServicesMutex) {
            return locationFromGoogleServices != null;
        }
    }

    private static final Object locationFromGoogleServicesMutex = new Object();

    /**
     * Bundle storing manifest's meta data, so that it can be used outside of MainActivity
     */
    private static Bundle metaData;

    /**
     * Registers GNSS measurement event manager callback.
     */
    private void registerLocationManager() {

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        gnssCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                super.onGnssMeasurementsReceived(eventArgs);

                Log.d(TAG, "onGnssMeasurementsReceived (MainActivity): invoked!");

                if (rawMeasurementsLogger.isStarted())
                    rawMeasurementsLogger.onGnssMeasurementsReceived(eventArgs);
            }
        };

        final LocationRequest locationRequest = new LocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(500);
        locationRequest.setInterval(100);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {

                final Location lastLocation = locationResult.getLocations().get(locationResult.getLocations().size()-1);

                if(lastLocation != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for(DataViewer dataViewer : mPagerAdapter.getViewers()) {
                                dataViewer.onLocationFromGoogleServicesResult(lastLocation);
                            }
                        }
                    });

                    Log.i(TAG, "locationFromGoogleServices: New location (phone): "
                            + lastLocation.getLatitude() + ", "
                            + lastLocation.getLongitude() + ", "
                            + lastLocation.getAltitude());

                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null);

            mLocationManager.registerGnssMeasurementsCallback(
                    gnssCallback);


        }
    }

    /**
     * Initializes ViewPager, it's adapter and page indicator view
     */
    private void initializePager(){
        mPager = findViewById(R.id.pager);
        mPagerAdapter = new DataViewerAdapter(getSupportFragmentManager());
        mPagerAdapter.initialize();
        mPager.setAdapter(mPagerAdapter);
        PageIndicatorView pageIndicatorView = findViewById(R.id.pageIndicatorView);
        pageIndicatorView.setViewPager(mPager);
    }

    /**
     * Initializes the toolbar
     */
    public void initializeToolbar(){
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null)
            savedState = savedInstanceState;

        initializeGnssCompareMainActivity();

        if (hasGnssAndLogPermissions()) {
            registerLocationManager();
        } else {
            requestGnssAndLogPermissions();
        }

        mainView = findViewById(R.id.main_view);

        showInitializationDisclamer();

        startService(new Intent(this, GnssCoreService.class));
    }

    private void initializeGnssCompareMainActivity() {

        setContentView(R.layout.activity_main);

        try {
            TextView versionTextView = findViewById(R.id.versionCode);
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;

            versionTextView.setText(getResources().getString(R.string.version_text_view,  version));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        initializeMetaDataHandler();

        calculationModuleObserver = new Observer() {

            class UiThreadRunnable implements Runnable {

                CalculationModulesArrayList calculationModules;

                public void setCalculationModules(CalculationModulesArrayList newCalculationModules){
                    synchronized (this) {
                        calculationModules = newCalculationModules;
                    }
                }

                @Override
                public void run() {
                    synchronized (this) {
                        for (DataViewer viewer : mPagerAdapter.getViewers())
                            viewer.updateOnUiThread(calculationModules);
                    }
                }
            }

            UiThreadRunnable uiThreadRunnable = new UiThreadRunnable();

            @Override
            public void update(Observable o, Object calculationModules) {

                for(DataViewer viewer : mPagerAdapter.getViewers()){
                    viewer.update((CalculationModulesArrayList) calculationModules);
                }

                uiThreadRunnable.setCalculationModules((CalculationModulesArrayList) calculationModules);

                runOnUiThread(uiThreadRunnable);

            }
        };

        initializeMetaDataHandler();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PixelUtils.init(this);

        initializePager();
        initializeToolbar();

        dismissableNotificationTextColor = ContextCompat.getColor(this, R.color.colorPrimaryBright2);
    }

    private void showInitializationDisclamer() {

        makeDismissableNotification(
                "All calculations are initialized with phone's FINE location",
                Snackbar.LENGTH_LONG
        );
    }

    private void initializeMetaDataHandler() {
        ApplicationInfo ai = null;
        try {
            ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if(ai!=null) {
            synchronized (metaDataMutex) {
                metaData = ai.metaData;
            }
        }
    }

    public static String getMetaDataString(String key){
        return metaData.getString(key);
    }

    public static int getMetaDataInt(String key){
        return metaData.getInt(key);
    }

    public static boolean getMetaDataBoolean(String key){
        return metaData.getBoolean(key);
    }

    public static float getMetaDataFloat(String key){
        return metaData.getFloat(key);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop: invoked");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_create_module:
                // User chose the "Settings" item, show the app settings UI...
                Intent preferenceIntent = new Intent(this, CreateModulePreference.class);
                startActivityForResult(preferenceIntent, PREFERENCES_REQUEST);
                return true;

            case R.id.action_modify_module:
                Intent preferencesIntent = new Intent(this, ModifyModulePreference.class);
                startActivity(preferencesIntent);
                return true;

            case R.id.action_start_stop_log:
                MenuItem logButton = menu.findItem(R.id.action_start_stop_log);
                if (!rawMeasurementsLogger.isStarted()) {
                    rawMeasurementsLogger.startNewLog();
                    makeNotification("Starting raw GNSS measurements log...");
                    logButton.setTitle(R.string.stop_log_button_description);
                } else {
                    rawMeasurementsLogger.closeLog();
                    makeNotification("Stopping raw GNSS measurements log...");
                    logButton.setTitle(R.string.start_log_button_description);
                }
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Called when activity is resumed
     * restarts the data generating threads
     */
    @Override
    public void onResume() {
        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {
                startAndBindGnssCoreService();
            }
        }).start();

        Log.d(TAG, "onResume: invoked");
    }

    public void startAndBindGnssCoreService(){
        if(!GnssCoreService.isServiceStarted()) {
            startService(new Intent(MainActivity.this, GnssCoreService.class));

            if(!GnssCoreService.waitForServiceStarted()){
                makeDismissableNotification(
                        "Issue starting GNSS Core service...",
                        Snackbar.LENGTH_INDEFINITE );

                //todo: consider a return here?
            }

        }

        bindService(
                new Intent(MainActivity.this, GnssCoreService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
    }

    /**
     * Called when activity is paused
     * stops the data generating threads
     */
    @Override
    public void onPause() {
        super.onPause();

        if(mGnssCoreBound) {
            unbindService(mConnection);
            ((GnssCoreServiceConnector) mConnection).resetConnection();
        }

        Log.d(TAG, "onPause: invoked");
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savedState = new Bundle();

        mLocationManager.unregisterGnssMeasurementsCallback(gnssCallback);
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PREFERENCES_REQUEST && resultCode == RESULT_OK) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            try {
                final CalculationModule newModule = CalculationModule.createFromDescriptions(
                        sharedPreferences.getString(CreateModulePreference.KEY_NAME, null),
                        sharedPreferences.getString(CreateModulePreference.KEY_CONSTELLATION, null),
                        sharedPreferences.getStringSet(CreateModulePreference.KEY_CORRECTION_MODULES, null),
                        sharedPreferences.getString(CreateModulePreference.KEY_PVT_METHOD, null),
                        sharedPreferences.getString(CreateModulePreference.KEY_FILE_LOGGER, null));

                if(mGnssCoreBound) {
                    gnssCoreBinder.addModule(newModule);
                    CreateModulePreference.notifyModuleCreated();
                    makeNotification("Module " + newModule.getName() + " created...");
                } else {
                    makeNotification("Error, GNSS Core service not running...");
                }

            } catch (CalculationModule.NameAlreadyRegisteredException
                    | CalculationModule.NumberOfSeriesExceededLimitException
                    | CalculationModule.CalculationSettingsIncompleteException e) {

                Snackbar snackbar = Snackbar
                        .make(mainView, e.getMessage(), Snackbar.LENGTH_LONG);

                snackbar.show();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerLocationManager();
            }
        }
    }

    /**
     * Checks if the permission has been granted
     * @return True of false depending on if permission has been granted
     */
    @SuppressLint("ObsoleteSdkInt")
    private boolean hasGnssAndLogPermissions() {
        // Permissions granted at install time.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (ContextCompat.checkSelfPermission(this, GNSS_REQUIRED_PERMISSIONS)
                            == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, LOG_REQUIRED_PERMISSIONS)
                                == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Requests permission to access GNSS measurements
     */
    private void requestGnssAndLogPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{GNSS_REQUIRED_PERMISSIONS, LOG_REQUIRED_PERMISSIONS}, PERMISSION_REQUEST_CODE);
    }

    public static void makeNotification(final String note){
        Snackbar snackbar = Snackbar
                .make(mainView, note, Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    public static void makeDismissableNotification(String note, int length){

        final Snackbar snackbar = Snackbar
                .make(mainView, note, length);

        snackbar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });

        snackbar.setActionTextColor(dismissableNotificationTextColor);

        snackbar.show();
    }

    public static void makeRnpFailedNotification(){

        if(rnpFailedSnackbar==null) {
            rnpFailedSnackbar = Snackbar.make(
                    mainView,
                    "Failed to get ephemeris data. Retrying...",
                    Snackbar.LENGTH_LONG
            );
            rnpFailedSnackbar.show();
        } else if (!rnpFailedSnackbar.isShown())
            rnpFailedSnackbar.show();

    }

}

