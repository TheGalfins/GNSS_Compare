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

import android.location.cts.nano.Ephemeris;
import android.location.cts.nano.GalileoEphemeris;

import com.galfins.gogpsextracts.Streamable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * The Class IonoGps.
 *
 * @author Lorenzo Patocchi
 */
public class IonoGps implements Streamable {

	private final static int STREAM_V = 1;


	/** Bitmask, every bit represenst a GPS SV (1-32). If the bit is set the SV is healthy. */
	private long health = 0;

	/** UTC - parameter A1. */
	private double utcA1;

	/** UTC - parameter A0. */
	private double utcA0;

	/** UTC - reference time of week. */
	private long utcTOW;

	/** UTC - reference week number. */
	private int utcWNT;

	/** UTC - time difference due to leap seconds before event. */
	private int utcLS;

	/** UTC - week number when next leap second event occurs. */
	private int utcWNF;

	/** UTC - day of week when next leap second event occurs. */
	private int utcDN;

	/** UTC - time difference due to leap seconds after event. */
	private int utcLSF;

	/** Klobuchar - alpha. */
	private float alpha[] = new float[4];

	/** Klobuchar - beta. */
	private float beta[] = new float[4];

	/** Healthmask field in this message is valid. */
	private boolean validHealth;

	/** UTC parameter fields in this message are valid. */
	private boolean validUTC;

	/** Klobuchar parameter fields in this message are valid. */
	private boolean validKlobuchar;

	/** Reference time. */
	private Time refTime;

	public IonoGps(){

	}
	public IonoGps(DataInputStream dai, boolean oldVersion) throws IOException{
		read(dai,oldVersion);
	}
	public IonoGps(Ephemeris.IonosphericModelProto iono, Ephemeris.UtcModelProto utcModel){
	    this.alpha[0] = (float) iono.alpha[0];
        this.alpha[1] = (float) iono.alpha[1];
        this.alpha[2] = (float) iono.alpha[2];
        this.alpha[3] = (float) iono.alpha[3];

        this.beta[0] = (float) iono.beta[0];
        this.beta[1] = (float) iono.beta[1];
        this.beta[2] = (float) iono.beta[2];
        this.beta[3] = (float) iono.beta[3];

        if (utcModel!=null) {
            this.utcA1 = utcModel.a1;
            this.utcA0 = utcModel.a0;
            this.utcTOW = utcModel.tow;
        }

        //health =
        //utcWNT =
        //utcLS =
        //utcWNF =
        //utcDN =
        //utcLSF =
        //validHealth =
        //validUTC =
        //validKlobuchar =
	}

	public IonoGps(GalileoEphemeris.NeQuickModelProto iono, GalileoEphemeris.UtcModelProto utcModel) {
		this.alpha[0] = (float) iono.alpha[0];
		this.alpha[1] = (float) iono.alpha[1];
		this.alpha[2] = (float) iono.alpha[2];

		if (utcModel!=null) {
			this.utcA1 = utcModel.a1;
			this.utcA0 = utcModel.a0;
			this.utcTOW = utcModel.tow;
		}
	}

	/**
	 * Gets the reference time.
	 *
	 * @return the refTime
	 */
	public Time getRefTime() {
		return refTime;
	}

	/**
	 * Sets the reference time.
	 *
	 * @param refTime the refTime to set
	 */
	public void setRefTime(Time refTime) {
		this.refTime = refTime;
	}

	/**
	 * Instantiates a new iono gps.
	 */
	public IonoGps(Time refTime) {
		this.refTime = refTime;
	}

	/**
	 * Gets the bitmask, every bit represenst a GPS SV (1-32).
	 *
	 * @return the health
	 */
	public long getHealth() {
		return health;
	}

	/**
	 * Sets the bitmask, every bit represenst a GPS SV (1-32).
	 *
	 * @param health the health to set
	 */
	public void setHealth(long health) {
		this.health = health;
	}

