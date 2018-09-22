package com.galfins.gnss_compare;

import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.Constellations.GalileoConstellation;
import com.galfins.gnss_compare.Constellations.GalileoGpsConstellation;
import com.galfins.gnss_compare.Constellations.GpsConstellation;
import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gnss_compare.Corrections.ShapiroCorrection;
import com.galfins.gnss_compare.Corrections.TropoCorrection;
import com.galfins.gnss_compare.FileLoggers.FileLogger;
import com.galfins.gnss_compare.FileLoggers.NmeaFileLogger;
import com.galfins.gnss_compare.PvtMethods.DynamicExtendedKalmanFilter;
import com.galfins.gnss_compare.PvtMethods.PvtMethod;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Mateusz on 9/6/2018.
 * This class is for:
 */
public class GnssCoreService extends Service {


    /**
     * Tag used to mark module names for savedInstanceStates of the onCreate method.
     */
    private final String MODULE_NAMES_BUNDLE_TAG = "__module_names";

    private Bundle savedModulesBundle = null;

    CalculationModulesArrayList calculationModules = new CalculationModulesArrayList();

    Observable gnssCoreObservable = new Observable(){
        @Override
        public void notifyObservers() {
            setChanged();
            super.notifyObservers(calculationModules); // by default passing the calculation modules
        }
    };

    CalculationModulesArrayList.PoseUpdatedListener poseListener = new CalculationModulesArrayList.PoseUpdatedListener(){
        @Override
        public void onPoseUpdated() {
            gnssCoreObservable.notifyObservers();
        }
    };

    private final String TAG = this.getClass().getSimpleName();

    public class GnssCoreBinder extends Binder{

        public void addObserver(Observer observer){
            gnssCoreObservable.addObserver(observer);
        }

        public void removeObserver(Observer observer){
            gnssCoreObservable.deleteObserver(observer);
        }

        public CalculationModulesArrayList getCalculationModules(){
            return calculationModules;
        }

        public void addModule(CalculationModule newModule){
            calculationModules.add(newModule);
        }

        public void removeModule(CalculationModule removedModule){
            calculationModules.remove(removedModule);
        }
    }

    private IBinder binder = new GnssCoreBinder();

    private boolean serviceStarted = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Constellation.initialize();
        Correction.initialize();
        PvtMethod.initialize();
        FileLogger.initialize();

        if(calculationModules.size() == 0){
            if(savedModulesBundle==null)
                createInitialCalculationModules();
            else
                createCalculationModulesFromBundle();

            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

            calculationModules.registerForGnssUpdates(mFusedLocationClient, mLocationManager);
            calculationModules.assignPoseUpdatedListener(poseListener);

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        serviceStarted = true;

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        saveModulesDescriptionToBundle();

        calculationModules.unregisterFromGnssUpdates();
        calculationModules.clear();
        CalculationModule.clear();

        serviceStarted = false;

        stopSelf();
    }

    /**
     * Saves the descriptions of calculation modules to a bundle object
     * todo: move to calcualtionModulesArrayList?
     */
    private void saveModulesDescriptionToBundle() {

        ArrayList<String> modulesNames = new ArrayList<>();
        savedModulesBundle = new Bundle();

        for (CalculationModule module: calculationModules)
            modulesNames.add(module.getName());

        savedModulesBundle.putStringArrayList(MODULE_NAMES_BUNDLE_TAG, modulesNames);

        for (CalculationModule module : calculationModules){
            ArrayList<String> moduleDescription = module.getConstructorArrayList();
            savedModulesBundle.putStringArrayList(module.getName(), moduleDescription);
        }
    }

    /**
     * Creates new calculation modules, based on data stored in the bundle
     * todo: move to calcualtionModulesArrayList?
     */
    private void createCalculationModulesFromBundle() {

        if(savedModulesBundle!=null) {

            ArrayList<String> modulesNames = savedModulesBundle.getStringArrayList(MODULE_NAMES_BUNDLE_TAG);

            if (modulesNames != null) {
                for (String name : modulesNames) {
                    try {
                        ArrayList<String> constructorArrayList = savedModulesBundle.getStringArrayList(name);
                        if (constructorArrayList != null)
                            calculationModules.add(CalculationModule.fromConstructorArrayList(constructorArrayList));
                    } catch (CalculationModule.NameAlreadyRegisteredException | CalculationModule.NumberOfSeriesExceededLimitException e) {
                        e.printStackTrace();
                    }
                }
            }
            savedModulesBundle = null;

        }
    }

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

        for(CalculationModule calculationModule : initialModules) {
            calculationModules.add(calculationModule); // when simplified to addAll, doesn't work properly
        }
    }
}
