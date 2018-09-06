package com.galfins.gnss_compare;

import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createInitialCalculationModules();

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

        calculationModules.registerForGnssUpdates(mFusedLocationClient, mLocationManager);

        calculationModules.assignPoseUpdatedListener(poseListener);

        Constellation.initialize();
        Correction.initialize();
        PvtMethod.initialize();
        FileLogger.initialize();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        calculationModules.unregisterFromGnssUpdates();
        calculationModules.clear();
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
