package com.galfins.gnss_compare.Constellations;

import android.location.GnssMeasurementsEvent;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.Time;

public class GalileoGpsConstellation extends Constellation {

    private GpsConstellation gpsConstellation = new GpsConstellation();
    private GalileoConstellation galileoConstellation = new GalileoConstellation();

    private static final String NAME = "Galileo + GPS";

    /**
     * List holding observed satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();

    @Override
    public Coordinates getRxPos() {
        synchronized (this) {
            return gpsConstellation.getRxPos();
        }
    }

    @Override
    public void setRxPos(Coordinates rxPos) {
        synchronized (this) {
            gpsConstellation.setRxPos(rxPos);
            galileoConstellation.setRxPos(rxPos);
        }
    }

    @Override
    public SatelliteParameters getSatellite(int index) {
        synchronized (this) {
            return observedSatellites.get(index);
        }
    }

    @Override
    public List<SatelliteParameters> getSatellites() {
        synchronized (this) {
            return observedSatellites;
        }
    }

    @Override
    public int getVisibleConstellationSize() {
        synchronized (this) {
            return gpsConstellation.getVisibleConstellationSize() + galileoConstellation.getVisibleConstellationSize();
        }
    }

    @Override
    public int getUsedConstellationSize() {
        synchronized (this) {
            return observedSatellites.size();
        }
    }

    @Override
    public void calculateSatPosition(Location location, Coordinates position) {
        synchronized (this) {
            gpsConstellation.calculateSatPosition(location, position);
            galileoConstellation.calculateSatPosition(location, position);
        }
    }

    public void updateMeasurements(GnssMeasurementsEvent event) {
        synchronized (this) {

            galileoConstellation.updateMeasurements(event);
            gpsConstellation.updateMeasurements(event);

            observedSatellites.clear();

            for (int i = 0; i < gpsConstellation.getUsedConstellationSize(); i++)
                observedSatellites.add(gpsConstellation.getSatellite(i));

            for (int i = 0; i < galileoConstellation.getUsedConstellationSize(); i++)
                observedSatellites.add(galileoConstellation.getSatellite(i));
        }
    }

    @Override
    public double getSatelliteSignalStrength(int index) {
        synchronized (this) {
            return observedSatellites.get(index).getSignalStrength();
        }
    }

    @Override
    public int getConstellationId() {
        synchronized (this) {
            return Constellation.CONSTELLATION_GALILEO_GPS;
        }
    }

    @Override
    public void addCorrections(ArrayList<Correction> corrections) {
        synchronized (this) {
            gpsConstellation.addCorrections(corrections);
            galileoConstellation.addCorrections(corrections);
        }
    }

    @Override
    public Time getTime() {
        synchronized (this) {
            return gpsConstellation.getTime();
        }
    }

    @Override
    public String getName() {
        synchronized (this) {
            return NAME;
        }
    }

    public static void registerClass() {
        register(
                NAME,
                GalileoGpsConstellation.class);
    }
}
