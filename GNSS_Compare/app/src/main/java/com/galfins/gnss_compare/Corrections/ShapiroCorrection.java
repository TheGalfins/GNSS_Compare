package com.galfins.gnss_compare.Corrections;

import android.location.Location;

import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.Constants;

import org.ejml.simple.SimpleMatrix;

/**
 * Created by Sebastian Ciuban on 10/02/2018.
 *
 * Correction for the Shapiro delay that is also known as the relativistic path range correction
 *
 */

public class ShapiroCorrection extends Correction {

    private final static String NAME = "Relativistic path range correction";

    private double correctionValue;

    public ShapiroCorrection(){
        super();
    }

    @Override
    public void calculateCorrection(Time currentTime, Coordinates approximatedPose, SatellitePosition satelliteCoordinates, NavigationProducer navigationProducer, Location initialLocation) {
        // Compute the difference vector between the receiver and the satellite
        SimpleMatrix diff = approximatedPose.minusXYZ(satelliteCoordinates);

        // Compute the geometric distance between the receiver and the satellite

        double geomDist = Math.sqrt(Math.pow(diff.get(0), 2) + Math.pow(diff.get(1), 2) + Math.pow(diff.get(2), 2));

        // Compute the geocentric distance of the receiver
        double geoDistRx = Math.sqrt(Math.pow(approximatedPose.getX(), 2) + Math.pow(approximatedPose.getY(), 2) + Math.pow(approximatedPose.getZ(), 2));

        // Compute the geocentric distance of the satellite
        double geoDistSv = Math.sqrt(Math.pow(satelliteCoordinates.getX(), 2) + Math.pow(satelliteCoordinates.getY(), 2) + Math.pow(satelliteCoordinates.getZ(), 2));


        // Compute the shapiro correction
        correctionValue = ((2.0 * Constants.EARTH_GRAVITATIONAL_CONSTANT)/Math.pow(Constants.SPEED_OF_LIGHT, 2)) * Math.log((geoDistSv + geoDistRx + geomDist ) / (geoDistSv + geoDistRx - geomDist));

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
        register(NAME, ShapiroCorrection.class);
    }
}