	/**
	 * Gets the UTC - parameter A1.
	 *
	 * @return the utcA1
	 */
	public double getUtcA1() {
		return utcA1;
	}

	/**
	 * Sets the UTC - parameter A1.
	 *
	 * @param utcA1 the utcA1 to set
	 */
	public void setUtcA1(double utcA1) {
		this.utcA1 = utcA1;
	}

	/**
	 * Gets the UTC - parameter A0.
	 *
	 * @return the utcA0
	 */
	public double getUtcA0() {
		return utcA0;
	}

	/**
	 * Sets the UTC - parameter A0.
	 *
	 * @param utcA0 the utcA0 to set
	 */
	public void setUtcA0(double utcA0) {
		this.utcA0 = utcA0;
	}

	/**
	 * Gets the UTC - reference time of week.
	 *
	 * @return the utcTOW
	 */
	public long getUtcTOW() {
		return utcTOW;
	}

	/**
	 * Sets the UTC - reference time of week.
	 *
	 * @param utcTOW the utcTOW to set
	 */
	public void setUtcTOW(long utcTOW) {
		this.utcTOW = utcTOW;
	}

	/**
	 * Gets the UTC - reference week number.
	 *
	 * @return the utcWNT
	 */
	public int getUtcWNT() {
		return utcWNT;
	}

	/**
	 * Sets the UTC - reference week number.
	 *
	 * @param utcWNT the utcWNT to set
	 */
	public void setUtcWNT(int utcWNT) {
		this.utcWNT = utcWNT;
	}

	/**
	 * Gets the UTC - time difference due to leap seconds before event.
	 *
	 * @return the utcLS
	 */
	public int getUtcLS() {
		return utcLS;
	}

	/**
	 * Sets the UTC - time difference due to leap seconds before event.
	 *
	 * @param utcLS the utcLS to set
	 */
	public void setUtcLS(int utcLS) {
		this.utcLS = utcLS;
	}

	/**
	 * Gets the UTC - week number when next leap second event occurs.
	 *
	 * @return the utcWNF
	 */
	public int getUtcWNF() {
		return utcWNF;
	}

	/**
	 * Sets the UTC - week number when next leap second event occurs.
	 *
	 * @param utcWNF the utcWNF to set
	 */
	public void setUtcWNF(int utcWNF) {
		this.utcWNF = utcWNF;
	}

	/**
	 * Gets the UTC - day of week when next leap second event occurs.
	 *
	 * @return the utcDN
	 */
	public int getUtcDN() {
		return utcDN;
	}

	/**
	 * Sets the UTC - day of week when next leap second event occurs.
	 *
	 * @param utcDN the utcDN to set
	 */
	public void setUtcDN(int utcDN) {
		this.utcDN = utcDN;
	}

	/**
	 * Gets the UTC - time difference due to leap seconds after event.
	 *
	 * @return the utcLSF
	 */
	public int getUtcLSF() {
		return utcLSF;
	}

	/**
	 * Sets the UTC - time difference due to leap seconds after event.
	 *
	 * @param utcLSF the utcLSF to set
	 */
	public void setUtcLSF(int utcLSF) {
		this.utcLSF = utcLSF;
	}

	/**
	 * Gets the klobuchar - alpha.
	 *
	 * @param i the i<sup>th<sup> value in the range 0-3
	 * @return the alpha
	 */
	public float getAlpha(int i) {
		return alpha[i];
	}

	/**
	 * Sets the klobuchar - alpha.
	 *
	 * @param alpha the alpha to set
	 */
	public void setAlpha(float[] alpha) {
		this.alpha = alpha;
	}

	/**
	 * Gets the klobuchar - beta.
	 *
	 * @param i the i<sup>th<sup> value in the range 0-3
	 * @return the beta
	 */
	public float getBeta(int i) {
		return beta[i];
	}

