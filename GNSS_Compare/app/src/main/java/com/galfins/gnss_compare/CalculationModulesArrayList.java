package com.galfins.gnss_compare;

import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.util.Log;

import com.galfins.gnss_compare.DataViewers.DataViewer;
import com.galfins.gnss_compare.DataViewers.DataViewerAdapter;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.ArrayList;
import java.util.Observer;

/**
 * Class encapsulating generic operations on created CalculationModules.
 */
public class CalculationModulesArrayList extends ArrayList<CalculationModule> {

    private final String TAG="CalculationModulesArrayList";

    private GnssMeasurementsEvent.Callback gnssCallback;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    public GnssMeasurementsEvent.Callback getGnssCallback() {
        return gnssCallback;
    }

    public LocationCallback getLocationCallback() {
        return locationCallback;
    }

    public LocationRequest getLocationRequest() {
        return locationRequest;
    }

    public CalculationModulesArrayList(){
        gnssCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                super.onGnssMeasurementsReceived(eventArgs);

                Log.d(TAG, "onGnssMeasurementsReceived: invoked!");

                for (CalculationModule calculationModule : CalculationModulesArrayList.this)
                    calculationModule.updateMeasurements(eventArgs);

                notifyObservers();
            }
        };

        locationRequest = new LocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(500);
        locationRequest.setInterval(100);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {

                final Location lastLocation = locationResult.getLocations().get(locationResult.getLocations().size()-1);

                if(lastLocation != null) {
                    synchronized (this) {
                        for (CalculationModule calculationModule : CalculationModulesArrayList.this)
                            calculationModule.updateLocationFromGoogleServices(lastLocation);

                    }
                }
            }
        };
    }

    /**
     * Start threads associated with added CalculationModules. This is a single execution
     * of a calculation module's notifyObservers() method
     */
    public void notifyObservers() {
        Log.d(TAG, "notifyObservers: invoked");
        synchronized (this) {
            for (CalculationModule calculationModule : this) {
                calculationModule.notifyObservers();
            }
        }
    }

    public void addObserver(Observer observer){
        for (CalculationModule calculationModule : this){
            calculationModule.addObserver(observer);
        }
    }

    public void removeObserver(Observer observer){
        for (CalculationModule calculationModule : this){
            calculationModule.removeObserver(observer);
        }
    }
}
