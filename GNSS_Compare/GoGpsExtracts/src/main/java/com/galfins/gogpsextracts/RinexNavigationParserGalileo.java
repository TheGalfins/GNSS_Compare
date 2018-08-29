package com.galfins.gogpsextracts;

import android.location.Location;
import android.location.cts.nano.GalileoEphemeris;
import android.util.Log;

import com.galfins.gogpsextracts.EphemerisSystemGalileo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;



/**
 * <p>
 * Class for parsing RINEX navigation files
 * </p>
 *
 * @author Eugenio Realini, Cryms.com
 *
 *
 * adapted for Galileo system by: Sebastian Ciuban
 */
public class RinexNavigationParserGalileo extends EphemerisSystemGalileo implements NavigationProducer{

	private File fileNav;
	private FileInputStream streamNav;
	private InputStreamReader inStreamNav;
	private BufferedReader buffStreamNav;

	private FileOutputStream cacheOutputStream;
	private OutputStreamWriter cacheStreamWriter;

	public static String newline = System.getProperty("line.separator");

	private final String TAG = this.getClass().getSimpleName();

	public BroadcastGGTO ggto;

	private ArrayList<EphGalileo> eph = new ArrayList<EphGalileo>(); /* GPS broadcast ephemerides */
	//private double[] iono = new double[8]; /* Ionosphere model parameters */
	private IonoGalileo iono = null; /* Ionosphere model parameters */
	//	private double A0; /* Delta-UTC parameters: A0 */
	//	private double A1; /* Delta-UTC parameters: A1 */
	//	private double T; /* Delta-UTC parameters: T */
	//	private double W; /* Delta-UTC parameters: W */
	//	private int leaps; /* Leap seconds */


	// RINEX Read constructors
	public RinexNavigationParserGalileo(File fileNav) {
		this.fileNav = fileNav;
	}

	// RINEX Read constructors
	public RinexNavigationParserGalileo(InputStream is, File cache) {
		this.inStreamNav = new InputStreamReader(is);
		if(cache!=null){
			File path = cache.getParentFile();
			if(!path.exists()) path.mkdirs();
			try {
				cacheOutputStream = new FileOutputStream(cache);
				cacheStreamWriter = new OutputStreamWriter(cacheOutputStream);
			} catch (FileNotFoundException e) {
				System.err.println("Exception writing "+cache);
				e.printStackTrace();
			}
		}
	}

	public RinexNavigationParserGalileo(GalileoEphemeris.GalNavMessageProto galNavMsg) {
        for (int iSv = 0; iSv<galNavMsg.ephemerids.length; iSv++) {
            this.eph.add(new EphGalileo(galNavMsg.ephemerids[iSv]));
        }
        this.iono = new IonoGalileo(galNavMsg.iono, galNavMsg.utcModel);
	}

	/* (non-Javadoc)
	 * @see org.gogpsproject.Navigation#init()
	 */
	@Override
	public void init() {
		open();
		int ver = parseHeaderNav();
		if(ver != 0){

			if (ver == 2){

								System.out.println("Ver. 2.x");
				parseDataNavV2();

			} else if (ver == 212){

				//				System.out.println("Ver. 2.12");
				parseDataNavV2();

			} else if (ver == 3){

				//				System.out.println("Ver. 3.01");
				parseDataNavV3();

			}
	    close();
		}
		else{
		  close();
      throw new RuntimeException( fileNav.toString() + " is invalid ");
		}
	}



	/* (non-Javadoc)
	 * @see org.gogpsproject.Navigation#release()
	 */
	@Override
	public void release(boolean waitForThread, long timeoutMs) throws InterruptedException {

	}

