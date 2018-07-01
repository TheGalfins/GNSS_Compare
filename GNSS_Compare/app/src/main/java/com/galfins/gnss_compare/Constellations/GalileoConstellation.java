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
import com.galfins.gogpsextracts.RinexNavigationGalileo;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.TopocentricCoordinates;

/**
 * Created by Sebastian Ciuban on 17/03/2018.
 */

public class GalileoConstellation extends Constellation {

    private final static char satType = 'E';
    private static final String NAME = "Galileo";
    private static final String TAG = "GalileoConstellation";
    private static int constellationId = GnssStatus.CONSTELLATION_GALILEO;

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    private Coordinates rxPos;

    private double tRxGalileoTOW;
    private double tRxGalileoE1_2nd;
    private double weekNumberNanos;

    /**
     * Time of the measurement
     */
    private Time timeRefMsec;

    private int visibleButNotUsed = 0;

    /**
     * List holding observed satellites
     */
    private List<SatelliteParameters> observedSatellites = new ArrayList<>();


//    private long timeRx;

    private NavigationProducer rinexNavGalileo = null;

    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections;

    public GalileoConstellation() {

        // URL template from where the Galileo ephemerides should be downloaded
        //String GNSS_BEV_GALILEO_RINEX = "ftp://gnss.bev.gv.at/pub/nrt/${ddd}/${yy}/bute${ddd}s.${yy}l.Z";
        String IGS_GALILEO_RINEX = "ftp://igs.bkg.bund.de/IGS/BRDC/${yyyy}/${ddd}/BRDC00WRD_R_${yyyy}${ddd}0000_01D_EN.rnx.gz";
        //String EUREF_GALILEO_RINEX = "ftp://igs.bkg.bund.de/EUREF/BRDC/${yyyy}/${ddd}/BRDC00WRD_R_${yyyy}${ddd}0000_01D_EN.rnx.gz";


        // Declare a RinexNavigation type object
        if(rinexNavGalileo == null)
            rinexNavGalileo = new RinexNavigationGalileo(IGS_GALILEO_RINEX);
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
        return NAME;
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
            double galileoTime, pseudorangeTOW, pseudorangeE1_2nd, tTxGalileo;

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

                // Galileo Time generation (GSA White Paper - page 20)
                galileoTime =
                        TimeNanos - (FullBiasNanos + BiasNanos);

                // Measurement time in full Galileo time without taking into account weekNumberNanos(the number of
                // nanoseconds that have occurred from the beginning of GPS time to the current
                // week number)

                weekNumberNanos =
                        Math.floor((-1. * FullBiasNanos) / Constants.NUMBER_NANO_SECONDS_PER_WEEK)
                                * Constants.NUMBER_NANO_SECONDS_PER_WEEK;


                // Compute the time of signal reception for when  GNSS_MEASUREMENT_STATE_TOW_KNOWN or GNSS_MEASUREMENT_STATE_TOW_DECODED are true
                tRxGalileoTOW = galileoTime % Constants.NUMBER_NANO_SECONDS_PER_WEEK;

                // Compute the signal reception for when GNSS_MEASUREMENT_STATE_GAL_E1C_2ND_CODE_LOCK is true
                tRxGalileoE1_2nd = galileoTime % Constants.NumberNanoSeconds100Milli;

                // Compute the time of signal transmission
                tTxGalileo = ReceivedSvTimeNanos + TimeOffsetNanos;


                // Galileo pseudorange computation

                // Valid only if GNSS_MEASUREMENT_STATE_TOW_KNOWN or GNSS_MEASUREMENT_STATE_TOW_DECODED are true
                pseudorangeTOW = (tRxGalileoTOW - tTxGalileo) * 1e-9 * Constants.SPEED_OF_LIGHT;

                // Valid only if GNSS_MEASUREMENT_STATE_GAL_E1C_2ND_CODE_LOCK
                pseudorangeE1_2nd = ((galileoTime - tTxGalileo) % Constants.NumberNanoSeconds100Milli) * 1e-9 * Constants.SPEED_OF_LIGHT;

            /*

                According to https://developer.android.com/ and GSA White Paper (pg.20)
                the GnssMeasurements States required for GALILEO valid pseudoranges are:

                STATE_TOW_KNOWN                   = 16384                            (1 << 11)
                STATE_TOW_DECODED                 =     8                            (1 <<  3)
                STATE_GAL_E1C_2ND_CODE_LOCK       =  2048                            (1 << 11)

            */

                // Get the measurement state
                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) > 0;
                boolean towDecoded = (measState & GnssMeasurement.STATE_TOW_DECODED) > 0;
                boolean codeLock = (measState & GnssMeasurement.STATE_GAL_E1C_2ND_CODE_LOCK) > 0;

