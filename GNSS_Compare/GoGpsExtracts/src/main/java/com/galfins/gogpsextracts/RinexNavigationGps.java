package com.galfins.gogpsextracts;

import android.location.Location;
import android.location.cts.nano.Ephemeris;
import android.location.cts.nano.GalileoEphemeris;
import android.location.cts.suplClient.SuplRrlpController;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

// import java.util.Base64;

/**
 * @author Lorenzo Patocchi, cryms.com
 *         <p>
 *         This class retrieve RINEX file on-demand from known server structures
 */
public class RinexNavigationGps implements NavigationProducer {

    public final static String GARNER_NAVIGATION_AUTO = "ftp://garner.ucsd.edu/pub/nav/${yyyy}/${ddd}/auto${ddd}0.${yy}n.Z";
    public final static String IGN_MULTI_NAVIGATION_DAILY = "ftp://igs.ign.fr/pub/igs/data/campaign/mgex/daily/rinex3/${yyyy}/${ddd}/brdm${ddd}0.${yy}p.Z";
    public final static String GARNER_NAVIGATION_ZIM2 = "ftp://garner.ucsd.edu/pub/nav/${yyyy}/${ddd}/zim2${ddd}0.${yy}n.Z";
    public final static String IGN_NAVIGATION_HOURLY_ZIM2 = "ftp://igs.ensg.ign.fr/pub/igs/data/hourly/${yyyy}/${ddd}/zim2${ddd}${h}.${yy}n.Z";
    public final static String NASA_NAVIGATION_DAILY = "ftp://cddis.gsfc.nasa.gov/pub/gps/data/daily/${yyyy}/${ddd}/${yy}n/brdc${ddd}0.${yy}n.Z";
    public final static String NASA_NAVIGATION_HOURLY = "ftp://cddis.gsfc.nasa.gov/pub/gps/data/hourly/${yyyy}/${ddd}/hour${ddd}0.${yy}n.Z";
    public final static String GARNER_NAVIGATION_AUTO_HTTP = "http://garner.ucsd.edu/pub/rinex/${yyyy}/${ddd}/auto${ddd}0.${yy}n.Z"; // ex http://garner.ucsd.edu/pub/rinex/2016/034/auto0340.16n.Z
    public final static String BKG_HOURLY_SUPER_SEVER = "ftp://igs.bkg.bund.de/IGS/BRDC/${yyyy}/${ddd}/brdc${ddd}0.${yy}n.Z";

    private final static String TAG="RinexNavigationGps";

    /**
     * cache for negative answers
     */
    private Hashtable<String, Date> negativeChache = new Hashtable<String, Date>();

    /**
     * Folder containing downloaded files
     */
    public String RNP_CACHE = "./rnp-cache";

    private boolean waitForData = true;

    /**
     * @param //args
     */
    public static void main(String[] args) {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Calendar c = Calendar.getInstance();

		/*
        c.set(Calendar.YEAR, 2018);
		c.set(Calendar.MONTH, 2);
		c.set(Calendar.DAY_OF_MONTH, 15);
		c.set(Calendar.HOUR_OF_DAY, 15);
		c.set(Calendar.MINUTE, 30);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.setTimeZone(new SimpleTimeZone(0,""));
		*/

        Time t = new Time(c.getTimeInMillis());

        System.out.println("ts: " + t.getMsec() + " " + (new Date(t.getMsec())));
        System.out.println("week: " + t.getGpsWeek());
        System.out.println("week sec: " + t.getGpsWeekSec());
        System.out.println("week day: " + t.getGpsWeekDay());
        System.out.println("week hour in day: " + t.getGpsHourInDay());


        System.out.println("ts2: " + (new Time(t.getGpsWeek(), t.getGpsWeekSec())).getMsec());

        //RinexNavigation rn = new RinexNavigation(IGN_NAVIGATION_HOURLY_ZIM2);
        //RinexNavigationGps rn = new RinexNavigationGps(NASA_NAVIGATION_HOURLY);
        RinexNavigationGps rn = new RinexNavigationGps(BKG_HOURLY_SUPER_SEVER);

        rn.init();
//		SatellitePosition sp = rn.getGpsSatPosition(c.getTimeInMillis(), 2, 0, 0);
        Observations obs = new Observations(new Time(c.getTimeInMillis()), 0);
        SatellitePosition sp = rn.getGpsSatPosition(obs, 2, 'G', 0);

        if (sp != null) {
            System.out.println("found " + (new Date(sp.getUtcTime())) + " " + (sp.isPredicted() ? " predicted" : ""));
        } else {
            System.out.println("Epoch not found " + (new Date(c.getTimeInMillis())));
        }


    }

