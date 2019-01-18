package com.galfins.gnss_compare.Constellations;

import android.location.GnssMeasurementsEvent;
import android.location.Location;

import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gogpsextracts.Constants;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.Time;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class GpsIonoFreeConstellation extends GpsConstellation {

    private GpsConstellation gpsL1Constellation = new GpsConstellation();
    private GpsL5Constellation gpsL5Constellation = new GpsL5Constellation();
//    private GpsL1Constellation gpsIonoFreeConstellation = new GpsL1Constellation();

    //    private static final String NAME = "GPS<sub><small><small>IF</small></small></sub>";
    private static final String NAME = "GPS IF";

    private Time timeRefMsec;


    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {
        synchronized (this) {

            timeRefMsec = new Time(System.currentTimeMillis());

            gpsL1Constellation.updateMeasurements(event);
            gpsL5Constellation.updateMeasurements(event);

            observedSatellites.clear();

            for(SatelliteParameters satelliteL1 : gpsL1Constellation.getSatellites()) {

                SatelliteParameters satelliteL5 = null;

                for(SatelliteParameters satelliteParameters: gpsL5Constellation.getSatellites()){
                    if(satelliteParameters.getSatId() == satelliteL1.getSatId()) {
                        satelliteL5 = satelliteParameters;
                        break;
                    }
                }

                if(satelliteL5 != null) {
                    // Get the code based pseudoranges on the E1 and E5a frequencies
                    double pseudorangeL1    =  satelliteL1.getPseudorange();
                    double pseudorangeL5   =  satelliteL5.getPseudorange();

                    // Form the ionosphere-free (IF) combination
                    double pseudorangeIF    = (Math.pow(Constants.FL1,2) * pseudorangeL1 - Math.pow(Constants.FL5,2) * pseudorangeL5)/(Math.pow(Constants.FL1,2)-Math.pow(Constants.FL5,2));

                    SatelliteParameters newSatellite = new SatelliteParameters(
                            satelliteL1.getSatId(),
                            new Pseudorange(pseudorangeIF, 0.0));

//                    newSatellite.setUniqueSatId("G"+satelliteL1.getSatId()+"<sub><small><small>IF</small></small></sub>");
                    newSatellite.setUniqueSatId("G"+satelliteL1.getSatId()+"_IF");

                    //todo: assign properly
                    newSatellite.setSignalStrength(
                            (satelliteL1.getSignalStrength() + satelliteL5.getSignalStrength())/2);

                    newSatellite.setConstellationType(satelliteL1.getConstellationType());

                    observedSatellites.add(newSatellite);
                }
            }

            visibleButNotUsed = max(gpsL1Constellation.getVisibleConstellationSize(), gpsL5Constellation.getVisibleConstellationSize())-observedSatellites.size();

            tRxGPS = gpsL1Constellation.gettRxGPS();
            weekNumberNanos = gpsL1Constellation.getWeekNumber();
        }
    }

    @Override
    public void calculateSatPosition(Location initialLocation, Coordinates position) {
        super.calculateSatPosition(initialLocation, position);

        gpsL1Constellation.calculateSatPosition(initialLocation, position);
        gpsL5Constellation.calculateSatPosition(initialLocation, position);

        visibleButNotUsed = max(gpsL1Constellation.getVisibleConstellationSize(), gpsL5Constellation.getVisibleConstellationSize())-observedSatellites.size();
    }

    @Override
    public Time getTime() {
        synchronized (this) {
            return timeRefMsec;
        }
    }


    @Override
    public void addCorrections(ArrayList<Correction> corrections) {
        super.addCorrections(corrections);

        gpsL1Constellation.addCorrections(corrections);
        gpsL5Constellation.addCorrections(corrections);
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getConstellationId() {
        synchronized (this) {
            return Constellation.CONSTELLATION_GPS_IonoFree;
        }
    }

    public static void registerClass() {
        register(
                NAME,
                GpsIonoFreeConstellation.class);
    }
}