/*
 * Copyright (c) 2010, Eugenio Realini, Mirko Reguzzoni, Cryms sagl - Switzerland. All Rights Reserved.
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
 *
 */
package com.galfins.gogpsextracts;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <p>
 * Class for unifying time representations
 * </p>
 *
 * @author Eugenio Realini, Cryms.com
 */
public class Time {
	private long msec; /* time in milliseconds since January 1, 1970 (UNIX standard) */
	private double fraction; /* fraction of millisecond */

	private Date[] leapDates;
	private Calendar gc =  GregorianCalendar.getInstance();
	TimeZone zone = TimeZone.getTimeZone("GMT Time");
	DateFormat df = new SimpleDateFormat("yyyy MM dd HH mm ss.SSS");
	DateFormat logTime = new SimpleDateFormat("HHmmss.SSS");

	void initleapDates() throws ParseException{
		leapDates = new Date[19];
		leapDates[0]  = df.parse("1980 01 06 00 00 00.0");
		leapDates[1]  = df.parse("1981 07 01 00 00 00.0");
		leapDates[2]  = df.parse("1982 07 01 00 00 00.0");
		leapDates[3]  = df.parse("1983 07 01 00 00 00.0");
		leapDates[4]  = df.parse("1985 07 01 00 00 00.0");
		leapDates[5]  = df.parse("1988 01 01 00 00 00.0");
		leapDates[6]  = df.parse("1990 01 01 00 00 00.0");
		leapDates[7]  = df.parse("1991 01 01 00 00 00.0");
		leapDates[8]  = df.parse("1992 07 01 00 00 00.0");
		leapDates[9]  = df.parse("1993 07 01 00 00 00.0");
		leapDates[10] = df.parse("1994 07 01 00 00 00.0");
		leapDates[11] = df.parse("1996 01 01 00 00 00.0");
		leapDates[12] = df.parse("1997 07 01 00 00 00.0");
		leapDates[13] = df.parse("1999 01 01 00 00 00.0");
		leapDates[14] = df.parse("2006 01 01 00 00 00.0");
		leapDates[15] = df.parse("2009 01 01 00 00 00.0");
		leapDates[16] = df.parse("2012 07 01 00 00 00.0");
		leapDates[17] = df.parse("2015 07 01 00 00 00.0");
		leapDates[18] = df.parse("2017 01 01 00 00 00.0");
	}

	public Time(long msec){
		df.setTimeZone(zone);
		gc.setTimeZone(zone);
		this.gc.setTimeInMillis(msec);
		this.msec = msec;
		this.fraction = 0;
	}
	public Time(long msec, double fraction){
		df.setTimeZone(zone);
		gc.setTimeZone(zone);
		this.msec = msec;
		this.gc.setTimeInMillis(msec);
		this.fraction = fraction;
	}
	public Time(String dateStr) throws ParseException{
		df.setTimeZone(zone);
		gc.setTimeZone(zone);
		this.msec = dateStringToTime(dateStr);
		this.gc.setTimeInMillis(this.msec);
		this.fraction = 0;
	}
	public Time(int gpsWeek, double weekSec){
		df.setTimeZone(zone);
		gc.setTimeZone(zone);
		double fullTime = (Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY + gpsWeek*Constants.DAYS_IN_WEEK*Constants.SEC_IN_DAY + weekSec) * 1000L;
		this.msec = (long) (fullTime);
		this.fraction = fullTime - this.msec;
		this.gc.setTimeInMillis(this.msec);
	}

	public Time(int week, double weekSec, char satID){
		if (satID == 'E') {
			df.setTimeZone(zone);
			gc.setTimeZone(zone);
			double fullTime = (Constants.UNIX_GST_DAYS_DIFF * Constants.SEC_IN_DAY + week * Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY + weekSec) * 1000L;
			this.msec = (long) (fullTime);
			this.fraction = fullTime - this.msec;
			this.gc.setTimeInMillis(this.msec);
		}
		else {
			df.setTimeZone(zone);
			gc.setTimeZone(zone);
			double fullTime = (Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY + week * Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY + weekSec) * 1000L;
			this.msec = (long) (fullTime);
			this.fraction = fullTime - this.msec;
			this.gc.setTimeInMillis(this.msec);
		}
	}

	/**
	 * @param dateStr
	 * @return
	 * @throws ParseException
	 */
	private long dateStringToTime(String dateStr) throws ParseException {

		long dateTime = 0;

		try {
			Date dateObj = df.parse(dateStr);
			dateTime = dateObj.getTime();
		} catch (ParseException e) {
			throw e;
		}

		return dateTime;
	}

//	/**
//	 * @param time
//	 *            (GPS time in seconds)
//	 * @return UNIX standard time in milliseconds
//	 */
//	private static long gpsToUnixTime(double time, int week) {
//		// Shift from GPS time (January 6, 1980 - sec)
//		// to UNIX time (January 1, 1970 - msec)
//		time = (time + (week * Constants.DAYS_IN_WEEK + Constants.UNIX_GPS_DAYS_DIFF) * Constants.SEC_IN_DAY) * Constants.MILLISEC_IN_SEC;
//
//		return (long)time;
//	}

	/**
	 * @param time
	 *            (GPS time in seconds)
	 * @return UNIX standard time in milliseconds
	 */
	public static long gpsToUnixTime(double time) {
		// Shift from GPS time (January 6, 1980 - sec)
		// to UNIX time (January 1, 1970 - msec)
		time = (time + Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY ) * Constants.MILLISEC_IN_SEC;


		return (long)time;
	}

