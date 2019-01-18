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
    private List<SatelliteParameters> unusedSatellites = new ArrayList<>();

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
    public List<SatelliteParameters> getUnusedSatellites() {
        return unusedSatellites;
    }

    @Override
    public int getVisibleConstellationSize() {
        synchronized (this) {
            return observedSatellites.size()+unusedSatellites.size();
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

            observedSatellites.clear();
            unusedSatellites.clear();

            observedSatellites.addAll(gpsConstellation.getSatellites());
            observedSatellites.addAll(galileoConstellation.getSatellites());

            unusedSatellites.addAll(gpsConstellation.getUnusedSatellites());
            unusedSatellites.addAll(galileoConstellation.getUnusedSatellites());
        }
    }

    public void updateMeasurements(GnssMeasurementsEvent event) {
        synchronized (this) {

            observedSatellites.clear();
            unusedSatellites.clear();

            galileoConstellation.updateMeasurements(event);
            gpsConstellation.updateMeasurements(event);

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
