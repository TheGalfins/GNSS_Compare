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
import com.galfins.gogpsextracts.RinexNavigationGalileo;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;
import com.galfins.gogpsextracts.TopocentricCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Ciuban on 8/10/2018.
 */

public class GalileoE5aConstellation extends Constellation {
    private final static char satType = 'E';
    private static final String NAME = "Galileo E5a";
    private static final String TAG = "GalileoE5aConstellation";
    private static int constellationId = GnssStatus.CONSTELLATION_GALILEO;
    private static double E5a_FREQUENCY = 1.17645e9;
    private static double FREQUENCY_MATCH_RANGE = 0.1e9;
    private static double MASK_ELEVATION = 15; // degrees
    private static double MASK_CN0 = 10; // dB-Hz

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    private Coordinates rxPos;

    private double tRxGalileoTOW;
    private double weekNumber;

    /**
     * Time of the measurement
     */
    private Time timeRefMsec;

    private int visibleButNotUsed = 0;

    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]


    /**
     * List holding observed satellites
     */
    private List<SatelliteParameters> observedSatellites = new ArrayList<>();

    protected List<SatelliteParameters> unusedSatellites = new ArrayList<>();

//    private long timeRx;

    private NavigationProducer rinexNavGalileo = null;

    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections = new ArrayList<>();

    public GalileoE5aConstellation(){
        // URL template from where the Galileo ephemerides should be downloaded
        //String GNSS_BEV_GALILEO_RINEX = "ftp://gnss.bev.gv.at/pub/nrt/${ddd}/${yy}/bute${ddd}s.${yy}l.Z";
        String IGS_GALILEO_RINEX = "ftp://igs.bkg.bund.de/IGS/BRDC/${yyyy}/${ddd}/BRDC00WRD_R_${yyyy}${ddd}0000_01D_EN.rnx.gz";
        //String EUREF_GALILEO_RINEX = "ftp://igs.bkg.bund.de/EUREF/BRDC/${yyyy}/${ddd}/BRDC00WRD_R_${yyyy}${ddd}0000_01D_EN.rnx.gz";


        // Declare a RinexNavigation type object
        if(rinexNavGalileo == null)
            rinexNavGalileo = new RinexNavigationGalileo(IGS_GALILEO_RINEX);
    }

    public static boolean approximateEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
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
    public void updateMeasurements(GnssMeasurementsEvent event){
        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            unusedSatellites.clear();
            GnssClock gnssClock       = event.getClock();
            long TimeNanos            = gnssClock.getTimeNanos();
            timeRefMsec               = new Time(System.currentTimeMillis());
            double BiasNanos          = gnssClock.getBiasNanos();
            String strFullBiasNanos   = Long.toString(gnssClock.getFullBiasNanos());
            long dayFullBias          = Long.valueOf(strFullBiasNanos.substring(0,11));
            long podFullBiasNanos     = (long) -1.0 * Long.valueOf(strFullBiasNanos.substring(11,20));

            double tRx, tTx, modDayFullBias, PrSeconds, pseudorangeE5a, galileoTime;

//            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
                FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }



            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                if(measurement.getSvid() == 27 || measurement.getSvid() == 25) //todo: hardcoded exlusion of a faulty satellite (SUPL not working)
                    continue;

                if(!(measurement.hasCarrierFrequencyHz()
                        && approximateEqual(measurement.getCarrierFrequencyHz(), E5a_FREQUENCY, FREQUENCY_MATCH_RANGE)))
                    continue;


                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                double TimeOffsetNanos = measurement.getTimeOffsetNanos();

                // Compute the reception time in nanoseconds (this method is needed for later processing, is not a duplicate)
                galileoTime             = TimeNanos - (FullBiasNanos + BiasNanos);
                tRxGalileoTOW           = galileoTime % Constants.NUMBER_NANO_SECONDS_PER_WEEK;

                // Compute the time of reception in seconds
                tRx                     = 1e-9 * (TimeNanos - (podFullBiasNanos + BiasNanos));

                // Compute the weeknumber
                weekNumber              = Math.floor(-dayFullBias / Constants.WEEKSEC);
                modDayFullBias          = (dayFullBias + weekNumber * Constants.WEEKSEC);

                // Compute the time of signal transmission
                tTx = 1e-9 * (ReceivedSvTimeNanos + TimeOffsetNanos);
                tTx = tTx + modDayFullBias;

                // Galileo pseudorange computation
                PrSeconds = tRx - tTx;

                // Get the measurement state
                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean towKnown        = false;
                boolean towKnownValid   = false;

                // this is just a security measure, so that the code will not be crashing
                // if someone uses it on a lower API phone.
                // No phones with dual frequency will fail this condition
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) > 0;
                    towKnownValid = true;
                }

                boolean towDecoded      = (measState & GnssMeasurement.STATE_TOW_DECODED) > 0;

                boolean codeLockE5a = (measState & GnssMeasurement.STATE_CODE_LOCK) > 0;
                boolean msecAmbiguity = (measState & GnssMeasurement.STATE_MSEC_AMBIGUOUS) > 0;

                if (towKnownValid) {
                    // Solve for the 100 millisecond ambiguity
                    if (towDecoded && towKnown)
                        PrSeconds = PrSeconds % Constants.HUNDREDSMILLI;

                    // Solve for the 1 millisecond ambiguity
                    if (!towDecoded && !towKnown && msecAmbiguity)
                        PrSeconds = Math.floor(PrSeconds / 1e-3) * 1e-3 + PrSeconds % Constants.ONEMILLI;
                }

                // Compute the pseudorange in meters
                pseudorangeE5a = PrSeconds * Constants.SPEED_OF_LIGHT;


                // Variables for debugging
                int svID = measurement.getSvid();
                boolean condition = false;
                if (towKnownValid)
                    condition = towDecoded || msecAmbiguity ||towKnown;
                else
                    condition = towDecoded;

                if ( condition ) {

                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            new Pseudorange(pseudorangeE5a, 0.0));

//                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "<sub>E5a</sub>");
                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E5a");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    observedSatellites.add(satelliteParameters);
                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumber + ", " + tRxGalileoTOW + ", " + pseudorangeE5a);
                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);


                } else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(
                            measurement.getSvid(),
                            null
                    );
                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E5a");
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
        return null;
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

        // Make a list to hold the satellites that are to be excluded based on elevation/CN0 masking criteria
        List<SatelliteParameters> excludedSatellites = new ArrayList<>();

        synchronized (this) {
            rxPos = Coordinates.globalXYZInstance(position.getX(), position.getY(), position.getZ());
            for (SatelliteParameters observedSatellite : observedSatellites) {

                /*
                  Computation of the Galileo satellite coordinates in ECEF frame
                */

                // Determine the current Galileo week number (info: is the same as GPS week number)
                int galileoWeek = (int) weekNumber;

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
                    excludedSatellites.add(observedSatellite);
                    GnssCoreService.notifyUser("Faled getting ephemeris data!", Snackbar.LENGTH_SHORT, RNP_NULL_MESSAGE);
                    continue;
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

                // Add to the exclusion list the satellites that do not pass the masking criteria
                if(observedSatellite.getRxTopo().getElevation() < MASK_ELEVATION){
                    excludedSatellites.add(observedSatellite);
                }

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

            // Remove from the list all the satellites that did not pass the masking criteria
            visibleButNotUsed += excludedSatellites.size();
            observedSatellites.removeAll(excludedSatellites);
            unusedSatellites.addAll(excludedSatellites);
        }
    }


    public static void registerClass() {
        register(
                NAME,
                GalileoE5aConstellation.class);
    }
}
