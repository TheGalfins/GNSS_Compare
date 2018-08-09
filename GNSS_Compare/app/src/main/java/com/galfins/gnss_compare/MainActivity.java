package com.galfins.gnss_compare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
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

import com.androidplot.util.PixelUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.rd.PageIndicatorView;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.Constellations.GalileoConstellation;
import com.galfins.gnss_compare.Constellations.GalileoGpsConstellation;
import com.galfins.gnss_compare.Constellations.GpsConstellation;
import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gnss_compare.Corrections.ShapiroCorrection;
import com.galfins.gnss_compare.Corrections.TropoCorrection;
import com.galfins.gnss_compare.DataViewers.DataViewer;
import com.galfins.gnss_compare.DataViewers.DataViewerAdapter;
import com.galfins.gnss_compare.FileLoggers.FileLogger;
import com.galfins.gnss_compare.FileLoggers.NmeaFileLogger;
import com.galfins.gnss_compare.FileLoggers.RawMeasurementsFileLogger;
import com.galfins.gnss_compare.PvtMethods.DynamicExtendedKalmanFilter;
import com.galfins.gnss_compare.PvtMethods.PvtMethod;


public class MainActivity extends AppCompatActivity {

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
     * Object storing created calculation modules
     */
    public static CalculationModulesArrayList createdCalculationModules;

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

    private Observer calculationModuleObserver;