    /**
     * Template string where to retrieve files on the net
     */
    private String urltemplate;
    private HashMap<String, RinexNavigationParserGps> pool = new HashMap<String, RinexNavigationParserGps>();

    /**
     * Instantiates a new RINEX navigation retriever and parser.
     *
     * @param urltemplate the template URL where to get the files on the net.
     */
    public RinexNavigationGps(String urltemplate) {
        this.urltemplate = urltemplate;

    }

    /** Compute the GPS satellite coordinates

    INPUT:
      @param unixTime       = time of measurement reception - UNIX        [milliseconds]
      @param range          = pseudorange measuremnent                          [meters]
      @param satID          = satellite ID
      @param satType        = satellite type indicating the constellation (E: Galileo,
                            G: GPS)
      @param receiverClockError = 0.0
    */
    public SatellitePosition getSatPositionAndVelocities(long unixTime, double range, int satID, char satType, double receiverClockError, Location initialLocation) {

        //long unixTime = obs.getRefTime().getMsec();
        //double range = obs.getSatByIDType(satID, satType).getPseudorange(0);

        RinexNavigationParserGps rnp = getRNPByTimestamp(unixTime, initialLocation);

        if (rnp != null) {
            if (rnp.isTimestampInEpocsRange(unixTime)) {
                return rnp.getSatPositionAndVelocities(unixTime, range, satID, satType, receiverClockError);
            } else {
                return null;
            }
        }

        return null;
    }

    public EphGps findEph(long unixTime, int satID, char satType, Location initialLocation) {
        long requestedTime = unixTime;
        EphGps eph = null;
        int maxBack = 12;
        while (eph == null && (maxBack--) > 0) {

            RinexNavigationParserGps rnp = getRNPByTimestamp(requestedTime, initialLocation);

            if (rnp != null) {
                if (rnp.isTimestampInEpocsRange(unixTime)) {
                    eph = rnp.findEph(unixTime, satID, satType);
                }
            }
            if (eph == null)
                requestedTime -= (1L * 3600L * 1000L);
        }

        return eph;
    }

    /* Convenience method for adding an rnp to memory cache*/
    public void put(long reqTime, RinexNavigationParserGps rnp) {
        Time t = new Time(reqTime);
        String url = t.formatTemplate(urltemplate);
        if (!pool.containsKey(url))
            pool.put(url, rnp);
    }

    ArraySet<String> retrievingFromServer = new ArraySet<>();

    protected RinexNavigationParserGps getRNPByTimestamp(long unixTime, final Location initialLocation) {

        RinexNavigationParserGps rnp = null;
        long reqTime = unixTime;

//        do {
            // found none, retrieve from urltemplate
            Time t = new Time(reqTime);
            //System.out.println("request: "+unixTime+" "+(new Date(t.getMsec()))+" week:"+t.getGpsWeek()+" "+t.getGpsWeekDay());

            //final String url = t.formatTemplate(urltemplate);
            final String url = "supl.google.com";

            if (pool.containsKey(url)) {
                synchronized (this) {
                    rnp = pool.get(url);
                }
            } else {
                if(!retrievingFromServer.contains(url)) {
                    retrievingFromServer.add(url);
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            RinexNavigationParserGps rnp;

                            try {
                                rnp = getFromSUPL(url, initialLocation);
                                synchronized (RinexNavigationGps.this) {
                                    if (rnp != null) {
                                        pool.put(url, rnp);
                                    }
                                    retrievingFromServer.remove(url);
                                }
                            } catch (IOException e) {
                                System.out.println(e.getClass().getName() + " url: " + url);
                            }
                        }
                    })).start();
                }
                return null;
            }
            return rnp;

