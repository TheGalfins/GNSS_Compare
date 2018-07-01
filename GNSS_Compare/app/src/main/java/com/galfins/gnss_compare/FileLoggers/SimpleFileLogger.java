package com.galfins.gnss_compare.FileLoggers;

import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gogpsextracts.Coordinates;

/**
 * Created by domin on 4/4/2018.
 */

public class SimpleFileLogger extends FileLogger{

    private static final String NAME = "SimpleFormat";

    public SimpleFileLogger() {
        TAG = "SimpleFileLogger";
        filePrefix = "";
        initialLine = "Timestamp UTC; X coordinate; Y coordinate; Z coordinate; Latitude; Longitude; Height";
    }

    public SimpleFileLogger(String calculationName) {
        TAG = "SimpleFileLogger";
        filePrefix = calculationName;
        initialLine = "Timestamp UTC; X coordinate; Y coordinate; Z coordinate; Latitude; Longitude; Height";
    }

    /**
     * Add new pose to the file
     */
    public void addNewPose(Coordinates pose, Constellation constellation) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }

            String locationStream =
                    String.format(Locale.ENGLISH,
                            "%s; %f; %f; %f; %f; %f; %f",
                            (constellation.getTime() == null) ? "" : constellation.getTime().toLogString(),
                            pose.getX(),
                            pose.getY(),
                            pose.getZ(),
                            pose.getGeodeticLatitude(),
                            pose.getGeodeticLongitude(),
                            pose.getGeodeticHeight());
            try {
                mFileWriter.write(locationStream);
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

    public static void registerClass(){
        register(NAME, SimpleFileLogger.class);
    }

}