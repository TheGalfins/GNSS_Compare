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

package com.galfins.gnss_compare.PvtMethods;

import org.ejml.data.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gnss_compare.FileLoggers.KalmanFilterFileLogger;
import com.galfins.gogpsextracts.Constants;
import com.galfins.gogpsextracts.Coordinates;

import android.location.Location;
import android.util.Log;

/**
 * This is a static extended Kalman filter to be used as a pedestrian.
 * The estimated variables are position and clock bias. Velocity is not considered.
 * The process noise matrix has some noise for the horizontal coordinates x and y,
 * to allow a faster adjustment to a position changing with walking speed compared
 * to the purely Static Extended Kalman Filter.
 */

public class PedestrianStaticExtendedKalmanFilter extends PvtMethod{

    /** initial guess for the clock bias in the state vector. Default: 0.0
     */
    private static final double INITIAL_CLOCK_BIAS = 0.0;
    /** initial sigma of the position in meters for the process noise matrix Q
     */
    private static final double INITIAL_SIGMAPOS = 1.;
    /** initial guess of sigma of the clock bias in meters for the process noise matrix Q
     */
    private static final double INITIAL_SIGMACLOCKBIAS = 500.;
    /** initial guess of sigma of the clock bias drift in meters.
     */
    private static final double INITIAL_SIGMACLOCKDRIFT = 50.;
    /** noise in the horizontal direction on the position to allow positioning at pedestrians
     * velocity. This value is already in units of meters squared.
     */
    private static final double xyNoise = 0.4;
    /** name of the pvt method as it appears in the selection menu for pvt methods
     */
    private static final String NAME = "Pedestrian EKF";


    // dimensions and indices for matrices.
    // these numbers should change if a dynamic Kalman filter is implemented.
    /** number of entries of the state vector. 5: x, y, z, clock bias and clock bias drift
     */
    private int numStates = 5;
    /** index of x coordinate of the state vector
     */
    private int idxX = 0;
    /** index of y coordinate of the state vector
     */
    private int idxY = 1;
    /** index of z coordinate of the state vector
     */
    private int idxZ = 2;
    /** index of the clock bias of the state vector
     */
    private int idxClockBias = 3;
    /** index of clock drift of the state vector
     */
    private int idxClockDrift = 4;
    /** Kalman filter parameters file logger
     */
    private KalmanFilterFileLogger kalmanParamLogger = new KalmanFilterFileLogger();

    /** vector for the predicted state
     */
    SimpleMatrix x_pred = new SimpleMatrix(numStates,1);
    /** state transition matrix F
     */
    SimpleMatrix F = SimpleMatrix.identity(numStates);
    /** process noise matrix Q
     */
    SimpleMatrix Q = new SimpleMatrix(numStates,numStates);
    /** variance-covariance matrix of the state vector
     */
    SimpleMatrix P_pred = new SimpleMatrix(numStates,numStates);

    /** Allan variance coefficient h_{-2} in meters. TCXO low quality
     */
    private static final double h_2 = 2.0e-20 * Math.pow(Constants.SPEED_OF_LIGHT,2);
    /** Allan variance coefficient h_0 in meters. TCXO low quality
     */
    private static final double h_0 = 2.0e-19 * Math.pow(Constants.SPEED_OF_LIGHT,2);
    /** receiver clock phase error
     */
    private static final double S_g = 2.0 * Math.pow(Constants.PI_ORBIT,2) * h_2;
    /** receiver clock frequency error
     */
    private double S_f = h_0 / 2.0;

    /** time between measurements in seconds
     */
    final double DELTA_T = 1.0;

    // Define the parameters for the elevation dependent weighting method [Jaume Subirana et al. GNSS Data Processing: Fundamentals and Algorithms]
    private double a = 0.13;
    private double b = 0.53;
    private double elev;

    /** measurement vector with numStates entries
     */
    SimpleMatrix x_meas = new SimpleMatrix(numStates,1);
    /** variance-covariance matrix of the measurement vector
     */
    SimpleMatrix P_meas = new SimpleMatrix(numStates,numStates);
    /** flag to control the initialization of the variables if not initialiyed yet.
     */
    private boolean firstExecution = true;


