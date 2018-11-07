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

import android.util.Log;

import org.ejml.data.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.TopocentricCoordinates;

/**
 * Created by Mateusz Krainski on 17/02/2018.
 * This class is for...
 */

public class WeightedLeastSquares extends PvtMethod {

    private final static String NAME = "Weighted Least Squares";

    private final int NUMBER_OF_ITERATIONS = 10;
    private final static String TAG="WLS";

    private final static Coordinates ZERO_POSE = Coordinates.globalGeodInstance(51.000, 3.000, 0.000); // Right at the edge of the plot
    private double clockBias; // =0.0;
    //private final static Coordinates ZERO_POSE = Coordinates.globalGeodInstance(47.642795, 23.622892, 350.0);   // Romania

    // Define the parameters for the elevation dependent weighting method [Jaume Subirana et al. GNSS Data Processing: Fundamentals and Algorithms]
    private double a = 0.13;
    private double b = 0.53;
    private double sigma2Meas = Math.pow(5,2);


    public WeightedLeastSquares(){
        super();
    }

    @Override
    public Coordinates calculatePose(Constellation constellation) {

        final int CONSTELLATION_SIZE = constellation.getUsedConstellationSize();

        // Initialize matrices for data storage

        SimpleMatrix rxPosSimpleVector = Constellation.getRxPosAsVector(constellation.getRxPos());
        SimpleMatrix satPosMat = new SimpleMatrix(CONSTELLATION_SIZE, 3);
        SimpleMatrix tropoCorr = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        SimpleMatrix svClkBias = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        SimpleMatrix ionoCorr = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        SimpleMatrix shapiroCorr = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        SimpleMatrix sigma2 = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        SimpleMatrix prVect = new SimpleMatrix(CONSTELLATION_SIZE, 1);
        SimpleMatrix vcvMeasurement;
        SimpleMatrix PDOP = new SimpleMatrix(1,1);

        double elevation, measVar,  measVarC1;
        int CN0;

        TopocentricCoordinates topo = new TopocentricCoordinates();
        Coordinates origin; // = new Coordinates(){};
        Coordinates target; // = new Coordinates(){};

        ///////////////////////////// SV coordinates/velocities + PR corrections computation ////////////////////////////////////////////////////

        try {

            for (int ii = 0; ii < CONSTELLATION_SIZE; ii++) {

                // Set the measurements into a vector
                prVect.set(ii, constellation.getSatellite(ii).getPseudorange());

                // Compute the satellite coordinates
                svClkBias.set(ii, constellation.getSatellite(ii).getClockBias());

                ///////////////////////////// PR corrections computations ////////////////////////////////////////////////////


                // Assign the computed SV coordinates into a matrix
                satPosMat.set(ii, 0, constellation.getSatellite(ii).getSatellitePosition().getX());
                satPosMat.set(ii, 1, constellation.getSatellite(ii).getSatellitePosition().getY());
                satPosMat.set(ii, 2, constellation.getSatellite(ii).getSatellitePosition().getZ());


                // Compute the elevation and azimuth angles for each satellite
                origin = Coordinates.globalXYZInstance(rxPosSimpleVector.get(0), rxPosSimpleVector.get(1), rxPosSimpleVector.get(2));
                target = Coordinates.globalXYZInstance(satPosMat.get(ii, 0),satPosMat.get(ii, 1),satPosMat.get(ii, 2) );
//                origin.setXYZ(rxPosSimpleVector.get(0), rxPosSimpleVector.get(1),rxPosSimpleVector.get(2));
//                target.setXYZ(satPosMat.get(ii, 0),satPosMat.get(ii, 1),satPosMat.get(ii, 2) );
                topo.computeTopocentric(origin, target);
                elevation = topo.getElevation() * (Math.PI / 180.0);

                // Set the variance of the measurement for each satellite
                measVar = sigma2Meas * Math.pow(a + b * Math.exp(-elevation/10.0),2);
                sigma2.set(ii, measVar);

                ///////////////////////////// Printing results  ////////////////////////////////////////////////////

                // Print the computed satellite coordinates \ velocities
                /*
                System.out.println();
                System.out.println();
                System.out.println("The GPS ECEF coordinates of " + "SV" + constellation.getSatellite(ii).getSatId() + " are:");
                System.out.printf("%.4f", satPosMat.get(ii, 0));
                System.out.println();
                System.out.printf("%.4f", satPosMat.get(ii, 1));
                System.out.println();
                System.out.printf("%.4f", satPosMat.get(ii, 2));
                System.out.println();
                System.out.printf("Tropo corr: %.4f", tropoCorr.get(ii));
                System.out.println();
                System.out.printf("Iono corr: %.4f", ionoCorr.get(ii));
                System.out.println();
                */
                System.out.printf("Shapiro corr: %.4f", shapiroCorr.get(ii));

            }
        } catch(NullPointerException | IndexOutOfBoundsException e){
            e.printStackTrace();

            if(e.getClass() == IndexOutOfBoundsException.class){
                Log.e(TAG, "calculatePose: Satellites cleared before calculating result!");
            }

            constellation.setRxPos(ZERO_POSE); // Right at the edge of the plot
            rxPosSimpleVector = Constellation.getRxPosAsVector(constellation.getRxPos());
            return Coordinates.globalXYZInstance(rxPosSimpleVector.get(0), rxPosSimpleVector.get(1), rxPosSimpleVector.get(2));
        }

		/*
		 * [WEIGHTED LEAST SQUARES] Determination of the position + clock bias
		 */

        try{
            // VCV matrix of the pseudorange measurements
            vcvMeasurement = sigma2.diag();

            SimpleMatrix W = vcvMeasurement.invert();

            // Initialization of the required matrices and vectors
            SimpleMatrix xHat = new SimpleMatrix(4, 1); 			 // vector holding the WLS estimates
            SimpleMatrix z = new SimpleMatrix(CONSTELLATION_SIZE, 1);        // observation vector
            SimpleMatrix H = new SimpleMatrix(CONSTELLATION_SIZE, 4);        // observation matrix
            SimpleMatrix distPred = new SimpleMatrix(CONSTELLATION_SIZE, 1); // predicted distances
            SimpleMatrix measPred = new SimpleMatrix(CONSTELLATION_SIZE, 1); // predicted measurements


            // Start the estimation (10 loops)
            for (int iter = 0; iter < NUMBER_OF_ITERATIONS; iter++){

                // Calculation of the components within the observation matrix (H)
                for (int k = 0; k < CONSTELLATION_SIZE; k++ ){

                    // Computation of the geometric distance
                    distPred.set(k, Math.sqrt(
                            Math.pow( satPosMat.get(k, 0) - rxPosSimpleVector.get(0), 2 )
                            + Math.pow( satPosMat.get(k, 1) - rxPosSimpleVector.get(1), 2 )
                            + Math.pow( satPosMat.get(k, 2) - rxPosSimpleVector.get(2), 2 )
                    ));


                    // Measurement prediction
                    measPred.set( k, distPred.get(k)
                                    + constellation.getSatellite(k).getAccumulatedCorrection() - svClkBias.get(k) );

                    // Compute the observation matrix (H)
                    H.set(k, 0, (constellation.getRxPos().getX() - satPosMat.get(k, 0)) / distPred.get(k));
                    H.set(k, 1, (constellation.getRxPos().getY() - satPosMat.get(k, 1)) / distPred.get(k));
                    H.set(k, 2, (constellation.getRxPos().getZ() - satPosMat.get(k, 2)) / distPred.get(k));
                    H.set(k, 3, 1.0);

                }

                // Compute the prefit vector (z)
                z.set(prVect.minus(measPred));

                // Estimate the unknowns (dxHat)
                SimpleMatrix Cov = H.transpose().mult(W).mult(H);
                SimpleMatrix CovDOP = H.transpose().mult(H);
                SimpleMatrix dopMatrix = CovDOP.invert();

                xHat.set(Cov.invert().mult(H.transpose()).mult(W).mult(z));
                PDOP.set(Math.sqrt(dopMatrix.get(0,0) + dopMatrix.get(1,1)));

                // Update the receiver position
                // rxPosSimpleVector.set(rxPosSimpleVector.plus(xHat));
                rxPosSimpleVector.set(0, rxPosSimpleVector.get(0)+ xHat.get(0));
                rxPosSimpleVector.set(1, rxPosSimpleVector.get(1)+ xHat.get(1));
                rxPosSimpleVector.set(2, rxPosSimpleVector.get(2)+ xHat.get(2));
                rxPosSimpleVector.set(3, xHat.get(3));

            }

            clockBias = rxPosSimpleVector.get(3);

        } catch (SingularMatrixException | IndexOutOfBoundsException e) {
            if(e.getClass() == IndexOutOfBoundsException.class){
                Log.e(TAG, "calculatePose: Satellites cleared before calculating result!");
            } else if (e.getClass() == SingularMatrixException.class) {
                Log.e(TAG, "calculatePose: SingularMatrixException caught!");
            }
            constellation.setRxPos(ZERO_POSE); // Right at the edge of the plot
            rxPosSimpleVector = Constellation.getRxPosAsVector(constellation.getRxPos());
            e.printStackTrace();
        }
        System.out.println();

        // Print the estimated receiver position
//        rxPosSimpleVector.print();

        Log.d(TAG, "calculatePose: rxPosSimpleVector (ECEF): " + rxPosSimpleVector.get(0) + ", " + rxPosSimpleVector.get(1) + ", " + rxPosSimpleVector.get(2) + ";");

        Coordinates pose = Coordinates.globalXYZInstance(rxPosSimpleVector.get(0), rxPosSimpleVector.get(1), rxPosSimpleVector.get(2));
        Log.d(TAG, "calculatePose: pose (ECEF): " + pose.getX() + ", " + pose.getY() + ", " + pose.getZ() + ";");
        Log.d(TAG, "calculatePose: pose (lat-lon): " + pose.getGeodeticLatitude() + ", " + pose.getGeodeticLongitude() + ", " + pose.getGeodeticHeight() + ";");
        Log.d(TAG, "calculated PDOP:" + PDOP.get(0) + ";");

        return pose;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public double getClockBias() {
        return clockBias;
    }

    public static void registerClass(){
        register(NAME, WeightedLeastSquares.class);
    }
}
