package com.galfins.gnss_compare.Constellations;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.galfins.gnss_compare.Corrections.Correction;
import com.galfins.gnss_compare.GnssCoreService;
import com.galfins.gnss_compare.MainActivity;
import com.galfins.gogpsextracts.Constants;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.RinexNavigationGps;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.TopocentricCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Ciuban on 22/10/2018.
 * This class is for...
 * <p>
 * GPS Pseudorange computation algorithm by: GSA White Paper
 * - variable name changes and comments were added
 * to fit the description in the GSA white paper
 * by: Sebastian Ciuban
 */

public class GpsL5Constellation extends Constellation {

    private final static char satType = 'G';
    private static final String NAME = "GPS L5";
    private static final String TAG = "GpsL5Constellation";
    private static double L5_FREQUENCY = 1.17645e9;
    private static double FREQUENCY_MATCH_RANGE = 0.1e9;
    private static double MASK_ELEVATION = 15; // degrees
    private static double MASK_CN0 = 10; // dB-Hz

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    private Coordinates rxPos;
    private double tRxGPS;
    private double weekNumberNanos;
    private double weekNumber;

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

    protected List<SatelliteParameters> unusedSatellites = new ArrayList<>();

    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections;

    public GpsL5Constellation() {
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

    public static boolean approximateEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {

        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            unusedSatellites.clear();
            GnssClock gnssClock       = event.getClock();
            long TimeNanos            = gnssClock.getTimeNanos();
            timeRefMsec               = new Time(System.currentTimeMillis());
            double BiasNanos          = gnssClock.getBiasNanos();

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

                if(!(measurement.hasCarrierFrequencyHz()
                        && approximateEqual(measurement.getCarrierFrequencyHz(), L5_FREQUENCY, FREQUENCY_MATCH_RANGE)))
                    continue;

                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                double TimeOffsetNanos = measurement.getTimeOffsetNanos();


                // Compute the reception time in nanoseconds (this method is needed for later processing, is not a duplicate)
                gpsTime = TimeNanos - (FullBiasNanos + BiasNanos);

                // Measurement time in full GPS time without taking into account weekNumberNanos(the number of
                // nanoseconds that have occurred from the beginning of GPS time to the current
                // week number)
                tRxGPS =
                        gpsTime + TimeOffsetNanos;


                // Compute the weeknumber
                weekNumberNanos =
                        Math.floor((-1. * FullBiasNanos) / Constants.NUMBER_NANO_SECONDS_PER_WEEK)
                                * Constants.NUMBER_NANO_SECONDS_PER_WEEK;


                // GPS pseudorange computation
                pseudorange =
                        (tRxGPS - weekNumberNanos - ReceivedSvTimeNanos) / 1.0E9
                                * Constants.SPEED_OF_LIGHT;

                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean codeLock      = (measState & GnssMeasurement.STATE_CODE_LOCK) > 0;
                boolean towDecoded    = (measState & GnssMeasurement.STATE_TOW_DECODED) > 0;
                boolean towKnown      = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) > 0;
                }
                boolean towUncertainty = measurement.getReceivedSvTimeUncertaintyNanos() < MAXTOWUNCNS;


                if (codeLock && (towDecoded || towKnown)  && pseudorange < 1e9) {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            new Pseudorange(pseudorange, 0.0));

//                    satelliteParameters.setUniqueSatId("G" + satelliteParameters.getSatId() + "<sub>L5</sub>");
                    satelliteParameters.setUniqueSatId("G" + satelliteParameters.getSatId() + "_L5");


                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    observedSatellites.add(satelliteParameters);

                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGPS + ", " + pseudorange);
                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
                } else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            null
                    );
                    satelliteParameters.setUniqueSatId("G" + satelliteParameters.getSatId() + "_L5");
                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());
                    satelliteParameters.setConstellationType(measurement.getConstellationType());
                    if (measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());
                    unusedSatellites.add(satelliteParameters);
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

        // Make a list to hold the satellites that are to be excluded based on elevation/CN0 masking criteria
        List<SatelliteParameters> excludedSatellites = new ArrayList<>();

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

                if (rnp == null) {
                    excludedSatellites.add(observedSatellite);
                    GnssCoreService.notifyUser("Faled getting ephemeris data!", Snackbar.LENGTH_SHORT, RNP_NULL_MESSAGE);
                    continue;
                }

                observedSatellite.setSatellitePosition(rnp);

                observedSatellite.setRxTopo(
                        new TopocentricCoordinates(
                                rxPos,
                                observedSatellite.getSatellitePosition()));

                // Add to the exclusion list the satellites that do not pass the masking criteria
                if(observedSatellite.getRxTopo().getElevation() < MASK_ELEVATION){
                    excludedSatellites.add(observedSatellite);
                }

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

            // Remove from the list all the satellites that did not pass the masking criteria
            visibleButNotUsed += excludedSatellites.size();
            observedSatellites.removeAll(excludedSatellites);
            unusedSatellites.addAll(excludedSatellites);
        }
    }


    public static void registerClass() {
        register(
                NAME,
                GpsL5Constellation.class);
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
    public List<SatelliteParameters> getUnusedSatellites() {
        return unusedSatellites;
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
