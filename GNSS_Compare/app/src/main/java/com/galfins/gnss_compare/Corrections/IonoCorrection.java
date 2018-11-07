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

package com.galfins.gnss_compare.Corrections;

import android.location.Location;


import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.IonoGps;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.TopocentricCoordinates;
import com.galfins.gogpsextracts.Constants;

/**
 * Created by Sebastian Ciuban on 10/02/2018.
 *
 * Ionospheric Correction based on Klobuchar's Algorithm
 *
 * This algorithm can be applied to Galileo, GPS pseudoranges or any other constellation
 * However the required coefficients to compute this correction are contained only in the
 * GPS Navigation message. So for Galileo satellites this correciton will always return 0.0
 *
 * It accounts for roughly 50% of the total ionospheric error affecting the pseudoranges
 *
 *
 */

public class IonoCorrection extends Correction {


    private double correctionValue;

    private final static String NAME = "Klobuchar Iono Correction";


    public IonoCorrection(){
        super();
    }

    public void calculateCorrection(Time currentTime, Coordinates approximatedPose, SatellitePosition satelliteCoordinates, NavigationProducer navigationProducer, Location initialLocation) {

        IonoGps iono = navigationProducer.getIono(currentTime.getMsec(), initialLocation);

        if (iono.getBeta(0) == 0){

            correctionValue = 0.0;

        }else {


            // Compute the elevation and azimuth angles for each satellite
            TopocentricCoordinates topo = new TopocentricCoordinates();
            topo.computeTopocentric(approximatedPose, satelliteCoordinates);

            // Assign the elevation and azimuth information to new variables
            double elevation = topo.getElevation();
            double azimuth = topo.getAzimuth();

            double ionoCorr = 0;

            if (iono == null)
                return;
            //		    double a0 = navigation.getIono(currentTime.getMsec(),0);
            //		    double a1 = navigation.getIono(currentTime.getMsec(),1);
            //		    double a2 = navigation.getIono(currentTime.getMsec(),2);
            //		    double a3 = navigation.getIono(currentTime.getMsec(),3);
            //		    double b0 = navigation.getIono(currentTime.getMsec(),4);
            //		    double b1 = navigation.getIono(currentTime.getMsec(),5);
            //		    double b2 = navigation.getIono(currentTime.getMsec(),6);
            //		    double b3 = navigation.getIono(currentTime.getMsec(),7);

            elevation = Math.abs(elevation);

            // Parameter conversion to semicircles
            double lon = approximatedPose.getGeodeticLongitude() / 180; // geod.get(0)
            double lat = approximatedPose.getGeodeticLatitude() / 180; //geod.get(1)
            azimuth = azimuth / 180;
            elevation = elevation / 180;

            // Klobuchar algorithm

            // Compute the slant factor
            double f = 1 + 16 * Math.pow((0.53 - elevation), 3);

            // Compute the earth-centred angle
            double psi = 0.0137 / (elevation + 0.11) - 0.022;

            // Compute the latitude of the Ionospheric Pierce Point (IPP)
            double phi = lat + psi * Math.cos(azimuth * Math.PI);

            if (phi > 0.416) {
                phi = 0.416;

            }
            if (phi < -0.416) {
                phi = -0.416;
            }

            // Compute the longitude of the IPP
            double lambda = lon + (psi * Math.sin(azimuth * Math.PI))
                    / Math.cos(phi * Math.PI);

            // Find the geomagnetic latitude of the IPP
            double ro = phi + 0.064 * Math.cos((lambda - 1.617) * Math.PI);

            // Find the local time at the IPP
            double t = lambda * 43200 + currentTime.getGpsTime();

            while (t >= 86400)
                t = t - 86400;

            while (t < 0)
                t = t + 86400;

            // Compute the period of ionospheric delay
            double p = iono.getBeta(0) + iono.getBeta(1) * ro + iono.getBeta(2) * Math.pow(ro, 2) + iono.getBeta(3) * Math.pow(ro, 3);

            if (p < 72000)
                p = 72000;

            // Compute the amplitude of ionospheric delay
            double a = iono.getAlpha(0) + iono.getAlpha(1) * ro + iono.getAlpha(2) * Math.pow(ro, 2) + iono.getAlpha(3) * Math.pow(ro, 3);

            if (a < 0)
                a = 0;

            // Compute the phase of ionospheric delay
            double x = (2 * Math.PI * (t - 50400)) / p;

            // Compute the ionospheric correction
            if (Math.abs(x) < 1.57) {
                ionoCorr = Constants.SPEED_OF_LIGHT
                        * f
                        * (5e-9 + a
                        * (1 - (Math.pow(x, 2)) / 2 + (Math.pow(x, 4)) / 24));
            } else {
                ionoCorr = Constants.SPEED_OF_LIGHT * f * 5e-9;
            }

            correctionValue = ionoCorr;
        }
    }

    @Override
    public double getCorrection() {
        return correctionValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static void registerClass(){
        register(NAME, IonoCorrection.class);
    }
}