                // Variables for debugging
                double prTOW = pseudorangeTOW;
                double prE1_2nd = pseudorangeE1_2nd;
                double diffPR = prTOW - prE1_2nd;
                int svID = measurement.getSvid();


                if ((towKnown || towDecoded)) {

                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            new Pseudorange(pseudorangeTOW, 0.0));

                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId());

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    observedSatellites.add(satelliteParameters);
                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGalileoTOW + ", " + pseudorangeTOW);
                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);


                } else if (codeLock) {

                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            new Pseudorange(pseudorangeE1_2nd, 0.0));

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    observedSatellites.add(satelliteParameters);
                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGalileoTOW + ", " + pseudorangeE1_2nd);
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

    @Override
    public void calculateSatPosition(Location initialLocation, Coordinates position) {


//        rxPos = pose;
//        rxPos = Coordinates.globalGeodInstance(52.1628855, 4.523316, 179.547);

        synchronized (this) {
            rxPos = Coordinates.globalXYZInstance(position.getX(), position.getY(), position.getZ());
            for (SatelliteParameters observedSatellite : observedSatellites) {

                /*
                  Computation of the Galileo satellite coordinates in ECEF frame
                */

                // Determine the current Galileo week number (info: is the same as GPS week number)
                int galileoWeek = (int) (weekNumberNanos / Constants.NUMBER_NANO_SECONDS_PER_WEEK);

                // Time of signal reception in Galileo Seconds of the Week (SoW)
                double galileoSow = (tRxGalileoTOW) * 1e-9;
                Time tGalileo = new Time(galileoWeek, galileoSow);

                // Convert the time of reception from GPS SoW to UNIX time (milliseconds)
                long timeRx = tGalileo.getMsec();


                /**Compute the Galileo satellite coordinates

                 INPUT:
                 @param timeRx         = time of measurement reception - UNIX        [milliseconds]
                 @param pseudorange    = pseudorange measuremnent                          [meters]
                 @param satID          = satellite ID
                 @param satType        = satellite type indicating the constellation (E: Galileo)

                 */
                SatellitePosition rnp = ((RinexNavigationGalileo) rinexNavGalileo).getGalileoSatPosition(
                        timeRx,
                        observedSatellite.getPseudorange(),
                        observedSatellite.getSatId(),
                        satType,
                        0.0,
                        initialLocation);

                if (rnp == null) {
                    break;
                }

                observedSatellite.setSatellitePosition(rnp);


                /** Compute the azimuth and elevation w.r.t the user's approximate location

                 INPUT:
                 @param rxPos                = user's approximate ECEF coordinates       [cartesian]
                 @param satellitePosition    = satellite ECEF coordinates                [cartesian]

                 */
                observedSatellite.setRxTopo(
                        new TopocentricCoordinates(
                                rxPos,
                                observedSatellite.getSatellitePosition()));

                double accumulatedCorrection = 0;

                /** Compute the accumulated corrections for the pseudorange measurements
                 * Currently the accumulated corrections contain the following effects:
                 *                  - Ionosphere
                 *                  - Troposphere
                 *                  - Shapiro delay (i.e, relativistic path range correction)

                 INPUT:
                 @param timeRx               = time of measurement reception - UNIX   [milliseconds]
                 @param rxPos                = user's approximate ECEF coordinates       [cartesian]
                 @param satellitePosition    = satellite ECEF coordinates                [cartesian]
                 @param rinexNavGalileo      = RinexNavigationGalileo type object
                 */
                for (Correction correction : corrections) {

                    correction.calculateCorrection(
                            new Time(timeRx),
                            rxPos,
                            observedSatellite.getSatellitePosition(),
                            rinexNavGalileo,
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
                GalileoConstellation.class);
    }
}