	/**
	 * @param time
	 *            (UNIX standard time in milliseconds)
	 * @return GPS time in seconds
	 */
	private static double unixToGpsTime(double time) {
		// Shift from UNIX time (January 1, 1970 - msec)
		// to GPS time (January 6, 1980 - sec)
		time = time / Constants.MILLISEC_IN_SEC - Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY;
		time = time%(Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY);
		return time;
	}

	public int getGpsWeek(){
		// Shift from UNIX time (January 1, 1970 - msec)
		// to GPS time (January 6, 1980 - sec)
		long time = msec / Constants.MILLISEC_IN_SEC - Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY;
		return (int)(time/(Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY));
	}
	public int getGpsWeekSec(){
		// Shift from UNIX time (January 1, 1970 - msec)
		// to GPS time (January 6, 1980 - sec)
		long time = msec / Constants.MILLISEC_IN_SEC - Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY;
		return (int)(time%(Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY));
	}
	public int getGpsWeekDay(){
		return (int)(getGpsWeekSec()/Constants.SEC_IN_DAY);
	}
	public int getGpsHourInDay(){
		long time = msec / Constants.MILLISEC_IN_SEC - Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY;
		return (int)((time%(Constants.SEC_IN_DAY))/Constants.SEC_IN_HOUR);
	}
	public int getYear(){
		return gc.get(Calendar.YEAR);
	}
	public int getYear2c(){
		return gc.get(Calendar.YEAR)-2000;
	}
	public int getDayOfYear(){
		return gc.get(Calendar.DAY_OF_YEAR);
	}
	public String getHourOfDayLetter(){
		char c = (char)('a'+getGpsHourInDay());
		return ""+c;
	}

	/*
	 * Locating IGS data, products, and format definitions	Key to directory and file name variables
	 * d	day of week (0-6)
	 * ssss	4-character IGS site ID or 4-character LEO ID
	 * yyyy	4-digit year
	 * yy	2-digit year
	 * wwww	4-digit GPS week
	 * ww	2-digit week of year(01-53)
	 * ddd	day of year (1-366)
	 * hh	2-digit hour of day (00-23)
	 * h	single letter for hour of day (a-x = 0-23)
	 * mm	minutes within hour
	 *
	 */
	public String formatTemplate(String template){
		String tmpl = template.replaceAll("\\$\\{wwww\\}", (new DecimalFormat("0000")).format(this.getGpsWeek()));
		tmpl = tmpl.replaceAll("\\$\\{d\\}", (new DecimalFormat("0")).format(this.getGpsWeekDay()));
		tmpl = tmpl.replaceAll("\\$\\{ddd\\}", (new DecimalFormat("000")).format(this.getDayOfYear()));
		tmpl = tmpl.replaceAll("\\$\\{yy\\}", (new DecimalFormat("00")).format(this.getYear2c()));
		tmpl = tmpl.replaceAll("\\$\\{yyyy\\}", (new DecimalFormat("0000")).format(this.getYear()));
		int hh4 = this.getGpsHourInDay();
		tmpl = tmpl.replaceAll("\\$\\{hh\\}", (new DecimalFormat("00")).format(hh4));
		if(0<=hh4&&hh4<6) hh4=0;
		if(6<=hh4&&hh4<12) hh4=6;
		if(12<=hh4&&hh4<18) hh4=12;
		if(18<=hh4&&hh4<24) hh4=18;
		tmpl = tmpl.replaceAll("\\$\\{hh4\\}", (new DecimalFormat("00")).format(hh4));
		tmpl = tmpl.replaceAll("\\$\\{h\\}", this.getHourOfDayLetter());
		return tmpl;
	}

	public double getGpsTime(){
		return unixToGpsTime(msec);
	}

	public double getRoundedGpsTime(){
		double tow = unixToGpsTime((msec+499)/1000*1000);
		return tow;
	}

	public int getLeapSeconds(){
		if( leapDates == null )
			try {
				initleapDates();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		int leapSeconds = leapDates.length - 1;
		double delta;
		for (int d = 0; d < leapDates.length; d++) {
			delta = leapDates[d].getTime() - msec;
			if (delta > 0) {
				leapSeconds = d - 1;
				break;
			}
		}
		return leapSeconds;
	}

	//
	//	private static double unixToGpsTime(double time) {
	//		// Shift from UNIX time (January 1, 1970 - msec)
	//		// to GPS time (January 6, 1980 - sec)
	//		time = (long)(time / Constants.MILLISEC_IN_SEC) - Constants.UNIX_GPS_DAYS_DIFF * Constants.SEC_IN_DAY;
	//
	//		// Remove integer weeks, to get Time Of Week
	//		double dividend  = time;
	//		double divisor = Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY;
	//		time = dividend  - (divisor * round(dividend / divisor));
	//
	//		//time = Math.IEEEremainder(time, Constants.DAYS_IN_WEEK * Constants.SEC_IN_DAY);
	//
	//		return time;
	//	}



	/**
	 * @return the msec
	 */
	public long getMsec() {
		return msec;
	}

	/**
	 * @param msec the msec to set
	 */
	public void setMsec(long msec) {
		this.msec = msec;
	}

	/**
	 * @return the fraction
	 */
	public double getFraction() {
		return fraction;
	}

	/**
	 * @param fraction the fraction to set
	 */
	public void setFraction(double fraction) {
		this.fraction = fraction;
	}

	public Object clone(){
		return new Time(this.msec,this.fraction);
	}

	public String toString(){
		return df.format(gc.getTime())+" "+gc.getTime();
	}

	public String toLogString(){
		return logTime.format(gc.getTime());
	}

	public int getMonth() {

		return df.getCalendar().get(Calendar.MONTH);


	}

	public int getHourUTC() {

		return df.getCalendar().get(Calendar.HOUR_OF_DAY);


	}
}