    private Observable uiThreadObservable;

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
     * Method to synchronize execution of calculations.
     */
    public void notifyCalculationObservers() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createdCalculationModules.notifyObservers();
            }
        });
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

            mFusedLocationClient.requestLocationUpdates(
                    createdCalculationModules.getLocationRequest(),
                    createdCalculationModules.getLocationCallback(),
                    null);

            mLocationManager.registerGnssMeasurementsCallback(
                    gnssCallback);

            mLocationManager.registerGnssMeasurementsCallback(
                    createdCalculationModules.getGnssCallback());

        }
    }

    /**
     * Initializes ViewPager, it's adapter and page indicator view
     */
    private void initializePager(){
        mPager = findViewById(R.id.pager);
        mPagerAdapter = new DataViewerAdapter(getSupportFragmentManager());
        mPagerAdapter.initialize();
        mPagerAdapter.registerUiThreadObservable(uiThreadObservable);
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

    /**
     * Creates initial calculation modules
     */
    public void initializeCalculationModules(){
        if(createdCalculationModules==null) {
            createdCalculationModules = new CalculationModulesArrayList(mPagerAdapter);
            if(savedState == null)
                createInitialCalculationModules();
            else if (savedState != null){
                createCalculationModulesFromBundle(savedState);
            }
            createdCalculationModules.addObserver(calculationModuleObserver);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createdCalculationModules.reinitialize();
                    createdCalculationModules.addObserver(calculationModuleObserver);
                }
            });
        }

    }

    public void initializeGnssCompareModules(){
        Constellation.initialize();
        Correction.initialize();
        PvtMethod.initialize();
        FileLogger.initialize();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null)
            savedState = savedInstanceState;

        setContentView(R.layout.activity_main);

        initializeGnssCompareMainActivity();

        initializeGnssCompareModules();

        initializeCalculationModules();

        if (hasGnssAndLogPermissions()) {
            registerLocationManager();
        } else {
            requestGnssAndLogPermissions();
        }

        mainView = findViewById(R.id.main_view);

        showInitializationDisclamer();
    }

    private void initializeGnssCompareMainActivity() {
        uiThreadObservable = new Observable(){
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        };

        calculationModuleObserver = new Observer() {

            Runnable notifyObservers = new Runnable() {
                @Override
                public void run() {
                    uiThreadObservable.notifyObservers();
                }
            };

            @Override
            public void update(Observable o, Object arg) {
                runOnUiThread(notifyObservers);
            }
        };

        initializeMetaDataHandler();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PixelUtils.init(this);

        initializePager();
        initializeToolbar();
    }

    private void showInitializationDisclamer() {
        final Snackbar snackbar = Snackbar
                .make(mainView,
                        "All calculations are initialized with phone's FINE location",
                        Snackbar.LENGTH_LONG);

        snackbar.setAction("Acknowledge", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    snackbar.dismiss();
                }
            });

        snackbar.show();
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

    /**
     * Creates new calculation modules, based on data stored in the bundle
     * TODO - Add flags for status of "Active" and "Log"
     * @param savedInstanceState bundle describing created calculation modules
     */
    private void createCalculationModulesFromBundle(final Bundle savedInstanceState) {

        final ArrayList<String> modulesNames = savedInstanceState.getStringArrayList(MODULE_NAMES_BUNDLE_TAG);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(String name : modulesNames){
                    try {
                        createdCalculationModules.add(CalculationModule.fromConstructorArrayList(savedInstanceState.getStringArrayList(name)));
                    } catch (CalculationModule.NameAlreadyRegisteredException | CalculationModule.NumberOfSeriesExceededLimitException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        saveInstanceState(bundle);
    }

    /**
     * Parses created calculation module definitions to a bundle. This bundle can later be used with
     * createCalculationModulesFromBundle to create a new set of calculationModules
     * @param bundle reference to a Bundle object to which the information is to be stored.
     */
    private void saveInstanceState(Bundle bundle){
        ArrayList<String> modulesNames = new ArrayList<>();

        for (CalculationModule module: createdCalculationModules)
            modulesNames.add(module.getName());

        bundle.putStringArrayList(MODULE_NAMES_BUNDLE_TAG, modulesNames);

        for (CalculationModule module : createdCalculationModules){
            ArrayList<String> moduleDescription = module.getConstructorArrayList();
            bundle.putStringArrayList(module.getName(), moduleDescription);
        }
    }

    /**
     * Creates default, initial calculation modules
     */
    private void createInitialCalculationModules(){

        final List<CalculationModule> initialModules = new ArrayList<>();

        try {
            initialModules.add(new CalculationModule(
                    "Galileo+GPS",
                    GalileoGpsConstellation.class,
                    new ArrayList<Class<? extends Correction>>() {{
                        add(ShapiroCorrection.class);
                        add(TropoCorrection.class);
                    }},
                    DynamicExtendedKalmanFilter.class,
                    NmeaFileLogger.class));

            initialModules.add(new CalculationModule(
                    "GPS",
                    GpsConstellation.class,
                    new ArrayList<Class<? extends Correction>>() {{
                        add(ShapiroCorrection.class);
                        add(TropoCorrection.class);
                    }},
                    DynamicExtendedKalmanFilter.class,
                    NmeaFileLogger.class));

            initialModules.add(new CalculationModule(
                    "Galileo",
                    GalileoConstellation.class,
                    new ArrayList<Class<? extends Correction>>() {{
                        add(ShapiroCorrection.class);
                        add(TropoCorrection.class);
                    }},
                    DynamicExtendedKalmanFilter.class,
                    NmeaFileLogger.class));
        } catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "createInitialCalculationModules: Exception when creating modules");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    for(CalculationModule module : initialModules)
                        createdCalculationModules.add(module);
                } catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "createInitalCalculationModules: adding modules failed");
                    for(CalculationModule module : initialModules) {
                        try {
                            createdCalculationModules.remove(module);
                        } catch (Exception e2){
                            e2.printStackTrace();
                            Log.e(TAG, "createInitalCalculationModules: Removal of initial module failed");
                        }
                    }
                    CalculationModule.clear();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
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

        Log.d(TAG, "onResume: invoked");
    }

    /**
     * Called when activity is paused
     * stops the data generating threads
     */
    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause: invoked");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savedState = new Bundle();
        saveInstanceState(savedState);

        mLocationManager.unregisterGnssMeasurementsCallback(gnssCallback);
        mLocationManager.unregisterGnssMeasurementsCallback(createdCalculationModules.getGnssCallback());

        mFusedLocationClient.removeLocationUpdates(locationCallback);
        mFusedLocationClient.removeLocationUpdates(createdCalculationModules.getLocationCallback());

        while(createdCalculationModules.size() > 0) {
            createdCalculationModules.remove(createdCalculationModules.get(0));
        }

        createdCalculationModules = null;
        CalculationModule.clear();
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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createdCalculationModules.add(newModule);
                        CreateModulePreference.notifyModuleCreated();
                        makeNotification("Module " + newModule.getName() + " created...");
                    }
                });

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

}

