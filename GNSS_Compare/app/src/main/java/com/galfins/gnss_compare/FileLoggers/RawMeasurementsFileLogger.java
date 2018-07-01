package com.galfins.gnss_compare.FileLoggers;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gogpsextracts.Coordinates;

/**
 * Created by domin on 4/4/2018.
 */

public class RawMeasurementsFileLogger extends FileLogger{

    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final String VERSION_TAG = "Version: ";
    private static final String NAME = "RawMeasurements";

    public RawMeasurementsFileLogger(String calculationName) {
        TAG = "RawMeasurementsFileLogger";
        filePrefix = calculationName;
        initialLine = createInitialLine();
    }

    private String createInitialLine() {
        StringBuilder initialLine = new StringBuilder();

        initialLine.append(COMMENT_START);
        initialLine.append('\n');
        initialLine.append(COMMENT_START);
        initialLine.append("Header Description:");
        initialLine.append('\n');
        initialLine.append(COMMENT_START);
        initialLine.append('\n');
        initialLine.append(COMMENT_START);
        initialLine.append(VERSION_TAG);
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String fileVersion =
                        " Platform: "
                        + Build.VERSION.RELEASE
                        + " "
                        + "Manufacturer: "
                        + manufacturer
                        + " "
                        + "Model: "
                        + model;
        initialLine.append(fileVersion);
        initialLine.append('\n');
        initialLine.append(COMMENT_START);
        initialLine.append('\n');
        initialLine.append(COMMENT_START);
        initialLine.append(
                "Raw,ElapsedRealtimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,"
                        + "BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,"
                        + "HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,"
                        + "ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,"
                        + "PseudorangeRateUncertaintyMetersPerSecond,"
                        + "AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,"
                        + "AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,"
                        + "CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,"
                        + "ConstellationType,AgcDb,CarrierFrequencyHz");
        initialLine.append('\n');
        initialLine.append(COMMENT_START);
        initialLine.append('\n');

        return initialLine.toString();
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
                            "Fix,%s,%f,%f,%f,%f,%f,%d",
                            constellation.getConstellationId(),
                            pose.getGeodeticLatitude(),
                            pose.getGeodeticLongitude(),
                            pose.getGeodeticHeight(),
                            0.0, //speed
                            0.0, //accuracy
                            pose.getRefTime());
            try {
                mFileWriter.write(locationStream);
                mFileWriter.newLine();
            } catch (IOException e) {
                Log.e(TAG, ERROR_WRITING_FILE, e);
            }
        }
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            GnssClock gnssClock = event.getClock();
            for (GnssMeasurement measurement : event.getMeasurements()) {
                try {
                    writeGnssMeasurementToFile(gnssClock, measurement);
                } catch (IOException e) {
                    Log.e(TAG, ERROR_WRITING_FILE, e);
                }
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurement measurement)
            throws IOException {
        String clockStream =
                String.format(
                        "Raw,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        SystemClock.elapsedRealtime(),
                        clock.getTimeNanos(),
                        clock.hasLeapSecond() ? clock.getLeapSecond() : "",
                        clock.hasTimeUncertaintyNanos() ? clock.getTimeUncertaintyNanos() : "",
                        clock.getFullBiasNanos(),
                        clock.hasBiasNanos() ? clock.getBiasNanos() : "",
                        clock.hasBiasUncertaintyNanos() ? clock.getBiasUncertaintyNanos() : "",
                        clock.hasDriftNanosPerSecond() ? clock.getDriftNanosPerSecond() : "",
                        clock.hasDriftUncertaintyNanosPerSecond()
                                ? clock.getDriftUncertaintyNanosPerSecond()
                                : "",
                        clock.getHardwareClockDiscontinuityCount() + ",");
        mFileWriter.write(clockStream);

        String measurementStream =
                String.format(
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        measurement.getSvid(),
                        measurement.getTimeOffsetNanos(),
                        measurement.getState(),
                        measurement.getReceivedSvTimeNanos(),
                        measurement.getReceivedSvTimeUncertaintyNanos(),
                        measurement.getCn0DbHz(),
                        measurement.getPseudorangeRateMetersPerSecond(),
                        measurement.getPseudorangeRateUncertaintyMetersPerSecond(),
                        measurement.getAccumulatedDeltaRangeState(),
                        measurement.getAccumulatedDeltaRangeMeters(),
                        measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
                        measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "",
                        measurement.hasCarrierCycles() ? measurement.getCarrierCycles() : "",
                        measurement.hasCarrierPhase() ? measurement.getCarrierPhase() : "",
                        measurement.hasCarrierPhaseUncertainty()
                                ? measurement.getCarrierPhaseUncertainty()
                                : "",
                        measurement.getMultipathIndicator(),
                        measurement.hasSnrInDb() ? measurement.getSnrInDb() : "",
                        measurement.getConstellationType(),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                && measurement.hasAutomaticGainControlLevelDb()
                                ? measurement.getAutomaticGainControlLevelDb()
                                : "",
                        measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "");
        mFileWriter.write(measurementStream);
        mFileWriter.newLine();
    }


    public static void registerClass(){
        register(NAME, RawMeasurementsFileLogger.class);
    }

}