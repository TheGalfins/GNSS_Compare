package com.galfins.gnss_compare.PvtMethods;

import java.util.HashMap;
import java.util.Set;

import com.galfins.gnss_compare.Constellations.Constellation;

import com.galfins.gogpsextracts.Coordinates;

/**
 * Created by Mateusz Krainski on 1/20/2018.
 * This class is for implementing a generic interface for PVT calculation methods
 */

public abstract class PvtMethod {
    /**
     * Calculates pose based on given parameters
     * @param constellation satellite constellation object for which the calculations are to
     *                      be performed
     * @return calculated pose of the receiver
     */
    public abstract Coordinates calculatePose(
            Constellation constellation
    );

    private static HashMap<String, Class<? extends PvtMethod>> registeredObjects = new HashMap<>();

    protected static void register(String constellationName, Class<?extends PvtMethod> objectClass) {
//        if(registeredObjects.containsKey(constellationName))
//            throw new IllegalArgumentException("This key is already registered! Select a different name!");
        if(!registeredObjects.containsKey(constellationName))
            registeredObjects.put(constellationName, objectClass);
    }

    public static Set<String> getRegistered(){
        return registeredObjects.keySet();
    }

    /**
     * @return Name of the PVT method, which is to be displayed in the UI
     */
    public abstract String getName();

    public static Class<? extends PvtMethod> getClassByName(String name) {
        return registeredObjects.get(name);
    }

    public abstract double getClockBias();

    private static boolean initialized = false;

    public static void initialize() {
        if(!initialized) {
            WeightedLeastSquares.registerClass();
            StaticExtendedKalmanFilter.registerClass();
            DynamicExtendedKalmanFilter.registerClass();
            PedestrianStaticExtendedKalmanFilter.registerClass();
            initialized = true;
        }
    }
}
