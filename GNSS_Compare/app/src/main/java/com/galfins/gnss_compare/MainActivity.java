package com.galfins.gnss_compare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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
        createdCalculationModules.notifyObservers();
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
    private void registerLocationManagerCallbacks() {
        gnssCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                super.onGnssMeasurementsReceived(eventArgs);

                Log.d(TAG, "onGnssMeasurementsReceived: invoked!");

                    for (CalculationModule calculationModule : createdCalculationModules)
                        calculationModule.updateMeasurements(eventArgs);

                    notifyCalculationObservers();

                    if (rawMeasurementsLogger.isStarted())
                        rawMeasurementsLogger.onGnssMeasurementsReceived(eventArgs);
                }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.registerGnssMeasurementsCallback(
                    gnssCallback);
        }
    }

    /**
     * Class encapsulating generic operations on created CalculationModules.
     */
    public class CalculationModulesArrayList extends ArrayList<CalculationModule> {

        @Override
        public boolean add(final CalculationModule calculationModule) {

            for (final DataViewer viewer : mPagerAdapter.getViewers()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewer.addSeries(calculationModule);
                    }
                });
            }

            synchronized (this) {
                return super.add(calculationModule);
            }
        }

        @Override
        public boolean remove(Object o){
            for (DataViewer viewer : mPagerAdapter.getViewers()) {
                viewer.removeSeries((CalculationModule)o);
            }

            synchronized (this) {
                return super.remove(o);
            }
        }

        /**
         * Start threads associated with added CalculationModules. This is a single execution
         * of a calculation module's run() method
         */
        public void notifyObservers() {
            Log.d(TAG, "notifyObservers: invoked");
            synchronized (this) {
                for (CalculationModule calculationModule : this) {
                    runOnUiThread(calculationModule);
                }
            }

        }

        /**
         * Adds all created modules to viewers. This should be called on reset of the application
         * where created calculation modules stay in memory, but data viewers are recreated
         */
        public void reinitialize() {
            for (final CalculationModule calculationModule : this) {
                for (final DataViewer viewer : mPagerAdapter.getViewers()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewer.addSeries(calculationModule);
                        }
                    });
                }
            }
        }
    }

    /**
     * Initializes ViewPager, it's adapter and page indicator view
     */
    private void initializePager(){
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

    /**
     * Creates initial calculation modules
     */
    public void initializeCalculationModules(){
        if(createdCalculationModules==null) {
            createdCalculationModules = new CalculationModulesArrayList();
            if(savedState == null)
                createInitialCalculationModules();
            else if (savedState != null){
                createCalculationModulesFromBundle(savedState);
            }
        } else
            createdCalculationModules.reinitialize();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeMetaDataHandler();

        if(savedInstanceState != null)
            savedState = savedInstanceState;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PixelUtils.init(this);

        Constellation.initialize();
        Correction.initialize();
        PvtMethod.initialize();
        FileLogger.initialize();

        initializePager();
        initializeToolbar();
        initializeCalculationModules();

        requestPermission(this);

        mainView = findViewById(R.id.main_view);

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
    private void createCalculationModulesFromBundle(Bundle savedInstanceState) {

        ArrayList<String> modulesNames = savedInstanceState.getStringArrayList(MODULE_NAMES_BUNDLE_TAG);

        for(String name : modulesNames){
            try {
                createdCalculationModules.add(CalculationModule.fromConstructorArrayList(savedInstanceState.getStringArrayList(name)));
            } catch (CalculationModule.NameAlreadyRegisteredException | CalculationModule.NumberOfSeriesExceededLimitException e) {
                e.printStackTrace();
            }
        }
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {

                        List<CalculationModule> initialModules = new ArrayList<>();

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
//                                    add(IonoCorrection.class);
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

                        try {
                            for(CalculationModule module : initialModules)
                                createdCalculationModules.add(module);
                        } catch (Exception e){
                            for(CalculationModule module : initialModules) {
                                try {
                                    createdCalculationModules.remove(module);
                                } catch (Exception e2){
                                    e2.printStackTrace();
                                    Log.e(TAG, "run: Removal of initial module failed");
                                }
                            }
                            CalculationModule.clear();
                        }

                        Log.i(TAG, "run: Calculation modules initialized");
                        break;
                    } catch (CalculationModule.NameAlreadyRegisteredException e) {
                        CalculationModule.clear();
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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

                    synchronized (MainActivity.this) {
                        for (CalculationModule calculationModule : createdCalculationModules)
                            calculationModule.updateLocationFromGoogleServices(lastLocation);

                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        }
    }

    /**
     * Called when activity is paused
     * stops the data generating threads
     */
    @Override
    public void onPause() {
        super.onPause();

        mFusedLocationClient.removeLocationUpdates(locationCallback);

        Log.d(TAG, "onPause: invoked");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savedState = new Bundle();
        saveInstanceState(savedState);

        while(createdCalculationModules.size() > 0) {
            createdCalculationModules.remove(createdCalculationModules.get(0));
        }

        createdCalculationModules = null;
        CalculationModule.clear();

        mLocationManager.unregisterGnssMeasurementsCallback(gnssCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PREFERENCES_REQUEST && resultCode == RESULT_OK) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            CalculationModule newModule;

            try {
                newModule = CalculationModule.createFromDescriptions(
                        sharedPreferences.getString(CreateModulePreference.KEY_NAME, null),
                        sharedPreferences.getString(CreateModulePreference.KEY_CONSTELLATION, null),
                        sharedPreferences.getStringSet(CreateModulePreference.KEY_CORRECTION_MODULES, null),
                        sharedPreferences.getString(CreateModulePreference.KEY_PVT_METHOD, null),
                        sharedPreferences.getString(CreateModulePreference.KEY_FILE_LOGGER, null));

            } catch (CalculationModule.NameAlreadyRegisteredException
                    | CalculationModule.NumberOfSeriesExceededLimitException
                    | CalculationModule.CalculationSettingsIncompleteException e) {

                Snackbar snackbar = Snackbar
                        .make(mainView, e.getMessage(), Snackbar.LENGTH_LONG);

                snackbar.show();
                return;
            }

            createdCalculationModules.add(newModule);
            CreateModulePreference.notifyModuleCreated();
            makeNotification("Module " + newModule.getName() + " created...");

        }
    }

    /**
     * Creates a reference to the location manager service
     */
    private void registerLocationManagerReference() {
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerLocationManagerReference();
                registerLocationManagerCallbacks();
            }
        }
    }

    /**
     * Checks if the permission has been granted
     * @param activity The activity asking for permission
     * @return True of false depending on if permission has been granted
     */
    @SuppressLint("ObsoleteSdkInt")
    private boolean hasPermissions(Activity activity) {
        // Permissions granted at install time.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (ContextCompat.checkSelfPermission(activity, GNSS_REQUIRED_PERMISSIONS)
                            == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(activity, LOG_REQUIRED_PERMISSIONS)
                                == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Requests permission to access GNSS measurements
     * @param activity The activity asking for the permission
     */
    private void requestPermission(final Activity activity) {
        if (hasPermissions(activity)) {
            registerLocationManagerReference();
            registerLocationManagerCallbacks();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{GNSS_REQUIRED_PERMISSIONS, LOG_REQUIRED_PERMISSIONS}, PERMISSION_REQUEST_CODE);
        }
    }

    public static void makeNotification(final String note){
        Snackbar snackbar = Snackbar
                .make(mainView, note, Snackbar.LENGTH_LONG);

        snackbar.show();
    }

}

