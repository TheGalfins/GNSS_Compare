package com.galfins.gnss_compare.Constellations;

import android.location.GnssMeasurementsEvent;
import android.location.Location;

import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.Time;


/**
 * Created by Mateusz Krainski on 1/28/2018.
 * This class implements an abstract class Constellation, which should be extended by each
 * implemented satellite constellation which is to be used.
 */

public abstract class Constellation {

    /**
     * Indicates if initialization has already been performed
     */
    private static boolean initialized = false;

    /**
     * Registers all constellation classes which extend this
     */
    public static void initialize() {
        if(!initialized) {
            GpsConstellation.registerClass();
            GalileoConstellation.registerClass();
            GalileoGpsConstellation.registerClass();
            initialized = true;
        }
    }

    /**
     * Additional definition of an ID for a new constellation type
     */
    public static final int CONSTELLATION_GALILEO_GPS = 999; //todo is there a better way to define this?

    /**
     *
     * @return the estimated receiver position
     */
    public abstract Coordinates getRxPos();

    /**
     *
     * @param rxPos new estimated receiver position
     */
    public abstract void setRxPos(Coordinates rxPos);

    /**
     * Factory method for converting RxPos to a SimpleMatrix
     * @param rxPos rxPos Coordinates object
     * @return RxPos as a 4z1 vector
     */
    public static SimpleMatrix getRxPosAsVector(Coordinates rxPos){
        SimpleMatrix rxPosSimpleVector = new SimpleMatrix(4, 1);
        rxPosSimpleVector.set(0, rxPos.getX());
        rxPosSimpleVector.set(1, rxPos.getY());
        rxPosSimpleVector.set(2, rxPos.getZ());
        rxPosSimpleVector.set(3, 0);

        return rxPosSimpleVector;
    }

    /**
     *
     * @param index id
     * @return satellite of that id
     */
    public abstract SatelliteParameters getSatellite(int index);

    /**
     *
     * @return all satellites registered in the object
     */
    public abstract List<SatelliteParameters> getSatellites();

    /**
     *
     * @return size of the visible constellation
     */
    public abstract int getVisibleConstellationSize();

    /**
     *
     * @return size of the used constellation
     */
    public abstract int getUsedConstellationSize();

    /**
     * Method which is to calculate the satellite positions based on current satellite parameters
     * and passed location objects
     * @param initialLocation initial location, can be used to retrieve the navigation message
     * @param position current position of the receiver
     */
    public abstract void calculateSatPosition(Location initialLocation, Coordinates position);

    /**
     * stores all classes which extend the Constellation class and were registered with the
     * register method
     */
    private static HashMap<String, Class<? extends Constellation>> registeredObjects = new HashMap<>();

    /**
     * Registers the new Constellation class
     * @param constellationName name of the class
     * @param objectClass reference to the class
     */
    protected static void register(
            String constellationName,
            Class <? extends Constellation> objectClass) {

        if(!registeredObjects.containsKey(constellationName)) {
            registeredObjects.put(constellationName, objectClass);
        }
    }

    /**
     *
     * @return names of all registered classes
     */
    public static Set<String> getRegistered(){
        return registeredObjects.keySet();
    }

    /**
     *
     * @param name name of the class
     * @return class reference
     */
    public static Class<? extends Constellation> getClassByName(String name){
        return registeredObjects.get(name);
    }

    /**
     * Returns signal strength to a satellite given by an index.
     * Warning: index is the index of the satellite as stored in internal list, not it's id.
     * @param index index of satellite
     * @return signal strength for the satellite given by {@code index}.
     */
    public abstract double getSatelliteSignalStrength(int index);

    /**
     * @return ID of the constellation
     */
    public abstract int getConstellationId();

    /**
     * Adds corrections to the processing flow
     * @param corrections corrections to be added
     */
    public abstract void addCorrections(ArrayList<Correction> corrections);

    /**
     *
     * @return time of measurement
     */
    public abstract Time getTime();

    /**
     *
     * @return name of the constellation
     */
    public abstract String getName();

    /**
     * method invoked on every GNSS measurement event update. It should update satellite's internal
     * parameters.
     * @param event GNSS event
     */
    public abstract void updateMeasurements(GnssMeasurementsEvent event);
}
