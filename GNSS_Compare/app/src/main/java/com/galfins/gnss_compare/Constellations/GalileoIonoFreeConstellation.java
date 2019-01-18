package com.galfins.gnss_compare.Constellations;

import android.location.GnssMeasurementsEvent;
import android.location.Location;

import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.Constants;

import java.util.ArrayList;

import static java.lang.Math.max;

public class GalileoIonoFreeConstellation extends GalileoConstellation {

    private GalileoConstellation galileoConstellation = new GalileoConstellation();
    private GalileoE5aConstellation galileoE5aConstellation = new GalileoE5aConstellation();
//    GalileoE1Constellation galileoIonoFreeConstellation = new GalileoE1Constellation();

    private static final String NAME = "Galileo IF";

    private Time timeRefMsec;

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {
        synchronized (this) {

            timeRefMsec = new Time(System.currentTimeMillis());

            galileoConstellation.updateMeasurements(event);
            galileoE5aConstellation.updateMeasurements(event);

            observedSatellites.clear();

            for(SatelliteParameters satelliteE1 : galileoConstellation.getSatellites()) {

                SatelliteParameters satelliteE5a = null;

                for(SatelliteParameters satelliteParameters: galileoE5aConstellation.getSatellites()){
                    if(satelliteParameters.getSatId() == satelliteE1.getSatId()) {
                        satelliteE5a = satelliteParameters;
                        break;
                    }
                }

                if(satelliteE5a != null) {
                    // Get the code based pseudoranges on the E1 and E5a frequencies
                    double pseudorangeE1    =  satelliteE1.getPseudorange();
                    double pseudorangeE5a   =  satelliteE5a.getPseudorange();

                    // Form the ionosphere-free (IF) combination
                    double pseudorangeIF    = (Math.pow(Constants.FE1,2) * pseudorangeE1 - Math.pow(Constants.FE5a,2) * pseudorangeE5a)/(Math.pow(Constants.FE1,2)-Math.pow(Constants.FE5a,2));

                    SatelliteParameters newSatellite = new SatelliteParameters(
                            satelliteE1.getSatId(),
                            new Pseudorange(pseudorangeIF, 0.0));

//                    newSatellite.setUniqueSatId("E"+satelliteE1.getSatId()+"<sub>IF</sub>");
                    newSatellite.setUniqueSatId("E"+satelliteE1.getSatId()+"_IF");

                    //todo: assign properly
                    newSatellite.setSignalStrength(
                            (satelliteE1.getSignalStrength() + satelliteE5a.getSignalStrength())/2);

                    newSatellite.setConstellationType(satelliteE1.getConstellationType());

                    observedSatellites.add(newSatellite);
                }
            }

            visibleButNotUsed = max(galileoConstellation.getVisibleConstellationSize(), galileoE5aConstellation.getVisibleConstellationSize())-observedSatellites.size();

            tRxGalileoTOW = galileoConstellation.gettRxGalileoTOW();

            weekNumber = galileoConstellation.getWeekNumber()*Constants.NUMBER_NANO_SECONDS_PER_WEEK;

        }
    }

    @Override
    public void calculateSatPosition(Location initialLocation, Coordinates position) {
        super.calculateSatPosition(initialLocation, position);

        galileoE5aConstellation.calculateSatPosition(initialLocation, position);
        galileoConstellation.calculateSatPosition(initialLocation, position);

        visibleButNotUsed = max(galileoConstellation.getVisibleConstellationSize(), galileoE5aConstellation.getVisibleConstellationSize())-observedSatellites.size();
    }

    @Override
    public void addCorrections(ArrayList<Correction> corrections) {
        super.addCorrections(corrections);

        galileoE5aConstellation.addCorrections(corrections);
        galileoConstellation.addCorrections(corrections);
    }

    @Override
    public Time getTime() {
        synchronized (this) {
            return timeRefMsec;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getConstellationId() {
        synchronized (this) {
            return Constellation.CONSTELLATION_GALILEO_IonoFree;
        }
    }

    public static void registerClass() {
        register(
                NAME,
                GalileoIonoFreeConstellation.class);
    }
}
