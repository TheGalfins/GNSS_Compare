/*
 * Copyright (c) 2011 Eugenio Realini, Mirko Reguzzoni, Cryms sagl - Switzerland. All Rights Reserved.
 *
 * This file is part of goGPS Project (goGPS).
 *
 * goGPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * goGPS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with goGPS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.galfins.gogpsextracts;

import android.location.Location;
import android.location.cts.nano.Ephemeris;
import android.location.cts.nano.GalileoEphemeris;
import android.location.cts.suplClient.SuplRrlpController;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

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

/**
 * @author Lorenzo Patocchi, cryms.com
 *
 * This class retrieve RINEX file on-demand from known server structures
 *
 */
public class RinexNavigationGalileo implements NavigationProducer {

    public final static String GARNER_NAVIGATION_AUTO = "ftp://garner.ucsd.edu/pub/nav/${yyyy}/${ddd}/auto${ddd}0.${yy}n.Z";
    public final static String IGN_MULTI_NAVIGATION_DAILY = "ftp://igs.ign.fr/pub/igs/data/campaign/mgex/daily/rinex3/${yyyy}/${ddd}/brdm${ddd}0.${yy}p.Z";
    public final static String GARNER_NAVIGATION_ZIM2 = "ftp://garner.ucsd.edu/pub/nav/${yyyy}/${ddd}/zim2${ddd}0.${yy}n.Z";
    public final static String IGN_NAVIGATION_HOURLY_ZIM2 = "ftp://igs.ensg.ign.fr/pub/igs/data/hourly/${yyyy}/${ddd}/zim2${ddd}${h}.${yy}n.Z";
    public final static String NASA_NAVIGATION_DAILY = "ftp://cddis.gsfc.nasa.gov/pub/gps/data/daily/${yyyy}/${ddd}/${yy}n/brdc${ddd}0.${yy}n.Z";
    public final static String NASA_NAVIGATION_HOURLY = "ftp://cddis.gsfc.nasa.gov/pub/gps/data/hourly/${yyyy}/${ddd}/hour${ddd}0.${yy}n.Z";
    public final static String GARNER_NAVIGATION_AUTO_HTTP = "http://garner.ucsd.edu/pub/rinex/${yyyy}/${ddd}/auto${ddd}0.${yy}n.Z"; // ex http://garner.ucsd.edu/pub/rinex/2016/034/auto0340.16n.Z
    public final static String BKG_GALILEO_RINEX = "ftp://igs.bkg.bund.de/EUREF/BRDC/${yyyy}/${ddd}/BRDC00WRD_R_${yyyy}${ddd}0000_01D_EN.rnx.gz";
    public final static String ESA_GALILEO_RINEX = "ftp://gssc.esa.int/gnss/data/daily/${yyyy}/${ddd}/ankr${ddd}0.${yy}l.Z";

    private final static String TAG = "RinexNavigationGalileo";


    /**
     * cache for negative answers
     */
    private Hashtable<String, Date> negativeChache = new Hashtable<String, Date>();

	/** Folder containing downloaded files */
	public String RNP_CACHE = "./rnp-cache";

    private boolean waitForData = true;

    /**
     * @param args
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
        RinexNavigationGalileo rn = new RinexNavigationGalileo(NASA_NAVIGATION_HOURLY);

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
    private HashMap<String, RinexNavigationParserGalileo> pool = new HashMap<String, RinexNavigationParserGalileo>();

    /**
     * Instantiates a new RINEX navigation retriever and parser.
     *
     * @param urltemplate the template URL where to get the files on the net.
     */
    public RinexNavigationGalileo(String urltemplate) {
        this.urltemplate = urltemplate;

	}
	/* (non-Javadoc)
	 * @see org.gogpsproject.NavigationProducer#getGpsSatPosition(long, int, double)
	 */
	public SatellitePosition getGalileoSatPosition(long unixTime,double range,int satID, char satType, double receiverClockError, Location initialLocation) {

		RinexNavigationParserGalileo rnp = getRNPByTimestamp(unixTime, initialLocation);
		if(rnp!=null){
			if(rnp.isTimestampInEpocsRange(unixTime)){
				return rnp.getGalileoSatPosition(unixTime,range, satID, satType, receiverClockError);
			}else{
				return null;
			}
		}

		return null;
	}
	
