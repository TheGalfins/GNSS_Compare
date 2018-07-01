package com.galfins.gnss_compare.FileLoggers;

import android.location.GnssStatus;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import com.galfins.gnss_compare.Constellations.Constellation;
import com.galfins.gogpsextracts.Coordinates;

/**
 * Created by domin on 4/15/2018.
 */

public class NmeaFileLogger extends FileLogger {

    private static final String NAME = "NMEA";

    public NmeaFileLogger() {
        TAG = "NmeaFileLogger";
        filePrefix = "";
        initialLine = "$<TalkerID>GGA,<Timestamp>,<Lat>,<N/S>,<Long>,<E/W>,<GPSQual>,<Sats>,<HDOP>,<Alt>,<AltVal>,<GeoSep>,<GeoVal>,<DGPSAge>,<DGPSRef>,<checksum>";
    }

    public NmeaFileLogger(String calculationName) {
        TAG = "NmeaFileLogger";
        filePrefix = calculationName;
        initialLine = "$<TalkerID>GGA,<Timestamp>,<Lat>,<N/S>,<Long>,<E/W>,<GPSQual>,<Sats>,<HDOP>,<Alt>,<AltVal>,<GeoSep>,<GeoVal>,<DGPSAge>,<DGPSRef>,<checksum>";
    }

    /**
     * Add new pose to the file
     */
    @Override
    public void addNewPose(Coordinates pose, Constellation constellation) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            String TalkerID;
            switch (constellation.getConstellationId()) {
                case GnssStatus.CONSTELLATION_GPS:
                    TalkerID = new String("GP");
                    break;
                case GnssStatus.CONSTELLATION_SBAS:
                    TalkerID = new String("SB");
                    break;
                case GnssStatus.CONSTELLATION_GLONASS:
                    TalkerID = new String("GL");
                    break;
                case GnssStatus.CONSTELLATION_QZSS:
                    TalkerID = new String("QZ");
                    break;
                case GnssStatus.CONSTELLATION_BEIDOU:
                    TalkerID = new String("BD");
                    break;
                case GnssStatus.CONSTELLATION_GALILEO:
                    TalkerID = new String("GA");
                    break;
                case Constellation.CONSTELLATION_GALILEO_GPS:
                    TalkerID = new String("GN");
                    break;
                default:
                    TalkerID = new String("");
            }

            char NS = 'N';
            char EW = 'E';
            if (pose.getGeodeticLatitude() < 0)
                NS = 'S';
            if (pose.getGeodeticLongitude() < 0)
                EW = 'W';
            int GPSQual = 0; // use format %1d
            int HDOP = 0; // use format %3d
            int GeoSep = 0; // use format %4d
            int Checksum = 0; // use format %02x
            String locationStream =
                    String.format(Locale.ENGLISH,
                            "$%sGGA,%s,%04.5f,%c,%04.5f,%c,,%02d,,%s,%c,,%c,,*%s",
                            TalkerID,
                            (constellation.getTime() == null) ? "" : constellation.getTime().toLogString(),
                            pose.getGeodeticLatitude() * 100,
                            NS,
                            pose.getGeodeticLongitude() * 100,
                            EW,
                            constellation.getUsedConstellationSize(),
                            ((int) pose.getGeodeticHeight() < 100000) ? String.format ("%05d", (int) pose.getGeodeticHeight()) : "100000",
                            'M',
                            'M', "");
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
        register(NAME, NmeaFileLogger.class);
    }

}