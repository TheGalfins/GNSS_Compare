package com.galfins.gnss_compare.Constellations;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gogpsextracts.Constants;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.RinexNavigationGps;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.TopocentricCoordinates;

/**
 * Created by Mateusz Krainski on 17/02/2018.
 * This class is for...
 * <p>
 * GPS Pseudorange computation algorithm by: Mareike Burba
 * - variable name changes and comments were added
 * to fit the description in the GSA white paper
 * by: Sebastian Ciuban
 */

public class GpsConstellation extends Constellation {

    private final static char satType = 'G';
    private static final String NAME = "GPS";
    private static final String TAG = "GpsConstellation";

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    private Coordinates rxPos;
    private double tRxGPS;
    private double weekNumberNanos;

    private static final int constellationId = GnssStatus.CONSTELLATION_GPS;

    /**
     * Time of the measurement
     */
    private Time timeRefMsec;

    private int visibleButNotUsed = 0;

    // Condition for the pseudoranges that takes into account a maximum uncertainty for the TOW
    // (as done in gps-measurement-tools MATLAB code)
    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]

    private NavigationProducer rinexNavGps = null;

    /**
     * List holding observed satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();

    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections;

    public GpsConstellation() {
        // URL template from where the GPS ephemerides should be downloaded
        String IGN_NAVIGATION_HOURLY_ZIM2 = "ftp://igs.ensg.ign.fr/pub/igs/data/hourly/${yyyy}/${ddd}/zim2${ddd}${h}.${yy}n.Z";
        String NASA_NAVIGATION_HOURLY = "ftp://cddis.gsfc.nasa.gov/pub/gps/data/hourly/${yyyy}/${ddd}/hour${ddd}0.${yy}n.Z";
        String GARNER_NAVIGATION_AUTO_HTTP = "http://garner.ucsd.edu/pub/rinex/${yyyy}/${ddd}/auto${ddd}0.${yy}n.Z";
        String BKG_HOURLY_SUPER_SEVER = "ftp://igs.bkg.bund.de/IGS/BRDC/${yyyy}/${ddd}/brdc${ddd}0.${yy}n.Z";

        // Declare a RinexNavigation type object
        if(rinexNavGps == null)
            rinexNavGps = new RinexNavigationGps(BKG_HOURLY_SUPER_SEVER);
    }

    @Override
    public void addCorrections(ArrayList<Correction> corrections) {
        synchronized (this) {
            this.corrections = corrections;
        }
    }

    @Override
    public Time getTime() {
        synchronized (this) {
            return timeRefMsec;
        }
    }

    @Override
    public String getName() {
        synchronized (this) {
            return NAME;
        }
    }

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {

        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            GnssClock gnssClock = event.getClock();
            long TimeNanos = gnssClock.getTimeNanos();
            timeRefMsec = new Time(System.currentTimeMillis());
            double BiasNanos = gnssClock.getBiasNanos();
            double gpsTime, pseudorange;

            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
                FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }


            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                double TimeOffsetNanos = measurement.getTimeOffsetNanos();


                // GPS Time generation (GSA White Paper - page 20)
                gpsTime =
                        TimeNanos - (FullBiasNanos + BiasNanos); // TODO intersystem bias?

                // Measurement time in full GPS time without taking into account weekNumberNanos(the number of
                // nanoseconds that have occurred from the beginning of GPS time to the current
                // week number)
                tRxGPS =
                        gpsTime + TimeOffsetNanos;


                weekNumberNanos =
                        Math.floor((-1. * FullBiasNanos) / Constants.NUMBER_NANO_SECONDS_PER_WEEK)
                                * Constants.NUMBER_NANO_SECONDS_PER_WEEK;

                // GPS pseudorange computation
                pseudorange =
                        (tRxGPS - weekNumberNanos - ReceivedSvTimeNanos) / 1.0E9
                                * Constants.SPEED_OF_LIGHT;

                // TODO Check that the measurement have a valid state such that valid pseudoranges are used in the PVT algorithm

                /*

                According to https://developer.android.com/ the GnssMeasurements States required
                for GPS valid pseudoranges are:

                int STATE_CODE_LOCK         = 1      (1 << 0)
                int int STATE_TOW_DECODED   = 8      (1 << 3)

                */

                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean codeLock = (measState & GnssMeasurement.STATE_CODE_LOCK) > 0;
                boolean towDecoded = (measState & GnssMeasurement.STATE_TOW_DECODED) > 0;
                boolean towUncertainty = measurement.getReceivedSvTimeUncertaintyNanos() < MAXTOWUNCNS;


                if (codeLock && towDecoded && towUncertainty && pseudorange < 1e9) {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            new Pseudorange(pseudorange, 0.0));

                    satelliteParameters.setUniqueSatId("G" + satelliteParameters.getSatId());

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    observedSatellites.add(satelliteParameters);

                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGPS + ", " + pseudorange);
                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
                } else {
                    visibleButNotUsed++;
                }
            }
        }
    }

    @Override
    public double getSatelliteSignalStrength(int index) {
        synchronized (this) {
            return observedSatellites.get(index).getSignalStrength();
        }
    }

    @Override
    public int getConstellationId() {
        synchronized (this) {
            return constellationId;
        }
    }


    @Override
    public void calculateSatPosition(Location initialLocation, Coordinates position) {

        synchronized (this) {

            rxPos = Coordinates.globalXYZInstance(position.getX(), position.getY(), position.getZ());

            for (SatelliteParameters observedSatellite : observedSatellites) {
                // Computation of the GPS satellite coordinates in ECEF frame

                // Determine the current GPS week number
                int gpsWeek = (int) (weekNumberNanos / Constants.NUMBER_NANO_SECONDS_PER_WEEK);

                // Time of signal reception in GPS Seconds of the Week (SoW)
                double gpsSow = (tRxGPS - weekNumberNanos) * 1e-9;
                Time tGPS = new Time(gpsWeek, gpsSow);

                // Convert the time of reception from GPS SoW to UNIX time (milliseconds)
                long timeRx = tGPS.getMsec();

                SatellitePosition rnp = ((RinexNavigationGps) rinexNavGps).getSatPositionAndVelocities(
                        timeRx,
                        observedSatellite.getPseudorange(),
                        observedSatellite.getSatId(),
                        satType,
                        0.0,
                        initialLocation);

                if (rnp == null)
                    break;

                observedSatellite.setSatellitePosition(rnp);

                observedSatellite.setRxTopo(
                        new TopocentricCoordinates(
                                rxPos,
                                observedSatellite.getSatellitePosition()));

                double accumulatedCorrection = 0;

                for (Correction correction : corrections) {

                    correction.calculateCorrection(
                            new Time(timeRx),
                            rxPos,
                            observedSatellite.getSatellitePosition(),
                            rinexNavGps,
                            initialLocation);

                    accumulatedCorrection += correction.getCorrection();
                }

                observedSatellite.setAccumulatedCorrection(accumulatedCorrection);
            }
        }
    }


    public static void registerClass() {
        register(
                NAME,
                GpsConstellation.class);
    }


    @Override
    public Coordinates getRxPos() {
        synchronized (this) {
            return rxPos;
        }
    }

    @Override
    public void setRxPos(Coordinates rxPos) {
        synchronized (this) {
            this.rxPos = rxPos;
        }
    }

    @Override
    public SatelliteParameters getSatellite(int index) {
        synchronized (this) {
            return observedSatellites.get(index);
        }
    }

    @Override
    public List<SatelliteParameters> getSatellites() {
        synchronized (this) {
            return observedSatellites;
        }
    }

    @Override
    public int getVisibleConstellationSize() {
        synchronized (this) {
            return getUsedConstellationSize() + visibleButNotUsed;
        }
    }

    @Override
    public int getUsedConstellationSize() {
        synchronized (this) {
            return observedSatellites.size();
        }
    }

}
