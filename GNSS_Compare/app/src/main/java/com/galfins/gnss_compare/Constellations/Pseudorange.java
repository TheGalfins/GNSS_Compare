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
