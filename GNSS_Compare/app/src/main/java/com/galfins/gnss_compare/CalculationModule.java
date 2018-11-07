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

package com.galfins.gnss_compare;

import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.graphics.Color;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gnss_compare.FileLoggers.FileLogger;
import com.galfins.gnss_compare.PvtMethods.PvtMethod;

import com.galfins.gogpsextracts.Coordinates;

/**
 * Created by Mateusz Krainski on 1/20/2018.
 * This class is for performing all of the calculations and retrieving GNSS data from the chip.
 */
public class CalculationModule{

    /**
     * Choose format for logging file
     */
    public void setActive(boolean active) {
        this.active = active;
    }

//        returnedValue.add(getName());
//        returnedValue.add(String.valueOf(active));
//        returnedValue.add(String.valueOf(logToFile));
//        returnedValue.add(constellation.getName());
//        returnedValue.add(pvtMethod.getName());
//        returnedValue.add(fileLogger.getName());
//
//        for(Correction corr : corrections)
//            returnedValue.add(corr.getName());



    /**
     * @return true if the module is active and performing calculations
     */
    public boolean getActive() {
        return active;
    }

    /**
     *
     * @return reference to the used pvt method object
     */
    public PvtMethod getPvtMethod() {
        return pvtMethod;
    }

    /**
     *
     * @return reference to used correction objects
     */
    public ArrayList<Correction> getCorrections() {
        return corrections;
    }

    /**
     *
     * @return reference to used file logger object
     */
    public FileLogger getFileLogger() {
        return fileLogger;
    }

    /**
     * Raised when newly created calculation module has a name which has already been registered
     *
     */
    public class NameAlreadyRegisteredException extends Exception{
        public NameAlreadyRegisteredException(String message) {
            super(message);
        }
    }

    /**
     * Raised when the number of created calculation modules exceeds allowable limit.
     */
    public class NumberOfSeriesExceededLimitException extends Exception{
        public NumberOfSeriesExceededLimitException(String message) {
            super(message);
        }
    }

    /**
     * Color used for data plotting
     */
    private int DATA_COLOR;

    /**
     * PVT method associated with this calculation module
     */
    private PvtMethod pvtMethod;

    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections;

    /**
     * List of registered module ids
     */
    private static List<Integer> registeredIds = new ArrayList<>();

    /**
     * List of registered names
     */
    private static List<String> registeredNames = new ArrayList<>();

    /**
     * Name of the module
     */
    private String NAME;

    /**
     * Array of colors from which the data color is selected
     */
    private static final int seriesColors[] = {
            Color.rgb(84,110,122),
            Color.rgb(142,36,170),
            Color.rgb(229,57,53),
            Color.rgb(3,155,229),
            Color.rgb(0,137,123),
            Color.rgb(124,179,66),
            Color.rgb(253,216,53),
            Color.rgb(251,140,0),
            Color.rgb(109,76,65),
            Color.rgb(84,110,122)
    };

    /**
     * Calculated pose of the receiver
     */
    private Coordinates pose;

    /**
     * Flag indicating that the locationFromGoogleServices has been initialized.
     */
    private boolean poseInitialized = false;

    /**
     * Satellite constellation object
     */
    private Constellation constellation;

    /**
     * @return location as calculated by the phone's location service
     */
    public Location getLocationFromGoogleServices() {
        return locationFromGoogleServices;
    }

    /**
     * Location calculated by the Google services
     */
    private Location locationFromGoogleServices;

    /**
     * Tag used for logging
     */
    private final static String TAG = "Calculation Module";

    /**
     * String description of the module
     */
    private String moduleDescription;

    /**
     * Flag indicating if the calculation module is active and performing calculations
     */
    private boolean active;

    /**
     *
     * @return true if the logging to file functionality is currently on
     */
    public boolean isLogToFile() {
        return logToFile;
    }

    /**
     * File logger object used for this calculation module
     */
    private FileLogger fileLogger;

    /**
     * @param logToFile if true, starts the logging functionality, if false - stops the logging
     *                  functionality
     */
    public void setLogToFile(boolean logToFile) {
        this.logToFile = logToFile;
        if (logToFile){
            fileLogger.startNewLog();
            pvtMethod.startLog(NAME);
        } else {
            fileLogger.closeLog();
            pvtMethod.stopLog();
        }
    }

