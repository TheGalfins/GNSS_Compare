package com.galfins.gnss_compare.DataViewers;

import android.location.Location;

import com.galfins.gnss_compare.CalculationModule;

/**
 * Created by Mateusz Krainski on 25/03/2018.
 * This class is defining an interface for date viewers
 */

public interface DataViewer {

    /**
     * This is called whenever a new calculation module is created.
     * @param calculationModule new calculation module
     */
    void addSeries(CalculationModule calculationModule);

    /**
     * This is called when a calculation module is destroyed
     * @param calculationModule destroyed calculation module
     */
    void removeSeries(CalculationModule calculationModule);

    void onLocationFromGoogleServicesResult(Location location);

    /**
     * Simple interface assuring that data series of classes extending GnssPlot will have a method
     * to return their calculation module reference.
     */
    interface CalculationModuleDataSeries{
        CalculationModule getCalculationModuleReference();
    }
}