//        } while (waitForData && rnp == null);
    }

    private RinexNavigationParserGps getFromSUPL(String url, Location initialLocation) throws IOException {
        RinexNavigationParserGps rnp = null;

        String suplName = url;
        File rnf = new File(RNP_CACHE, suplName);

        if (rnf.exists()) {
            System.out.println("Supl from cache file " + rnf);
            try {
                rnp = SuplFileToRnpParserGps(rnf);
                return rnp;
            } catch (Exception e) {
                rnf.delete();
            }
        }

        try {

            Log.w(TAG, "getFromSUPL: Getting data using SUPL client..." );
            SuplRrlpController mSuplController = new SuplRrlpController(suplName,7276); // non-SSL
            Pair<Ephemeris.GpsNavMessageProto, GalileoEphemeris.GalNavMessageProto> navMsg;
            navMsg = mSuplController.generateNavMessage((long) (initialLocation.getLatitude()*1e7), (long) (initialLocation.getLongitude()*1e7));

            Ephemeris.GpsNavMessageProto gpsNavMsg = navMsg.first;

            if (gpsNavMsg.rpcStatus == Ephemeris.GpsNavMessageProto.SUCCESS) {
                rnp = new RinexNavigationParserGps(gpsNavMsg);
            }

            Log.w(TAG, "getFromSUPL: Received data from SUPL server" );

        } catch (IOException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Exception thrown getting msg from SUPL server", e);
        }
        return rnp;
    }

    private RinexNavigationParserGps SuplFileToRnpParserGps(File rnf) {
        RinexNavigationParserGps rnp = null;


        return rnp;
    }

    private RinexNavigationParserGps getFromFTP(String url) throws IOException {
        RinexNavigationParserGps rnp = null;

        String origurl = url;
        if (negativeChache.containsKey(url)) {
            if (System.currentTimeMillis() - negativeChache.get(url).getTime() < 60 * 60 * 1000) {
                throw new FileNotFoundException("cached answer");
            } else {
                negativeChache.remove(url);
            }
        }

        String filename = url.replaceAll("[ ,/:]", "_");
        if (filename.endsWith(".Z")) filename = filename.substring(0, filename.length() - 2);
        File rnf = new File(RNP_CACHE, filename);

        if (rnf.exists()) {
            System.out.println(url + " from cache file " + rnf);
            rnp = new RinexNavigationParserGps(rnf);
            try {
                rnp.init();
                return rnp;
            } catch (Exception e) {
                rnf.delete();
            }
        }

        // if the file doesn't exist of is invalid
        System.out.println(url + " from the net.");
        FTPClient ftp = new FTPClient();

        try {

            Log.w(TAG, "getFromFTP: Getting data from FTP server..." );

            int reply;
            System.out.println("URL: " + url);
            url = url.substring("ftp://".length());
            String server = url.substring(0, url.indexOf('/'));
            String remoteFile = url.substring(url.indexOf('/'));
            String remotePath = remoteFile.substring(0, remoteFile.lastIndexOf('/'));
            remoteFile = remoteFile.substring(remoteFile.lastIndexOf('/') + 1);

            ftp.connect(server);
            ftp.login("anonymous", "info@eriadne.org");

            System.out.print(ftp.getReplyString());

            // After connection attempt, you should check the reply code to
            // verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return null;
            }

            ftp.enterLocalPassiveMode();
            ftp.setRemoteVerificationEnabled(false);

            System.out.println("cwd to " + remotePath + " " + ftp.changeWorkingDirectory(remotePath));
            System.out.println(ftp.getReplyString());
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println(ftp.getReplyString());

            System.out.println("open " + remoteFile);
            InputStream is = ftp.retrieveFileStream(remoteFile);
            System.out.println(ftp.getReplyString());
            if (ftp.getReplyString().startsWith("550")) {
                negativeChache.put(origurl, new Date());
                throw new FileNotFoundException();
            }
            InputStream uis = is;

            if (remoteFile.endsWith(".Z")) {
                uis = new UncompressInputStream(is);
            }

            rnp = new RinexNavigationParserGps(uis, rnf);
            rnp.init();
            is.close();


            ftp.completePendingCommand();

            ftp.logout();

            Log.w(TAG, "getFromFTP: Received data from server" );

        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }
        return rnp;
    }

    private RinexNavigationParserGps getFromHTTP(String tUrl) throws IOException {
        RinexNavigationParserGps rnp = null;

        String origurl = tUrl;
        if (negativeChache.containsKey(tUrl)) {
            if (System.currentTimeMillis() - negativeChache.get(tUrl).getTime() < 60 * 60 * 1000) {
                throw new FileNotFoundException("cached answer");
            } else {
                negativeChache.remove(tUrl);
            }
        }

        String filename = tUrl.replaceAll("[ ,/:]", "_");
        if (filename.endsWith(".Z")) filename = filename.substring(0, filename.length() - 2);
        File rnf = new File(RNP_CACHE, filename);

        if (rnf.exists()) {
            System.out.println(tUrl + " from cache file " + rnf);
            rnp = new RinexNavigationParserGps(rnf);
            rnp.init();
        } else {
            System.out.println(tUrl + " from the net.");

            System.out.println("URL: " + tUrl);
            tUrl = tUrl.substring("http://".length());
            String remoteFile = tUrl.substring(tUrl.indexOf('/'));
            remoteFile = remoteFile.substring(remoteFile.lastIndexOf('/') + 1);

            URL url = new URL("http://" + tUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Basic "+ new String(Base64.encode(("anonymous:info@eriadne.org").getBytes(), Base64.DEFAULT)));
//            con.setRequestProperty("Authorization", "Basic " + new String(Base64.getEncoder().encode((new String("anonymous:info@eriadne.org").getBytes()))));

            int reply = con.getResponseCode();

            if (reply > 200) {
                if (reply == 404)
                    System.err.println("404 Not Found");
                else
                    System.err.println("HTTP server refused connection.");
//        System.out.print(new String(res.getContent()));

                return null;
            }

            try {
                if (remoteFile.endsWith(".Z")) {
                    try {
//            InputStream is = new ByteArrayInputStream(res.getContent());
                        InputStream is = con.getInputStream();
                        InputStream uis = new UncompressInputStream(is);
                        rnp = new RinexNavigationParserGps(uis, rnf);
                        rnp.init();
                        uis.close();
                    } catch (IOException e) {
                        InputStream is = con.getInputStream();
                        InputStream uis = new GZIPInputStream(is);
                        rnp = new RinexNavigationParserGps(uis, rnf);
                        rnp.init();
                        //        Reader decoder = new InputStreamReader(gzipStream, encoding);
                        //        BufferedReader buffered = new BufferedReader(decoder);
                        uis.close();
                    }
                } else {
                    InputStream is = con.getInputStream();
                    rnp = new RinexNavigationParserGps(is, rnf);
                    rnp.init();
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                // TODO delete file, maybe it's corrupt
            }
        }
        return rnp;
    }


    /* (non-Javadoc)
     * @see org.gogpsproject.NavigationProducer#getIono(int)
     */
    @Override
    public IonoGps getIono(long unixTime, Location initialLocation) {
        RinexNavigationParserGps rnp = getRNPByTimestamp(unixTime, initialLocation);
        if (rnp != null) return rnp.getIono(unixTime, initialLocation);
        return null;
    }

    @Override
    public IonoGalileo getIonoNeQuick(long unixTime, Location initialLocation) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.gogpsproject.NavigationProducer#init()
     */
    @Override
    public void init() {

    }

    /* (non-Javadoc)
     * @see org.gogpsproject.NavigationProducer#release()
     */
    @Override
    public void release(boolean waitForThread, long timeoutMs) throws InterruptedException {
        waitForData = false;
    }

    @Override
    public SatellitePosition getGpsSatPosition(Observations obs, int satID, char satType, double receiverClockError) {
        // TODO Auto-generated method stub
        return null;
    }


}

