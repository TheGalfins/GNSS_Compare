package com.galfins.gnss_compare.Constellations;

/**
 * Created by Mateusz Krainski on 1/20/2018.
 * This class is for representing the received pseudorange
 */

public class Pseudorange {

    /**
     * Pseudorange
     */
    private double pseudorange;

    /**
     * Pseudorange rate
     */
    private double pseudorangeRate;

    public Pseudorange(double pseudorange,
                       double pseudorangeRate) {
        this.pseudorange = pseudorange;
        this.pseudorangeRate = pseudorangeRate;

    }

    /**
     * Variance of the measurement. Updated by the Constellation object, based on
     * the satellite's topocentric coordinates
     */
    private double measurementVariance = 0;

    public double getPseudorange(){
        return pseudorange;
    }

    public double getPseudorangeRate(){
        return pseudorangeRate;
    }


    public double getMeasurementVariance() {
        return measurementVariance;
    }

    public void setMeasurementVariance(double measurementVariance) {
        this.measurementVariance = measurementVariance;
    }
}
