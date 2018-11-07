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

package com.galfins.gnss_compare.FileLoggers;

import android.location.Location;
import android.util.Log;
import org.ejml.simple.SimpleMatrix;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gogpsextracts.Coordinates;

import java.io.IOException;
import java.util.Locale;

public class KalmanFilterFileLogger extends FileLogger {
    private static final String NAME = "EKF";

    public KalmanFilterFileLogger() {
        TAG = "KalmanFilterFileLogger";
        filePrefix = "";
        initialLine = "%% E, Latitude error, Longitude error" +
                "\n%% x, x_meas[0], ..., x_meas[numStates] " +
                "\n%% P, P_meas[0,0], ..., P_meas[numStates,numStates] " +
                "\n%% I, gamma[0], ..., gamma[constellationSize] " +
                "\n%% S, S[0,0], ..., S[constellationSize,constellationSize] " +
                "\n%% id, satID[0], ..., satID[constellationSize]" +
                "\n%% PR, pseudoranges[0], ..., pseudoranges[constellationSize]" +
                "\n%% Fl, fineLocation.Latitude, fineLocation.Longitude, fineLocation.Altitude\n\n";
    }

    public void logError(double latError, double lonError){
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }

            String locationStream =
                    String.format(Locale.ENGLISH,
                            "E, %f, %f",
                            latError,
                            lonError);
            try {
                mFileWriter.write(locationStream);
                mFileWriter.newLine();
            } catch (IOException e) {
                Log.e(TAG, ERROR_WRITING_FILE, e);
            }
        }
    }

    public void addNewPose(Coordinates pose, Constellation constellation) {}

    public void logKalmanParam(SimpleMatrix x_meas, SimpleMatrix P_meas, int numStates, SimpleMatrix gamma, SimpleMatrix S, int constellationSize, Constellation constellation) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }

            StringBuilder buildStream = new StringBuilder();

            buildStream.append("x");
            for (int i = 0; i < numStates; i++) {
                buildStream.append("," + String.valueOf(x_meas.get(i)));
            }
            buildStream.append("\n");

            buildStream.append("P,");
            for (int i = 0; i < numStates; i++) {
                buildStream.append("," + String.valueOf(P_meas.get(i, i)));
            }
            buildStream.append("\n");

            buildStream.append("I,");
            for (int i = 0; i < constellationSize; i++) {
                buildStream.append("," + String.valueOf(gamma.get(i)));
            }
            buildStream.append("\n");

            buildStream.append("S,");
            for (int i = 0; i < constellationSize; i++) {
                buildStream.append("," + String.valueOf(S.get(i, i)));
            }
            buildStream.append("\n");

            buildStream.append("id,");
            for (int i = 0; i < constellationSize; i++) {
                buildStream.append("," + constellation.getSatellites().get(i).getUniqueSatId());
            }
            buildStream.append("\n");

            buildStream.append("PR,");
            for (int i = 0; i < constellationSize; i++) {
                buildStream.append("," + constellation.getSatellites().get(i).getPseudorange());
            }

            try {
                mFileWriter.write(buildStream.toString());
                mFileWriter.newLine();
            } catch (IOException e) {
                Log.e(TAG, ERROR_WRITING_FILE, e);
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void logFineLocation(Location fineLocation) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }

            String locationStream =
                    String.format(Locale.ENGLISH,
                            "FL, %f, %f, %f",
                            fineLocation.getLatitude(),
                            fineLocation.getLongitude(),
                            fineLocation.getAltitude());
            try {
                mFileWriter.write(locationStream);
                mFileWriter.newLine();
            } catch (IOException e) {
                Log.e(TAG, ERROR_WRITING_FILE, e);
            }
        }
    }
}