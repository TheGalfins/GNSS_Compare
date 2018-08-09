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

    private DataViewerAdapter pagerAdapterReference = null;
    private final String TAG="CalculationModulesArrayList";

    private GnssMeasurementsEvent.Callback gnssCallback;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    public CalculationModulesArrayList(DataViewerAdapter pagerAdapter){
        pagerAdapterReference = pagerAdapter;

        gnssCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                super.onGnssMeasurementsReceived(eventArgs);

                Log.d(TAG, "onGnssMeasurementsReceived: invoked!");

                for (CalculationModule calculationModule : CalculationModulesArrayList.this)
                    calculationModule.updateMeasurements(eventArgs);

                MainActivity.createdCalculationModules.notifyObservers();
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
                        for (CalculationModule calculationModule : MainActivity.createdCalculationModules)
                            calculationModule.updateLocationFromGoogleServices(lastLocation);

                    }
                }
            }
        };
    }

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
    }

    @Override
    public boolean add(final CalculationModule calculationModule) {

        if(pagerAdapterReference != null)
            for (DataViewer viewer : pagerAdapterReference.getViewers()) {
                viewer.addSeries(calculationModule);
            }

        synchronized (this) {
            return super.add(calculationModule);
        }
    }

    @Override
    public boolean remove(Object o){
        if(pagerAdapterReference != null)
            for (DataViewer viewer : pagerAdapterReference.getViewers())
                viewer.removeSeries((CalculationModule)o);

        synchronized (this) {
            return super.remove(o);
        }
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

    /**
     * Adds all created modules to viewers. This should be called on reset of the application
     * where created calculation modules stay in memory, but data viewers are recreated
     */
    public void reinitialize() {
        if(pagerAdapterReference != null)
            for (CalculationModule calculationModule : this)
                for (DataViewer viewer : pagerAdapterReference.getViewers())
                    viewer.addSeries(calculationModule);
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