    /**
     * Flag controlling the log to file functionality
     */
    private boolean logToFile;

    /**
     * @return current calculated pose of the receiver
     */
    public Coordinates getPose() {
        return pose;
    }

    /**
     * Reference to the class used to create the constellation object
     */
    private Class <? extends Constellation> constellationClass;

    /**
     * Reference to classes used to create correction objects
     */
    private List<Class <?extends Correction>> correctionClasses;

    /**
     * Reference to the class used for creating the pvt method object
     */
    private Class <? extends PvtMethod> pvtMethodClass;

    /**
     * Reference to the class used for creating the file logger object
     */
    private Class <? extends FileLogger> fileLoggerClass;

    /**
     * @return Constellation of observed satellites.
     */
    public Constellation getConstellation(){
        return constellation;
    }

    /**
     * Constructor
     * @param name name of this calculation module
     * @param constellationClass reference to the class used to create the constellation object
     * @param correctionClasses reference to the classes used to create correction obejcts
     * @param pvtMethodClass reference to the class used to create the pvt method object
     * @param fileLoggerClass reference to the class used to create the file logger object
     * @throws NameAlreadyRegisteredException when name is already on the list of registered modules
     * @throws NumberOfSeriesExceededLimitException when no more modules can be created
     */
    public CalculationModule(
            String name,
            Class <? extends Constellation> constellationClass,
            List<Class <?extends Correction>> correctionClasses,
            Class <? extends PvtMethod> pvtMethodClass,
            Class <? extends FileLogger> fileLoggerClass
    ) throws NameAlreadyRegisteredException, NumberOfSeriesExceededLimitException {

        this.constellationClass = constellationClass;
        this.correctionClasses = correctionClasses;
        this.pvtMethodClass = pvtMethodClass;
        this.fileLoggerClass = fileLoggerClass;

        if(registeredNames.contains(name)){
            throw new NameAlreadyRegisteredException("This name is already registered, select a different one!");
        } else if (registeredIds.size() >= seriesColors.length){
            throw new NumberOfSeriesExceededLimitException("Can't add more series!");
        }

        NAME = name;
        registeredNames.add(name);

        pose = Coordinates.globalGeodInstance(0.000001, 0.000001, 0.000001); // can't be zeros - ECEF conversion crashes

        corrections = new ArrayList<>();
        try {
            updateSetup();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        int id = 0;
        while(registeredIds.contains(id)){
            id++;
        }

        registeredIds.add(id);

        DATA_COLOR = seriesColors[id];
    }


    /**
     * Method which reads the current settings for this calculation module and updates
     * satellite constellation, PVT modules and correction modules
     */
    private void updateSetup() throws IllegalAccessException, InstantiationException {
        moduleDescription = "";

        constellation = constellationClass.newInstance();
        moduleDescription += constellation.getName() + "\n";


        pvtMethod = pvtMethodClass.newInstance();
        moduleDescription += pvtMethod.getName() + "\n";

        corrections.clear();
        for(Class<? extends Correction> correctionClass : correctionClasses){
            Correction correction = correctionClass.newInstance();
            corrections.add(correction);
            moduleDescription += correction.getName() + "\n";
        }

        constellation.addCorrections(corrections);

        boolean isLogStarted = false;
        if (fileLogger != null) {
            isLogStarted = fileLogger.isStarted();
            if (isLogStarted){
                fileLogger.closeLog();
            }
        }

        fileLogger = fileLoggerClass.newInstance();
        fileLogger.setName(NAME);
        moduleDescription += fileLogger.getName() + "\n";

        if (isLogStarted) {
            fileLogger.startNewLog();
        }


        active = true;
    }

    /**
     * Updates existing calculation module with new settings
     * @param name name of this calculation module
     * @param constellationClass reference to the class used to create the constellation object
     * @param correctionClasses reference to the classes used to create correction obejcts
     * @param pvtMethodClass reference to the class used to create the pvt method object
     * @param fileLoggerClass reference to the class used to create the file logger object
     * @throws NameAlreadyRegisteredException when name is already on the list of registered modules
     * @throws NumberOfSeriesExceededLimitException when no more modules can be created
     */
    private void updateSetup(String name,
        Class <? extends Constellation> constellationClass,
        List<Class <?extends Correction>> correctionClasses,
        Class <? extends PvtMethod> pvtMethodClass,
        Class <? extends FileLogger> fileLoggerClass
    ) throws NameAlreadyRegisteredException, NumberOfSeriesExceededLimitException {

        synchronized (this) {

            this.constellationClass = constellationClass;
            this.correctionClasses = correctionClasses;
            this.pvtMethodClass = pvtMethodClass;
            this.fileLoggerClass = fileLoggerClass;

            if (!NAME.equals(name)) {
                if (registeredNames.contains(name)) {
                    throw new NameAlreadyRegisteredException("This name is already registered, select a different one!");
                } else if (registeredIds.size() >= seriesColors.length) {
                    throw new NumberOfSeriesExceededLimitException("Can't add more series!");
                }

                registeredNames.remove(NAME);
                NAME = name;
                registeredNames.add(name);
            }

            corrections = new ArrayList<>();
            try {
                updateSetup();
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @return module description
     */
    public String getModuleDescription(){
        return moduleDescription;
    }

    /**
     * Performs the update on the GNSS event and all following calculations.
     * @param event GNSS event
     */
    public void updateMeasurements(GnssMeasurementsEvent event){

        if (active) {

            constellation.updateMeasurements(event);

            if (poseInitialized) {
                constellation.calculateSatPosition(locationFromGoogleServices, pose);
                if (constellation.getUsedConstellationSize() != 0) {
                    pose = pvtMethod.calculatePose(constellation);
                    Log.i(TAG, "newPose: " + pose.getGeodeticLatitude() + ", " + pose.getGeodeticLongitude() + ", " + pose.getGeodeticHeight());
                    if (logToFile) {
                        fileLogger.addNewPose(pose, constellation);
                    }
                }
            }
        }
    }

    /**
     * Updates phone's location as calculated by the google services. On the first execution,
     * the location is assigned to initial location
     * @param location new location
     */
    public void updateLocationFromGoogleServices(Location location){

        locationFromGoogleServices = location;
        pvtMethod.logFineLocation(location);

        if (!poseInitialized && locationFromGoogleServices != null) {
            pose = Coordinates.globalGeodInstance(
                    locationFromGoogleServices.getLatitude(),
                    locationFromGoogleServices.getLongitude(),
                    locationFromGoogleServices.getAltitude());

            poseInitialized = true;
        }
    }

    /**
     *
     * @return data color
     */
    public int getDataColor() {
        return DATA_COLOR;
    }

    /**
     *
     * @return name of the module
     */
    public String getName() {return NAME; }

    @Override
    public String toString() {
        return NAME;
    }

    /**
     * Raised when passed settings are not complete
     */
    public static class CalculationSettingsIncompleteException extends Exception{
        public CalculationSettingsIncompleteException(String message) {
            super(message);
        }
    }

    /**
     *
     * @param name name of the constellation class
     * @return class indicated by the name parameter
     */
    private static Class<? extends Constellation> getConstellationClassFromName(String name){
        return Constellation.getClassByName(name);
    }

    /**
     *
     * @param names names of the correction classes
     * @return list of correction classes as indicated by the names parameter
     */
    private static List <Class<? extends Correction>> getCorrectionClassesFromNames(Set<String> names){
        List <Class<? extends Correction>> correctionClasses = new ArrayList<>();

        for(String correctionName : names){
            correctionClasses.add(Correction.getClassByName(correctionName));
        }

        return correctionClasses;
    }

    /**
     *
     * @param name name of the pvt method class
     * @return pvt method class as indicated by the name parameter
     */
    private static Class<? extends PvtMethod> getPvtMethodClassFromName(String name){
        return PvtMethod.getClassByName(name);
    }

    /**
     *
     * @param name name of the file logger class
     * @return file logger class as indicated by the name parameter
     */
    private static Class<? extends FileLogger> getFileLoggerClassFromName(String name){
        return FileLogger.getClassByName(name);
    }

    /**
     * Checks if the passed setting has been set. Throws exception if not
     * @param value vlue of the checked setting
     */
    private static boolean checkSettingsValid(String value){
        return value != null;
    }

    /**
     * Creates a new calculation modules based on String parameters
     * @param name new name of the module
     * @param constellationClassName name of the new constellation class
     * @param correctionClassNames names of the new correction classes
     * @param pvtMethodClassName name of the new pvt method class
     * @param fileLoggerClassName name of the new file logger class
     * @return new calculation module created based on parameters
     * @throws NameAlreadyRegisteredException when the name is already registered
     * @throws NumberOfSeriesExceededLimitException when the number of created modules is exceeded
     * @throws CalculationSettingsIncompleteException when not all of the settings have been
     *                          properly defined
     */
    public static CalculationModule createFromDescriptions(
            String name,
            String constellationClassName,
            Set<String> correctionClassNames,
            String pvtMethodClassName,
            String fileLoggerClassName) throws NameAlreadyRegisteredException, NumberOfSeriesExceededLimitException, CalculationSettingsIncompleteException {

        if(checkSettingsValid(name) &&
                checkSettingsValid(constellationClassName) &&
                checkSettingsValid(pvtMethodClassName) &&
                checkSettingsValid(fileLoggerClassName)) {

            return new CalculationModule(
                    name,
                    getConstellationClassFromName(constellationClassName),
                    getCorrectionClassesFromNames(correctionClassNames),
                    getPvtMethodClassFromName(pvtMethodClassName),
                    getFileLoggerClassFromName(fileLoggerClassName));
        } else {
            throw new CalculationSettingsIncompleteException("Settings not complete!");
        }

    }

    /**
     * Update the existing calculation module with new settings
     * @param name new name of the module
     * @param constellationClassName name of the new constellation class
     * @param correctionClassNames names of the new correction classes
     * @param pvtMethodClassName name of the new pvt method class
     * @param fileLoggerClassName name of the new file logger class
     * @throws NameAlreadyRegisteredException when the name is already registered
     * @throws NumberOfSeriesExceededLimitException when the number of created modules is exceeded
     */
    public void updateFromDescription(
            String name,
            String constellationClassName,
            Set<String> correctionClassNames,
            String pvtMethodClassName,
            String fileLoggerClassName) throws NameAlreadyRegisteredException, NumberOfSeriesExceededLimitException {

        updateSetup(
                name,
                getConstellationClassFromName(constellationClassName),
                getCorrectionClassesFromNames(correctionClassNames),
                getPvtMethodClassFromName(pvtMethodClassName),
                getFileLoggerClassFromName(fileLoggerClassName));
    }

    /**
     * Resets the static fields to default values
     */
    public static void clear(){
        registeredNames.clear();
        registeredIds.clear();
    }

    /**
     * @return an ArrayList which can be later used in fromConstructorArrayList to create a
     * calculation module
     */
    public ArrayList<String> getConstructorArrayList(){
        ArrayList<String> returnedValue = new ArrayList<>();

        returnedValue.add(getName());
        returnedValue.add(String.valueOf(active));
        returnedValue.add(String.valueOf(logToFile));
        returnedValue.add(constellation.getName());
        returnedValue.add(pvtMethod.getName());
        returnedValue.add(fileLogger.getName());

        for(Correction corr : corrections)
            returnedValue.add(corr.getName());

        return returnedValue;
    }

    private final static String CONSTRUCTOR_BUNDLE_NAME_TAG = "_name";
    private final static String CONSTRUCTOR_BUNDLE_ACTIVE_TAG = "_active";
    private final static String CONSTRUCTOR_BUNDLE_LOG_TO_FILE_TAG = "_log_to_file";
    private final static String CONSTRUCTOR_BUNDLE_CONSTELLATION_TAG = "_constellation";
    private final static String CONSTRUCTOR_BUNDLE_PVT_METHOD_TAG = "_pvt_method";
    private final static String CONSTRUCTOR_BUNDLE_FILE_LOGGER_TAG = "_file_logger";
    private final static String CONSTRUCTOR_BUNDLE_CORRECTIONS_TAG = "_corrections";
    private final static String CONSTRUCTOR_BUNDLE_SANITY_CHECK = "_sanity_check";

    /**
     * @return a Bundle which can be later used in fromConstructorBundle to create a
     * calculation module
     */
    public Bundle getConstructorBundle(){

        Bundle returnedBundle = new Bundle();
        ArrayList<String> correctionsList = new ArrayList<>();

        returnedBundle.putString(CONSTRUCTOR_BUNDLE_NAME_TAG, getName());
        returnedBundle.putBoolean(CONSTRUCTOR_BUNDLE_ACTIVE_TAG, active);
        returnedBundle.putBoolean(CONSTRUCTOR_BUNDLE_LOG_TO_FILE_TAG, logToFile);
        returnedBundle.putString(CONSTRUCTOR_BUNDLE_CONSTELLATION_TAG, constellation.getName());
        returnedBundle.putString(CONSTRUCTOR_BUNDLE_PVT_METHOD_TAG, pvtMethod.getName());
        returnedBundle.putString(CONSTRUCTOR_BUNDLE_FILE_LOGGER_TAG, fileLogger.getName());

        for(Correction corr : corrections)
            correctionsList.add(corr.getName());

        returnedBundle.putStringArrayList(CONSTRUCTOR_BUNDLE_CORRECTIONS_TAG, correctionsList);

        returnedBundle.putBoolean(CONSTRUCTOR_BUNDLE_SANITY_CHECK, true);

        return returnedBundle;
    }

    /**
     * @param constructorBundle Bundle generated by getConstructorArrayList
     * @return new calculation module based on the constructorBundle parameter
     * @throws NameAlreadyRegisteredException when the name is already registered
     * @throws NumberOfSeriesExceededLimitException when the number of created modules is exceeded
     */
    public static CalculationModule fromConstructorBundle(Bundle constructorBundle)
            throws NameAlreadyRegisteredException, NumberOfSeriesExceededLimitException {

        if(constructorBundle.getBoolean(CONSTRUCTOR_BUNDLE_SANITY_CHECK, false)) {

            String name = constructorBundle.getString(CONSTRUCTOR_BUNDLE_NAME_TAG);
            boolean valueOfActive = constructorBundle.getBoolean(CONSTRUCTOR_BUNDLE_ACTIVE_TAG);
            boolean valueOfLogToFile = constructorBundle.getBoolean(CONSTRUCTOR_BUNDLE_LOG_TO_FILE_TAG);
            String constellationClassName = constructorBundle.getString(CONSTRUCTOR_BUNDLE_CONSTELLATION_TAG);
            String pvtMethodClassName = constructorBundle.getString(CONSTRUCTOR_BUNDLE_PVT_METHOD_TAG);
            String fileLoggerClassName = constructorBundle.getString(CONSTRUCTOR_BUNDLE_FILE_LOGGER_TAG);
            Set<String> correctionClassNames = new ArraySet<>();
            correctionClassNames.addAll(Objects.requireNonNull(constructorBundle.getStringArrayList(CONSTRUCTOR_BUNDLE_CORRECTIONS_TAG)));

            CalculationModule createdModule = new CalculationModule(
                    name,
                    getConstellationClassFromName(constellationClassName),
                    getCorrectionClassesFromNames(correctionClassNames),
                    getPvtMethodClassFromName(pvtMethodClassName),
                    getFileLoggerClassFromName(fileLoggerClassName));

            createdModule.active = valueOfActive;
            createdModule.logToFile = valueOfLogToFile;

            return createdModule;
        } else {
            return null;
        }
    }

    /**
     * @param arrayList list generated by getConstructorArrayList
     * @return new calculation module based on the arrayList parameter
     * @throws NameAlreadyRegisteredException when the name is already registered
     * @throws NumberOfSeriesExceededLimitException when the number of created modules is exceeded
     */
    public static CalculationModule fromConstructorArrayList(ArrayList<String> arrayList) throws NameAlreadyRegisteredException, NumberOfSeriesExceededLimitException {
        String name = arrayList.get(0);
        boolean valueOfActive = Boolean.parseBoolean(arrayList.get(1));
        boolean valueOfLogToFile = Boolean.parseBoolean(arrayList.get(2));
        String constellationClassName = arrayList.get(3);
        String pvtMethodClassName = arrayList.get(4);
        String fileLoggerClassName = arrayList.get(5);
        Set<String> correctionClassNames = new ArraySet<>();

        for(int i=6; i<arrayList.size(); i++)
            correctionClassNames.add(arrayList.get(i));

        CalculationModule createdModule = new CalculationModule(
                name,
                getConstellationClassFromName(constellationClassName),
                getCorrectionClassesFromNames(correctionClassNames),
                getPvtMethodClassFromName(pvtMethodClassName),
                getFileLoggerClassFromName(fileLoggerClassName));

        createdModule.active = valueOfActive;
        createdModule.logToFile = valueOfLogToFile;

        return createdModule;
    }

    /**
     *
     * @return pvtMethod's calculated clock bias
     */
    public double getClockBias(){
        return pvtMethod.getClockBias();
    }
}