	public SatellitePosition getGalileoSatVelocities(long unixTime,double range,int satID, char satType, double receiverClockError, Location initialLocation) {

		RinexNavigationParserGalileo rnp = getRNPByTimestamp(unixTime, initialLocation);
		if(rnp!=null){
			if(rnp.isTimestampInEpocsRange(unixTime)){
			
				return rnp.getGalileoSatVelocities(unixTime,range, satID, satType, receiverClockError);
				
			}else{
				return null;
			}
		}

		return null;
	}
	
	
	private RinexNavigationParserGalileo rnp;
	
	public BroadcastGGTO getRnpGgto(){
		return rnp.ggto;
	}
	
	public EphGalileo findEph(long unixTime, int satID, char satType, Location initialLocation) {
		long requestedTime = unixTime;
		EphGalileo eph = null;
		int maxBack = 12;
		while(eph==null && (maxBack--)>0){

			rnp = getRNPByTimestamp(requestedTime, initialLocation);

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
  public void put(long reqTime, RinexNavigationParserGalileo rnp) {
    Time t = new Time(reqTime);
     String url = t.formatTemplate(urltemplate);
     if(!pool.containsKey(url))
       pool.put(url, rnp);
   }

    ArraySet<String> retrievingFromServer = new ArraySet<>();

    protected RinexNavigationParserGalileo getRNPByTimestamp(long unixTime, final Location initialLocation) {
        RinexNavigationParserGalileo rnp = null;
        long reqTime = unixTime;

 //       do {
            // found none, retrieve from urltemplate
            Time t = new Time(reqTime);
            //System.out.println("request: "+unixTime+" "+(new Date(t.getMsec()))+" week:"+t.getGpsWeek()+" "+t.getGpsWeekDay());

            //String url = t.formatTemplate(urltemplate);
            final String url = "supl-dev.google.com";

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
                        RinexNavigationParserGalileo rnp = null;

                        try {
                            try {
                                rnp = getFromSUPL(url, initialLocation);
                            } catch (IndexOutOfBoundsException e){
//                                try {
//                                    MainActivity.makeNotification("SUPL client exception...");
//                                } catch (Exception e1){
//                                    e1.printStackTrace();
//                                }
                                e.printStackTrace();
                            }
                            synchronized (RinexNavigationGalileo.this) {
                                if (rnp != null) {
                                    pool.put(url, rnp);
                                }
                                retrievingFromServer.remove(url);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Supl Client error:", e);
                        }
                    }
                })).start();
            }
            return null;
        }
        return rnp;