	/**
	 *
	 */
	public void open() {
		try {

			if(fileNav!=null) streamNav = new FileInputStream(fileNav);
			if(streamNav!=null) inStreamNav = new InputStreamReader(streamNav);
			if(inStreamNav!=null) buffStreamNav = new BufferedReader(inStreamNav);

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	public void close() {
		try {
			if(cacheStreamWriter!=null){
				cacheStreamWriter.flush();
				cacheStreamWriter.close();
			}
			if(cacheOutputStream!=null){
				cacheOutputStream.flush();
				cacheOutputStream.close();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		try {

			if(buffStreamNav!=null) buffStreamNav.close();
			if(inStreamNav!=null) inStreamNav.close();
			if(streamNav!=null) streamNav.close();


		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	/**
	 *
	 */
	public int parseHeaderNav() {

		
		ggto = new BroadcastGGTO();
		
		//Navigation.iono = new double[8];
		String sub;
		int ver = 0;

		try {

			while (buffStreamNav.ready()) {

				try {
					String line = buffStreamNav.readLine();
					if(cacheStreamWriter!=null){
						cacheStreamWriter.write(line);
						cacheStreamWriter.write(newline);
					}

					String typeField = line.substring(60, line.length());
					typeField = typeField.trim();

					if (typeField.equals("RINEX VERSION / TYPE")) {

						if (!line.substring(20, 21).equals("N")) {

							// Error if navigation file identifier was not found
							System.err.println("Navigation file identifier is missing in file " + fileNav.toString() + " header");
							return ver = 0;

						} else if (line.substring(5, 7).equals("3.")){

							//							System.out.println("Ver. 3.01");
							ver = 3;

						} else if (line.substring(5, 9).equals("2.12")){

							//							System.out.println("Ver. 2.12");
							ver = 212;

						} else {

							//							System.out.println("Ver. 2.x");
							ver = 2;
						}

					}

					switch (ver){ 	
					/* RINEX ver. 2.x */
					case 2:

						if (typeField.equals("IONOSPHERIC CORR")) {

							float a[] = new float[4];
							sub = line.substring(3, 17).replace('D', 'e');
							//Navigation.iono[0] = Double.parseDouble(sub.trim());
							a[0] = Float.parseFloat(sub.trim());

							sub = line.substring(19, 29).replace('D', 'e');
							//Navigation.iono[1] = Double.parseDouble(sub.trim());
							a[1] = Float.parseFloat(sub.trim());

							sub = line.substring(30, 41).replace('D', 'e');
							//Navigation.iono[2] = Double.parseDouble(sub.trim());
							a[2] = Float.parseFloat(sub.trim());

							sub = line.substring(43, 53).replace('D', 'e');
							//Navigation.iono[3] = Double.parseDouble(sub.trim());
							a[3] = Float.parseFloat(sub.trim());

							if(iono==null) iono = new IonoGalileo();
							iono.setAlpha(a);

						} else if (typeField.equals("ION BETA")) {

							float b[] = new float[4];

							sub = line.substring(3, 14).replace('D', 'e');
							//Navigation.iono[4] = Double.parseDouble(sub.trim());
							//setIono(4, Double.parseDouble(sub.trim()));
							b[0] = Float.parseFloat(sub.trim());


							sub = line.substring(15, 26).replace('D', 'e');
							//Navigation.iono[5] = Double.parseDouble(sub.trim());
							//setIono(5, Double.parseDouble(sub.trim()));
							b[1] = Float.parseFloat(sub.trim());

							sub = line.substring(27, 38).replace('D', 'e');
							//Navigation.iono[6] = Double.parseDouble(sub.trim());
							//setIono(6, Double.parseDouble(sub.trim()));
							b[2] = Float.parseFloat(sub.trim());

							sub = line.substring(39, 50).replace('D', 'e');
							//Navigation.iono[7] = Double.parseDouble(sub.trim());
							//setIono(7, Double.parseDouble(sub.trim()));
							b[3] = Float.parseFloat(sub.trim());

							if(iono==null) iono = new IonoGalileo();
							iono.setBeta(b);

						} else if (typeField.equals("DELTA-UTC: A0,A1,T,W")) {

							if(iono==null) iono = new IonoGalileo();

							sub = line.substring(3, 22).replace('D', 'e');
							//setA0(Double.parseDouble(sub.trim()));
							iono.setUtcA0(Double.parseDouble(sub.trim()));

							sub = line.substring(22, 41).replace('D', 'e');
							//setA1(Double.parseDouble(sub.trim()));
							iono.setUtcA1(Double.parseDouble(sub.trim()));

							sub = line.substring(41, 50).replace('D', 'e');
							//setT(Integer.parseInt(sub.trim()));
							// TODO need check
							iono.setUtcWNT(Integer.parseInt(sub.trim()));

							sub = line.substring(50, 59).replace('D', 'e');
							//setW(Integer.parseInt(sub.trim()));
							// TODO need check
							iono.setUtcTOW(Integer.parseInt(sub.trim()));

						} else if (typeField.equals("LEAP SECONDS")) {
							if(iono==null) iono = new IonoGalileo();
							sub = line.substring(0, 6).trim().replace('D', 'e');
							//setLeaps(Integer.parseInt(sub.trim()));
							// TODO need check
							iono.setUtcLS(Integer.parseInt(sub.trim()));

						} else if (typeField.equals("END OF HEADER")) {	
							return ver;
						}
						break;


						/* RINEX ver. 2.12 */
					case 212:

						//							System.out.println("Ver. 2.12");

						String typeField2 = line.substring(0, 4);
						typeField2 = typeField2.trim();

						if (typeField2.equals("GAL")) {

							float a[] = new float[4];
							sub = line.substring(6, 17).replace('D', 'e');
							//Navigation.iono[0] = Double.parseDouble(sub.trim());
							a[0] = Float.parseFloat(sub.trim());

							sub = line.substring(18, 29).replace('D', 'e');
							//Navigation.iono[1] = Double.parseDouble(sub.trim());
							a[1] = Float.parseFloat(sub.trim());

							sub = line.substring(30, 41).replace('D', 'e');
							//Navigation.iono[2] = Double.parseDouble(sub.trim());
							a[2] = Float.parseFloat(sub.trim());

							sub = line.substring(42, 53).replace('D', 'e');
							//Navigation.iono[3] = Double.parseDouble(sub.trim());
							a[3] = Float.parseFloat(sub.trim());

							if(iono==null) iono = new IonoGalileo();
							iono.setAlpha(a);

						} else if (typeField2.equals("GPGA")) {


							sub = line.substring(5, 22).replace('D', 'e');
							ggto.setGgtoA0G(Double.parseDouble(sub.trim()));


							sub = line.substring(22, 38).replace('D', 'e');
							ggto.setGgtoA1G(Double.parseDouble(sub.trim()));
							
							sub = line.substring(38, 45).replace('D', 'e');
							ggto.setGgtoT0G(Double.parseDouble(sub.trim()));


							sub = line.substring(45, 51).replace('D', 'e');
							ggto.setGgtoWN0G(Double.parseDouble(sub.trim()));


						} else if (typeField.equals("END OF HEADER")) {	
							return ver;
						}
						break;

						/* RINEX ver. 3.01 */
					case 3: 

						String typeField3 = line.substring(0, 4);
						typeField3 = typeField3.trim();

						//						String typeField3 = line.substring(60, line.length());
						//						typeField3 = typeField3.trim();

						//						System.out.println(typeField2);


						if (typeField3.equals("GAL")) {

							//							System.out.println("GPSA");

							float a[] = new float[4];
							sub = line.substring(6, 17).replace('D', 'e');
							//Navigation.iono[0] = Double.parseDouble(sub.trim());
							a[0] = Float.parseFloat(sub.trim());

							sub = line.substring(18, 29).replace('D', 'e');
							//Navigation.iono[1] = Double.parseDouble(sub.trim());
							a[1] = Float.parseFloat(sub.trim());

							sub = line.substring(30, 41).replace('D', 'e');
							//Navigation.iono[2] = Double.parseDouble(sub.trim());
							a[2] = Float.parseFloat(sub.trim());

							sub = line.substring(42, 53).replace('D', 'e');
							//Navigation.iono[3] = Double.parseDouble(sub.trim());
							a[3] = Float.parseFloat(sub.trim());

							if(iono==null) iono = new IonoGalileo();
							iono.setAlpha(a);


						} else if (typeField3.equals("GPGA")) {

							sub = line.substring(5, 22).replace('D', 'e');
							ggto.setGgtoA0G(Double.parseDouble(sub.trim()));


							sub = line.substring(22, 38).replace('D', 'e');
							ggto.setGgtoA1G(Double.parseDouble(sub.trim()));
							
							sub = line.substring(38, 45).replace('D', 'e');
							ggto.setGgtoT0G(Double.parseDouble(sub.trim()));


							sub = line.substring(45, 51).replace('D', 'e');
							ggto.setGgtoWN0G(Double.parseDouble(sub.trim()));

						} else if (typeField.equals("END OF HEADER")) {	
							//							System.out.println("END OF HEADER");

							return ver;
						}


						break;
					}  // End of Switch 

				} catch (StringIndexOutOfBoundsException e) {
					// Skip over blank lines
				}
			}

			// Display an error if END OF HEADER was not reached
			System.err.println("END OF HEADER was not found in file "
					+ fileNav.toString());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Read all navigation data
	 */
	public void parseDataNavV2() {
		try {

			// Resizable array
			//Navigation.eph = new ArrayList<EphGalileo>();

			//			int j = 0;

			EphGalileo eph = null;

			while (buffStreamNav.ready()) {

				String sub;
				char satType = 'E'; 				

				eph = new EphGalileo();
				addEph(eph);
				eph.setSatType(satType);

				// read 8 lines
				for (int i = 0; i < 8; i++) {

					String line = buffStreamNav.readLine();
					if(cacheStreamWriter!=null){
						cacheStreamWriter.write(line);
						cacheStreamWriter.write(newline);
					}

					try {

						int len = line.length();

						if (len != 0) {							

							if (i == 0) { // LINE 1

								//Navigation.eph.get(j).refTime = new Time();


								//Navigation.eph.add(eph);
								//								addEph(eph);

								// Get satellite ID
								sub = line.substring(1, 3).trim();
								eph.setSatID(Integer.parseInt(sub));

								// Get and format date and time string
								String dT = line.substring(3, 23);
								dT = dT.replace("  ", " 0").trim();
								dT = dT.concat(".0");
								dT.length();
								
								//dT = "20" + dT;
								//								System.out.println(dT);


								try {
									//Time timeEph = new Time(dT);
									// Convert String to UNIX standard time in
									// milliseconds
									//timeEph.msec = Time.dateStringToTime(dT);
									Time toc = new Time(dT);
									eph.setRefTime(toc);
									eph.setToc(toc.getGpsWeekSec());

									// sets Iono reference time
									if(iono!=null && iono.getRefTime()==null) iono.setRefTime(new Time(dT));

								} catch (ParseException e) {
									System.err.println("Time parsing failed");
								}

								sub = line.substring(23, 42).replace('D', 'e');
								eph.setAf0(Double.parseDouble(sub.trim()));

								sub = line.substring(42, 61).replace('D', 'e');
								eph.setAf1(Double.parseDouble(sub.trim()));

								sub = line.substring(61, len).replace('D', 'e');
								eph.setAf2(Double.parseDouble(sub.trim()));

							} else if (i == 1) { // LINE 2

								sub = line.substring(3, 23).replace('D', 'e');
								double iode = Double.parseDouble(sub.trim());
								// TODO check double -> int conversion ?
								eph.setIode((int) iode);

								sub = line.substring(23, 42).replace('D', 'e');
								eph.setCrs(Double.parseDouble(sub.trim()));

								sub = line.substring(42, 61).replace('D', 'e');
								eph.setDeltaN(Double.parseDouble(sub.trim()));

								sub = line.substring(61, len).replace('D', 'e');
								eph.setM0(Double.parseDouble(sub.trim()));

							} else if (i == 2) { // LINE 3

								sub = line.substring(3, 23).replace('D', 'e');
								eph.setCuc(Double.parseDouble(sub.trim()));

								sub = line.substring(23, 42).replace('D', 'e');
								eph.setE(Double.parseDouble(sub.trim()));

								sub = line.substring(42, 61).replace('D', 'e');
								eph.setCus(Double.parseDouble(sub .trim()));

								sub = line.substring(61, len).replace('D', 'e');
								eph.setRootA(Double.parseDouble(sub.trim()));

							} else if (i == 3) { // LINE 4

								sub = line.substring(3, 23).replace('D', 'e');
								eph.setToe(Double.parseDouble(sub.trim()));

								sub = line.substring(23, 42).replace('D', 'e');
								eph.setCic(Double.parseDouble(sub.trim()));

								sub = line.substring(42, 61).replace('D', 'e');
								eph.setOmega0(Double.parseDouble(sub.trim()));

								sub = line.substring(61, len).replace('D', 'e');
								eph.setCis(Double.parseDouble(sub.trim()));

							} else if (i == 4) { // LINE 5

								sub = line.substring(3, 23).replace('D', 'e');
								eph.setI0(Double.parseDouble(sub.trim()));

								sub = line.substring(23, 42).replace('D', 'e');
								eph.setCrc(Double.parseDouble(sub.trim()));

								sub = line.substring(42, 61).replace('D', 'e');
								eph.setOmega(Double.parseDouble(sub.trim()));

								sub = line.substring(61, len).replace('D', 'e');
								eph.setOmegaDot(Double.parseDouble(sub.trim()));

							} else if (i == 5) { // LINE 6

								sub = line.substring(3, 23).replace('D', 'e');
								eph.setiDot(Double.parseDouble(sub.trim()));

								sub = line.substring(23, 42).replace('D', 'e');
								double L2Code = Double.parseDouble(sub.trim());
								eph.setL2Code((int) L2Code);

								sub = line.substring(42, 61).replace('D', 'e');
								double week = Double.parseDouble(sub.trim());
								eph.setWeek((int) week);

								sub = line.substring(61, len).replace('D', 'e');
								double L2Flag = Double.parseDouble(sub.trim());
								eph.setL2Flag((int) L2Flag);

							} else if (i == 6) { // LINE 7

								sub = line.substring(3, 23).replace('D', 'e');
								double svAccur = Double.parseDouble(sub.trim());
								eph.setSvAccur((int) svAccur);

								sub = line.substring(23, 42).replace('D', 'e');
								double svHealth = Double.parseDouble(sub.trim());
								eph.setSvHealth((int) svHealth);

								sub = line.substring(42, 61).replace('D', 'e');
								eph.setTgd(Double.parseDouble(sub.trim()));

								sub = line.substring(61, len).replace('D', 'e');
								double iodc = Double.parseDouble(sub.trim());
								eph.setIodc((int) iodc);

							} else if (i == 7) { // LINE 8

								sub = line.substring(3, 23).replace('D', 'e');
								eph.setTom(Double.parseDouble(sub.trim()));

								if (len > 23) {
									sub = line.substring(23, 42).replace('D', 'e');
									eph.setFitInt((long) Double.parseDouble(sub.trim()));

								} else {
									eph.setFitInt(0);
								}
							}
						} else {
							i--;
						}
					} catch (NullPointerException e) {
						// Skip over blank lines
					}
				}

				// Increment array index
				//				j++;
				// Store the number of ephemerides
				//Navigation.n = j;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}


	public void parseDataNavV3() {

		try {

			// Resizable array
			//Navigation.eph = new ArrayList<EphGalileo>();

			//			int j = 0;

			EphGalileo eph = null;

			while (buffStreamNav.ready()) {

				String sub;
				char satType;

				satType = (char)buffStreamNav.read();
				if(cacheStreamWriter!=null){
					cacheStreamWriter.write(satType);
				}
				//				System.out.println(s);

				if (satType != 'R' && satType != 'S'){  // other than GLONASS and SBAS data
					//						System.out.println(satType);

					// read 8 lines
					for (int i = 0; i < 8; i++) {

						String line = buffStreamNav.readLine();
						if(cacheStreamWriter!=null){
							cacheStreamWriter.write(line);
							cacheStreamWriter.write(newline);
						}

						try {
							int len = line.length();

							if (len != 0) {
								if (i == 0) { // LINE 1

									//Navigation.eph.get(j).refTime = new Time();

									eph = new EphGalileo();
									//Navigation.eph.add(eph);
									addEph(eph);

									eph.setSatType(satType);

									// Get satellite ID
									sub = line.substring(0, 2).trim();
									//										System.out.println(sub);
									eph.setSatID(Integer.parseInt(sub));

									// Get and format date and time string
									String dT = line.substring(3, 22);
									//								dT = dT.replace("  ", " 0").trim();
									dT = dT + ".0";
									//										System.out.println(dT);

									try {
										//Time timeEph = new Time(dT);
										// Convert String to UNIX standard time in
										// milliseconds
										//timeEph.msec = Time.dateStringToTime(dT);
										Time toc = new Time(dT);
										eph.setRefTime(toc);
										eph.setToc(toc.getGpsWeekSec());

										// sets Iono reference time
										if(iono!=null && iono.getRefTime()==null) iono.setRefTime(new Time(dT));

									} catch (ParseException e) {
										System.err.println("Time parsing failed");
									}

									sub = line.substring(22, 41).replace('D', 'e');
									eph.setAf0(Double.parseDouble(sub.trim()));

									sub = line.substring(41, 60).replace('D', 'e');
									eph.setAf1(Double.parseDouble(sub.trim()));

									sub = line.substring(60, len).replace('D', 'e');
									eph.setAf2(Double.parseDouble(sub.trim()));

								} else if (i == 1) { // LINE 2

									sub = line.substring(4, 23).replace('D', 'e');
									double iode = Double.parseDouble(sub.trim());
									// TODO check double -> int conversion ?
									eph.setIode((int) iode);

									sub = line.substring(23, 42).replace('D', 'e');
									eph.setCrs(Double.parseDouble(sub.trim()));

									sub = line.substring(42, 61).replace('D', 'e');
									eph.setDeltaN(Double.parseDouble(sub.trim()));

									sub = line.substring(61, len).replace('D', 'e');
									eph.setM0(Double.parseDouble(sub.trim()));

								} else if (i == 2) { // LINE 3

									sub = line.substring(4, 23).replace('D', 'e');
									eph.setCuc(Double.parseDouble(sub.trim()));

									sub = line.substring(23, 42).replace('D', 'e');
									eph.setE(Double.parseDouble(sub.trim()));

									sub = line.substring(42, 61).replace('D', 'e');
									eph.setCus(Double.parseDouble(sub .trim()));

									sub = line.substring(61, len).replace('D', 'e');
									eph.setRootA(Double.parseDouble(sub.trim()));

								} else if (i == 3) { // LINE 4

									sub = line.substring(4, 23).replace('D', 'e');
									eph.setToe(Double.parseDouble(sub.trim()));

									sub = line.substring(23, 42).replace('D', 'e');
									eph.setCic(Double.parseDouble(sub.trim()));

									sub = line.substring(42, 61).replace('D', 'e');
									eph.setOmega0(Double.parseDouble(sub.trim()));

									sub = line.substring(61, len).replace('D', 'e');
									eph.setCis(Double.parseDouble(sub.trim()));

								} else if (i == 4) { // LINE 5

									sub = line.substring(4, 23).replace('D', 'e');
									eph.setI0(Double.parseDouble(sub.trim()));

									sub = line.substring(23, 42).replace('D', 'e');
									eph.setCrc(Double.parseDouble(sub.trim()));

									sub = line.substring(42, 61).replace('D', 'e');
									eph.setOmega(Double.parseDouble(sub.trim()));

									sub = line.substring(61, len).replace('D', 'e');
									eph.setOmegaDot(Double.parseDouble(sub.trim()));

								} else if (i == 5) { // LINE 6

									sub = line.substring(4, 23).replace('D', 'e');
									eph.setiDot(Double.parseDouble(sub.trim()));

									sub = line.substring(23, 42).replace('D', 'e');
									double L2Code = Double.parseDouble(sub.trim());
									eph.setL2Code((int) L2Code);

									sub = line.substring(42, 61).replace('D', 'e');
									double week = Double.parseDouble(sub.trim());
									eph.setWeek((int) week);

									sub = line.substring(61, len).replace('D', 'e');
									if (!sub.trim().isEmpty()) {
										double L2Flag = Double.parseDouble(sub.trim());
										eph.setL2Flag((int) L2Flag);
									} else {
										eph.setL2Flag(0);
									}

								} else if (i == 6) { // LINE 7

									sub = line.substring(4, 23).replace('D', 'e');
									double svAccur = Double.parseDouble(sub.trim());
									eph.setSvAccur((int) svAccur);

									sub = line.substring(23, 42).replace('D', 'e');
									double svHealth = Double.parseDouble(sub.trim());
									eph.setSvHealth((int) svHealth);

									sub = line.substring(42, 61).replace('D', 'e');
									eph.setTgd(Double.parseDouble(sub.trim()));

									sub = line.substring(61, len).replace('D', 'e');
									double iodc = Double.parseDouble(sub.trim());
									eph.setIodc((int) iodc);

								} else if (i == 7) { // LINE 8

									sub = line.substring(4, 23).replace('D', 'e');
									eph.setTom(Double.parseDouble(sub.trim()));

									if (line.trim().length() > 22) {
										sub = line.substring(23, 42).replace('D', 'e');
										//eph.setFitInt(Long.parseLong(sub.trim()));
										
										//Added by Sebastian on 15.01.2018
										eph.setFitInt((long) Double.parseDouble(sub.trim()));
										
										
									} else {
										eph.setFitInt(0);
									}
								}
							} else {
								i--;
							}


						} catch (NullPointerException e) {
							// Skip over blank lines
						}



					}  // End of for
				}

			} // End of while

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}


	}

	private double gpsToUnixTime(Time toc, int tow) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param unixTime
	 * @param satID
	 * @return Reference ephemeris set for given time and satellite
	 */
	public EphGalileo findEph(long unixTime, int satID, char satType) {

		long dt = 0;
		long dtMin = 0;
		long dtMax = 0;
		long delta = 0;
		EphGalileo refEph = null;

		//long gpsTime = (new Time(unixTime)).getGpsTime();

		for (int i = 0; i < eph.size(); i++) {
			// Find ephemeris sets for given satellite
			if (eph.get(i).getSatID() == satID && eph.get(i).getSatType() == satType) {
				// Consider BeiDou time (BDT) for BeiDou satellites (14 sec difference wrt GPS time)
				if (satType == 'C') {
					delta = 14000;
					unixTime = unixTime - delta;
				}
				// Compare current time and ephemeris reference time
				dt = Math.abs(eph.get(i).getRefTime().getMsec() - unixTime /*getGpsTime() - gpsTime*/)/1000;
				// If it's the first round, set the minimum time difference and
				// select the first ephemeris set candidate; if the current ephemeris set
				// is closer in time than the previous candidate, select new candidate
				if (refEph == null || dt < dtMin) {
					dtMin = dt;
					refEph = eph.get(i);
				}
			}
		}

    if( refEph == null )
      return null;

		if( refEph.getSvHealth() != 0) {
		  return EphGalileo.UnhealthyEph;
		}

		//maximum allowed interval from ephemeris reference time
		long fitInterval = refEph.getFitInt();
		
		if (fitInterval != 0) {
			dtMax = fitInterval*3600/2; 
		} else {
			switch (refEph.getSatType()) {
  			case 'R': dtMax = 950;
  			case 'J': dtMax = 3600;
  			default: dtMax = 7200;
			}
		}
		if(dtMin > dtMax) {
			refEph = null;
		}

		return refEph;
	}

	public int getEphSize(){
		return eph.size();
	}

	public void addEph(EphGalileo eph){
		this.eph.add(eph);
	}

	//	public void setIono(int i, double val){
	//		this.iono[i] = val;
	//	}
	public IonoGps getIono(long unixTime, Location initialLocation){
		return null;
	}

	@Override
	public IonoGalileo getIonoNeQuick(long unixTime, Location initialLocation) {
		return iono;
	}
	//	/**
	//	 * @return the a0
	//	 */
	//	public double getA0() {
	//		return A0;
	//	}
	//	/**
	//	 * @param a0 the a0 to set
	//	 */
	//	public void setA0(double a0) {
	//		A0 = a0;
	//	}
	//	/**
	//	 * @return the a1
	//	 */
	//	public double getA1() {
	//		return A1;
	//	}
	//	/**
	//	 * @param a1 the a1 to set
	//	 */
	//	public void setA1(double a1) {
	//		A1 = a1;
	//	}
	//	/**
	//	 * @return the t
	//	 */
	//	public double getT() {
	//		return T;
	//	}
	//	/**
	//	 * @param t the t to set
	//	 */
	//	public void setT(double t) {
	//		T = t;
	//	}
	//	/**
	//	 * @return the w
	//	 */
	//	public double getW() {
	//		return W;
	//	}
	//	/**
	//	 * @param w the w to set
	//	 */
	//	public void setW(double w) {
	//		W = w;
	//	}
	//	/**
	//	 * @return the leaps
	//	 */
	//	public int getLeaps() {
	//		return leaps;
	//	}
	//	/**
	//	 * @param leaps the leaps to set
	//	 */
	//	public void setLeaps(int leaps) {
	//		this.leaps = leaps;
	//	}

	public boolean isTimestampInEpocsRange(long unixTime){
		return eph.size()>0/* &&
				eph.get(0).getRefTime().getMsec() <= unixTime /*&&
		unixTime <= eph.get(eph.size()-1).getRefTime().getMsec() missing interval +epochInterval*/;
	}


	/* (non-Javadoc)
	 * @see org.gogpsproject.NavigationProducer#getGpsSatPosition(long, int, double)
	 */
	@Override
	public SatellitePosition getGpsSatPosition(Observations obs, int satID, char satType, double receiverClockError) {
		long unixTime = obs.getRefTime().getMsec();
		double range = obs.getSatByIDType(satID, satType).getPseudorange(0);

		if( range == 0 )
			return null;

		EphGalileo eph = findEph(unixTime, satID, satType);
		if( eph.equals( EphGalileo.UnhealthyEph ))
			return SatellitePosition.UnhealthySat;

		if (eph != null) {

			//			char satType = eph.getSatType();

			//SatellitePosition sp = computePositionGalileo(obs, satID, satType, eph, receiverClockError);
			//			SatellitePosition sp = computePositionGps(unixTime, satType, satID, eph, range, receiverClockError);
			//if(receiverPosition!=null) earthRotationCorrection(receiverPosition, sp);
			//return sp;// new SatellitePosition(eph, unixTime, satID, range);
		}
		return null;
	}
	
	
	public SatellitePosition getGalileoSatPosition(long unixTime,double range, int satID, char satType, double receiverClockError) {
		//long unixTime = obs.getRefTime().getMsec();
		//double range = obs.getSatByIDType(satID, satType).getPseudorange(0);
		
		if( range == 0 )
			return null;
					
		EphGalileo eph = findEph(unixTime, satID, satType);
		//if( eph.equals( EphGalileo.UnhealthyEph ))
			//return SatellitePosition.UnhealthySat;

		if (eph == null){
            Log.e(TAG, "getGalileoSatPosition: Ephemeris failed to load..." );
            return null;
        }

        //			char satType = eph.getSatType();

        SatellitePosition sp = computePositionGalileo(unixTime, range, satID, satType, eph, receiverClockError);
        //			SatellitePosition sp = computePositionGps(unixTime, satType, satID, eph, range, receiverClockError);
        //if(receiverPosition!=null) earthRotationCorrection(receiverPosition, sp);

        return sp;// new SatellitePosition(eph, unixTime, satID, range);

	}
	
	public SatellitePosition getGalileoSatVelocities(long unixTime,double range, int satID, char satType, double receiverClockError) {
		//long unixTime = obs.getRefTime().getMsec();
		//double range = obs.getSatByIDType(satID, satType).getPseudorange(0);
		
		if( range == 0 )
			return null;
					
		EphGalileo eph = findEph(unixTime, satID, satType);
		if( eph.equals( EphGalileo.UnhealthyEph ))
			return SatellitePosition.UnhealthySat;
		
		if (eph != null) {

			SatellitePosition sv = computePositionSpeedGalileo(unixTime, range, satID, satType, eph, receiverClockError);
	
			
			return sv;
		}
		return null;
	}


	
	

  public String getFileName() {
    if( fileNav == null )
      return null;
    else
      return fileNav.getName();
  }
}