    /**
     * static extended Kalman filter with noise on the horizontal part of the process noise matrix
     */
    public PedestrianStaticExtendedKalmanFilter(){

        // Initialization of the state transition matrix
        F.set(idxClockBias, idxClockDrift, DELTA_T);

        // Initialization of the process noise matrix
        initQ();

    }

    @Override
    public void startLog(String name){
        kalmanParamLogger.setName(name);
        kalmanParamLogger.startNewLog();
    }
    @Override
    public void stopLog() {
        kalmanParamLogger.closeLog();
    }
    @Override
    public void logError(double latError, double lonError) {
        if (kalmanParamLogger.isStarted()) {
            kalmanParamLogger.logError(latError, lonError);
        }
    }
    @Override
    public void logFineLocation(Location fineLocation){
        if (kalmanParamLogger.isStarted()) {
            kalmanParamLogger.logFineLocation(fineLocation);
        }
    }

    /**
     *
     * @param constellation satellite constellation object for which the calculations are to
     *                      be performed
     * @return Coordinates object of the computed position
     */
    @Override
    public Coordinates calculatePose(Constellation constellation) {

        /** number of satellites in constellation
         */
        final int CONSTELLATION_SIZE = constellation.getUsedConstellationSize();

        /** innovation sequence vector
         */
        SimpleMatrix gamma;

        // Initialize a variable to hold the predicted (geometric) distance towards each satellite
        /** geometric distance in meters to every satellite. internal variable.
         */
        double distPred = 0.; // geometric Distance
        /** counter for the satellites used in the position computation
         */
        int usedInCalculations = 0;

        /**
         * approximate position of the receiver in ECEF.
         */
        SimpleMatrix rxPosSimpleVector = Constellation.getRxPosAsVector(constellation.getRxPos());

        if(firstExecution){
            // Initialize the state vector
            x_pred.set(idxX, rxPosSimpleVector.get(0));
            x_pred.set(idxY, rxPosSimpleVector.get(1));
            x_pred.set(idxZ, rxPosSimpleVector.get(2));
            x_pred.set(idxClockBias, INITIAL_CLOCK_BIAS);
            x_pred.set(idxClockDrift, 0.0); // clock bias drift

            // Initialize the VCM of the state vector
            P_pred.set(idxX, idxX, Math.pow(INITIAL_SIGMAPOS, 2));
            P_pred.set(idxY, idxY, Math.pow(INITIAL_SIGMAPOS, 2));
            P_pred.set(idxZ, idxZ, Math.pow(INITIAL_SIGMAPOS, 2));
            P_pred.set(idxClockBias, idxClockBias, Math.pow(INITIAL_SIGMACLOCKBIAS, 2));
            P_pred.set(idxClockDrift, idxClockDrift, Math.pow(INITIAL_SIGMACLOCKDRIFT, 2));

            x_pred.set(F.mult(x_pred));
            P_pred = F.mult(P_pred.mult(F.transpose())).plus(Q);

        } else {

            // Perform time-prediction of the state vector and its VCM
            x_pred.set(F.mult(x_meas));
            P_pred = F.mult(P_meas.mult(F.transpose())).plus(Q);
        }

        /** Kalman gain matrix K
         */
        SimpleMatrix K;
        /** Innovation covariance
         */
        SimpleMatrix S;

        // Initialize the variables related to the measurement model
        /** Observation Matrix H
         */
        SimpleMatrix H = new SimpleMatrix(CONSTELLATION_SIZE, numStates);
        /** pseudorange vector, one entry for every used satellite
         */
        SimpleMatrix prVect = new SimpleMatrix(CONSTELLATION_SIZE,1);
        /** predicted pseudoranges vector, one entry for every used satellite
         */
        SimpleMatrix measPred = new SimpleMatrix(CONSTELLATION_SIZE,1);
        /** variance-covariance matrix of the measurements R
         */
        SimpleMatrix R = SimpleMatrix.identity(CONSTELLATION_SIZE);
//        R = R.divide(1.0/100.0);

        // meas variance of each satellite
//        SimpleMatrix sigma2C1 = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        double sigma2Meas = Math.pow(5,2);




        // Form the observation matrix H
        for(int k = 0; k < CONSTELLATION_SIZE; k++){
            if(constellation.getSatellite(k).getSatellitePosition() == null)
                continue;

            // Get the raw pseudoranges for each satellite
            prVect.set(k, constellation.getSatellite(k).getPseudorange());



            // Compute the predicted (geometric) distance towards each satellite
            distPred = Math.sqrt(
                    Math.pow( constellation.getSatellite(k).getSatellitePosition().getX()
                            - x_pred.get(idxX), 2 )
                            + Math.pow( constellation.getSatellite(k).getSatellitePosition().getY()
                            - x_pred.get(idxY), 2 )
                            + Math.pow( constellation.getSatellite(k).getSatellitePosition().getZ()
                            - x_pred.get(idxZ), 2 )
            );

            // Set the values inside the H matrix
            H.set(k, idxX,  (x_pred.get(idxX) - constellation.getSatellite(k).getSatellitePosition().getX()) / distPred);
            H.set(k, idxY,  (x_pred.get(idxY) - constellation.getSatellite(k).getSatellitePosition().getY()) / distPred);
            H.set(k, idxZ,  (x_pred.get(idxZ) - constellation.getSatellite(k).getSatellitePosition().getZ()) / distPred);
            H.set(k, idxClockBias, 1.0);

            // Form the predicted measurement towards each satellite
            measPred.set(k, distPred + x_pred.get(idxClockBias)
                    - constellation.getSatellite(k).getClockBias()
                    + constellation.getSatellite(k).getAccumulatedCorrection());

            // Form the VCM of the measurements (R)
            elev = constellation.getSatellite(k).getRxTopo().getElevation() * (Math.PI / 180.0);
            R.set(k,k,sigma2Meas * Math.pow(a + b * Math.exp(-elev/10.0),2));

            usedInCalculations ++;
        }



        if(usedInCalculations > 0) {
            // Compute the Kalman Gain
            try {
                K = P_pred.mult(H.transpose().mult((H.mult(P_pred.mult(H.transpose())).plus(R)).invert()));
                S = H.mult(P_pred.mult(H.transpose())).plus(R);
            } catch (SingularMatrixException e) {
                Log.e(NAME, new String(" Matrix inversion failed"), e);
                return Coordinates.globalXYZInstance(
                    rxPosSimpleVector.get(0),
                    rxPosSimpleVector.get(1),
                    rxPosSimpleVector.get(2));
            }
            // Compute the Kalman innovation sequence
            gamma = prVect.minus(measPred);

            // Perform the measurement update
            x_meas = x_pred.plus(K.mult(gamma));
            P_meas = (SimpleMatrix.identity(numStates).minus((K.mult(H)))).mult(P_pred);

            // x_meas and P_meas are being used for the next set of measurements
            if (kalmanParamLogger.isStarted())
                kalmanParamLogger.logKalmanParam(x_meas, P_meas, numStates, gamma, S, CONSTELLATION_SIZE, constellation);

            firstExecution = false;

            return Coordinates.globalXYZInstance(
                    x_meas.get(idxX),
                    x_meas.get(idxY),
                    x_meas.get(idxZ));
        } else { // return approximate coordinates if there are no usable satellite available
            return Coordinates.globalXYZInstance(
                    rxPosSimpleVector.get(0),
                    rxPosSimpleVector.get(1),
                    rxPosSimpleVector.get(2));
        }
    }

    /**
     * initialize process noise matrix Q using receiver clock frequency and phase errors, DELTA_T
     */
    private void initQ(){
        // Tuning of the process noise matrix (Q)
        Q.set(idxClockBias, idxClockBias, S_f + S_g * Math.pow(DELTA_T,3) / 3.0);
        Q.set(idxClockBias, idxClockDrift, S_g * Math.pow(DELTA_T,2) / 2.0);
        Q.set(idxClockDrift, idxClockBias, S_g * Math.pow(DELTA_T,2) / 2.0);
        Q.set(idxClockDrift, idxClockDrift, S_g * DELTA_T);

        Q.set(idxX, idxX, xyNoise);
        Q.set(idxY, idxY, xyNoise);
    }

    /**
     * get the name of the pvt class as it appears in the pvt method selection menu
     * @return NAME String with the name of the pvt class
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * get the clock bias from the measured state vector
     * @return receiver clock bias
     */
    @Override
    public double getClockBias() {
        return x_meas.get(idxClockBias);
    }

    public static void registerClass(){
        register(NAME, PedestrianStaticExtendedKalmanFilter.class);
    }

}