//            try {
//                if (pool.containsKey(url)) {
//                    rnp = pool.get(url);
//                } else {
//                    rnp = getFromSUPL(url, initialLocation);
//                    if (rnp != null) {
//                        pool.put(url, rnp);
//                    }
//                }
//                return rnp;
//            } catch (IOException e) {
//                System.out.println(e.getClass().getName() + " url: " + url);
//                return null;
//            }
//
//        } while (waitForData && rnp == null);
    }

    private RinexNavigationParserGalileo getFromSUPL(String url, Location initialLocation) throws IOException {
        RinexNavigationParserGalileo rnp = null;

        String suplName = url;
        File rnf = new File(RNP_CACHE, suplName);

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar c = Calendar.getInstance();
        Time t = new Time(c.getTimeInMillis());

        if (rnf.exists()) {
            System.out.println("Supl from cache file " + rnf);
            try {
                rnp = SuplFileToRnpParserGal(rnf);
                return rnp;
            } catch (Exception e) {
                rnf.delete();
            }
        }

        try {
            Log.w(TAG, "getFromSUPL: Getting data using SUPL client..." );

            //SuplRrlpController mSuplController = new SuplRrlpController("supl.google.com",7276); // use this once the google server is operational
            SuplRrlpController mSuplController = new SuplRrlpController(suplName,7280); // non-SSL

            Pair<Ephemeris.GpsNavMessageProto, GalileoEphemeris.GalNavMessageProto> navMsg;
            navMsg = mSuplController.generateNavMessage((long) (initialLocation.getLatitude()*1e7), (long) (initialLocation.getLongitude()*1e7));
            GalileoEphemeris.GalNavMessageProto galNavMsg = navMsg.second;

            if (galNavMsg.rpcStatus == GalileoEphemeris.GalNavMessageProto.SUCCESS) {
                rnp = new RinexNavigationParserGalileo(galNavMsg);
            }

            Log.w(TAG, "getFromSUPL: Received data from SUPL server" );

        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Exception thrown getting msg from SUPL server", e);
        }
        return rnp;
    }

    private RinexNavigationParserGalileo SuplFileToRnpParserGal(File rnf) {
        RinexNavigationParserGalileo rnp = null;

        return rnp;
    }

    private RinexNavigationParserGalileo getFromFTP(String url) throws IOException{
		RinexNavigationParserGalileo rnp = null;

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
        if (filename.endsWith(".gz")) filename = filename.substring(0, filename.length() - 3);
        File rnf = new File(RNP_CACHE, filename);

        if (rnf.exists()) {
            System.out.println(url + " from cache file " + rnf);
            rnp = new RinexNavigationParserGalileo(rnf);
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

            Log.w(TAG, "getFromFTP: Getting Galileo data from FTP server...");

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

            if (remoteFile.endsWith(".gz")) {
                uis = new GZIPInputStream(is);
            }

            rnp = new RinexNavigationParserGalileo(uis, rnf);
            rnp.init();
            is.close();


            ftp.completePendingCommand();

            ftp.logout();

            Log.w(TAG, "getFromFTP: Received Galileo data from server");

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

    private RinexNavigationParserGalileo getFromHTTP(String tUrl) throws IOException {
        RinexNavigationParserGalileo rnp = null;

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
        if (filename.endsWith(".gz")) filename = filename.substring(0, filename.length() - 3);
        File rnf = new File(RNP_CACHE, filename);

        if (rnf.exists()) {
            System.out.println(tUrl + " from cache file " + rnf);
            rnp = new RinexNavigationParserGalileo(rnf);
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
            con.setRequestProperty("Authorization", "Basic " + new String(Base64.encode(("anonymous:info@eriadne.org").getBytes(), Base64.DEFAULT)));
//      con.setRequestProperty("Authorization", "Basic "+ new String(Base64.getEncoder().encode((new String("anonymous:info@eriadne.org").getBytes()))));

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
            InputStream is  = con.getInputStream();
            InputStream uis = new UncompressInputStream(is);
            rnp = new RinexNavigationParserGalileo(uis,rnf);
            rnp.init();
            uis.close();
          }
          catch( IOException e ){
            InputStream is  = con.getInputStream();
            InputStream uis = new GZIPInputStream(is);
            rnp = new RinexNavigationParserGalileo(uis,rnf);
            rnp.init();
  //        Reader decoder = new InputStreamReader(gzipStream, encoding);
  //        BufferedReader buffered = new BufferedReader(decoder);
            uis.close();
          }
        }
        else {
          InputStream is  = con.getInputStream();
          rnp = new RinexNavigationParserGalileo(is,rnf);
          rnp.init();
          is.close();
        }
      }
      catch(IOException e ){
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
		RinexNavigationParserGalileo rnp = getRNPByTimestamp(unixTime, initialLocation);
		if(rnp!=null) return rnp.getIono(unixTime, initialLocation);
		return null;
	}

    @Override
    public IonoGalileo getIonoNeQuick(long unixTime, Location initialLocation) {
        RinexNavigationParserGalileo rnp = getRNPByTimestamp(unixTime, initialLocation);
        if(rnp!=null) return rnp.getIonoNeQuick(unixTime, initialLocation);
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
