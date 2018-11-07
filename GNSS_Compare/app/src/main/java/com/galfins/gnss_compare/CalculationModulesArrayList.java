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

import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
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
    private FusedLocationProviderClient fusedLocationProviderClientReference = null;
    private LocationManager locationManagerReference = null;

    public static abstract class PoseUpdatedListener {
        public abstract void onPoseUpdated();
    }

    private PoseUpdatedListener mPoseUpdatedListener = null;

    public void assignPoseUpdatedListener(PoseUpdatedListener newCallback){
        mPoseUpdatedListener = newCallback;
    }

    public class CallbacksNotAssignedException extends IllegalStateException{
        public CallbacksNotAssignedException(String message) {
            super(message);
        }
    }

    public CalculationModulesArrayList(){
        gnssCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);

            Log.d(TAG, "onGnssMeasurementsReceived: invoked!");

            for (CalculationModule calculationModule : CalculationModulesArrayList.this)
                calculationModule.updateMeasurements(eventArgs);

            if(mPoseUpdatedListener !=null)
                mPoseUpdatedListener.onPoseUpdated();
            }
        };

        locationRequest = new LocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(500);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(100);

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

    public void registerForGnssUpdates(FusedLocationProviderClient fusedLocationClient, LocationManager locationManager){
        try {

            fusedLocationProviderClientReference = fusedLocationClient;
            locationManagerReference = locationManager;

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper());

            locationManager.registerGnssMeasurementsCallback(
                    gnssCallback);

        } catch (SecurityException e){
            e.printStackTrace();
        }
    }

    public void unregisterFromGnssUpdates(){
        if (fusedLocationProviderClientReference!=null)
            fusedLocationProviderClientReference.removeLocationUpdates(locationCallback);
        if(locationManagerReference!=null)
            locationManagerReference.unregisterGnssMeasurementsCallback(gnssCallback);
    }
}