	/**
	 * Sets the klobuchar - beta.
	 *
	 * @param beta the beta to set
	 */
	public void setBeta(float[] beta) {
		this.beta = beta;
	}

	/**
	 * Checks if is healthmask field in this message is valid.
	 *
	 * @return the validHealth
	 */
	public boolean isValidHealth() {
		return validHealth;
	}

	/**
	 * Sets the healthmask field in this message is valid.
	 *
	 * @param validHealth the validHealth to set
	 */
	public void setValidHealth(boolean validHealth) {
		this.validHealth = validHealth;
	}

	/**
	 * Checks if is UTC parameter fields in this message are valid.
	 *
	 * @return the validUTC
	 */
	public boolean isValidUTC() {
		return validUTC;
	}

	/**
	 * Sets the UTC parameter fields in this message are valid.
	 *
	 * @param validUTC the validUTC to set
	 */
	public void setValidUTC(boolean validUTC) {
		this.validUTC = validUTC;
	}

	/**
	 * Checks if is klobuchar parameter fields in this message are valid.
	 *
	 * @return the validKlobuchar
	 */
	public boolean isValidKlobuchar() {
		return validKlobuchar;
	}

	/**
	 * Sets the klobuchar parameter fields in this message are valid.
	 *
	 * @param validKlobuchar the validKlobuchar to set
	 */
	public void setValidKlobuchar(boolean validKlobuchar) {
		this.validKlobuchar = validKlobuchar;
	}
	/* (non-Javadoc)
	 * @see org.gogpsproject.Streamable#write(java.io.DataOutputStream)
	 */
	@Override
	public int write(DataOutputStream dos) throws IOException {
		int size=5;
		dos.writeUTF(MESSAGE_IONO); // 5
		dos.writeInt(STREAM_V); size +=4;
		dos.writeLong(health); size +=8;
		dos.writeDouble(utcA1); size +=8;
		dos.writeDouble(utcA0); size +=8;
		dos.writeLong(utcTOW); size +=8;
		dos.writeInt(utcWNT); size +=4;
		dos.writeInt(utcLS); size +=4;
		dos.writeInt(utcWNF); size +=4;
		dos.writeInt(utcDN); size +=4;
		dos.writeInt(utcLSF); size +=4;
		for(int i=0;i<alpha.length;i++){
			dos.writeFloat(alpha[i]); size +=4;
		}
		for(int i=0;i<beta.length;i++){
			dos.writeFloat(beta[i]); size +=4;
		}
		dos.writeBoolean(validHealth);  size +=1;
		dos.writeBoolean(validUTC); size +=1;
		dos.writeBoolean(validKlobuchar); size +=1;
		dos.writeLong(refTime==null?-1:refTime.getMsec());  size +=8;

		return size;
	}
	/* (non-Javadoc)
	 * @see org.gogpsproject.Streamable#read(java.io.DataInputStream)
	 */
	@Override
	public void read(DataInputStream dai, boolean oldVersion) throws IOException {
		int v=1;
		if(!oldVersion) v=dai.readInt();

		if(v==1){

			health = dai.readLong();
			utcA1 = dai.readDouble();
			utcA0 = dai.readDouble();
			utcTOW = dai.readLong();
			utcWNT = dai.readInt();
			utcLS = dai.readInt();
			utcWNF = dai.readInt();
			utcDN = dai.readInt();
			utcLSF = dai.readInt();
			for(int i=0;i<alpha.length;i++){
				alpha[i] = dai.readFloat();
			}
			for(int i=0;i<beta.length;i++){
				beta[i] = dai.readFloat();
			}
			validHealth = dai.readBoolean();
			validUTC = dai.readBoolean();
			validKlobuchar = dai.readBoolean();
			long l = dai.readLong();
			refTime = new Time(l>0?l:System.currentTimeMillis());
		}else{
			throw new IOException("Unknown format version:"+v);
		}
	}


}
