package com.galfins.gnss_compare.Corrections;

import android.location.Location;

import java.util.HashMap;
import java.util.Set;


import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.SatellitePosition;

/**
 * Created by Mateusz Krainski on 1/20/2018.
 * This class is for implementing a uniform interface for correction modules
 */
public abstract class Correction {

    /**
     * Calculates current correction for given parameters
     * @param currentTime current timestamp
     * @param approximatedPose approximate pose of the receiver
     * @param satelliteCoordinates satellite coordinates
     * @param navigationProducer Klobuchar coefficients from the naivgation message (ephemeris)
     * @return calculated correction to be applied to the pseudorange
     */
    public abstract void calculateCorrection(
            Time currentTime,
            Coordinates approximatedPose,
            SatellitePosition satelliteCoordinates,
            NavigationProducer navigationProducer,
            Location initialLocation);

    /**
     *
     * @return calculated correction
     */
    public abstract double getCorrection();

    /**
     * stores all classes which extend the Correction class and were registered with the
     * register method
     */
    private static HashMap<String, Class <? extends Correction>> registeredObjects = new HashMap<>();

    /**
     * Registers the new Correction class
     * @param correctionName name of the class
     * @param objectClass reference to the class
     */
    protected static void register(String correctionName, Class <?extends Correction> objectClass) {
        if(!registeredObjects.containsKey(correctionName))
            registeredObjects.put(correctionName, objectClass);
    }

    /**
     * names of all registered classes
     */
    public static Set<String> getRegistered(){
        return registeredObjects.keySet();
    }

    /**
     *
     * @param name name of the class
     * @return class reference
     */
    public static Class<? extends Correction> getClassByName(String name) {
        return registeredObjects.get(name);
    }

    /**
     *
     * @return name of the constellation
     */
    public abstract String getName();

    /**
     * Indicates if initialization has already been performed
     */
    private static boolean initialized = false;

    /**
     * Registers all constellation classes which extend this
     */
    public static void initialize() {
        if(!initialized) {
            IonoCorrection.registerClass();
            ShapiroCorrection.registerClass();
            TropoCorrection.registerClass();
            initialized = true;
        }
    }
}
